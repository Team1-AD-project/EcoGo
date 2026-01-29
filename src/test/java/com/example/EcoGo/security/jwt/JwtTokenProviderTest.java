package com.example.EcoGo.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT Token Provider 单元测试
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-provider-testing-minimum-256-bits-required";
    private static final String TEST_USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET);
        // 设置测试用的过期时间
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 3600000L); // 1小时
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 86400000L); // 1天
    }

    @Test
    void testGenerateAccessToken_ShouldContainCorrectClaims() {
        // Given
        boolean isAdmin = true;

        // When
        String token = jwtTokenProvider.generateAccessToken(TEST_USER_ID, isAdmin);

        // Then
        assertNotNull(token);
        assertTrue(token.length() > 0);

        // 验证Token内容
        Key key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(TEST_USER_ID, claims.getSubject());
        assertEquals(true, claims.get("isAdmin", Boolean.class));
        assertEquals("access", claims.get("type", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void testGenerateAccessToken_ForRegularUser() {
        // Given
        boolean isAdmin = false;

        // When
        String token = jwtTokenProvider.generateAccessToken(TEST_USER_ID, isAdmin);

        // Then
        Key key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(false, claims.get("isAdmin", Boolean.class));
    }

    @Test
    void testGenerateRefreshToken_ShouldContainRefreshType() {
        // When
        String token = jwtTokenProvider.generateRefreshToken(TEST_USER_ID);

        // Then
        assertNotNull(token);
        
        Key key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(TEST_USER_ID, claims.getSubject());
        assertEquals("refresh", claims.get("type", String.class));
        assertNull(claims.get("isAdmin")); // 刷新令牌不包含isAdmin
    }

    @Test
    void testGetAccessTokenExpiration() {
        // When
        long expiration = jwtTokenProvider.getAccessTokenExpiration();

        // Then
        assertEquals(3600000L, expiration);
    }

    @Test
    void testGetRefreshTokenExpiration() {
        // When
        long expiration = jwtTokenProvider.getRefreshTokenExpiration();

        // Then
        assertEquals(86400000L, expiration);
    }

    @Test
    void testGeneratedTokensAreUnique() {
        // When
        String token1 = jwtTokenProvider.generateAccessToken(TEST_USER_ID, false);
        String token2 = jwtTokenProvider.generateAccessToken(TEST_USER_ID, false);

        // Then
        assertNotEquals(token1, token2); // 由于时间戳不同，生成的Token应该不同
    }
}
