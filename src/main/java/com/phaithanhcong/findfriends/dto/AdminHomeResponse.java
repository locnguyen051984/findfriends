package com.phaithanhcong.findfriends.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminHomeResponse {
    private String adminName;
    private List<UserResponse> users;
}