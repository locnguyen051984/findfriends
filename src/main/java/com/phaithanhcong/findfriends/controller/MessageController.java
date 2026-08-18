package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.repository.UserRepository;
import com.phaithanhcong.findfriends.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

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

        List<Map<String, Object>> timeline = messageService.buildTimeline(currentUser, otherUser);

        model.addAttribute("timeline", timeline);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherUser", otherUser);

        return "message";
    }

    @PostMapping("/{otherUserId}")
    public String sendMessage(
            @PathVariable Long otherUserId,
            @RequestParam String content,
            HttpSession session,
            Model model) {

        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/";
        }

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        try {
            messageService.sendMessage(currentUser, otherUser, content);
            return "redirect:/messages/" + otherUserId;
        } catch (RuntimeException e) {
            List<Map<String, Object>> timeline = messageService.buildTimeline(currentUser, otherUser);
            model.addAttribute("timeline", timeline);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("otherUser", otherUser);
            model.addAttribute("error", e.getMessage());
            return "message";
        }
    }

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}