package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.service.BrowserTrustService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/browser")
@RequiredArgsConstructor
public class BrowserController {

    private final BrowserTrustService browserTrustService;

    // POST: có thể tạo mới hoặc đổi trạng thái 1 bản ghi -> có ghi dữ liệu
    @PostMapping("/check")
    @ResponseBody
    public Map<String, String> check(@RequestParam String browserToken, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        User currentUser = getCurrentUser(session);

        if (currentUser == null) {
            response.put("status", "NOT_LOGGED_IN");
            return response;
        }

        boolean trusted = browserTrustService.checkOrRegisterBrowser(currentUser, browserToken);
        response.put("status", trusted ? "TRUSTED" : "PENDING");
        return response;
    }

    // GET: chỉ render 1 trang, không ghi gì vào CSDL
    @GetMapping("/waiting")
    public String waiting(@RequestParam String browserToken, HttpSession session, Model model) {
        if (getCurrentUser(session) == null) {
            return "redirect:/";
        }
        model.addAttribute("browserToken", browserToken);
        return "browser-waiting";
    }

    // GET: chỉ đọc trạng thái hiện có, không thay đổi gì
    @GetMapping("/status")
    @ResponseBody
    public Map<String, String> status(@RequestParam String browserToken, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        User currentUser = getCurrentUser(session);

        if (currentUser == null) {
            response.put("status", "DENIED");
            return response;
        }

        response.put("status", browserTrustService.getStatus(currentUser, browserToken).name());
        return response;
    }

    // GET: chỉ đọc danh sách đang chờ, không thay đổi gì
    @GetMapping("/pending")
    @ResponseBody
    public List<Map<String, Object>> pending(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return List.of();
        }

        return browserTrustService.getPendingRequests(currentUser).stream()
                .map(bt -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", bt.getId());
                    return m;
                })
                .toList();
    }

    // POST: ghi đè trạng thái 1 bản ghi có sẵn -> có ghi dữ liệu
    @PostMapping("/approve")
    @ResponseBody
    public Map<String, String> approve(@RequestParam Long requestId, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        if (getCurrentUser(session) == null) {
            response.put("error", "Chưa đăng nhập");
            return response;
        }
        browserTrustService.approve(requestId);
        response.put("result", "approved");
        return response;
    }

    // POST: ghi đè trạng thái 1 bản ghi có sẵn -> có ghi dữ liệu
    @PostMapping("/deny")
    @ResponseBody
    public Map<String, String> deny(@RequestParam Long requestId, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        if (getCurrentUser(session) == null) {
            response.put("error", "Chưa đăng nhập");
            return response;
        }
        browserTrustService.deny(requestId);
        response.put("result", "denied");
        return response;
    }

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}