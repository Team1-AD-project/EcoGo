package com.example.EcoGo.security.handlers;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.security.audit.SecurityAuditLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 自定义访问拒绝处理器
 * 
 * <p>当已认证的用户尝试访问没有权限的资源时，此处理器会被触发。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>返回403禁止访问状态码</li>
 *   <li>返回统一的JSON错误响应</li>
 *   <li>记录访问拒绝的审计日志</li>
 *   <li>区分认证失败（401）和权限不足（403）</li>
 * </ul>
 * 
 * <p>典型触发场景：
 * <ul>
 *   <li>普通用户尝试访问管理员端点</li>
 *   <li>用户尝试访问其他用户的私有资源</li>
 *   <li>角色权限不满足方法级安全注解要求</li>
 * </ul>
 * 
 * <p>响应格式：
 * <pre>
 * {
 *   "code": 403,
 *   "message": "Access Denied: You do not have permission to access this resource.",
 *   "data": null
 * }
 * </pre>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;
    private final SecurityAuditLogger auditLogger;

    /**
     * 构造函数
     * 
     * @param auditLogger 安全审计日志记录器
     */
    public CustomAccessDeniedHandler(SecurityAuditLogger auditLogger) {
        this.objectMapper = new ObjectMapper();
        this.auditLogger = auditLogger;
    }

    /**
     * 处理访问拒绝异常
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param accessDeniedException 访问拒绝异常
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // 获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication != null ? authentication.getName() : "unknown";
        String authorities = authentication != null ? authentication.getAuthorities().toString() : "[]";

        // 记录访问拒绝日志
        logger.warn("访问被拒绝 - 用户: {}, 权限: {}, URI: {}, IP: {}, 原因: {}",
                userId,
                authorities,
                request.getRequestURI(),
                getClientIpAddress(request),
                accessDeniedException.getMessage());

        // 记录审计日志
        auditLogger.logAccessDenied(
                userId,
                userId,
                request.getRequestURI(),
                getClientIpAddress(request),
                request.getHeader("User-Agent"),
                "权限不足: " + accessDeniedException.getMessage()
        );

        // 设置响应状态和内容类型
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        // 构建错误响应
        ResponseMessage<Void> responseMessage = new ResponseMessage<>(
                ErrorCode.NO_PERMISSION.getCode(),
                "访问被拒绝：您没有权限访问此资源",
                null
        );

        // 写入响应
        response.getWriter().write(objectMapper.writeValueAsString(responseMessage));
    }

    /**
     * 获取客户端IP地址
     * 
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果通过多个代理，X-Forwarded-For会包含多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
