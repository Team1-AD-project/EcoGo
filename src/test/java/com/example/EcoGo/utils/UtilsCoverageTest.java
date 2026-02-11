package com.example.EcoGo.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for all utility classes: JwtUtils, InputValidator, PasswordUtils, LogSanitizer.
 */
class UtilsCoverageTest {

    // ==================== JwtUtils ====================
    private final JwtUtils jwtUtils = new JwtUtils();

    @Test
    void generateToken_regularUser() {
        String token = jwtUtils.generateToken("user1", false);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateToken_adminUser() {
        String token = jwtUtils.generateToken("admin", true);
        assertNotNull(token);
    }

    @Test
    void validateToken_success() {
        String token = jwtUtils.generateToken("user1", false);
        Claims claims = jwtUtils.validateToken(token);
        assertEquals("user1", claims.getSubject());
        assertFalse(claims.get("isAdmin", Boolean.class));
    }

    @Test
    void validateToken_adminClaim() {
        String token = jwtUtils.generateToken("admin", true);
        Claims claims = jwtUtils.validateToken(token);
        assertEquals("admin", claims.getSubject());
        assertTrue(claims.get("isAdmin", Boolean.class));
    }

    @Test
    void validateToken_invalidToken_throwsException() {
        assertThrows(RuntimeException.class, () -> jwtUtils.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_nullToken_throwsException() {
        assertThrows(RuntimeException.class, () -> jwtUtils.validateToken(null));
    }

    @Test
    void getUserIdFromToken_success() {
        String token = jwtUtils.generateToken("user42", false);
        String userId = jwtUtils.getUserIdFromToken(token);
        assertEquals("user42", userId);
    }

    @Test
    void getExpirationDate_success() {
        String token = jwtUtils.generateToken("user1", false);
        Date expiration = jwtUtils.getExpirationDate(token);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date())); // should be in the future
    }

    // ==================== InputValidator ====================
    @Test
    void validateEmail_valid() {
        assertTrue(InputValidator.validateEmail("test@example.com"));
    }

    @Test
    void validateEmail_validMultipleDots() {
        assertTrue(InputValidator.validateEmail("user@mail.example.com"));
    }

    @Test
    void validateEmail_null() {
        assertFalse(InputValidator.validateEmail(null));
    }

    @Test
    void validateEmail_empty() {
        assertFalse(InputValidator.validateEmail(""));
    }

    @Test
    void validateEmail_noAt() {
        assertFalse(InputValidator.validateEmail("testexample.com"));
    }

    @Test
    void validateEmail_noDot() {
        assertFalse(InputValidator.validateEmail("test@examplecom"));
    }

    @Test
    void validateAge_valid() {
        assertTrue(InputValidator.validateAge(25));
        assertTrue(InputValidator.validateAge(0));
        assertTrue(InputValidator.validateAge(150));
    }

    @Test
    void validateAge_invalid() {
        assertFalse(InputValidator.validateAge(-1));
        assertFalse(InputValidator.validateAge(151));
        assertFalse(InputValidator.validateAge(-100));
        assertFalse(InputValidator.validateAge(200));
    }

    // ==================== PasswordUtils ====================
    @Test
    void passwordUtils_encodeAndMatch() {
        PasswordUtils pwUtils = new PasswordUtils();
        String raw = "mySecretPassword";
        String encoded = pwUtils.encode(raw);

        assertNotNull(encoded);
        assertNotEquals(raw, encoded); // should be hashed
        assertTrue(pwUtils.matches(raw, encoded));
        assertFalse(pwUtils.matches("wrongPassword", encoded));
    }

    @Test
    void passwordUtils_encodeDifferentEachTime() {
        PasswordUtils pwUtils = new PasswordUtils();
        String raw = "samePassword";
        String enc1 = pwUtils.encode(raw);
        String enc2 = pwUtils.encode(raw);
        // BCrypt produces different hashes due to random salt
        assertNotEquals(enc1, enc2);
        // But both should match the original
        assertTrue(pwUtils.matches(raw, enc1));
        assertTrue(pwUtils.matches(raw, enc2));
    }

    // ==================== LogSanitizer ====================
    @Test
    void sanitize_null() {
        assertEquals("null", LogSanitizer.sanitize(null));
    }

    @Test
    void sanitize_cleanString() {
        assertEquals("hello", LogSanitizer.sanitize("hello"));
    }

    @Test
    void sanitize_newline() {
        assertEquals("line1_line2", LogSanitizer.sanitize("line1\nline2"));
    }

    @Test
    void sanitize_carriageReturn() {
        assertEquals("line1_line2", LogSanitizer.sanitize("line1\rline2"));
    }

    @Test
    void sanitize_tab() {
        assertEquals("col1_col2", LogSanitizer.sanitize("col1\tcol2"));
    }

    @Test
    void sanitize_mixed() {
        assertEquals("a_b_c_d", LogSanitizer.sanitize("a\nb\rc\td"));
    }
}
