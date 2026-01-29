package com.example.EcoGo.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

/**
 * JWT Token 验证器
 * 
 * <p>负责验证JWT令牌的有效性，提供以下功能：
 * <ul>
 *   <li>验证Token签名</li>
 *   <li>验证Token过期时间</li>
 *   <li>提取Token中的声明信息</li>
 *   <li>详细的错误日志记录</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * Claims claims = jwtTokenValidator.validateToken(token);
 * String userId = jwtTokenValidator.getUserIdFromToken(token);
 * boolean isValid = jwtTokenValidator.isTokenValid(token);
 * </pre>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Component
public class JwtTokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenValidator.class);

    /**
     * JWT签名密钥
     */
    private final Key secretKey;

    /**
     * 构造函数
     * 
     * @param jwtSecret JWT密钥（从配置文件读取，必须与JwtTokenProvider一致）
     */
    public JwtTokenValidator(@Value("${jwt.secret:}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            // 如果没有配置密钥，生成一个随机密钥（仅用于开发环境）
            // 注意：这与JwtTokenProvider中的密钥不同，生产环境必须配置
            this.secretKey = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
            logger.warn("未配置JWT密钥，使用随机生成的密钥。生产环境必须配置jwt.secret!");
        } else {
            this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        }
    }

    /**
     * 验证Token并返回Claims
     * 
     * @param token JWT令牌字符串
     * @return Claims对象，包含Token中的所有声明
     * @throws JwtException 当Token无效时抛出
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (SignatureException ex) {
            logger.error("JWT签名验证失败: {}", ex.getMessage());
            throw new JwtException("Invalid JWT signature", ex);
        } catch (MalformedJwtException ex) {
            logger.error("JWT格式错误: {}", ex.getMessage());
            throw new JwtException("Invalid JWT token", ex);
        } catch (ExpiredJwtException ex) {
            logger.error("JWT已过期: {}", ex.getMessage());
            throw new JwtException("Expired JWT token", ex);
        } catch (UnsupportedJwtException ex) {
            logger.error("不支持的JWT: {}", ex.getMessage());
            throw new JwtException("Unsupported JWT token", ex);
        } catch (IllegalArgumentException ex) {
            logger.error("JWT声明字符串为空: {}", ex.getMessage());
            throw new JwtException("JWT claims string is empty", ex);
        }
    }

    /**
     * 检查Token是否有效（不抛出异常）
     * 
     * @param token JWT令牌字符串
     * @return true表示有效，false表示无效
     */
    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 从Token中提取用户ID
     * 
     * @param token JWT令牌字符串
     * @return 用户ID（UUID）
     */
    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从Token中提取过期时间
     * 
     * @param token JWT令牌字符串
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从Token中提取签发时间
     * 
     * @param token JWT令牌字符串
     * @return 签发时间
     */
    public Date getIssuedAtFromToken(String token) {
        return getClaimFromToken(token, Claims::getIssuedAt);
    }

    /**
     * 从Token中提取指定的Claim
     * 
     * @param token JWT令牌字符串
     * @param claimsResolver Claim提取函数
     * @param <T> Claim的类型
     * @return 提取的Claim值
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = validateToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 检查Token是否已过期
     * 
     * @param token JWT令牌字符串
     * @return true表示已过期，false表示未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * 获取Token类型（access或refresh）
     * 
     * @param token JWT令牌字符串
     * @return Token类型
     */
    public String getTokenType(String token) {
        Claims claims = validateToken(token);
        return claims.get("type", String.class);
    }

    /**
     * 检查是否为管理员Token
     * 
     * @param token JWT令牌字符串
     * @return true表示管理员，false表示普通用户
     */
    public boolean isAdminToken(String token) {
        try {
            Claims claims = validateToken(token);
            Boolean isAdmin = claims.get("isAdmin", Boolean.class);
            return isAdmin != null && isAdmin;
        } catch (JwtException e) {
            return false;
        }
    }
}
