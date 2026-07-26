package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.User;
import jakarta.servlet.http.HttpSession;
import com.phaithanhcong.findfriends.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserRepository userRepository;

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            // Chưa đăng nhập mà cố vào /home -> đá về trang login
            return "redirect:/";
        }
        if (user.getRole() == null) {
            // User tồn tại nhưng không có role -> không cho vào trang chủ
            session.invalidate();
            return "redirect:/";
        }

        // Lấy danh sách tất cả các user khác trong DB (trừ bản thân mình)
        List<User> otherUsers = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(user.getId()))
                .toList();

        model.addAttribute("username", user.getUserName());
        model.addAttribute("otherUsers", otherUsers);
        return "home";
    }
}
