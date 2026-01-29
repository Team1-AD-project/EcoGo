package com.example.EcoGo.security.rememberme;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 持久化Token数据访问层
 * 
 * <p>提供持久化Token的查询、保存和删除功能
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Repository
public interface PersistentTokenRepository extends MongoRepository<PersistentToken, String> {

    /**
     * 根据series查找Token
     * 
     * @param series Token系列标识
     * @return Token（如果存在）
     */
    Optional<PersistentToken> findBySeries(String series);

    /**
     * 根据用户名查找所有Token
     * 
     * @param username 用户名
     * @return Token列表
     */
    List<PersistentToken> findByUsername(String username);

    /**
     * 删除用户的所有Token
     * 
     * @param username 用户名
     */
    void deleteByUsername(String username);

    /**
     * 删除指定series的Token
     * 
     * @param series Token系列标识
     */
    void deleteBySeries(String series);

    /**
     * 删除过期的Token
     * 
     * @param now 当前时间
     */
    void deleteByExpiresAtBefore(LocalDateTime now);

    /**
     * 统计用户的Token数量
     * 
     * @param username 用户名
     * @return Token数量
     */
    long countByUsername(String username);
}
