package com.daol.concierge.feature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 관리자 인터셉터를 `/api/concierge/admin/**` 경로에만 묶어준다.
 */
@Configuration
public class AdminWebMvcConfig implements WebMvcConfigurer {

	@Autowired private AdminAuthInterceptor adminAuthInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(adminAuthInterceptor)
				.addPathPatterns("/api/concierge/admin/**");
	}
}
