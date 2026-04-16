package com.daol.concierge.ccs.websocket;

import com.daol.concierge.ccs.auth.CcsJwtService;
import com.daol.concierge.ccs.auth.CcsPrincipal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CcsStompAuthInterceptor implements ChannelInterceptor {

    private final CcsJwtService ccsJwtService;

    public CcsStompAuthInterceptor(CcsJwtService ccsJwtService) {
        this.ccsJwtService = ccsJwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String token = authHeaders.get(0).replace("Bearer ", "");
                CcsPrincipal principal = ccsJwtService.parse(token);
                if (principal != null) {
                    accessor.setUser(new UsernamePasswordAuthenticationToken(
                            principal, null, List.of()));
                    return message;
                }
            }
            // No valid token — reject connection
            throw new IllegalArgumentException("WebSocket 인증 실패");
        }
        return message;
    }
}
