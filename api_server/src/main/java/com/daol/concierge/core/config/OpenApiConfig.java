package com.daol.concierge.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI conciergeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("다올 컨시어지 API")
                        .description("호텔 투숙객 요청 + CCS 스태프 시스템 API")
                        .version("1.0.0")
                        .contact(new Contact().name("DAOL").email("zeroheat0930@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("게스트 JWT 또는 CCS 스태프 JWT")));
    }
}
