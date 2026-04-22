package com.daol.concierge.feature;

import com.daol.concierge.ccs.auth.AdminRoles;
import com.daol.concierge.ccs.auth.CcsJwtService;
import com.daol.concierge.ccs.auth.CcsPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 관리자 API (/api/concierge/admin/**) JWT 기반 인증 인터셉터.
 *
 * 스태프 로그인으로 발급된 CCS JWT 를 Authorization: Bearer 로 전달받아,
 * PMS_USER_MTR.USER_TP 기반 역할이 관리자(00001/00002/00003) 인지 검증한다.
 *
 * 인증 통과 시 request attribute 에 CcsPrincipal 을 저장하여 컨트롤러가
 * 로그인 사용자의 propCd/cmpxCd/userTp 를 바로 쓸 수 있게 한다.
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

	public static final String ATTR_PRINCIPAL = "admin.principal";

	@Autowired private CcsJwtService jwtService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}

		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith("Bearer ")) {
			writeError(response, 401, "missing bearer token");
			return false;
		}

		String token = header.substring(7).trim();
		CcsPrincipal principal = jwtService.parse(token);
		if (principal == null) {
			writeError(response, 401, "invalid or expired token");
			return false;
		}

		if (!AdminRoles.isAdmin(principal.userTp())) {
			writeError(response, 403, "admin role required");
			return false;
		}

		request.setAttribute(ATTR_PRINCIPAL, principal);
		return true;
	}

	private void writeError(HttpServletResponse res, int status, String msg) throws IOException {
		res.setStatus(status);
		res.setContentType("application/json;charset=UTF-8");
		res.getWriter().write("{\"status\":-30,\"message\":\"\",\"error\":{\"message\":\"" + msg + "\"},\"redirect\":\"\"}");
	}
}
