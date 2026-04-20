package com.daol.concierge.ccs.routing;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 요청 유형 → 담당 부서 코드 기본 라우팅 규칙.
 *
 * US-013 에서 관리자 화면을 통한 동적 편집 예정. 현재는 하드코딩 기본값 사용.
 * null 반환은 "무시(디스패치 불필요)" 의미.
 */
@Component
public class CcsRoutingRuleDefault {

	private static final Map<String, String> DEFAULTS = Map.of(
		"AMENITY",	"HK",
		"HK",		"HK",
		"LATE_CO",	"FR",
		"PARKING",	"ENG"
		// CHAT, NEARBY → null (무시)
	);

	/**
	 * 요청 유형에 해당하는 부서 코드를 반환한다.
	 * 미등록·무시 유형은 null 반환.
	 */
	public String deptCdFor(String requestType) {
		if (requestType == null) return null;
		// HK_MU / HK_DND / HK_CLR 등 접두사 매칭
		if (requestType.startsWith("HK")) return DEFAULTS.get("HK");
		return DEFAULTS.get(requestType);
	}
}
