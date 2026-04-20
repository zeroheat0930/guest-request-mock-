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
				String cmpxCd = claims.get("cmpxCd", String.class);
				if (rsvNo != null && propCd != null) {
					GuestPrincipal principal = new GuestPrincipal(rsvNo, propCd,
							cmpxCd != null ? cmpxCd : "00001");
					UsernamePasswordAuthenticationToken auth =
							new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
					SecurityContextHolder.getContext().setAuthentication(auth);
				}
			}
		}
		chain.doFilter(req, res);
	}
}
