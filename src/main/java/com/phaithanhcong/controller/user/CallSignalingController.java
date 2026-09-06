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
        Long principalId = Long.valueOf(principal.getName());
        Long fromUserId = Long.valueOf(String.valueOf(message.get("fromUserId")));
        if (!principalId.equals(fromUserId)) {
            return; // ignore invalid sender
        }
        callService.userProcessSignal(message);
    }
}