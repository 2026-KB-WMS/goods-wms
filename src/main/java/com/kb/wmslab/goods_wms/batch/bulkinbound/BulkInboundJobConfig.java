package com.kb.wmslab.goods_wms.batch.bulkinbound;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BulkInboundJobConfig {

    public static final String JOB_NAME = "bulkInboundJob";
    public static final String STEP_NAME = "bulkInboundStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job bulkInboundJob(Step bulkInboundStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(bulkInboundStep)
                .build();
    }

    @Bean
    @JobScope
    public Step bulkInboundStep(
            @Qualifier("bulkInboundReader") ItemStreamReader<BulkInboundLineDto> reader,
            ItemProcessor<BulkInboundLineDto, BulkInboundLineDto> processor,
            @Qualifier("bulkInboundWriter") ItemWriter<BulkInboundLineDto> writer,
            @Qualifier("bulkInboundTaskExecutor") TaskExecutor taskExecutor,
            @Value("#{jobParameters['chunkSize'] ?: 1000}") Integer chunkSize) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<BulkInboundLineDto, BulkInboundLineDto>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .stream(reader)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemWriter<BulkInboundLineDto> bulkInboundWriter(
            @Value("#{jobParameters['writerType'] ?: 'jpa'}") String writerType,
            BulkInboundJpaWriter jpaWriter,
            BulkInboundJdbcWriter jdbcWriter) {
        return "jdbc".equalsIgnoreCase(writerType) ? jdbcWriter : jpaWriter;
    }

    @Bean
    @StepScope
    public ItemStreamReader<BulkInboundLineDto> bulkInboundReader(
            @Value("#{jobParameters['filePath']}") String filePath) {
        FlatFileItemReader<BulkInboundLineDto> delegate = buildFlatFileReader(filePath);

        SynchronizedItemStreamReader<BulkInboundLineDto> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(delegate);
        return reader;
    }

    @Bean
    @JobScope
    public TaskExecutor bulkInboundTaskExecutor(
            @Value("#{jobParameters['threadCount'] ?: 1}") Integer threadCount) {
        if (threadCount == null || threadCount <= 1) {
            return new SyncTaskExecutor();
        }
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("bulk-inbound-");
        executor.setConcurrencyLimit(threadCount);
        return executor;
    }

    private FlatFileItemReader<BulkInboundLineDto> buildFlatFileReader(String filePath) {
        BeanWrapperFieldSetMapper<BulkInboundLineDto> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(BulkInboundLineDto.class);

        return new FlatFileItemReaderBuilder<BulkInboundLineDto>()
                .name("bulkInboundFlatFileReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .delimited()
                .names("warehouseId", "handlerId", "supplierName", "productId",
                        "orderedQuantity", "normalQuantity", "damagedQuantity", "pendingInspectionQuantity")
                .fieldSetMapper(mapper)
                .build();
    }
}
