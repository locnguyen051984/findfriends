package com.phaithanhcong.findfriends.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHomeController {
    @GetMapping("/admin-home")
    public String show(){
        return "admin-home";
    }
}