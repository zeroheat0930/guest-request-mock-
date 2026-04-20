package com.daol.concierge.ccs.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * /api/ccs/** 경로 전용 JWT 필터.
 *
 * 게스트 JwtAuthFilter 와 별도로 동작하며, /api/ccs/* 가 아닌 요청은 즉시 통과시킨다.
 * /api/ccs/auth/login 도 통과 (토큰 발급 엔드포인트).
 * 토큰 검증 실패 시 SecurityContext 를 비워두고 통과 → 이후 authorization 단계에서 401 결정.
 */
@Component
public class CcsJwtFilter extends OncePerRequestFilter {

	@Autowired
	private CcsJwtService ccsJwtService;

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {
		String uri = req.getRequestURI();
		if (!uri.startsWith("/api/ccs/")) {
			chain.doFilter(req, res);
			return;
		}
		if (uri.startsWith("/api/ccs/auth/login")) {
			chain.doFilter(req, res);
			return;
		}

		String header = req.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7).trim();
			CcsPrincipal principal = ccsJwtService.parse(token);
			if (principal != null) {
				UsernamePasswordAuthenticationToken auth =
						new UsernamePasswordAuthenticationToken(principal, token, Collections.emptyList());
				SecurityContextHolder.getContext().setAuthentication(auth);
			}
		}
		chain.doFilter(req, res);
	}
}
