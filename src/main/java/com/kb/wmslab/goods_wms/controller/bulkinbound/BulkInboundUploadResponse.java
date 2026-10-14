package com.kb.wmslab.goods_wms.controller.bulkinbound;

import org.springframework.batch.core.JobExecution;

public record BulkInboundUploadResponse(
        Long executionId,
        String status,
        String writerType,
        int chunkSize,
        int threadCount,
        String filePath
) {
    public static BulkInboundUploadResponse of(JobExecution execution,
                                               String writerType,
                                               int chunkSize,
                                               int threadCount,
                                               String filePath) {
        return new BulkInboundUploadResponse(
                execution.getId(),
                execution.getStatus().name(),
                writerType,
                chunkSize,
                threadCount,
                filePath
        );
    }
}
