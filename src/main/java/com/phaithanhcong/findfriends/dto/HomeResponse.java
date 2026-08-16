package com.phaithanhcong.findfriends.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class HomeResponse {
    private String username;
    private List<UserResponse> otherUsers;
}