package com.phaithanhcong.findfriends.dto;

import com.phaithanhcong.findfriends.model.Admin;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminResponse {
    private Long id;
    private String adminName;
    private String email;

    public static AdminResponse fromEntity(Admin admin) {
        if (admin == null) {
            return null;
        }
        return new AdminResponse(
                admin.getId(),
                admin.getAdminName(),
                admin.getEmail()
        );
    }
}
