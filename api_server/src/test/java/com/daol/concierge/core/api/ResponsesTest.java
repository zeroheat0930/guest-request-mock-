package com.daol.concierge.core.api;

import com.daol.concierge.core.api.Responses.ListResponse;
import com.daol.concierge.core.api.Responses.MapResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResponsesTest {

    @Test
    void mapResponseOfWrapsMapCorrectly() {
        Map<String, Object> data = Map.of("key", "value", "num", 42);
        MapResponse response = MapResponse.of(data);

        assertEquals(ApiStatus.SUCCESS.getCode(), response.getStatus());
        assertEquals("SUCCESS", response.getMessage());
        assertNotNull(response.getMap());
        assertEquals("value", response.getMap().get("key"));
        assertEquals(42, response.getMap().get("num"));
    }

    @Test
    void mapResponseHasStatusZero() {
        MapResponse response = MapResponse.of(Map.of());
        assertEquals(0, response.getStatus());
    }

    @Test
    void listResponseOfWrapsListWithCorrectTotCnt() {
        List<String> items = List.of("a", "b", "c");
        ListResponse response = ListResponse.of(items);

        assertEquals(ApiStatus.SUCCESS.getCode(), response.getStatus());
        assertEquals("SUCCESS", response.getMessage());
        assertEquals(3, response.getList().size());
        assertNotNull(response.getPage());
        assertEquals(3L, response.getPage().getTotalElements());
    }

    @Test
    void listResponseOfWithEmptyListWorks() {
        ListResponse response = ListResponse.of(Collections.emptyList());

        assertNotNull(response);
        assertEquals(0, response.getList().size());
        assertEquals(0L, response.getPage().getTotalElements());
    }
}
