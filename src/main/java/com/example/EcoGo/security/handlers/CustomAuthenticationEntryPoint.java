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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 自定义认证入口点
 * 
 * <p>当未认证的用户尝试访问受保护的资源时，此处理器会被触发。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>返回401未授权状态码</li>
 *   <li>返回统一的JSON错误响应</li>
 *   <li>记录认证失败的审计日志</li>
 *   <li>不重定向到登录页面（适用于RESTful API）</li>
 * </ul>
 * 
 * <p>典型触发场景：
 * <ul>
 *   <li>请求头中缺少Authorization</li>
 *   <li>JWT令牌已过期</li>
 *   <li>JWT令牌格式错误或签名无效</li>
 * </ul>
 * 
 * <p>响应格式：
 * <pre>
 * {
 *   "code": 401,
 *   "message": "Unauthorized: Please log in.",
 *   "data": null
 * }
 * </pre>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper;
    private final SecurityAuditLogger auditLogger;

    /**
     * 构造函数
     * 
     * @param auditLogger 安全审计日志记录器
     */
    public CustomAuthenticationEntryPoint(SecurityAuditLogger auditLogger) {
        this.objectMapper = new ObjectMapper();
        this.auditLogger = auditLogger;
    }

    /**
     * 处理认证异常
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param authException 认证异常
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        // 记录认证失败日志
        logger.warn("认证失败 - URI: {}, IP: {}, 原因: {}",
                request.getRequestURI(),
                getClientIpAddress(request),
                authException.getMessage());

        // 记录审计日志
        auditLogger.logAuthenticationFailure(
                null,
                "anonymous",
                getClientIpAddress(request),
                request.getHeader("User-Agent"),
                "认证失败: " + authException.getMessage()
        );

        // 设置响应状态和内容类型
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        // 构建错误响应
        ResponseMessage<Void> responseMessage = new ResponseMessage<>(
                ErrorCode.NOT_LOGIN.getCode(),
                "未授权：请先登录",
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
