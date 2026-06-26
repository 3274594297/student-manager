package com.sms.service;

import com.sms.entity.User;
import java.util.List;

public interface UserService {
    User login(String username, String password);
    void changePassword(Integer userId, String oldPassword, String newPassword);
    void resetPassword(Integer targetUserId, String newPassword);
    void toggleUserStatus(Integer targetUserId, Integer status);
    List<User> listAllUsers();
    User registerUser(String username, String password, String realName, String roleName);
    void unlockUser(Integer targetUserId);
}
