package com.daol.concierge.ccs.sync;

import java.util.List;
import java.util.Map;

/**
 * PMS 원격 변경 API — CCS 어드민이 PMS 도메인 데이터를 변경할 때 통과하는 경계.
 *
 * 핵심 설계:
 *  - CCS 는 PMS 테이블을 **직접 UPDATE/INSERT 하지 않는다**.
 *  - 직원 USE_YN, 직원 부서 변경, 부서 마스터 추가 등 PMS 데이터 변경은
 *    이 인터페이스를 통해 PMS REST API 로 위임된다.
 *  - DAOL 배포에서는 {@link DaolPmsRemoteApi} (실제 PMS REST 호출) 가 활성화되고,
 *    시연/개발 환경에서는 {@link MockPmsRemoteApi} (메모리 오버라이드) 가 바인딩된다.
 *
 * 활성화 플래그: {@code concierge.pms.api.enabled} (기본 false → MockPmsRemoteApi 활성).
 *
 * {@link PmsSyncAdapter}(단방향 fire-and-forget) 과 다르게,
 * 이 API 는 사용자 액션에 동기 응답이 필요한 양방향 호출.
 */
public interface PmsRemoteApi {

	/** 실 PMS API 로 호출되는 모드인지 여부 (UI/로그 진단용). */
	default boolean isEnabled() { return false; }

	/**
	 * 직원 활성/비활성 토글.
	 * @param userId  PMS_USER_MTR.USER_ID
	 * @param useYn   "Y" or "N"
	 * @param actor   변경 수행 어드민 staffId (감사 로그용)
	 */
	void updateUserUseYn(String userId, String useYn, String actor);

	/**
	 * 직원 부서 변경.
	 * @param userId  PMS_USER_MTR.USER_ID
	 * @param deptCd  PMS_DIVISION.DEPT_CD
	 * @param actor   변경 수행 어드민 staffId
	 */
	void updateUserDept(String userId, String deptCd, String actor);

	/**
	 * 직원 목록 조회 결과에 mock 오버라이드 적용.
	 * Mock 모드에서만 의미 있고, Daol 모드에서는 PMS 가 이미 진실의 소스라 no-op.
	 * @param users  PMS_USER_MTR 조회 결과 (in-place 수정)
	 */
	default void applyUserOverrides(List<Map<String, Object>> users) {}
}
