package com.phaithanhcong.findfriends.dto;

import com.phaithanhcong.findfriends.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String userName;
    private String email;
    private boolean premium;

    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.isPremium()
        );
    }
}
