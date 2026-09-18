package com.phaithanhcong.controller.user;

import com.phaithanhcong.model.Message;
import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.UserRepository;
import com.phaithanhcong.service.user.MessageService;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/{otherUserId}")
    public String showConversation(
            @PathVariable Long otherUserId,
            HttpSession session,
            Model model) {

        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/";
        }

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        List<Map<String, Object>> timeline = messageService.userBuildTimeline(currentUser, otherUser);

        model.addAttribute("timeline", timeline);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherUser", otherUser);

        return "message";
    }

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    @PostMapping("/{otherUserId}/image")
    @ResponseBody
    public Map<String, Object> sendImage(
            @PathVariable Long otherUserId,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            throw new RuntimeException("Chưa đăng nhập");
        }

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Message saved = messageService.userSendImage(currentUser, otherUser, file);

        Map<String, Object> outgoing = new HashMap<>();
        outgoing.put("type", "IMAGE");
        outgoing.put("senderId", saved.getSenderId());
        outgoing.put("receiverId", saved.getReceiverId());
        outgoing.put("imageUrl", saved.getImageUrl());
        outgoing.put("sentAt", saved.getSentAt().toString());

        messagingTemplate.convertAndSendToUser(String.valueOf(otherUserId), "/queue/message", outgoing);
        messagingTemplate.convertAndSendToUser(String.valueOf(currentUser.getId()), "/queue/message", outgoing);

        return outgoing;
    }
}