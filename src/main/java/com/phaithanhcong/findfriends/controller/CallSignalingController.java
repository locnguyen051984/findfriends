package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.service.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

@RequiredArgsConstructor
@Controller
public class CallSignalingController {
    private final CallService callService;

    @MessageMapping("/call.signal")
    public void handleSignal(Map<String, Object> message) {
        callService.processSignal(message);
    }
}