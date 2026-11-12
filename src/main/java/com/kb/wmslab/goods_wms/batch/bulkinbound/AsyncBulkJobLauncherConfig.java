package com.kb.wmslab.goods_wms.batch.bulkinbound;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncBulkJobLauncherConfig {

    @Bean
    public Semaphore bulkInboundSemaphore() {
        return new Semaphore(1);
    }

    @Bean(name = "asyncBulkInboundTaskExecutor")
    public ThreadPoolTaskExecutor asyncBulkInboundTaskExecutor(Semaphore bulkInboundSemaphore) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setThreadNamePrefix("bulk-inbound-");
        executor.setTaskDecorator(runnable -> () -> {
            try {
                runnable.run();
            } finally {
                bulkInboundSemaphore.release();
            }
        });
        // initialize() 호출하지 않음 — ThreadPoolTaskExecutor는 InitializingBean이고
        // 컨테이너가 afterPropertiesSet에서 자동 초기화한다.
        return executor;
    }

    @Bean(name = "asyncBulkJobLauncher")
    public JobLauncher asyncBulkJobLauncher(JobRepository jobRepository,
                                            ThreadPoolTaskExecutor asyncBulkInboundTaskExecutor) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(asyncBulkInboundTaskExecutor);
        launcher.afterPropertiesSet();
        return launcher;
    }
}
