package com.example.EcoGo.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 生成器
 * 
 * <p>负责生成JWT访问令牌和刷新令牌，支持以下功能：
 * <ul>
 *   <li>生成访问令牌（Access Token）</li>
 *   <li>生成刷新令牌（Refresh Token）</li>
 *   <li>在Token中嵌入用户信息和角色</li>
 *   <li>支持可配置的过期时间</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * String accessToken = jwtTokenProvider.generateAccessToken("userId123", true);
 * String refreshToken = jwtTokenProvider.generateRefreshToken("userId123");
 * </pre>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    /**
     * JWT签名密钥
     * 在生产环境中，必须通过环境变量配置
     */
    private final Key secretKey;

    /**
     * 访问令牌有效期（毫秒）
     * 默认：7天
     */
    @Value("${jwt.expiration:604800000}")
    private long accessTokenExpiration;

    /**
     * 刷新令牌有效期（毫秒）
     * 默认：30天
     */
    @Value("${jwt.refresh-expiration:2592000000}")
    private long refreshTokenExpiration;

    /**
     * 构造函数
     * 
     * @param jwtSecret JWT密钥（从配置文件读取）
     */
    public JwtTokenProvider(@Value("${jwt.secret:}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            // 如果没有配置密钥，生成一个随机密钥（仅用于开发环境）
            this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            logger.warn("未配置JWT密钥，使用随机生成的密钥。生产环境必须配置jwt.secret!");
        } else {
            // 使用配置的密钥
            this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        }
    }

    /**
     * 生成访问令牌（Access Token）
     * 
     * @param userId 用户ID（UUID）
     * @param isAdmin 是否为管理员
     * @return JWT访问令牌字符串
     */
    public String generateAccessToken(String userId, boolean isAdmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("isAdmin", isAdmin);
        claims.put("type", "access");

        return buildToken(claims, userId, accessTokenExpiration);
    }

    /**
     * 生成刷新令牌（Refresh Token）
     * 
     * @param userId 用户ID（UUID）
     * @return JWT刷新令牌字符串
     */
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return buildToken(claims, userId, refreshTokenExpiration);
    }

    /**
     * 构建JWT令牌
     * 
     * @param claims 自定义声明
     * @param subject 主题（用户ID）
     * @param expiration 过期时间（毫秒）
     * @return JWT令牌字符串
     */
    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 获取访问令牌过期时间（毫秒）
     * 
     * @return 访问令牌过期时间
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * 获取刷新令牌过期时间（毫秒）
     * 
     * @return 刷新令牌过期时间
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
