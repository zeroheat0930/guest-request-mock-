package com.daol.concierge.ccs.auth;

import com.daol.concierge.core.api.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * SecurityContext 에서 현재 CCS 스태프 principal 을 꺼내는 헬퍼.
 */
public final class CcsSecurityContextUtil {

	private CcsSecurityContextUtil() {}

	public static Optional<CcsPrincipal> currentCcsPrincipal() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) return Optional.empty();
		Object p = auth.getPrincipal();
		return (p instanceof CcsPrincipal) ? Optional.of((CcsPrincipal) p) : Optional.empty();
	}

	public static CcsPrincipal requireCcsPrincipal() {
		return currentCcsPrincipal()
				.orElseThrow(() -> new BizException("9102", "인증 필요"));
	}
}
