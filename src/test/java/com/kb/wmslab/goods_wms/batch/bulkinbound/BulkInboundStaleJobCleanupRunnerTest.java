package com.kb.wmslab.goods_wms.batch.bulkinbound;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BulkInboundStaleJobCleanupRunnerTest {

    @Autowired JobExplorer jobExplorer;
    @Autowired JobRepository jobRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Job bulkInboundJob;

    JobRepositoryTestUtils jobRepoUtils;

    @BeforeEach
    void setUp() {
        jobRepoUtils = new JobRepositoryTestUtils(jobRepository);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM inbound_lines");
        jdbcTemplate.update("DELETE FROM inbounds");
        jobRepoUtils.removeJobExecutions();
    }

    @Test
    void 기동_이전에_생성된_STARTED_실행은_ABANDONED가_되고_이후_생성은_그대로다() throws Exception {
        // stale: Runner 생성 이전에 만들어진 STARTED JobExecution
        JobExecution stale = createStartedJobExecution();
        Thread.sleep(50);

        // Runner 생성 시점 — startupMarker가 "지금"으로 고정됨
        BulkInboundStaleJobCleanupRunner runner =
                new BulkInboundStaleJobCleanupRunner(jobExplorer, jobRepository);

        Thread.sleep(50);
        // fresh: Runner 생성 이후에 만들어진 STARTED JobExecution
        JobExecution fresh = createStartedJobExecution();

        // Runner 실행 — ApplicationRunner.run을 수동 호출
        runner.run(new DefaultApplicationArguments());

        JobExecution reloadedStale = jobExplorer.getJobExecution(stale.getId());
        JobExecution reloadedFresh = jobExplorer.getJobExecution(fresh.getId());

        assertThat(reloadedStale.getStatus())
                .as("startupMarker 이전에 생성된 STARTED 실행은 ABANDONED로 정리돼야 한다")
                .isEqualTo(BatchStatus.ABANDONED);
        assertThat(reloadedStale.getEndTime())
                .as("정리된 실행은 endTime이 세팅돼야 한다")
                .isNotNull();
        assertThat(reloadedStale.getExitStatus().getExitCode())
                .as("exitStatus.exitCode는 status와 맞춘다")
                .isEqualTo("ABANDONED");
        assertThat(reloadedStale.getExitStatus().getExitDescription())
                .contains("Server restart");

        assertThat(reloadedFresh.getStatus())
                .as("startupMarker 이후에 생성된 STARTED 실행은 그대로여야 한다")
                .isEqualTo(BatchStatus.STARTED);
    }

    private JobExecution createStartedJobExecution() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("test-id", UUID.randomUUID().toString())
                .addString("requestedAt", LocalDateTime.now().toString())
                .toJobParameters();
        JobExecution exec = jobRepository.createJobExecution(bulkInboundJob.getName(), params);
        exec.setStartTime(LocalDateTime.now());
        exec.setStatus(BatchStatus.STARTED);
        jobRepository.update(exec);
        return exec;
    }
}
