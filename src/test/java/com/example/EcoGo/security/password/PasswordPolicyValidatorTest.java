package com.example.EcoGo.security.password;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码策略验证器单元测试
 */
class PasswordPolicyValidatorTest {

    private PasswordPolicyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordPolicyValidator();
        // 设置测试用的策略参数
        ReflectionTestUtils.setField(validator, "minLength", 8);
        ReflectionTestUtils.setField(validator, "requireUppercase", true);
        ReflectionTestUtils.setField(validator, "requireLowercase", true);
        ReflectionTestUtils.setField(validator, "requireDigit", true);
        ReflectionTestUtils.setField(validator, "requireSpecialChar", true);
    }

    @Test
    void testValidatePassword_WithStrongPassword_ShouldBeValid() {
        // Given
        String strongPassword = "StrongP@ss123";

        // When
        var result = validator.validatePassword(strongPassword, "testuser");

        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidatePassword_TooShort_ShouldReturnError() {
        // Given
        String shortPassword = "Abc@1";

        // When
        var result = validator.validatePassword(shortPassword, "testuser");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getAllErrors().contains("密码长度必须至少为8个字符"));
    }

    @Test
    void testValidatePassword_NoUppercase_ShouldReturnError() {
        // Given
        String password = "lowercase@123";

        // When
        var result = validator.validatePassword(password, "testuser");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getAllErrors().contains("密码必须包含至少一个大写字母"));
    }

    @Test
    void testValidatePassword_NoLowercase_ShouldReturnError() {
        // Given
        String password = "UPPERCASE@123";

        // When
        var result = validator.validatePassword(password, "testuser");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getAllErrors().contains("密码必须包含至少一个小写字母"));
    }

    @Test
    void testValidatePassword_NoDigit_ShouldReturnError() {
        // Given
        String password = "Password@NoDigit";

        // When
        var result = validator.validatePassword(password, "testuser");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getAllErrors().contains("密码必须包含至少一个数字"));
    }

    @Test
    void testValidatePassword_NoSpecialChar_ShouldReturnError() {
        // Given
        String password = "Password123";

        // When
        var result = validator.validatePassword(password, "testuser");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getAllErrors().contains("密码必须包含至少一个特殊字符"));
    }

    @Test
    void testValidatePassword_CommonPassword_ShouldReturnError() {
        // Given
        String commonPassword = "password";

        // When
        var result = validator.validatePassword(commonPassword, "testuser");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getAllErrors().contains("密码过于简单，请使用更复杂的密码"));
    }

    @Test
    void testValidatePassword_ContainsUsername_ShouldReturnError() {
        // Given
        String password = "testuser@123A";

        // When
        var result = validator.validatePassword(password, "testuser");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getAllErrors().contains("密码不能包含用户名"));
    }

    @Test
    void testValidatePassword_ContainsEmail_ShouldReturnError() {
        // Given
        String password = "johnPass@123";

        // When
        var result = validator.validatePassword(password, "john@example.com");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getAllErrors().contains("密码不能包含邮箱地址"));
    }

    @Test
    void testValidatePassword_ConsecutiveChars_ShouldReturnError() {
        // Given
        String password = "Paaassword@123";

        // When
        var result = validator.validatePassword(password, "testuser");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getAllErrors().contains("密码不能包含3个或更多连续的相同字符"));
    }

    @Test
    void testValidatePassword_KeyboardSequence_ShouldReturnError() {
        // Given
        String password = "Qwerty@123";

        // When
        var result = validator.validatePassword(password, "testuser");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getAllErrors().contains("密码不能包含键盘序列"));
    }

    @Test
    void testValidatePassword_NullPassword_ShouldReturnError() {
        // When
        var result = validator.validatePassword(null, "testuser");

        // Then
        assertFalse(result.isValid());
        assertEquals("密码不能为空", result.getFirstError());
    }

    @Test
    void testValidatePassword_EmptyPassword_ShouldReturnError() {
        // When
        var result = validator.validatePassword("", "testuser");

        // Then
        assertFalse(result.isValid());
        assertEquals("密码不能为空", result.getFirstError());
    }

    @Test
    void testIsPasswordValid_WithValidPassword_ShouldReturnTrue() {
        // Given
        String validPassword = "ValidP@ss123";

        // When
        boolean isValid = validator.isPasswordValid(validPassword);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testIsPasswordValid_WithInvalidPassword_ShouldReturnFalse() {
        // Given
        String invalidPassword = "weak";

        // When
        boolean isValid = validator.isPasswordValid(invalidPassword);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidatePassword_MultipleErrors_ShouldReturnAll() {
        // Given
        String weakPassword = "abc"; // 太短、缺少大写、数字、特殊字符

        // When
        var result = validator.validatePassword(weakPassword, "testuser");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() > 1);
    }

    @Test
    void testValidatePassword_AllRequirementsMet() {
        // Given
        String excellentPassword = "MySecur3P@ssw0rd!";

        // When
        var result = validator.validatePassword(excellentPassword, "different_user");

        // Then
        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }
}
