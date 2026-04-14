package com.daol.concierge.auth;

import io.jsonwebtoken.Claims;
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
 * Authorization: Bearer <jwt> 헤더를 읽어 SecurityContext 에 GuestPrincipal 을 채우는 필터.
 *
 * 토큰이 없거나 유효하지 않으면 SecurityContext 를 비워둠 → 이후 authorization 규칙이 401 결정.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	@Autowired
	private JwtService jwtService;

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {
		String header = req.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7).trim();
			Claims claims = jwtService.parse(token);
			if (claims != null) {
				String rsvNo = claims.getSubject();
				String propCd = claims.get("propCd", String.class);
				if (rsvNo != null && propCd != null) {
					GuestPrincipal principal = new GuestPrincipal(rsvNo, propCd);
					UsernamePasswordAuthenticationToken auth =
							new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
					SecurityContextHolder.getContext().setAuthentication(auth);
				}
			}
		}
		chain.doFilter(req, res);
	}
}
