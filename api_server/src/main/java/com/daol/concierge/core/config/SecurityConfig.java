package com.daol.concierge.core.config;

import com.daol.concierge.auth.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * 보안 설정
 *
 * - /api/auth/**: 토큰 발급, 비인증 허용
 * - /api/ai/status: 배지 표시용, 비인증 허용
 * - /h2-console/**: 개발 편의, 비인증 허용 (프로덕션은 리버스 프록시로 차단)
 * - /api/gr/**, /api/ai/chat: JWT 필수
 * - OPTIONS(프리플라이트): 모두 허용
 * - 그 외 /, /error: 허용
 *
 * 세션 사용 안 함(STATELESS) — 모든 요청은 JWT 로 독립 인증.
 * CSRF 비활성 (JWT 기반이라 불필요).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private JwtAuthFilter jwtAuthFilter;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authz -> authz
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/ai/status").permitAll()
						.requestMatchers("/h2-console/**").permitAll()
						.requestMatchers("/", "/error").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll()
				)
				.headers(h -> h.frameOptions(f -> f.sameOrigin()))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(this::writeUnauthorized)
						.accessDeniedHandler((req, res, e) -> writeUnauthorized(req, res, null))
				)
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	/** 인증 실패/없음 → 401 + PMS 스타일 응답 바디 */
	private void writeUnauthorized(jakarta.servlet.http.HttpServletRequest req,
								   HttpServletResponse res,
								   org.springframework.security.core.AuthenticationException ex) throws IOException {
		res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		res.setContentType("application/json;charset=UTF-8");
		res.getWriter().write("{\"resCd\":\"9102\",\"resMsg\":\"인증 필요\",\"map\":{}}");
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration c = new CorsConfiguration();
		c.setAllowedOriginPatterns(List.of(
				"http://localhost:*",
				"http://127.0.0.1:*",
				"http://192.168.*.*:*",
				"http://10.*.*.*:*",
				"http://172.16.*.*:*", "http://172.17.*.*:*", "http://172.18.*.*:*",
				"http://172.19.*.*:*", "http://172.20.*.*:*", "http://172.21.*.*:*",
				"http://172.22.*.*:*", "http://172.23.*.*:*", "http://172.24.*.*:*",
				"http://172.25.*.*:*", "http://172.26.*.*:*", "http://172.27.*.*:*",
				"http://172.28.*.*:*", "http://172.29.*.*:*", "http://172.30.*.*:*",
				"http://172.31.*.*:*",
				"https://*.ngrok-free.app",
				"https://*.ngrok.io",
				"https://*.trycloudflare.com"
		));
		c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		c.setAllowedHeaders(List.of("*"));
		c.setExposedHeaders(List.of("Authorization"));
		c.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
		src.registerCorsConfiguration("/api/**", c);
		return src;
	}
}
