package com.daol.concierge.ccs.routing;

/**
 * CCS 태스크 SLA 규칙 (기본값).
 *
 * 기준: 태스크 생성 시각부터 SLA 분 초과하면 "overdue" — 대시보드 빨강 표시 +
 * 관리자 `/topic/ccs/esc/{propCd}` 토픽으로 에스컬레이션 푸시.
 *
 * 추후 INV.CCS_SLA_RULE 테이블로 프로퍼티별 커스터마이징 가능하게 확장 예정.
 */
public final class CcsSlaRules {

	private CcsSlaRules() {}

	public static int defaultSlaMin(String sourceType) {
		if (sourceType == null) return 30;
		switch (sourceType) {
			case "AMENITY":   return 15;  // 어메니티 — 15분 안에 전달
			case "LATE_CO":   return 30;  // 레이트체크아웃 승인
			case "PARKING":   return 20;  // 주차 등록 확인
			case "STAFF_REQ": return 60;  // 내부 요청
			case "COMPLAINT": return 10;  // 불만 — 최우선
			case "CHAT":      return 30;
			default:
				if (sourceType.startsWith("HK_")) return 45;  // 하우스키핑 상태 변경
				return 30;
		}
	}
}
