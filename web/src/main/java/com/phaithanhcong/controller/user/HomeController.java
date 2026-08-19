package com.phaithanhcong.controller.user;

import com.phaithanhcong.model.User;
import jakarta.servlet.http.HttpSession;
import com.phaithanhcong.repository.UserRepository;
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
            return "redirect:/";
        }

        List<User> otherUsers = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(user.getId()))
                .toList();

        model.addAttribute("username", user.getUserName());
        model.addAttribute("otherUsers", otherUsers);
        return "home";
    }

    
}
