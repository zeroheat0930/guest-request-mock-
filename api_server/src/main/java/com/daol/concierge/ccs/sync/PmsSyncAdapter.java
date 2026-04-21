package com.daol.concierge.ccs.sync;

import java.util.Map;

/**
 * PMS 동기화 어댑터 — CCS 도메인 이벤트를 외부 PMS 로 전파하기 위한 경계.
 *
 * 핵심 설계:
 *  - CCS 는 모든 도메인 데이터를 **자체 INV 스키마에 먼저 저장** (CCS-first).
 *  - 저장 직후 이 어댑터가 호출돼 PMS 쪽 테이블/REST API 로 전파.
 *  - DAOL 배포에서는 {@link DaolPmsSyncAdapter} 가 활성화되고,
 *    타 호텔(PMS 없음 or 다른 PMS)에서는 {@link NoopPmsSyncAdapter} 가 바인딩돼 CCS 독립 운영.
 *
 * 활성화 플래그: {@code concierge.pms.sync.enabled} (기본 false).
 *
 * 모든 메서드는 default 로 no-op. 구현체는 필요한 도메인만 override.
 */
public interface PmsSyncAdapter {

	/** 이 어댑터가 실제 PMS 로 동기화 하는지 여부. UI/로그 진단용. */
	default boolean isEnabled() { return false; }

	/**
	 * 분실물/습득물 동기화. CCS_LOSTFOUND → PMS.PMS_LOSTFOUND.
	 * @param payload CCS 내부 스키마 레코드 (camelCase 키)
	 */
	default void syncLostFound(Map<String, Object> payload) {}

	/**
	 * 고객 불만(VOC) 동기화. CCS_VOC → PMS.PMS_CUST_VOC.
	 */
	default void syncVoc(Map<String, Object> payload) {}

	/**
	 * 대여 품목 신청/반납 동기화. CCS_RENTAL_ORDER → PMS.PMS_LOAN.
	 */
	default void syncRental(Map<String, Object> payload) {}

	/**
	 * 당직 로그는 PMS 미보유 도메인이라 기본 구현 자체 없음 (future reserved).
	 */
}
