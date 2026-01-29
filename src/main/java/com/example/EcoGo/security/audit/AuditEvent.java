package com.example.EcoGo.security.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 安全审计事件模型
 * 
 * <p>记录系统中所有与安全相关的事件，用于：
 * <ul>
 *   <li>安全审计和合规要求</li>
 *   <li>问题排查和调试</li>
 *   <li>安全分析和威胁检测</li>
 *   <li>用户行为追踪</li>
 * </ul>
 * 
 * <p>事件类型包括：
 * <ul>
 *   <li>LOGIN_SUCCESS - 登录成功</li>
 *   <li>LOGIN_FAILURE - 登录失败</li>
 *   <li>LOGOUT - 登出</li>
 *   <li>ACCESS_DENIED - 访问被拒绝</li>
 *   <li>TOKEN_REFRESH - Token刷新</li>
 *   <li>PASSWORD_CHANGE - 密码修改</li>
 *   <li>ACCOUNT_LOCKED - 账号锁定</li>
 *   <li>PERMISSION_CHANGE - 权限变更</li>
 * </ul>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Document(collection = "audit_events")
public class AuditEvent {

    /**
     * 事件ID
     */
    @Id
    private String id;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 用户ID（UUID）
     */
    private String userId;

    /**
     * 用户名或邮箱
     */
    private String username;

    /**
     * 客户端IP地址
     */
    private String ipAddress;

    /**
     * User-Agent（浏览器和操作系统信息）
     */
    private String userAgent;

    /**
     * 事件时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 事件详情描述
     */
    private String details;

    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 受影响的资源（如访问的URL）
     */
    private String resource;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 默认构造函数
     */
    public AuditEvent() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 构造函数
     */
    public AuditEvent(String eventType, String userId, String username, String ipAddress, 
                      String userAgent, boolean success, String details) {
        this();
        this.eventType = eventType;
        this.userId = userId;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.details = details;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "AuditEvent{" +
                "id='" + id + '\'' +
                ", eventType='" + eventType + '\'' +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", timestamp=" + timestamp +
                ", success=" + success +
                ", details='" + details + '\'' +
                '}';
    }

    /**
     * 审计事件类型常量
     */
    public static class EventType {
        public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
        public static final String LOGIN_FAILURE = "LOGIN_FAILURE";
        public static final String LOGOUT = "LOGOUT";
        public static final String ACCESS_DENIED = "ACCESS_DENIED";
        public static final String TOKEN_REFRESH = "TOKEN_REFRESH";
        public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";
        public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
        public static final String PERMISSION_CHANGE = "PERMISSION_CHANGE";
        public static final String AUTHENTICATION_FAILURE = "AUTHENTICATION_FAILURE";
        public static final String ACCESS = "ACCESS";
    }
}
