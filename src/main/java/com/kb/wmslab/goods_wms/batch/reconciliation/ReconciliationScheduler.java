package com.kb.wmslab.goods_wms.batch.reconciliation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

/**
 * 매일 새벽 3시에 재고 정합성 검증 Job을 자동 실행한다.
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private final JobLauncher jobLauncher;
    private final Job inventoryReconciliationJob;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void runDailyReconciliation() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("triggeredAt", LocalDateTime.now().toString())
                    .addString("trigger", "scheduled")
                    .toJobParameters();
            JobExecution execution = jobLauncher.run(inventoryReconciliationJob, params);
            log.info("[Reconciliation] scheduled run started executionId={} status={}",
                    execution.getId(), execution.getStatus());
        } catch (Exception e) {
            log.error("[Reconciliation] scheduled run failed", e);
        }
    }
}
