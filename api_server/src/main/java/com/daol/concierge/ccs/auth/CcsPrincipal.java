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
		String staffNm
) {
	@Override
	public String toString() {
		return loginId + "@" + propCd + "/" + cmpxCd;
	}
}
