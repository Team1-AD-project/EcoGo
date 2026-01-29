package com.example.EcoGo.security.config;

import com.example.EcoGo.security.password.MultiAlgorithmPasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码策略配置
 * 
 * <p>配置密码编码器和密码策略验证器
 * 
 * <p>支持的密码算法：
 * <ul>
 *   <li>BCrypt - 默认算法</li>
 *   <li>Argon2 - 最安全的算法</li>
 *   <li>PBKDF2 - NIST推荐算法</li>
 * </ul>
 * 
 * <p>可以通过配置文件切换算法：
 * <pre>
 * spring.security.password.algorithm=argon2
 * </pre>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Configuration
public class PasswordPolicyConfig {

    /**
     * 配置密码编码器Bean
     * 
     * <p>使用多算法密码编码器，支持算法迁移
     * 
     * @param multiAlgorithmPasswordEncoder 多算法密码编码器
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder(MultiAlgorithmPasswordEncoder multiAlgorithmPasswordEncoder) {
        return multiAlgorithmPasswordEncoder;
    }
}
