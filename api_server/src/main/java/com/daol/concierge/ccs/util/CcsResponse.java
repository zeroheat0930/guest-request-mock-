package com.daol.concierge.ccs.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PMS 스타일 응답 봉투 {resCd, resMsg, map} 생성 헬퍼.
 *
 * CCS 컨트롤러들이 동일한 구조를 반복 생성하지 않도록 모았다.
 */
public final class CcsResponse {

    private CcsResponse() {}

    public static Map<String, Object> ok(Map<String, Object> map) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("resCd", "0000");
        res.put("resMsg", "OK");
        res.put("map", map);
        return res;
    }
}
