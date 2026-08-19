package com.phaithanhcong.service.admin;

import com.phaithanhcong.model.User;
import java.util.List;

public interface AdminUserService {
    List<User> adminGetAllUsers();
    void adminTogglePremium(Long userId);
}
