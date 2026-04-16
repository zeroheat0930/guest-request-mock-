package com.daol.concierge.ai;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiRateLimiter {

    private final ConcurrentHashMap<String, long[]> requests = new ConcurrentHashMap<>();

    // Max 20 requests per minute per guest
    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_MS = 60_000;

    public boolean isAllowed(String rsvNo) {
        long now = System.currentTimeMillis();
        long[] window = requests.compute(rsvNo, (key, val) -> {
            if (val == null || now - val[1] > WINDOW_MS) {
                return new long[]{1, now};
            }
            val[0]++;
            return val;
        });
        return window[0] <= MAX_REQUESTS;
    }
}
