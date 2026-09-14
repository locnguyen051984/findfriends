package com.phaithanhcong.controller.user;

import com.phaithanhcong.config.RabbitConfig;
import com.phaithanhcong.dto.ChatMessageTask;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Controller
public class MessageSignalingController {

    private final RabbitTemplate rabbitTemplate;
    private final Map<Long, Long> lastSentTime = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MS = 500;

    @MessageMapping("/message.send")
    public void handleSend(Map<String, Object> payload, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());

        long now = System.currentTimeMillis();
        Long lastTime = lastSentTime.get(senderId);
        if (lastTime != null && (now - lastTime) < RATE_LIMIT_MS) {
            return;
        }
        lastSentTime.put(senderId, now);

        Long receiverId = Long.valueOf(String.valueOf(payload.get("toUserId")));
        String content = String.valueOf(payload.get("content"));

        ChatMessageTask task = new ChatMessageTask(senderId, receiverId, content);
        rabbitTemplate.convertAndSend(RabbitConfig.CHAT_QUEUE, task);
    }
}