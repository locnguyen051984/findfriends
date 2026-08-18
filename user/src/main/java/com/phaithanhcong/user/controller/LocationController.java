package com.phaithanhcong.user.controller;

import com.phaithanhcong.user.model.User;
import com.phaithanhcong.user.repository.UserRepository;
import com.phaithanhcong.user.service.LocationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final UserRepository userRepository;

    @PostMapping("/record")
    @ResponseBody
    public Map<String, String> record(@RequestParam double latitude,
                                       @RequestParam double longitude,
                                       HttpSession session) {

        Map<String, String> response = new HashMap<>();
        User currentUser = getCurrentUser(session);

        if (currentUser == null) {
            response.put("error", "Chưa đăng nhập");
            return response;
        }

        locationService.recordLoginAndCheckAnomaly(currentUser, latitude, longitude)
                .ifPresent(warning -> response.put("warning", warning));

        return response;
    }

    @GetMapping("/distances")
    @ResponseBody
    public List<Map<String, Object>> distances(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return List.of();
        }

        List<User> otherUsers = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .toList();

        return locationService.getNearbyList(currentUser, otherUsers);
    }

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}