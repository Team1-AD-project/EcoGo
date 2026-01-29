package com.example.EcoGo.security.filters;

import com.example.EcoGo.security.audit.SecurityAuditLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 安全审计日志过滤器
 * 
 * <p>记录所有与安全相关的HTTP请求，包括：
 * <ul>
 *   <li>请求的URI和HTTP方法</li>
 *   <li>请求的IP地址和User-Agent</li>
 *   <li>认证用户信息</li>
 *   <li>请求的响应状态码</li>
 *   <li>请求处理时间</li>
 * </ul>
 * 
 * <p>此过滤器主要用于安全审计和问题排查。
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final SecurityAuditLogger auditLogger;

    /**
     * 构造函数
     * 
     * @param auditLogger 安全审计日志记录器
     */
    public AuditLoggingFilter(SecurityAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    /**
     * 执行审计日志记录
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            // 继续过滤器链
            filterChain.doFilter(request, response);
        } finally {
            // 记录请求完成后的审计日志
            long duration = System.currentTimeMillis() - startTime;
            logRequest(request, response, duration);
        }
    }

    /**
     * 记录请求日志
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param duration 请求处理时间（毫秒）
     */
    private void logRequest(HttpServletRequest request, HttpServletResponse response, long duration) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;
        String username = null;

        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            userId = authentication.getName();
            username = authentication.getName();
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();
        int statusCode = response.getStatus();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        // 记录审计日志
        auditLogger.logAccess(
                userId,
                username,
                method + " " + uri,
                ipAddress,
                userAgent,
                statusCode >= 200 && statusCode < 300,
                "HTTP " + statusCode + ", Duration: " + duration + "ms"
        );
    }

    /**
     * 获取客户端真实IP地址
     * 
     * <p>支持通过代理和负载均衡器获取真实IP
     * 
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For可能包含多个IP，取第一个
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 判断是否应该跳过此过滤器
     * 
     * @param request HTTP请求
     * @return true表示跳过，false表示执行
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 跳过静态资源和健康检查端点
        return path.startsWith("/static/") ||
               path.startsWith("/actuator/health") ||
               path.equals("/favicon.ico");
    }
}
