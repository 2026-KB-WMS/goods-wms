package com.kb.wmslab.goods_wms.controller.bulkinbound;

import org.springframework.batch.core.JobExecution;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

public record BulkInboundJobStatusResponse(
        Long executionId,
        String status,
        String exitCode,
        String startTime,
        String endTime,
        Long durationMs,
        long readCount,
        long writeCount,
        long skipCount,
        Map<String, String> jobParameters
) {
    public static BulkInboundJobStatusResponse from(JobExecution execution) {
        long readCount = execution.getStepExecutions().stream()
                .mapToLong(se -> se.getReadCount()).sum();
        long writeCount = execution.getStepExecutions().stream()
                .mapToLong(se -> se.getWriteCount()).sum();
        long skipCount = execution.getStepExecutions().stream()
                .mapToLong(se -> se.getSkipCount()).sum();
        Long durationMs = null;
        if (execution.getStartTime() != null && execution.getEndTime() != null) {
            durationMs = Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();
        }
        Map<String, String> jobParameters = execution.getJobParameters().getParameters().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.valueOf(e.getValue().getValue())));

        return new BulkInboundJobStatusResponse(
                execution.getId(),
                execution.getStatus().name(),
                execution.getExitStatus().getExitCode(),
                String.valueOf(execution.getStartTime()),
                String.valueOf(execution.getEndTime()),
                durationMs,
                readCount,
                writeCount,
                skipCount,
                jobParameters
        );
    }
}
