package com.phaithanhcong.controller.user;

import com.phaithanhcong.service.user.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.phaithanhcong.dto.CallSignalDTO;
import jakarta.validation.Valid;

@RequiredArgsConstructor
@Controller
public class CallSignalingController {
    private final CallService callService;

    @MessageMapping("/call.signal")
    public void handleSignal(@Valid CallSignalDTO message, java.security.Principal principal) {
        if (principal == null) return;
        Long principalId = Long.valueOf(principal.getName());

        // Ghi đè fromUserId từ token (Principal), không tin tưởng payload từ client
        message.setFromUserId(principalId);

        if (principalId.equals(message.getToUserId())) {
            return;
        }

        callService.processSignal(message);
    }
}