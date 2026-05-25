package com.tukangku.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil")
class JwtUtilTest {

    // Minimal secret ≥ 32 characters required by HS256
    private static final String SECRET       = "test-secret-key-that-is-at-least-32-chars";
    private static final long   EXPIRATION   = 3_600_000L; // 1 hour
    private static final long   VERY_SHORT   = 1L;          // 1 ms — immediately expired

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Test
    @DisplayName("generateToken returns non-null, non-empty string")
    void generateToken_returnsNonEmptyString() {
        String token = jwtUtil.generateToken("user-1", "budi@example.com", "ROLE_USER");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("extractUserId returns the subject embedded in the token")
    void extractUserId_matchesOriginalUserId() {
        String token = jwtUtil.generateToken("user-42", "user@test.com", "ROLE_USER");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-42");
    }

    @Test
    @DisplayName("extractRole returns the role claim embedded in the token")
    void extractRole_matchesOriginalRole() {
        String token = jwtUtil.generateToken("user-1", "admin@test.com", "ROLE_ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("isValid returns true for a fresh, correctly signed token")
    void isValid_freshToken_returnsTrue() {
        String token = jwtUtil.generateToken("user-1", "budi@example.com", "ROLE_USER");
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("isValid returns false for a tampered token")
    void isValid_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("user-1", "budi@example.com", "ROLE_USER");
        // Corrupt the signature part of the JWT
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("isValid returns false for a completely random string")
    void isValid_randomString_returnsFalse() {
        assertThat(jwtUtil.isValid("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("isValid returns false for a null/empty string")
    void isValid_emptyString_returnsFalse() {
        assertThat(jwtUtil.isValid("")).isFalse();
    }

    @Test
    @DisplayName("isValid returns false for an expired token")
    void isValid_expiredToken_returnsFalse() throws InterruptedException {
        JwtUtil shortLived = new JwtUtil(SECRET, VERY_SHORT);
        String token = shortLived.generateToken("user-1", "budi@example.com", "ROLE_USER");
        Thread.sleep(5); // wait for expiry
        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("token signed with different secret is invalid")
    void isValid_differentSecret_returnsFalse() {
        JwtUtil other = new JwtUtil("completely-different-secret-key-32+chars", EXPIRATION);
        String token = other.generateToken("user-1", "budi@example.com", "ROLE_USER");
        assertThat(jwtUtil.isValid(token)).isFalse();
    }
}
