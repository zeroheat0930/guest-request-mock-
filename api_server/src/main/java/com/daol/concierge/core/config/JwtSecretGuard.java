package com.daol.concierge.core.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * prod 프로파일 기동 시 jwt.secret 이 개발 기본값이면 즉시 fail.
 *
 * 사고 시나리오 차단: JWT_SECRET 환경변수를 깜빡 안 넣은 채 prod 로 배포 → 알려진 dev 시크릿으로
 * 토큰 서명 → 공격자가 임의 게스트 토큰 위조. 기동 직전에 강제 중단.
 */
@Component
@Profile("prod")
public class JwtSecretGuard {

	private static final String DEFAULT_DEV_SECRET =
			"dev-only-secret-please-change-in-production-32bytes-min!!";

	@Value("${jwt.secret}")
	private String secret;

	@PostConstruct
	public void guard() {
		if (secret == null || secret.isBlank() || DEFAULT_DEV_SECRET.equals(secret)) {
			throw new IllegalStateException(
					"jwt.secret must be explicitly set (env JWT_SECRET) in prod profile — refusing to start with dev default");
		}
		if (secret.getBytes().length < 32) {
			throw new IllegalStateException(
					"jwt.secret must be at least 32 bytes for HS256 — got " + secret.getBytes().length);
		}
	}
}
