package com.kb.wmslab.goods_wms.batch.bulkinbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class BulkInboundStaleJobCleanupRunner implements ApplicationRunner {

    private static final String JOB_NAME = "bulkInboundJob";

    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;

    // 빈 생성 시각 — 서블릿 ready 이전. 이 시점보다 뒤에 생성된 실행은 정리 대상이 아님.
    private final LocalDateTime startupMarker = LocalDateTime.now();

    @Override
    public void run(ApplicationArguments args) {
        Set<JobExecution> running = jobExplorer.findRunningJobExecutions(JOB_NAME);
        for (JobExecution execution : running) {
            LocalDateTime createdAt = execution.getCreateTime();
            if (createdAt != null && !createdAt.isBefore(startupMarker)) {
                continue;
            }
            log.warn("[stale-cleanup] abandoning bulkInboundJob execution id={} createTime={}",
                    execution.getId(), createdAt);
            execution.upgradeStatus(BatchStatus.ABANDONED);
            execution.setEndTime(LocalDateTime.now());
            execution.setExitStatus(new ExitStatus("ABANDONED", "Server restart"));
            jobRepository.update(execution);
        }
    }
}
