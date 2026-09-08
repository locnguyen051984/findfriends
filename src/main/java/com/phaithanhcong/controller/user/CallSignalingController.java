package com.phaithanhcong.controller.user;

import com.phaithanhcong.service.user.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

@RequiredArgsConstructor
@Controller
public class CallSignalingController {
    private final CallService callService;

    @MessageMapping("/call.signal")
    public void handleSignal(Map<String, Object> message, java.security.Principal principal) {
        if (principal == null) return;
        Long principalId = Long.valueOf(principal.getName());

        // Ghi đè fromUserId từ token (Principal), không tin tưởng payload từ client
        message.put("fromUserId", principalId);

        // Validate toUserId
        if (message.get("toUserId") == null) {
            return;
        }

        callService.userProcessSignal(message);
    }
}