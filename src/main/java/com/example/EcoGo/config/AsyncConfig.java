package com.example.EcoGo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 异步任务配置
 * 
 * <p>启用Spring的异步方法支持和定时任务支持
 * 
 * <p>主要用于：
 * <ul>
 *   <li>异步记录审计日志</li>
 *   <li>定时清理过期Token</li>
 *   <li>定时清理过期审计日志</li>
 * </ul>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // Spring默认的异步配置已经足够用于大多数场景
    // 如果需要自定义线程池，可以在这里配置Executor Bean
}
