package com.kb.wmslab.goods_wms.controller.reconciliation;

import org.springframework.batch.core.JobExecution;

public record ReconciliationTriggerResponse(
        Long executionId,
        String status,
        String startTime
) {
    public static ReconciliationTriggerResponse from(JobExecution execution) {
        return new ReconciliationTriggerResponse(
                execution.getId(),
                execution.getStatus().name(),
                String.valueOf(execution.getStartTime())
        );
    }
}
