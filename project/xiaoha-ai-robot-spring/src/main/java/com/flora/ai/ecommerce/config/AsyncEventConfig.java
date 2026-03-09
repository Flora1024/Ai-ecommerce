package com.flora.ai.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync // 开启异步支持
public class AsyncEventConfig {

    @Bean("eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 配置线程池参数
        executor.setMaxPoolSize(10);
        executor.setCorePoolSize(5);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("event-handler-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务结束才关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待中止最大时间
        executor.setAwaitTerminationSeconds(60);
        // 初始化线程池
        executor.initialize();

        return executor;
    }
}
