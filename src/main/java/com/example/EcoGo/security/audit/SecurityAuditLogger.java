package com.example.EcoGo.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 安全审计日志记录器
 * 
 * <p>提供统一的安全审计日志接口，所有安全相关的操作都应该通过此类记录。
 * 
 * <p>特性：
 * <ul>
 *   <li>异步记录，不影响主流程性能</li>
 *   <li>结构化日志存储到MongoDB</li>
 *   <li>同时记录到应用日志</li>
 *   <li>支持审计日志保留期配置</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * auditLogger.logLoginSuccess("userId", "user@example.com", "192.168.1.1", "Chrome/Win10");
 * auditLogger.logAccessDenied("userId", "user@example.com", "/api/admin", "192.168.1.1", "Chrome/Win10", "权限不足");
 * </pre>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Component
public class SecurityAuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditLogger.class);

    private final AuditEventRepository auditEventRepository;

    /**
     * 是否启用审计日志
     */
    @Value("${audit.enabled:true}")
    private boolean auditEnabled;

    /**
     * 是否异步记录
     */
    @Value("${audit.async:true}")
    private boolean asyncEnabled;

    /**
     * 构造函数
     * 
     * @param auditEventRepository 审计事件存储库
     */
    public SecurityAuditLogger(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * 记录登录成功
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     */
    @Async
    public void logLoginSuccess(String userId, String username, String ipAddress, String userAgent) {
        logEvent(AuditEvent.EventType.LOGIN_SUCCESS, userId, username, ipAddress, userAgent, true, "用户登录成功");
    }

    /**
     * 记录登录失败
     * 
     * @param userId 用户ID（可能为null）
     * @param username 用户名
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     * @param reason 失败原因
     */
    @Async
    public void logLoginFailure(String userId, String username, String ipAddress, String userAgent, String reason) {
        logEvent(AuditEvent.EventType.LOGIN_FAILURE, userId, username, ipAddress, userAgent, false, reason);
    }

    /**
     * 记录登出
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     */
    @Async
    public void logLogout(String userId, String username, String ipAddress, String userAgent) {
        logEvent(AuditEvent.EventType.LOGOUT, userId, username, ipAddress, userAgent, true, "用户登出");
    }

    /**
     * 记录访问被拒绝
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param resource 受保护资源
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     * @param reason 拒绝原因
     */
    @Async
    public void logAccessDenied(String userId, String username, String resource, String ipAddress, 
                                String userAgent, String reason) {
        AuditEvent event = new AuditEvent(
                AuditEvent.EventType.ACCESS_DENIED,
                userId,
                username,
                ipAddress,
                userAgent,
                false,
                reason
        );
        event.setResource(resource);
        saveEvent(event);
    }

    /**
     * 记录认证失败
     * 
     * @param userId 用户ID（可能为null）
     * @param username 用户名
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     * @param reason 失败原因
     */
    @Async
    public void logAuthenticationFailure(String userId, String username, String ipAddress, 
                                         String userAgent, String reason) {
        logEvent(AuditEvent.EventType.AUTHENTICATION_FAILURE, userId, username, ipAddress, userAgent, false, reason);
    }

    /**
     * 记录密码修改
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     */
    @Async
    public void logPasswordChange(String userId, String username, String ipAddress, String userAgent) {
        logEvent(AuditEvent.EventType.PASSWORD_CHANGE, userId, username, ipAddress, userAgent, true, "密码已修改");
    }

    /**
     * 记录Token刷新
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     */
    @Async
    public void logTokenRefresh(String userId, String username, String ipAddress, String userAgent) {
        logEvent(AuditEvent.EventType.TOKEN_REFRESH, userId, username, ipAddress, userAgent, true, "Token已刷新");
    }

    /**
     * 记录权限变更
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     * @param details 变更详情
     */
    @Async
    public void logPermissionChange(String userId, String username, String ipAddress, 
                                     String userAgent, String details) {
        logEvent(AuditEvent.EventType.PERMISSION_CHANGE, userId, username, ipAddress, userAgent, true, details);
    }

    /**
     * 记录资源访问
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param resource 访问的资源
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     * @param success 是否成功
     * @param details 详情
     */
    @Async
    public void logAccess(String userId, String username, String resource, String ipAddress, 
                          String userAgent, boolean success, String details) {
        AuditEvent event = new AuditEvent(
                AuditEvent.EventType.ACCESS,
                userId,
                username,
                ipAddress,
                userAgent,
                success,
                details
        );
        event.setResource(resource);
        saveEvent(event);
    }

    /**
     * 通用审计事件记录方法
     * 
     * @param eventType 事件类型
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     * @param success 是否成功
     * @param details 详情
     */
    private void logEvent(String eventType, String userId, String username, String ipAddress, 
                         String userAgent, boolean success, String details) {
        AuditEvent event = new AuditEvent(
                eventType,
                userId,
                username,
                ipAddress,
                userAgent,
                success,
                details
        );
        saveEvent(event);
    }

    /**
     * 保存审计事件
     * 
     * @param event 审计事件
     */
    private void saveEvent(AuditEvent event) {
        if (!auditEnabled) {
            return;
        }

        try {
            // 记录到应用日志
            if (event.isSuccess()) {
                logger.info("审计事件: {} - 用户: {}, IP: {}, 详情: {}",
                        event.getEventType(), event.getUsername(), event.getIpAddress(), event.getDetails());
            } else {
                logger.warn("审计事件（失败）: {} - 用户: {}, IP: {}, 详情: {}",
                        event.getEventType(), event.getUsername(), event.getIpAddress(), event.getDetails());
            }

            // 保存到数据库
            auditEventRepository.save(event);
        } catch (Exception e) {
            // 审计日志记录失败不应该影响主流程
            logger.error("保存审计事件失败: {}", e.getMessage(), e);
        }
    }
}
