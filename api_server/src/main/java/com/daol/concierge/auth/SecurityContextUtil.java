package com.daol.concierge.auth;

import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext 에서 현재 인증된 게스트 정보를 꺼내는 헬퍼.
 * Service 레이어에서 "요청자 != 리소스 주인" 케이스를 막는 용도.
 */
public final class SecurityContextUtil {

	private SecurityContextUtil() {}

	public static GuestPrincipal currentPrincipalOrNull() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) return null;
		Object p = auth.getPrincipal();
		return (p instanceof GuestPrincipal) ? (GuestPrincipal) p : null;
	}

	public static GuestPrincipal requirePrincipal() {
		GuestPrincipal p = currentPrincipalOrNull();
		if (p == null) {
			throw new ApiException(ApiStatus.ACCESS_DENIED, "인증 필요");
		}
		return p;
	}

	/**
	 * 요청자의 rsvNo 와 리소스 rsvNo 가 일치하는지 확인.
	 * 다르면 9102 로 거절 (정보 누출 방지 위해 구체적 사유는 로그에만).
	 */
	public static GuestPrincipal assertOwnsRsv(String rsvNo) {
		GuestPrincipal p = requirePrincipal();
		if (rsvNo == null || !rsvNo.equals(p.rsvNo())) {
			throw new ApiException(ApiStatus.ACCESS_DENIED, "권한 없음");
		}
		return p;
	}
}
