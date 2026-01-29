package com.example.EcoGo.security.rememberme;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 持久化Token模型
 * 
 * <p>用于"记住我"功能的Token存储。
 * 
 * <p>Token包含两部分：
 * <ul>
 *   <li><b>series</b>：标识一个登录会话，在自动登录过程中保持不变</li>
 *   <li><b>tokenValue</b>：每次使用后都会更新的随机值</li>
 * </ul>
 * 
 * <p>这种双Token机制可以防止Token被盗用：
 * 如果检测到使用了旧的tokenValue，说明Token可能被盗，系统会立即删除该series的所有Token。
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Document(collection = "persistent_tokens")
public class PersistentToken {

    /**
     * MongoDB文档ID
     */
    @Id
    private String id;

    /**
     * 用户名或用户ID
     */
    private String username;

    /**
     * Token系列标识（在整个记住我会话中不变）
     */
    private String series;

    /**
     * Token值（每次使用后更新）
     */
    private String tokenValue;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsed;

    /**
     * Token创建时间
     */
    private LocalDateTime createdAt;

    /**
     * Token过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 默认构造函数
     */
    public PersistentToken() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 构造函数
     */
    public PersistentToken(String username, String series, String tokenValue, LocalDateTime expiresAt) {
        this();
        this.username = username;
        this.series = series;
        this.tokenValue = tokenValue;
        this.lastUsed = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * 检查Token是否过期
     * 
     * @return true表示已过期
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "PersistentToken{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", series='" + series + '\'' +
                ", lastUsed=" + lastUsed +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
