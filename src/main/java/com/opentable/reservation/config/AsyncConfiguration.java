package com.opentable.reservation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures a custom thread pool for @Async methods to prevent blocking the main application threads.
 */
@Slf4j
@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

    /**
     * Configures a custom thread pool executor for async event processing.
     * This ensures event listeners don't block the main transaction threads.
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size: number of threads to keep alive
        executor.setCorePoolSize(5);

        // Max pool size: maximum number of threads
        executor.setMaxPoolSize(10);

        // Queue capacity: number of tasks to queue before creating new threads
        executor.setQueueCapacity(100);

        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("async-event-");

        // Graceful shutdown: wait for tasks to complete before shutting down
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("Async executor configured with core pool size: {}, max pool size: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }

    /**
     * Handles uncaught exceptions thrown from async methods.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Uncaught exception in async method: {} with params: {}",
                    method.getName(), params, throwable);
        };
    }
}