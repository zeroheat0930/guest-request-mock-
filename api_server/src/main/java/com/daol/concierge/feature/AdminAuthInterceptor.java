package com.daol.concierge.feature;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 관리자 API 헤더 인증 인터셉터.
 *
 * - `admin.password` 미설정 시 503 (실수로 미인증 상태 노출 방지)
 * - `X-Admin-Token` 헤더가 설정값과 일치해야 통과
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

	@Value("${admin.password:}")
	private String adminPassword;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		if (adminPassword == null || adminPassword.isBlank()) {
			writeError(response, 503, "9503", "admin auth not configured");
			return false;
		}
		String tok = request.getHeader("X-Admin-Token");
		if (tok == null || !adminPassword.equals(tok)) {
			writeError(response, 401, "9102", "admin auth failed");
			return false;
		}
		return true;
	}

	private void writeError(HttpServletResponse res, int status, String resCd, String msg) throws IOException {
		res.setStatus(status);
		res.setContentType("application/json;charset=UTF-8");
		res.getWriter().write("{\"resCd\":\"" + resCd + "\",\"resMsg\":\"" + msg + "\",\"map\":{}}");
	}
}
