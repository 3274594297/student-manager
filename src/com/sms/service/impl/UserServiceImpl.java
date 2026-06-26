package com.sms.service.impl;

import com.sms.dao.UserDAO;
import com.sms.dao.impl.UserDAOImpl;
import com.sms.entity.User;
import com.sms.entity.UserRole;
import com.sms.exception.AuthException;
import com.sms.exception.EntityNotFoundException;
import com.sms.service.UserService;
import com.sms.util.MD5Util;
import java.sql.Timestamp;
import java.util.List;

public class UserServiceImpl implements UserService {

    private final UserDAO userDAO = new UserDAOImpl();
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    @Override
    public User login(String username, String password) {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new AuthException("用户名不存在");
        }

        if (user.getStatus() == 0) {
            throw new AuthException("账户已被禁用，请联系管理员");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (user.getLockUntil() != null && user.getLockUntil().after(now)) {
            long remainingMins = (user.getLockUntil().getTime() - now.getTime()) / 60000 + 1;
            throw new AuthException("账户因多次登录失败被锁定，请 " + remainingMins + " 分钟后再试");
        }

        // Verify password
        String inputHash = MD5Util.md5WithSalt(password, user.getSalt());
        if (user.getPasswordHash().equals(inputHash)) {
            // Success: Reset failed attempts & lock time
            if (user.getFailedAttempts() > 0 || user.getLockUntil() != null) {
                user.setFailedAttempts(0);
                user.setLockUntil(null);
                userDAO.update(user);
            }
            return user;
        } else {
            // Failure: Increment counter
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= 5) {
                user.setLockUntil(new Timestamp(System.currentTimeMillis() + LOCK_DURATION_MS));
                userDAO.update(user);
                throw new AuthException("密码错误，连续失败达 5 次，账户已被锁定 15 分钟");
            } else {
                userDAO.update(user);
                throw new AuthException("密码错误，已失败 " + attempts + " 次，连续失败 5 次将锁定账户");
            }
        }
    }

    @Override
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new EntityNotFoundException("用户不存在");
        }

        String oldHash = MD5Util.md5WithSalt(oldPassword, user.getSalt());
        if (!user.getPasswordHash().equals(oldHash)) {
            throw new AuthException("原密码不正确");
        }

        // Generate new salt and hash for security
        String newSalt = MD5Util.generateSalt();
        user.setSalt(newSalt);
        user.setPasswordHash(MD5Util.md5WithSalt(newPassword, newSalt));
        userDAO.update(user);
    }

    @Override
    public void resetPassword(Integer targetUserId, String newPassword) {
        User user = userDAO.findById(targetUserId);
        if (user == null) {
            throw new EntityNotFoundException("目标用户不存在");
        }

        String salt = MD5Util.generateSalt();
        user.setSalt(salt);
        user.setPasswordHash(MD5Util.md5WithSalt(newPassword, salt));
        user.setFailedAttempts(0);
        user.setLockUntil(null);
        userDAO.update(user);
    }

    @Override
    public void toggleUserStatus(Integer targetUserId, Integer status) {
        User user = userDAO.findById(targetUserId);
        if (user == null) {
            throw new EntityNotFoundException("用户不存在");
        }
        user.setStatus(status);
        userDAO.update(user);
    }

    @Override
    public List<User> listAllUsers() {
        return userDAO.findAll();
    }

    @Override
    public User registerUser(String username, String password, String realName, String roleName) {
        User exist = userDAO.findByUsername(username);
        if (exist != null) {
            throw new AuthException("用户名已存在");
        }

        UserRole role = UserRole.fromString(roleName);
        if (role == null) {
            throw new IllegalArgumentException("角色不合法: " + roleName);
        }

        String salt = MD5Util.generateSalt();
        User u = new User();
        u.setUsername(username);
        u.setSalt(salt);
        u.setPasswordHash(MD5Util.md5WithSalt(password, salt));
        u.setRole(role);
        u.setRealName(realName);
        u.setStatus(1);
        u.setFailedAttempts(0);

        userDAO.insert(u);
        return u;
    }

    @Override
    public void unlockUser(Integer targetUserId) {
        User user = userDAO.findById(targetUserId);
        if (user == null) {
            throw new EntityNotFoundException("用户不存在");
        }
        user.setFailedAttempts(0);
        user.setLockUntil(null);
        userDAO.update(user);
    }
}
