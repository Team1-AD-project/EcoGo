package com.example.EcoGo.security.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT Token Validator 单元测试
 */
class JwtTokenValidatorTest {

    private JwtTokenValidator jwtTokenValidator;
    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-validator-testing-minimum-256-bits-required";
    private static final String TEST_USER_ID = "test-user-456";
    private Key secretKey;

    @BeforeEach
    void setUp() {
        jwtTokenValidator = new JwtTokenValidator(TEST_SECRET);
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET);
        secretKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
    }

    @Test
    void testValidateToken_WithValidToken_ShouldReturnClaims() {
        // Given
        String validToken = jwtTokenProvider.generateAccessToken(TEST_USER_ID, true);

        // When
        var claims = jwtTokenValidator.validateToken(validToken);

        // Then
        assertNotNull(claims);
        assertEquals(TEST_USER_ID, claims.getSubject());
        assertEquals(true, claims.get("isAdmin", Boolean.class));
    }

    @Test
    void testValidateToken_WithExpiredToken_ShouldThrowException() {
        // Given - 创建一个已过期的Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("isAdmin", false);
        
        String expiredToken = Jwts.builder()
                .setClaims(claims)
                .setSubject(TEST_USER_ID)
                .setIssuedAt(new Date(System.currentTimeMillis() - 10000))
                .setExpiration(new Date(System.currentTimeMillis() - 5000)) // 5秒前过期
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        // When & Then
        assertThrows(JwtException.class, () -> {
            jwtTokenValidator.validateToken(expiredToken);
        });
    }

    @Test
    void testValidateToken_WithInvalidSignature_ShouldThrowException() {
        // Given - 使用不同的密钥签名
        Key wrongKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String invalidToken = Jwts.builder()
                .setSubject(TEST_USER_ID)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();

        // When & Then
        assertThrows(JwtException.class, () -> {
            jwtTokenValidator.validateToken(invalidToken);
        });
    }

    @Test
    void testValidateToken_WithMalformedToken_ShouldThrowException() {
        // Given
        String malformedToken = "not.a.valid.jwt.token";

        // When & Then
        assertThrows(JwtException.class, () -> {
            jwtTokenValidator.validateToken(malformedToken);
        });
    }

    @Test
    void testIsTokenValid_WithValidToken_ShouldReturnTrue() {
        // Given
        String validToken = jwtTokenProvider.generateAccessToken(TEST_USER_ID, false);

        // When
        boolean isValid = jwtTokenValidator.isTokenValid(validToken);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testIsTokenValid_WithInvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtTokenValidator.isTokenValid(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testGetUserIdFromToken() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(TEST_USER_ID, false);

        // When
        String userId = jwtTokenValidator.getUserIdFromToken(token);

        // Then
        assertEquals(TEST_USER_ID, userId);
    }

    @Test
    void testGetTokenType() {
        // Given
        String accessToken = jwtTokenProvider.generateAccessToken(TEST_USER_ID, false);
        String refreshToken = jwtTokenProvider.generateRefreshToken(TEST_USER_ID);

        // When
        String accessType = jwtTokenValidator.getTokenType(accessToken);
        String refreshType = jwtTokenValidator.getTokenType(refreshToken);

        // Then
        assertEquals("access", accessType);
        assertEquals("refresh", refreshType);
    }

    @Test
    void testIsAdminToken_WithAdminToken_ShouldReturnTrue() {
        // Given
        String adminToken = jwtTokenProvider.generateAccessToken(TEST_USER_ID, true);

        // When
        boolean isAdmin = jwtTokenValidator.isAdminToken(adminToken);

        // Then
        assertTrue(isAdmin);
    }

    @Test
    void testIsAdminToken_WithUserToken_ShouldReturnFalse() {
        // Given
        String userToken = jwtTokenProvider.generateAccessToken(TEST_USER_ID, false);

        // When
        boolean isAdmin = jwtTokenValidator.isAdminToken(userToken);

        // Then
        assertFalse(isAdmin);
    }

    @Test
    void testIsTokenExpired_WithValidToken_ShouldReturnFalse() {
        // Given
        String validToken = jwtTokenProvider.generateAccessToken(TEST_USER_ID, false);

        // When
        boolean isExpired = jwtTokenValidator.isTokenExpired(validToken);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void testGetExpirationDateFromToken() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(TEST_USER_ID, false);

        // When
        Date expirationDate = jwtTokenValidator.getExpirationDateFromToken(token);

        // Then
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void testGetIssuedAtFromToken() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(TEST_USER_ID, false);

        // When
        Date issuedAt = jwtTokenValidator.getIssuedAtFromToken(token);

        // Then
        assertNotNull(issuedAt);
        assertTrue(issuedAt.before(new Date()) || issuedAt.equals(new Date()));
    }
}
