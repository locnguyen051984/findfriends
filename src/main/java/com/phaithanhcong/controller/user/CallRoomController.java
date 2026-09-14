package com.phaithanhcong.controller.user;

import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@RequiredArgsConstructor
@Controller
public class CallRoomController {

    private final UserRepository userRepository;

    @GetMapping("/call-room")
    public String getCallRoom(
            @RequestParam("action") String action,
            @RequestParam("type") String type,
            @RequestParam(value = "calleeId", required = false) Long calleeId,
            @RequestParam(value = "callerId", required = false) Long callerId,
            @RequestParam(value = "callLogId", required = false) Long callLogId,
            HttpSession session,
            Model model) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Long otherUserId = "start".equals(action) ? calleeId : callerId;
        if (otherUserId == null) {
            return "redirect:/home";
        }

        User otherUser = userRepository.findById(otherUserId).orElse(null);
        if (otherUser == null) {
            return "redirect:/home";
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherUser", otherUser);
        model.addAttribute("callAction", action);
        model.addAttribute("callType", type);
        model.addAttribute("callLogId", callLogId);

        return "call-room";
    }
}
