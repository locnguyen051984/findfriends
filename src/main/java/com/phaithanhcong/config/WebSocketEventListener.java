package com.phaithanhcong.config;

import com.phaithanhcong.service.user.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final CallService callService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        if (principal != null && principal.getName() != null) {
            try {
                Long userId = Long.valueOf(principal.getName());
                // Giải phóng trạng thái bận khi người dùng rớt kết nối
                callService.releaseUserBusyState(userId);
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
