package com.example.EcoGo.security.rememberme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * MongoDB持久化Token存储实现
 * 
 * <p>实现Spring Security的PersistentTokenRepository接口，
 * 将"记住我"Token存储到MongoDB中。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>创建新Token</li>
 *   <li>更新现有Token</li>
 *   <li>根据series加载Token</li>
 *   <li>删除用户的所有Token</li>
 * </ul>
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Component
public class MongoTokenRepositoryImpl implements PersistentTokenRepository {

    private static final Logger logger = LoggerFactory.getLogger(MongoTokenRepositoryImpl.class);

    private final com.example.EcoGo.security.rememberme.PersistentTokenRepository tokenRepository;

    /**
     * 构造函数
     * 
     * @param tokenRepository MongoDB Token存储库
     */
    public MongoTokenRepositoryImpl(
            com.example.EcoGo.security.rememberme.PersistentTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * 创建新的Token
     * 
     * @param token Spring Security的Token对象
     */
    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        logger.debug("创建新的Remember-Me Token - 用户: {}, series: {}",
                token.getUsername(), token.getSeries());

        // 计算过期时间（默认7天）
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        // 创建并保存Token
        com.example.EcoGo.security.rememberme.PersistentToken persistentToken =
                new com.example.EcoGo.security.rememberme.PersistentToken(
                        token.getUsername(),
                        token.getSeries(),
                        token.getTokenValue(),
                        expiresAt
                );

        tokenRepository.save(persistentToken);
    }

    /**
     * 更新Token值
     * 
     * <p>每次使用"记住我"功能自动登录时，都会更新tokenValue
     * 
     * @param series Token系列标识
     * @param tokenValue 新的Token值
     * @param lastUsed 最后使用时间
     */
    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        logger.debug("更新Remember-Me Token - series: {}", series);

        tokenRepository.findBySeries(series).ifPresent(token -> {
            token.setTokenValue(tokenValue);
            token.setLastUsed(LocalDateTime.ofInstant(lastUsed.toInstant(), ZoneId.systemDefault()));
            tokenRepository.save(token);
        });
    }

    /**
     * 根据series加载Token
     * 
     * @param seriesId Token系列标识
     * @return Spring Security的Token对象
     */
    @Override
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        logger.debug("加载Remember-Me Token - series: {}", seriesId);

        return tokenRepository.findBySeries(seriesId)
                .map(token -> {
                    // 检查Token是否过期
                    if (token.isExpired()) {
                        logger.warn("Remember-Me Token已过期 - series: {}", seriesId);
                        tokenRepository.deleteBySeries(seriesId);
                        return null;
                    }

                    // 转换为Spring Security的Token对象
                    Date lastUsed = Date.from(token.getLastUsed().atZone(ZoneId.systemDefault()).toInstant());
                    return new PersistentRememberMeToken(
                            token.getUsername(),
                            token.getSeries(),
                            token.getTokenValue(),
                            lastUsed
                    );
                })
                .orElse(null);
    }

    /**
     * 删除用户的所有Token
     * 
     * <p>用于用户登出或密码修改时清除所有"记住我"会话
     * 
     * @param username 用户名
     */
    @Override
    public void removeUserTokens(String username) {
        logger.debug("删除用户的所有Remember-Me Token - 用户: {}", username);
        tokenRepository.deleteByUsername(username);
    }

    /**
     * 清理过期的Token
     * 
     * <p>此方法可以定期调用以清理数据库中的过期Token
     */
    public void cleanupExpiredTokens() {
        logger.info("清理过期的Remember-Me Token");
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    /**
     * 生成唯一的series标识
     * 
     * @return series标识
     */
    public static String generateSeriesData() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成唯一的token值
     * 
     * @return token值
     */
    public static String generateTokenData() {
        return UUID.randomUUID().toString();
    }
}
