package com.daol.concierge.core.parameter;

import com.daol.concierge.core.api.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestParamsTest {

    private RequestParams params;

    @BeforeEach
    void setUp() {
        params = new RequestParams();
    }

    @Test
    void getStringReturnsValueForExistingKey() {
        params.put("name", "Alice");
        assertEquals("Alice", params.getString("name", "default"));
    }

    @Test
    void getStringReturnsDefaultForMissingKey() {
        assertEquals("default", params.getString("missing", "default"));
    }

    @Test
    void getIntParsesNumericString() {
        params.put("count", "42");
        assertEquals(42, params.getInt("count", 0));
    }

    @Test
    void getIntReturnsDefaultForMissingKey() {
        assertEquals(99, params.getInt("missing", 99));
    }

    @Test
    void getNotEmptyStringThrowsForMissingKey() {
        assertThrows(ApiException.class, () -> params.getNotEmptyString("missing"));
    }

    @Test
    void getNotEmptyStringThrowsForEmptyValue() {
        params.put("emptyKey", "");
        assertThrows(ApiException.class, () -> params.getNotEmptyString("emptyKey"));
    }

    @Test
    void putAllMergesMapsCorrectly() {
        params.put("a", "1");
        params.putAll(Map.of("b", "2", "c", "3"));

        assertEquals("1", params.getString("a"));
        assertEquals("2", params.getString("b"));
        assertEquals("3", params.getString("c"));
    }
}
