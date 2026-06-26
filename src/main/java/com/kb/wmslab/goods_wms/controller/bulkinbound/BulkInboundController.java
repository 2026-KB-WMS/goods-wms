package com.kb.wmslab.goods_wms.controller.bulkinbound;

import com.kb.wmslab.goods_wms.batch.bulkinbound.BulkInboundJobConfig;
import com.kb.wmslab.goods_wms.controller.common.ApiResponse;
import lombok.RequiredArgsConstructor;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/inbounds/bulk")
@RequiredArgsConstructor
public class BulkInboundController {

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job bulkInboundJob;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadBulkInbound(
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "writerType", defaultValue = "jpa") String writerType,
            @RequestParam(name = "chunkSize", defaultValue = "1000") Integer chunkSize,
            @RequestParam(name = "threadCount", defaultValue = "1") Integer threadCount) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드된 CSV 파일이 비어있습니다.");
        }

        Path tempFile = Files.createTempFile("bulk-inbound-", ".csv");
        file.transferTo(tempFile);

        JobParameters params = new JobParametersBuilder()
                .addString("filePath", tempFile.toAbsolutePath().toString())
                .addString("writerType", writerType)
                .addLong("chunkSize", (long) chunkSize)
                .addLong("threadCount", (long) threadCount)
                .addString("requestedAt", LocalDateTime.now().toString())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(bulkInboundJob, params);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(Map.of(
                        "executionId", execution.getId(),
                        "status", execution.getStatus().name(),
                        "writerType", writerType,
                        "chunkSize", chunkSize,
                        "threadCount", threadCount,
                        "filePath", tempFile.toAbsolutePath().toString()
                ), "대량 입고 처리가 시작되었습니다."));
    }

    @GetMapping("/jobs/{executionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJobExecution(@PathVariable Long executionId) {
        JobExecution execution = jobExplorer.getJobExecution(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("해당 executionId의 Job 실행 정보를 찾을 수 없습니다: " + executionId);
        }

        long readCount = execution.getStepExecutions().stream()
                .mapToLong(se -> se.getReadCount()).sum();
        long writeCount = execution.getStepExecutions().stream()
                .mapToLong(se -> se.getWriteCount()).sum();
        long skipCount = execution.getStepExecutions().stream()
                .mapToLong(se -> se.getSkipCount()).sum();
        Long durationMs = null;
        if (execution.getStartTime() != null && execution.getEndTime() != null) {
            durationMs = java.time.Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();
        }

        java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("executionId", execution.getId());
        body.put("status", execution.getStatus().name());
        body.put("exitCode", execution.getExitStatus().getExitCode());
        body.put("startTime", String.valueOf(execution.getStartTime()));
        body.put("endTime", String.valueOf(execution.getEndTime()));
        body.put("durationMs", durationMs);
        body.put("readCount", readCount);
        body.put("writeCount", writeCount);
        body.put("skipCount", skipCount);
        body.put("jobParameters", execution.getJobParameters().getParameters().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        e -> String.valueOf(e.getValue().getValue()))));
        return ResponseEntity.ok(ApiResponse.ok(body));
    }
}
