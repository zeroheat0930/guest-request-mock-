package com.daol.concierge.ccs.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * CCS 인증 관련 빈 정의.
 *
 * BCrypt strength 11 — PMS 분석 기준과 일치.
 */
@Configuration
public class CcsBeans {

	@Bean
	public BCryptPasswordEncoder ccsPasswordEncoder() {
		return new BCryptPasswordEncoder(11);
	}
}
