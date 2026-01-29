package com.example.EcoGo.security.password;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 多算法密码编码器
 * 
 * <p>支持多种密码加密算法，可以根据配置选择使用的算法：
 * <ul>
 *   <li><b>BCrypt</b>：默认算法，适合大多数场景</li>
 *   <li><b>Argon2</b>：最安全的算法，2015年密码哈希竞赛获胜者</li>
 *   <li><b>PBKDF2</b>：NIST推荐算法，适合政府和金融领域</li>
 * </ul>
 * 
 * <p>密码存储格式：
 * <pre>
 * {algorithmId}encodedPassword
 * 例如：{bcrypt}$2a$10$... 或 {argon2}$argon2id$...
 * </pre>
 * 
 * <p>此格式允许系统在不影响现有用户的情况下迁移到新算法。
 * 
 * <p>使用示例：
 * <pre>
 * String encoded = passwordEncoder.encode("password123");
 * boolean matches = passwordEncoder.matches("password123", encoded);
 * </pre>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Component
public class MultiAlgorithmPasswordEncoder implements PasswordEncoder {

    private static final Logger logger = LoggerFactory.getLogger(MultiAlgorithmPasswordEncoder.class);

    private final PasswordEncoder delegatingEncoder;
    private final String currentAlgorithm;

    /**
     * 构造函数
     * 
     * @param algorithm 当前使用的算法（bcrypt, argon2, pbkdf2）
     */
    public MultiAlgorithmPasswordEncoder(@Value("${spring.security.password.algorithm:bcrypt}") String algorithm) {
        this.currentAlgorithm = algorithm.toLowerCase();

        // 创建所有支持的编码器
        Map<String, PasswordEncoder> encoders = new HashMap<>();

        // BCrypt - 最常用的算法
        encoders.put("bcrypt", new BCryptPasswordEncoder(10));

        // Argon2 - 最安全的算法（需要更多内存和CPU）
        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());

        // PBKDF2 - NIST推荐算法
        Pbkdf2PasswordEncoder pbkdf2Encoder = new Pbkdf2PasswordEncoder(
                "", // secret可以从配置读取
                16, // salt长度
                310000, // 迭代次数（OWASP推荐）
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256
        );
        pbkdf2Encoder.setEncodeHashAsBase64(true);
        encoders.put("pbkdf2", pbkdf2Encoder);

        // 创建委托编码器，使用配置的算法作为默认
        this.delegatingEncoder = new DelegatingPasswordEncoder(this.currentAlgorithm, encoders);

        logger.info("密码编码器初始化完成，当前算法: {}", this.currentAlgorithm);
    }

    /**
     * 编码原始密码
     * 
     * <p>使用当前配置的算法对密码进行加密
     * 
     * @param rawPassword 原始密码
     * @return 加密后的密码（带算法前缀）
     */
    @Override
    public String encode(CharSequence rawPassword) {
        return delegatingEncoder.encode(rawPassword);
    }

    /**
     * 验证密码是否匹配
     * 
     * <p>自动识别密码的加密算法并进行验证
     * 
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return true表示匹配，false表示不匹配
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return delegatingEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 检查密码是否需要重新编码
     * 
     * <p>当密码使用的算法不是当前配置的算法时，返回true
     * 这允许系统逐步迁移到新的加密算法
     * 
     * @param encodedPassword 加密后的密码
     * @return true表示需要重新编码
     */
    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return delegatingEncoder.upgradeEncoding(encodedPassword);
    }

    /**
     * 获取当前使用的算法
     * 
     * @return 算法名称
     */
    public String getCurrentAlgorithm() {
        return currentAlgorithm;
    }

    /**
     * 获取密码使用的算法
     * 
     * @param encodedPassword 加密后的密码
     * @return 算法名称，如果无法识别则返回"unknown"
     */
    public String getEncodingAlgorithm(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return "unknown";
        }

        // 密码格式：{algorithm}hash
        if (encodedPassword.startsWith("{")) {
            int endIndex = encodedPassword.indexOf("}");
            if (endIndex > 0) {
                return encodedPassword.substring(1, endIndex);
            }
        }

        // 没有算法前缀，可能是旧密码
        return "legacy";
    }
}
