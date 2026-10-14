package com.kb.wmslab.goods_wms.controller.reconciliation;

import com.kb.wmslab.goods_wms.controller.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/reconciliations")
@RequiredArgsConstructor
public class ReconciliationController {

    private final JobLauncher jobLauncher;
    private final Job inventoryReconciliationJob;

    @PostMapping
    public ResponseEntity<ApiResponse<ReconciliationTriggerResponse>> trigger() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("triggeredAt", LocalDateTime.now().toString())
                .addString("trigger", "manual")
                .toJobParameters();

        JobExecution execution = jobLauncher.run(inventoryReconciliationJob, params);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(
                        ReconciliationTriggerResponse.from(execution),
                        "재고 정합성 검증 배치가 실행되었습니다."
                ));
    }
}
