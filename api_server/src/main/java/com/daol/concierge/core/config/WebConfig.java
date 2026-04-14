package com.daol.concierge.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 설정 훅. CORS 는 US-004 부터 Spring Security 의 CorsConfigurationSource 로 이관됨
 * ({@link SecurityConfig}).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
