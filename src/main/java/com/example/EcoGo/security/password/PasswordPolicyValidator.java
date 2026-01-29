package com.example.EcoGo.security.password;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 密码策略验证器
 * 
 * <p>验证密码是否符合安全策略要求：
 * <ul>
 *   <li>最小长度要求</li>
 *   <li>必须包含大写字母</li>
 *   <li>必须包含小写字母</li>
 *   <li>必须包含数字</li>
 *   <li>必须包含特殊字符</li>
 *   <li>不能是常用密码</li>
 *   <li>不能包含用户名或邮箱</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * PasswordValidationResult result = validator.validatePassword("password123", "user@example.com");
 * if (!result.isValid()) {
 *     System.out.println("密码不符合要求：" + result.getErrors());
 * }
 * </pre>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Component
public class PasswordPolicyValidator {

    /**
     * 最小密码长度
     */
    @Value("${spring.security.password.min-length:8}")
    private int minLength;

    /**
     * 是否要求大写字母
     */
    @Value("${spring.security.password.require-uppercase:true}")
    private boolean requireUppercase;

    /**
     * 是否要求小写字母
     */
    @Value("${spring.security.password.require-lowercase:true}")
    private boolean requireLowercase;

    /**
     * 是否要求数字
     */
    @Value("${spring.security.password.require-digit:true}")
    private boolean requireDigit;

    /**
     * 是否要求特殊字符
     */
    @Value("${spring.security.password.require-special-char:true}")
    private boolean requireSpecialChar;

    /**
     * 常用密码黑名单
     * 实际应用中应该从文件或数据库加载更完整的列表
     */
    private static final List<String> COMMON_PASSWORDS = Arrays.asList(
            "password", "123456", "123456789", "12345678", "12345", "1234567",
            "password1", "123123", "1234567890", "000000", "abc123", "qwerty",
            "111111", "admin", "welcome", "monkey", "login", "starwars",
            "dragon", "master", "sunshine", "princess", "football", "shadow"
    );

    /**
     * 特殊字符模式
     */
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    /**
     * 验证密码是否符合策略
     * 
     * @param password 要验证的密码
     * @param username 用户名或邮箱（用于检查密码是否包含用户信息）
     * @return 验证结果
     */
    public PasswordValidationResult validatePassword(String password, String username) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("密码不能为空");
            return new PasswordValidationResult(false, errors);
        }

        // 1. 检查长度
        if (password.length() < minLength) {
            errors.add(String.format("密码长度必须至少为%d个字符", minLength));
        }

        // 2. 检查大写字母
        if (requireUppercase && !password.chars().anyMatch(Character::isUpperCase)) {
            errors.add("密码必须包含至少一个大写字母");
        }

        // 3. 检查小写字母
        if (requireLowercase && !password.chars().anyMatch(Character::isLowerCase)) {
            errors.add("密码必须包含至少一个小写字母");
        }

        // 4. 检查数字
        if (requireDigit && !password.chars().anyMatch(Character::isDigit)) {
            errors.add("密码必须包含至少一个数字");
        }

        // 5. 检查特殊字符
        if (requireSpecialChar && !SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            errors.add("密码必须包含至少一个特殊字符 (!@#$%^&*等)");
        }

        // 6. 检查是否为常用密码
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            errors.add("密码过于简单，请使用更复杂的密码");
        }

        // 7. 检查是否包含用户名或邮箱
        if (username != null && !username.isEmpty()) {
            String lowerPassword = password.toLowerCase();
            String lowerUsername = username.toLowerCase();
            
            // 检查完整用户名
            if (lowerPassword.contains(lowerUsername)) {
                errors.add("密码不能包含用户名");
            }
            
            // 如果是邮箱，检查邮箱前缀
            if (lowerUsername.contains("@")) {
                String emailPrefix = lowerUsername.split("@")[0];
                if (emailPrefix.length() >= 3 && lowerPassword.contains(emailPrefix)) {
                    errors.add("密码不能包含邮箱地址");
                }
            }
        }

        // 8. 检查连续字符
        if (hasConsecutiveChars(password, 3)) {
            errors.add("密码不能包含3个或更多连续的相同字符");
        }

        // 9. 检查键盘序列
        if (containsKeyboardSequence(password)) {
            errors.add("密码不能包含键盘序列（如qwerty, asdfgh）");
        }

        return new PasswordValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 简化的密码强度检查（不抛出异常，仅返回是否有效）
     * 
     * @param password 密码
     * @return true表示有效，false表示无效
     */
    public boolean isPasswordValid(String password) {
        return validatePassword(password, null).isValid();
    }

    /**
     * 检查是否有连续重复字符
     * 
     * @param password 密码
     * @param maxConsecutive 最大连续次数
     * @return true表示有连续字符
     */
    private boolean hasConsecutiveChars(String password, int maxConsecutive) {
        int count = 1;
        char prev = '\0';

        for (char c : password.toCharArray()) {
            if (c == prev) {
                count++;
                if (count >= maxConsecutive) {
                    return true;
                }
            } else {
                count = 1;
                prev = c;
            }
        }

        return false;
    }

    /**
     * 检查是否包含键盘序列
     * 
     * @param password 密码
     * @return true表示包含键盘序列
     */
    private boolean containsKeyboardSequence(String password) {
        String[] sequences = {
                "qwerty", "asdfgh", "zxcvbn", "qazwsx", "123456", "abcdef"
        };

        String lowerPassword = password.toLowerCase();
        for (String seq : sequences) {
            if (lowerPassword.contains(seq)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 密码验证结果
     */
    public static class PasswordValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public PasswordValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        /**
         * 是否有效
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * 获取错误列表
         */
        public List<String> getErrors() {
            return errors;
        }

        /**
         * 获取第一个错误信息
         */
        public String getFirstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }

        /**
         * 获取所有错误信息（用换行符连接）
         */
        public String getAllErrors() {
            return String.join("\n", errors);
        }
    }
}
