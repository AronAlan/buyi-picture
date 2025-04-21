package com.xzc.buyipicturebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 全局跨域配置
 * @author xuzhichao
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public Executor customExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                10, // 核心线程数
                20, // 最大线程数
                60L, // 线程空闲时间
                TimeUnit.SECONDS, // 时间单位
                new LinkedBlockingQueue<>(50), // 工作队列
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
        return executor;
    }
}
