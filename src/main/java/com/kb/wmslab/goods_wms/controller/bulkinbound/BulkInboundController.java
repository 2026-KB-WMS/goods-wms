package com.kb.wmslab.goods_wms.controller.bulkinbound;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/inbounds/bulk")
@RequiredArgsConstructor
public class BulkInboundController {

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job bulkInboundJob;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BulkInboundUploadResponse>> uploadBulkInbound(
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
