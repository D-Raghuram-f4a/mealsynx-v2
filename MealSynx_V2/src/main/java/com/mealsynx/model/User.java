package com.mealsynx.model;

import com.mealsynx.util.PasswordUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String userId;
    private String username;
    private String fullName;
    private String registerNo;
    private String email;
    private String password;      // Stores salted SHA-256 hash in V2
    private UserRole role;
    private final List<String> notifications = new ArrayList<>();

    protected User(String userId, String username, String fullName, String registerNo,
                   String email, String password, UserRole role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.registerNo = registerNo;
        this.email = email;
        this.password = (password != null && password.contains(":")) ? password : PasswordUtil.hash(password);
        this.role = role;
    }

    public abstract String getDashboardTitle();

    public void pushNotification(String message) {
        notifications.add(0, message);
    }

    public boolean verifyPassword(String rawPassword) {
        return PasswordUtil.verify(rawPassword, this.password);
    }

    public void updatePasswordWithHash(String newRawPassword) {
        this.password = PasswordUtil.hash(newRawPassword);
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRegisterNo() { return registerNo; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public void setPassword(String password) {
        this.password = (password != null && password.contains(":")) ? password : PasswordUtil.hash(password);
    }
    public UserRole getRole() { return role; }
    public List<String> getNotifications() { return notifications; }
}
