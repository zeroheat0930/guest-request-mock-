package com.daol.concierge.ccs.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * CCS WebSocket STOMP 브로커 설정 — 부서/스태프 실시간 푸시용
 */
@Configuration
@EnableWebSocketMessageBroker
public class CcsWebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Autowired
	private CcsStompAuthInterceptor stompAuthInterceptor;

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthInterceptor);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic");
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws-ccs").setAllowedOriginPatterns("*");
	}
}
