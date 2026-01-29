package com.example.EcoGo.security.audit;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计事件数据访问层
 * 
 * <p>提供审计事件的查询和存储功能
 * 
 * @author EcoGo Team
 * @since 1.0.0
 */
@Repository
public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {

    /**
     * 根据用户ID查询审计事件
     * 
     * @param userId 用户ID
     * @return 审计事件列表
     */
    List<AuditEvent> findByUserId(String userId);

    /**
     * 根据事件类型查询
     * 
     * @param eventType 事件类型
     * @return 审计事件列表
     */
    List<AuditEvent> findByEventType(String eventType);

    /**
     * 根据时间范围查询
     * 
     * @param start 开始时间
     * @param end 结束时间
     * @return 审计事件列表
     */
    List<AuditEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 根据用户ID和时间范围查询
     * 
     * @param userId 用户ID
     * @param start 开始时间
     * @param end 结束时间
     * @return 审计事件列表
     */
    List<AuditEvent> findByUserIdAndTimestampBetween(String userId, LocalDateTime start, LocalDateTime end);

    /**
     * 根据IP地址查询
     * 
     * @param ipAddress IP地址
     * @return 审计事件列表
     */
    List<AuditEvent> findByIpAddress(String ipAddress);

    /**
     * 查询失败的事件
     * 
     * @param success 是否成功
     * @return 审计事件列表
     */
    List<AuditEvent> findBySuccess(boolean success);

    /**
     * 查询指定用户的失败登录尝试
     * 
     * @param userId 用户ID
     * @param eventType 事件类型
     * @param success 是否成功
     * @param start 开始时间
     * @return 审计事件列表
     */
    @Query("{ 'userId': ?0, 'eventType': ?1, 'success': ?2, 'timestamp': { $gte: ?3 } }")
    List<AuditEvent> findFailedAttempts(String userId, String eventType, boolean success, LocalDateTime start);

    /**
     * 删除指定时间之前的审计记录
     * 
     * @param timestamp 时间点
     */
    void deleteByTimestampBefore(LocalDateTime timestamp);

    /**
     * 统计指定时间范围内的事件数量
     * 
     * @param eventType 事件类型
     * @param start 开始时间
     * @param end 结束时间
     * @return 事件数量
     */
    long countByEventTypeAndTimestampBetween(String eventType, LocalDateTime start, LocalDateTime end);
}
