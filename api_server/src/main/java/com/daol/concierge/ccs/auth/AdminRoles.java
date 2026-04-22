package com.daol.concierge.ccs.auth;

import java.util.Objects;

/**
 * 관리자 역할 판정 유틸.
 *
 * PMS_USER_MTR.USER_TP 기반으로 역할을 해석한다 (별도 POS_LVL 컬럼 없음).
 *   00001 → SYS_ADMIN   시스템 관리자: 모든 프로퍼티 접근 가능
 *   00002 → PROP_ADMIN  프로퍼티 관리자: 자기 PROP_CD 범위만
 *   00003 → CMPX_ADMIN  컴플렉스 관리자: 자기 PROP_CD + CMPX_CD 범위만
 *   그 외 → STAFF        관리자 메뉴 진입 불가
 */
public final class AdminRoles {

	public static final String SYS_ADMIN  = "00001";
	public static final String PROP_ADMIN = "00002";
	public static final String CMPX_ADMIN = "00003";

	private AdminRoles() {}

	public static boolean isSystemAdmin(String userTp)   { return SYS_ADMIN.equals(userTp); }
	public static boolean isPropertyAdmin(String userTp) { return PROP_ADMIN.equals(userTp); }
	public static boolean isComplexAdmin(String userTp)  { return CMPX_ADMIN.equals(userTp); }

	public static boolean isAdmin(String userTp) {
		return isSystemAdmin(userTp) || isPropertyAdmin(userTp) || isComplexAdmin(userTp);
	}

	public static String label(String userTp) {
		if (SYS_ADMIN.equals(userTp))  return "SYS_ADMIN";
		if (PROP_ADMIN.equals(userTp)) return "PROP_ADMIN";
		if (CMPX_ADMIN.equals(userTp)) return "CMPX_ADMIN";
		return "STAFF";
	}

	/**
	 * 대상 리소스(targetPropCd, targetCmpxCd)에 접근 가능한지 검사.
	 * SYS_ADMIN은 항상 true, PROP_ADMIN은 propCd 일치, CMPX_ADMIN은 propCd+cmpxCd 모두 일치.
	 * target이 null이면 "범위 지정 없음" → SYS_ADMIN만 허용(안전한 기본값).
	 */
	public static boolean canAccess(String userTp,
	                                 String myPropCd, String myCmpxCd,
	                                 String targetPropCd, String targetCmpxCd) {
		if (isSystemAdmin(userTp)) return true;
		if (targetPropCd == null) return false;
		if (isPropertyAdmin(userTp)) return Objects.equals(myPropCd, targetPropCd);
		if (isComplexAdmin(userTp))  return Objects.equals(myPropCd, targetPropCd)
		                                   && Objects.equals(myCmpxCd, targetCmpxCd);
		return false;
	}
}
