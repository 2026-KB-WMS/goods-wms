package com.kb.wmslab.goods_wms.controller.bulkinbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.wmslab.goods_wms.batch.bulkinbound.BulkInboundProcessor;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@SpringBootTest
@AutoConfigureMockMvc
class AsyncBulkInboundControllerTest {

    private static final String CSV_HEADER =
            "warehouseId,handlerId,supplierName,productId,orderedQuantity,normalQuantity,damagedQuantity,pendingInspectionQuantity";
    private static final String CSV_WITH_ONE_ROW = CSV_HEADER + "\n1,1,공급사A,1,10,10,0,0\n";
    private static final String CSV_HEADER_ONLY = CSV_HEADER + "\n";
    private static final String JOB_NAME = "bulkInboundJob";

    @Autowired MockMvc mockMvc;
    @Autowired JobExplorer jobExplorer;
    @Autowired JobRepository jobRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;
    @MockitoSpyBean BulkInboundProcessor processor;
    @MockitoSpyBean(name = "asyncBulkJobLauncher") JobLauncher asyncBulkJobLauncher;

    CountDownLatch latch;
    JobRepositoryTestUtils jobRepoUtils;

    private final List<ExecutorService> executors = new ArrayList<>();
    private final List<Future<?>> submittedFutures = new ArrayList<>();

