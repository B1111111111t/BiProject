package com.yupi.springbootinit.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolExecutorConfig {
    ExecutorService tool = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(2);
    @Bean(name = "thread1")
    public ThreadPoolExecutor threadPoolExecutor() {
        ThreadFactory threadFactory =  new ThreadFactory() {
            private int count = 1;
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程" + count);
                count++;
                return thread;
            }
        };

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2,4,100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4),threadFactory);



        return threadPoolExecutor;
    }
    @Bean(name = "thread2")
    public ThreadPoolExecutor threadPoolExecutor1() {
        ThreadFactory threadFactory1 =  new ThreadFactory() {
            private int count = 1;
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程(1)" + count);
                count++;
                return thread;
            }
        };
        ThreadPoolExecutor threadPoolExecutor1 = new ThreadPoolExecutor(2,4,100,TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4),threadFactory1);

        return threadPoolExecutor1;
    }
}
