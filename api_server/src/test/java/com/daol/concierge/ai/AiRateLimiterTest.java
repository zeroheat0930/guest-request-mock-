package com.daol.concierge.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class AiRateLimiterTest {

    private AiRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new AiRateLimiter();
    }

    @Test
    void firstRequestIsAllowed() {
        assertTrue(limiter.isAllowed("RSV001"));
    }

    @Test
    void twentyRequestsWithinMinuteAllAllowed() {
        for (int i = 0; i < 20; i++) {
            assertTrue(limiter.isAllowed("RSV002"), "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void twentyFirstRequestIsRejected() {
        for (int i = 0; i < 20; i++) {
            limiter.isAllowed("RSV003");
        }
        assertFalse(limiter.isAllowed("RSV003"), "21st request should be rejected");
    }

    @Test
    void afterWindowExpiresRequestsAreAllowedAgain() throws Exception {
        // Use a small window via reflection — inject an expired window entry
        // so we can test expiry without sleeping 60 seconds
        Field requestsField = AiRateLimiter.class.getDeclaredField("requests");
        requestsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, long[]> requests =
                (ConcurrentHashMap<String, long[]>) requestsField.get(limiter);

        // Simulate a window that started 61 seconds ago with 20 requests
        long expiredStart = System.currentTimeMillis() - 61_000L;
        requests.put("RSV004", new long[]{20, expiredStart});

        // The window has expired, so the next call should reset and be allowed
        assertTrue(limiter.isAllowed("RSV004"), "After window expiry, request should be allowed");
    }
}
