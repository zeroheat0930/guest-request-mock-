package com.daol.concierge.auth;

/**
 * 인증된 게스트를 표현하는 principal.
 * SecurityContextHolder 에 담겨 Service 레이어까지 전달됨.
 */
public record GuestPrincipal(String rsvNo, String propCd) {
	@Override
	public String toString() {
		return rsvNo + "@" + propCd;
	}
}
