package com.example.EcoGo.security.filters;

import com.example.EcoGo.security.jwt.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证过滤器
 * 
 * <p>Spring Security 过滤器链中的核心组件，负责：
 * <ul>
 *   <li>从HTTP请求头中提取JWT令牌</li>
 *   <li>验证JWT令牌的有效性</li>
 *   <li>从令牌中提取用户信息和角色</li>
 *   <li>将认证信息设置到SecurityContext中</li>
 * </ul>
 * 
 * <p>过滤器执行流程：
 * <ol>
 *   <li>检查Authorization头是否存在且格式正确</li>
 *   <li>提取Bearer Token</li>
 *   <li>验证Token有效性</li>
 *   <li>提取用户ID和角色信息</li>
 *   <li>创建Authentication对象</li>
 *   <li>设置到SecurityContext</li>
 *   <li>继续过滤器链</li>
 * </ol>
 * 
 * <p>请求头格式：
 * <pre>
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * </pre>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenValidator jwtTokenValidator;

    /**
     * 构造函数
     * 
     * @param jwtTokenValidator JWT令牌验证器
     */
    public JwtAuthenticationFilter(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    /**
     * 执行过滤逻辑
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

        // 1. 提取Authorization头
        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // 2. 检查头是否存在且格式正确
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. 提取Token（去除"Bearer "前缀）
            final String token = authHeader.substring(BEARER_PREFIX.length());

            // 4. 验证Token并提取Claims
            Claims claims = jwtTokenValidator.validateToken(token);
            String userId = claims.getSubject();
            Boolean isAdmin = claims.get("isAdmin", Boolean.class);

            // 5. 确保用户ID存在且SecurityContext中没有认证信息
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 6. 根据isAdmin声明构建权限列表
                List<SimpleGrantedAuthority> authorities = (isAdmin != null && isAdmin)
                        ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

                // 7. 创建认证令牌
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities);

                // 8. 设置请求详情
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 9. 将认证信息设置到SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);

                logger.debug("用户 {} 认证成功，角色：{}", userId, authorities);
            }
        } catch (Exception e) {
            // Token验证失败，记录日志但不中断过滤器链
            // 请求将以匿名用户身份继续，如果路径需要认证，SecurityFilterChain会阻止访问
            logger.error("JWT认证失败 - URI: {}, 错误: {}", 
                    request.getRequestURI(), e.getMessage());
        }

        // 10. 继续过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否应该跳过此过滤器
     * 
     * <p>可以重写此方法来排除某些路径（如静态资源）
     * 
     * @param request HTTP请求
     * @return true表示跳过，false表示执行
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 跳过公开端点的过滤
        return path.startsWith("/api/v1/mobile/users/login") ||
               path.startsWith("/api/v1/mobile/users/register") ||
               path.startsWith("/api/v1/web/users/login") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/");
    }
}
