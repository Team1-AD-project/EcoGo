package com.example.EcoGo.security;

import com.example.EcoGo.security.jwt.JwtTokenProvider;
import com.example.EcoGo.security.jwt.JwtTokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Security 集成测试
 * 
 * <p>测试Security组件在Spring容器中的集成情况
 */
@SpringBootTest
class SecurityIntegrationTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtTokenValidator jwtTokenValidator;

    @Test
    void contextLoads() {
        // 验证Security相关Bean是否正确加载
        assertNotNull(jwtTokenProvider);
        assertNotNull(jwtTokenValidator);
    }

    @Test
    void testJwtTokenFlow() {
        // Given
        String userId = "integration-test-user";
        boolean isAdmin = true;

        // When - 生成Token
        String accessToken = jwtTokenProvider.generateAccessToken(userId, isAdmin);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // Then - 验证Token
        assertTrue(jwtTokenValidator.isTokenValid(accessToken));
        assertTrue(jwtTokenValidator.isTokenValid(refreshToken));

        // 提取用户信息
        String extractedUserId = jwtTokenValidator.getUserIdFromToken(accessToken);
        assertEquals(userId, extractedUserId);

        // 验证Token类型
        assertEquals("access", jwtTokenValidator.getTokenType(accessToken));
        assertEquals("refresh", jwtTokenValidator.getTokenType(refreshToken));

        // 验证管理员标识
        assertTrue(jwtTokenValidator.isAdminToken(accessToken));
    }

    @Test
    void testTokenProviderAndValidatorIntegration() {
        // Given
        String regularUserId = "regular-user-123";
        String adminUserId = "admin-user-456";

        // When
        String regularToken = jwtTokenProvider.generateAccessToken(regularUserId, false);
        String adminToken = jwtTokenProvider.generateAccessToken(adminUserId, true);

        // Then
        assertNotEquals(regularToken, adminToken);
        
        var regularClaims = jwtTokenValidator.validateToken(regularToken);
        var adminClaims = jwtTokenValidator.validateToken(adminToken);

        assertEquals(regularUserId, regularClaims.getSubject());
        assertEquals(adminUserId, adminClaims.getSubject());

        assertFalse(regularClaims.get("isAdmin", Boolean.class));
        assertTrue(adminClaims.get("isAdmin", Boolean.class));
    }
}