    @BeforeEach
    void setUp() {
        latch = new CountDownLatch(1);
        jobRepoUtils = new JobRepositoryTestUtils(jobRepository);
        doAnswer(inv -> {
            latch.await(10, TimeUnit.SECONDS);
            return inv.callRealMethod();
        }).when(processor).process(any());
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // 1) Processor block 해제
        latch.countDown();
        // 2) Job이 running 목록에서 빠질 때까지 대기 (Semaphore 빈에 의존하지 않음)
        waitUntil(() -> jobExplorer.findRunningJobExecutions(JOB_NAME).isEmpty(), 15_000);
        // 3) 테스트에서 띄운 요청 Future가 끝날 때까지 대기 — 예외/타임아웃은 테스트 결과에 이미 반영됨
        for (Future<?> f : submittedFutures) {
            try {
                f.get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // 테스트 메서드에서 이미 assertion됨. 여기선 "끝났다"는 것만 확인.
            }
        }
        // 4) ExecutorService 정리
        for (ExecutorService e : executors) {
            e.shutdownNow();
            e.awaitTermination(2, TimeUnit.SECONDS);
        }
        executors.clear();
        submittedFutures.clear();
        // 5) H2는 FK 참조 받는 테이블 TRUNCATE 불가 → 자식부터 DELETE
        jdbcTemplate.update("DELETE FROM inbound_lines");
        jdbcTemplate.update("DELETE FROM inbounds");
        jobRepoUtils.removeJobExecutions();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void 업로드하면_즉시_202와_executionId를_받고_Job은_아직_실행중이다() throws Exception {
        Future<MvcResult> future = submitUpload(CSV_WITH_ONE_ROW);
        MvcResult result;
        try {
            result = future.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError(
                    "컨트롤러가 2초 안에 응답하지 않았다 — 지금 동기 구조에서는 Job 완료까지 블로킹된다.", e);
        }
        assertThat(result.getResponse().getStatus())
                .as("업로드 응답 코드")
                .isEqualTo(202);
        Long executionId = extractExecutionId(result);
        JobExecution execution = jobExplorer.getJobExecution(executionId);
        assertThat(execution).as("executionId로 JobExecution을 찾을 수 있어야 한다").isNotNull();
        assertThat(execution.getStatus())
                .as("응답 시점에 Job은 아직 실행 중(STARTING/STARTED)이어야 한다")
                .isIn(BatchStatus.STARTING, BatchStatus.STARTED);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void 첫_Job이_블로킹된_상태에서_두번째_업로드는_503과_Retry_After를_받는다() throws Exception {
        // 첫 번째 업로드 — Processor에서 latch로 블로킹됨
        submitUpload(CSV_WITH_ONE_ROW);
        // 첫 Job이 실행 중이 될 때까지 대기
        waitUntil(() -> !jobExplorer.findRunningJobExecutions(JOB_NAME).isEmpty(), 5_000);

        // 두 번째 업로드 — 세마포어가 있다면 즉시 503이 와야 한다
        Future<MvcResult> second = submitUpload(CSV_WITH_ONE_ROW);
        MvcResult result;
        try {
            result = second.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError(
                    "두 번째 업로드가 1초 안에 응답하지 않았다 — 세마포어 거절이 미동작.", e);
        }
        assertThat(result.getResponse().getStatus())
                .as("두 번째 업로드 응답 코드")
                .isEqualTo(503);
        assertThat(result.getResponse().getHeader("Retry-After"))
                .as("Retry-After 헤더")
                .isNotNull();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void 첫_Job이_COMPLETED된_뒤의_업로드는_202를_받는다() throws Exception {
        // Processor를 즉시 통과시켜 Job이 자연스럽게 끝나도록 — CSV는 헤더만이라 Reader가 0개 읽고 종료 (Writer 호출 없음)
        latch.countDown();

        MvcResult first = mockMvc.perform(buildUpload(CSV_HEADER_ONLY)).andReturn();
        assertThat(first.getResponse().getStatus())
                .as("첫 업로드 응답 코드")
                .isEqualTo(202);
        Long firstExecutionId = extractExecutionId(first);

        // Job이 running 목록에서 빠질 때까지 대기(완료/실패/중단 중 하나)
        waitUntil(() -> jobExplorer.findRunningJobExecutions(JOB_NAME).isEmpty(), 10_000);

        assertThat(jobExplorer.getJobExecution(firstExecutionId).getStatus())
                .as("첫 Job은 COMPLETED 상태로 끝나야 한다 (FAILED/STOPPED 허용 안 함)")
                .isEqualTo(BatchStatus.COMPLETED);

        // 두 번째 업로드 — COMPLETED 직후라 비동기 전환 시 permit 반환 지연으로 잠깐 503이 날 수 있다.
        // tryAcquire가 파일 저장보다 먼저라 거절된 POST는 부작용(파일/DB) 없음 → 재시도로 202 확인.
        long deadline = System.currentTimeMillis() + 5_000;
        int lastStatus = -1;
        while (System.currentTimeMillis() < deadline) {
            MvcResult retry = mockMvc.perform(buildUpload(CSV_HEADER_ONLY)).andReturn();
            lastStatus = retry.getResponse().getStatus();
            if (lastStatus == 202) {
                Long secondExecutionId = extractExecutionId(retry);
                assertThat(secondExecutionId)
                        .as("재시도로 202를 받은 업로드의 executionId는 첫 업로드와 달라야 한다")
                        .isNotEqualTo(firstExecutionId);
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError(
                "5초 동안 재시도했으나 202를 받지 못했다. 마지막 응답 코드: " + lastStatus);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void run이_checked_exception을_던지면_permit이_누수되지_않는다() throws Exception {
        // JobLauncher.run은 JobParametersInvalidException 등 체크드 예외를 선언한다.
        // 체크드 예외는 RuntimeException이 아니므로 catch (RuntimeException | IOException) 같은
        // 좁은 catch로는 잡히지 않고 permit이 영구적으로 소진된다. catch (Exception e)로 바뀌어야 release된다.
        doThrow(new JobParametersInvalidException("test-induced"))
                .doCallRealMethod()
                .when(asyncBulkJobLauncher).run(any(), any());

        // 1) 첫 업로드 — 500 (GlobalExceptionHandler.handleException → INTERNAL_ERROR). permit은 catch에서 release돼야 한다.
        MvcResult first = mockMvc.perform(buildUpload(CSV_HEADER_ONLY)).andReturn();
        assertThat(first.getResponse().getStatus())
                .as("checked 예외(run) 발생 시 첫 업로드 응답 코드")
                .isEqualTo(500);

        // 2) 두 번째 업로드 — Processor block 해제하고 정상 진행 → 202.
        //    permit이 release됐으면 tryAcquire 성공해서 202. 누수됐으면 503.
        latch.countDown();
        MvcResult second = mockMvc.perform(buildUpload(CSV_HEADER_ONLY)).andReturn();
        assertThat(second.getResponse().getStatus())
                .as("permit이 release됐다면 두 번째 업로드는 202여야 한다")
                .isEqualTo(202);
    }

    // --- helpers ---

    private Future<MvcResult> submitUpload(String csvContent) {
        ExecutorService e = Executors.newSingleThreadExecutor();
        executors.add(e);
        Future<MvcResult> f = e.submit(() -> mockMvc.perform(buildUpload(csvContent)).andReturn());
        submittedFutures.add(f);
        return f;
    }

    private static MockMultipartHttpServletRequestBuilder buildUpload(String csvContent) {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bulk.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));
        return (MockMultipartHttpServletRequestBuilder) multipart("/inbounds/bulk")
                .file(file)
                .param("writerType", "jpa")
                .param("chunkSize", "1")
                .param("threadCount", "1");
    }

    private Long extractExecutionId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.path("data").path("executionId").asLong();
    }

    private static void waitUntil(BooleanSupplier cond, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!cond.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("waitUntil timed out after " + timeoutMs + "ms");
            }
            Thread.sleep(50);
        }
    }
}
