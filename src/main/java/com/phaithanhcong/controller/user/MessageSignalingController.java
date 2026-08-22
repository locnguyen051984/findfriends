package com.phaithanhcong.controller.user;

import com.phaithanhcong.model.Message;
import com.phaithanhcong.model.User;
import com.phaithanhcong.service.user.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Controller
public class MessageSignalingController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/message.send")
    public void handleSend(Map<String, Object> payload, Principal principal) {
        // senderId always trusted from Principal (session), never from client payload
        Long senderId = Long.valueOf(principal.getName());
        Long receiverId = Long.valueOf(String.valueOf(payload.get("toUserId")));
        String content = String.valueOf(payload.get("content"));

        User sender = User.builder().id(senderId).build();
        User receiver = User.builder().id(receiverId).build();

        Message saved = messageService.userSendMessage(sender, receiver, content);

        Map<String, Object> outgoing = new HashMap<>();
        outgoing.put("type", "MESSAGE");
        outgoing.put("senderId", saved.getSenderId());
        outgoing.put("receiverId", saved.getReceiverId());
        outgoing.put("content", saved.getContent());
        outgoing.put("sentAt", saved.getSentAt().toString());

        messagingTemplate.convertAndSendToUser(String.valueOf(receiverId), "/queue/message", outgoing);
        messagingTemplate.convertAndSendToUser(String.valueOf(senderId), "/queue/message", outgoing);
    }
}