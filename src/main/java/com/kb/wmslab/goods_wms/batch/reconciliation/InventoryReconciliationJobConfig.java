package com.kb.wmslab.goods_wms.batch.reconciliation;

import com.kb.wmslab.goods_wms.repository.inventory.InventoryJpaEntity;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class InventoryReconciliationJobConfig {

    public static final String JOB_NAME  = "inventoryReconciliationJob";
    public static final String STEP_NAME = "reconciliationStep";

    private static final int CHUNK_SIZE = 1000;
    private static final int PAGE_SIZE  = 1000;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job inventoryReconciliationJob(Step reconciliationStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(reconciliationStep)
                .build();
    }

    @Bean
    public Step reconciliationStep(
            JpaPagingItemReader<InventoryJpaEntity> reconciliationReader,
            ItemProcessor<InventoryJpaEntity, InventoryMismatchJpaEntity> reconciliationProcessor,
            JpaItemWriter<InventoryMismatchJpaEntity> reconciliationWriter) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<InventoryJpaEntity, InventoryMismatchJpaEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(reconciliationReader)
                .processor(reconciliationProcessor)
                .writer(reconciliationWriter)
                .build();
    }

    @Bean
    public JpaPagingItemReader<InventoryJpaEntity> reconciliationReader() {
        return new JpaPagingItemReaderBuilder<InventoryJpaEntity>()
                .name("reconciliationReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT i FROM InventoryJpaEntity i ORDER BY i.id")
                .pageSize(PAGE_SIZE)
                .build();
    }

    @Bean
    public JpaItemWriter<InventoryMismatchJpaEntity> reconciliationWriter() {
        return new JpaItemWriterBuilder<InventoryMismatchJpaEntity>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
