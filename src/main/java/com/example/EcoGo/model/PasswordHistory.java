package com.example.EcoGo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 密码历史记录模型
 * 
 * <p>用于防止用户重复使用最近的密码
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Document(collection = "password_history")
public class PasswordHistory {

    @Id
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 加密后的密码哈希
     */
    private String passwordHash;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 默认构造函数
     */
    public PasswordHistory() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 构造函数
     */
    public PasswordHistory(String userId, String passwordHash) {
        this();
        this.userId = userId;
        this.passwordHash = passwordHash;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PasswordHistory{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
