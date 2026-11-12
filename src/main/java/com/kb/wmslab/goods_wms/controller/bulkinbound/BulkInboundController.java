package com.kb.wmslab.goods_wms.controller.bulkinbound;

import com.kb.wmslab.goods_wms.controller.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.Semaphore;

@RestController
@RequestMapping("/inbounds/bulk")
@RequiredArgsConstructor
public class BulkInboundController {

    private final JobLauncher asyncBulkJobLauncher;
    private final JobExplorer jobExplorer;
    private final Job bulkInboundJob;
    private final Semaphore bulkInboundSemaphore;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BulkInboundUploadResponse>> uploadBulkInbound(
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "writerType", defaultValue = "jpa") String writerType,
            @RequestParam(name = "chunkSize", defaultValue = "1000") Integer chunkSize,
            @RequestParam(name = "threadCount", defaultValue = "1") Integer threadCount) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드된 CSV 파일이 비어있습니다.");
        }
        if (!bulkInboundSemaphore.tryAcquire()) {
            throw new BulkInboundBusyException();
        }

        boolean submitted = false;
        Path tempFile = null;
        JobExecution execution;
        try {
            tempFile = Files.createTempFile("bulk-inbound-", ".csv");
            file.transferTo(tempFile);

            JobParameters params = new JobParametersBuilder()
                    .addString("filePath", tempFile.toAbsolutePath().toString())
                    .addString("writerType", writerType)
                    .addLong("chunkSize", (long) chunkSize)
                    .addLong("threadCount", (long) threadCount)
                    .addString("requestedAt", LocalDateTime.now().toString())
                    .toJobParameters();

            execution = asyncBulkJobLauncher.run(bulkInboundJob, params);

            if (execution.getStatus() == BatchStatus.FAILED && execution.getStartTime() == null) {
                // 풀 거절 — TaskExecutorJobLauncher가 TaskRejectedException을 내부에서 삼키고 FAILED로 마킹한 경로
                bulkInboundSemaphore.release();
                throw new BulkInboundBusyException();
            }

            submitted = true;
        } catch (BulkInboundBusyException e) {
            // 풀 거절 분기만 이 예외. 이미 release됨.
            throw e;
        } catch (Exception e) {
            // 파일 저장 예외, run() 예외(체크드 포함) — 워커 진입 실패 경로. submitted=true 뒤에는 이 catch로 떨어질 일이 없다.
            if (!submitted) {
                bulkInboundSemaphore.release();
            }
            throw e;
        }

        // 정상 접수된 상태 — release는 TaskDecorator가 담당. 컨트롤러는 이 지점부터 절대 release하지 않는다.
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(
                        BulkInboundUploadResponse.of(
                                execution, writerType, chunkSize, threadCount,
                                tempFile.toAbsolutePath().toString()),
                        "대량 입고 처리가 시작되었습니다."
                ));
    }

    @GetMapping("/jobs/{executionId}")
    public ResponseEntity<ApiResponse<BulkInboundJobStatusResponse>> getJobExecution(@PathVariable Long executionId) {
        JobExecution execution = jobExplorer.getJobExecution(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("해당 executionId의 Job 실행 정보를 찾을 수 없습니다: " + executionId);
        }
        return ResponseEntity.ok(ApiResponse.ok(BulkInboundJobStatusResponse.from(execution)));
    }
}
