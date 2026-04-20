package com.daol.concierge.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    // 256-bit (32 char) secret required for HMAC-SHA256
    private static final String SECRET = "test-secret-key-that-is-32chars!";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "hoursValid", 72L);
    }

    @Test
    void issueAndParseClaimsMatch() {
        String future = LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String token = jwtService.issue("RSV123", "PROP01", "CMPX01", future);

        assertNotNull(token);
        Claims claims = jwtService.parse(token);
        assertNotNull(claims);
        assertEquals("RSV123", claims.getSubject());
        assertEquals("PROP01", claims.get("propCd", String.class));
        assertEquals("CMPX01", claims.get("cmpxCd", String.class));
    }

    @Test
    void expiredTokenReturnsNull() throws Exception {
        // Issue with hoursValid=0 and depDt=yesterday so expiry is in the past
        ReflectionTestUtils.setField(jwtService, "hoursValid", 0L);
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String token = jwtService.issue("RSV_EXP", "P1", "C1", yesterday);

        // Give JVM a moment to ensure the token is expired
        Thread.sleep(10);
        Claims claims = jwtService.parse(token);
        assertNull(claims, "Expired token should return null");
    }

    @Test
    void tamperedTokenReturnsNull() {
        String future = LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String token = jwtService.issue("RSV_TAMPER", "P1", "C1", future);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertNull(jwtService.parse(tampered), "Tampered token should return null");
    }

    @Test
    void tokenExpiryRespectsCheckoutDate() {
        // depDt = tomorrow → expiry should be day after tomorrow midnight (< 72h)
        String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String token = jwtService.issue("RSV_CHK", "P1", "C1", tomorrow);

        Claims claims = jwtService.parse(token);
        assertNotNull(claims, "Token within checkout window should be valid");
        // Expiry should be before now + 72h (hardCap)
        long expMs = claims.getExpiration().getTime();
        long hardCap = System.currentTimeMillis() + 72L * 3600 * 1000;
        assertTrue(expMs <= hardCap, "Expiry should not exceed 72h hard cap");
    }
}
