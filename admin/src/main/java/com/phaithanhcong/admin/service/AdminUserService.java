package com.phaithanhcong.admin.service;

import com.phaithanhcong.admin.model.User;
import java.util.List;

public interface AdminUserService {
    List<User> adminGetAllUsers();
    void adminTogglePremium(Long userId);
}
