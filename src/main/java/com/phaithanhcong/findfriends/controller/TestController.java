package com.phaithanhcong.findfriends.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {
    @GetMapping("/")
    public String home() {
        return "index"; // tên file index.html trong templates/
    }
}
