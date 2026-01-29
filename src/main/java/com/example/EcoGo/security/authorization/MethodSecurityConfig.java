package com.example.EcoGo.security.authorization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * 方法级安全配置
 * 
 * <p>启用方法级安全注解支持：
 * <ul>
 *   <li>@PreAuthorize - 方法执行前进行权限检查</li>
 *   <li>@PostAuthorize - 方法执行后进行权限检查</li>
 *   <li>@Secured - 简单的角色检查</li>
 *   <li>@RolesAllowed - JSR-250标准注解</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * // 要求ADMIN角色
 * {@code @PreAuthorize("hasRole('ADMIN')")}
 * public void deleteUser(String userId) { ... }
 * 
 * // 要求用户ID匹配或ADMIN角色
 * {@code @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")}
 * public UserProfile getProfile(String userId) { ... }
 * 
 * // 检查返回值的所有者
 * {@code @PostAuthorize("returnObject.userId == authentication.principal.id")}
 * public Order getOrder(String orderId) { ... }
 * </pre>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Configuration
@EnableMethodSecurity(
        prePostEnabled = true,    // 启用@PreAuthorize和@PostAuthorize
        securedEnabled = true,    // 启用@Secured
        jsr250Enabled = true      // 启用@RolesAllowed
)
public class MethodSecurityConfig {

    /**
     * 配置方法安全表达式处理器
     * 
     * <p>可以在这里注册自定义的权限评估器
     * 
     * @return MethodSecurityExpressionHandler
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = 
                new DefaultMethodSecurityExpressionHandler();
        
        // 可以在这里设置自定义的PermissionEvaluator
        // expressionHandler.setPermissionEvaluator(new CustomPermissionEvaluator());
        
        return expressionHandler;
    }
}
