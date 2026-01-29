package com.example.EcoGo.security.config;

import com.example.EcoGo.security.filters.AuditLoggingFilter;
import com.example.EcoGo.security.filters.JwtAuthenticationFilter;
import com.example.EcoGo.security.handlers.CustomAccessDeniedHandler;
import com.example.EcoGo.security.handlers.CustomAuthenticationEntryPoint;
import com.example.EcoGo.security.rememberme.MongoTokenRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 核心配置
 * 
 * <p>配置内容：
 * <ul>
 *   <li>Security过滤器链</li>
 *   <li>JWT认证过滤器</li>
 *   <li>审计日志过滤器</li>
 *   <li>CORS跨域配置</li>
 *   <li>异常处理器</li>
 *   <li>会话管理策略</li>
 *   <li>端点访问权限</li>
 * </ul>
 * 
 * <p>认证方式：使用JWT令牌，无状态会话
 * <p>权限控制：
 * <ul>
 *   <li>公开端点：登录、注册</li>
 *   <li>用户端点：需要USER角色</li>
 *   <li>管理端点：需要ADMIN角色</li>
 * </ul>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    /**
     * 配置Security过滤器链
     * 
     * @param http HttpSecurity配置对象
     * @param jwtAuthenticationFilter JWT认证过滤器
     * @param auditLoggingFilter 审计日志过滤器
     * @param accessDeniedHandler 访问拒绝处理器
     * @param authenticationEntryPoint 认证入口点
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuditLoggingFilter auditLoggingFilter,
            CustomAccessDeniedHandler accessDeniedHandler,
            CustomAuthenticationEntryPoint authenticationEntryPoint) throws Exception {

        http
                // 1. CORS配置
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. 禁用CSRF（JWT方案不需要）
                .csrf(AbstractHttpConfigurer::disable)

                // 3. 禁用默认登录表单
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 4. 会话管理：无状态
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 5. 授权配置
                .authorizeHttpRequests(auth -> auth
                        // 公开端点
                        .requestMatchers(
                                "/api/v1/mobile/users/login",
                                "/api/v1/mobile/users/register",
                                "/api/v1/web/users/login"
                        ).permitAll()

                        // Swagger和Actuator端点
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // 静态资源
                        .requestMatchers(
                                "/static/**",
                                "/favicon.ico",
                                "/js/**",
                                "/assets/**"
                        ).permitAll()

                        // 移动端端点：需要认证
                        .requestMatchers("/api/v1/mobile/**").authenticated()

                        // Web管理端点：需要ADMIN角色
                        .requestMatchers("/api/v1/web/**").hasRole("ADMIN")

                        // Actuator管理端点：需要ADMIN角色
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // 其他请求：需要认证
                        .anyRequest().authenticated()
                )

                // 6. 异常处理
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint))

                // 7. 添加自定义过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(auditLoggingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS跨域配置
     * 
     * <p>允许前端应用跨域访问后端API
     * 
     * @return CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源（生产环境应该配置具体的域名）
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:8081",
                "http://127.0.0.1:8081",
                "http://localhost:3000", // React开发服务器
                "http://localhost:5173"  // Vite开发服务器
        ));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 允许携带凭证（Cookies等）
        configuration.setAllowCredentials(true);

        // 预检请求的有效期（秒）
        configuration.setMaxAge(3600L);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With"
        ));

        // 应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
