package com.daol.concierge.ccs.auth;

/**
 * 인증된 CCS 스태프 principal.
 * SecurityContextHolder 에 담겨 컨트롤러/서비스 계층까지 전달됨.
 */
public record CcsPrincipal(
		String staffId,
		String loginId,
		String deptCd,
		String propCd,
		String cmpxCd,
		String staffNm,
		String userTp
) {
	/**
	 * PMS 관례:
	 *  00001 시스템관리자, 00002 프로퍼티관리자, 00003 컴플렉스관리자
	 *   → 해당 스코프 전체 태스크 viewer. "내가 받기" 불필요, 재배정/관찰 위주.
	 *  00004 일반 사용자, 00005 POS, 00007 객실정비관리자, 00008 객실정비사용자
	 *   → 내 부서 태스크만.
	 */
	public boolean isAdminViewer() {
		return "00001".equals(userTp) || "00002".equals(userTp) || "00003".equals(userTp);
	}

	@Override
	public String toString() {
		return loginId + "@" + propCd + "/" + cmpxCd;
	}
}
