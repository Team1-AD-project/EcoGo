package com.example.EcoGo.scheduler;

import com.example.EcoGo.dto.LeaderboardEntry;
import com.example.EcoGo.interfacemethods.LeaderboardInterface;
import com.example.EcoGo.model.LeaderboardReward;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.UserPointsLogRepository;
import com.example.EcoGo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for LeaderboardRewardScheduler and VipExpirationScheduler.
 */
class SchedulerCoverageTest {

    // ==================== LeaderboardRewardScheduler ====================
    private LeaderboardInterface leaderboardService;
    private UserRepository userRepository;
    private UserPointsLogRepository pointsLogRepository;
    private MongoTemplate mongoTemplate;
    private LeaderboardRewardScheduler leaderboardScheduler;

    @BeforeEach
    void setUp() throws Exception {
        leaderboardService = mock(LeaderboardInterface.class);
        userRepository = mock(UserRepository.class);
        pointsLogRepository = mock(UserPointsLogRepository.class);
        mongoTemplate = mock(MongoTemplate.class);

        leaderboardScheduler = new LeaderboardRewardScheduler();

        // Inject mocks via reflection
        setField(leaderboardScheduler, "leaderboardService", leaderboardService);
        setField(leaderboardScheduler, "userRepository", userRepository);
        setField(leaderboardScheduler, "pointsLogRepository", pointsLogRepository);
        setField(leaderboardScheduler, "mongoTemplate", mongoTemplate);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void distributeDailyRewards_alreadyDistributed_skips() {
        // Already distributed
        when(mongoTemplate.exists(any(), eq(LeaderboardReward.class))).thenReturn(true);

        leaderboardScheduler.distributeDailyRewards();

        verify(leaderboardService, never()).getTopUsers(any(), any(), anyInt());
    }

    @Test
    void distributeDailyRewards_success() {
        when(mongoTemplate.exists(any(), eq(LeaderboardReward.class))).thenReturn(false);

        // Create top users
        List<LeaderboardEntry> topUsers = new ArrayList<>();
        LeaderboardEntry entry1 = new LeaderboardEntry();
        entry1.setUserId("user1");
        entry1.setTotalCarbonSaved(50.0);
        LeaderboardEntry entry2 = new LeaderboardEntry();
        entry2.setUserId("user2");
        entry2.setTotalCarbonSaved(40.0);
        topUsers.add(entry1);
        topUsers.add(entry2);

        when(leaderboardService.getTopUsers(any(), any(), eq(10))).thenReturn(topUsers);

        User user1 = new User();
        user1.setUserid("user1");
        user1.setCurrentPoints(100);
        user1.setTotalPoints(500);
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(user1));

        User user2 = new User();
        user2.setUserid("user2");
        user2.setCurrentPoints(200);
        user2.setTotalPoints(600);
        when(userRepository.findByUserid("user2")).thenReturn(Optional.of(user2));

        leaderboardScheduler.distributeDailyRewards();

        verify(userRepository, times(2)).save(any(User.class));
        verify(pointsLogRepository, times(2)).save(any());
        verify(mongoTemplate, times(2)).save(any(LeaderboardReward.class));
    }

    @Test
    void distributeDailyRewards_userNotFound_skips() {
        when(mongoTemplate.exists(any(), eq(LeaderboardReward.class))).thenReturn(false);

        List<LeaderboardEntry> topUsers = new ArrayList<>();
        LeaderboardEntry missingEntry = new LeaderboardEntry();
        missingEntry.setUserId("missing_user");
        missingEntry.setTotalCarbonSaved(30.0);
        topUsers.add(missingEntry);

        when(leaderboardService.getTopUsers(any(), any(), eq(10))).thenReturn(topUsers);
        when(userRepository.findByUserid("missing_user")).thenReturn(Optional.empty());

        leaderboardScheduler.distributeDailyRewards();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void distributeDailyRewards_exceptionHandled() {
        when(mongoTemplate.exists(any(), eq(LeaderboardReward.class))).thenReturn(false);

        List<LeaderboardEntry> topUsers = new ArrayList<>();
        LeaderboardEntry errorEntry = new LeaderboardEntry();
        errorEntry.setUserId("user1");
        errorEntry.setTotalCarbonSaved(50.0);
        topUsers.add(errorEntry);

        when(leaderboardService.getTopUsers(any(), any(), eq(10))).thenReturn(topUsers);
        when(userRepository.findByUserid("user1")).thenThrow(new RuntimeException("DB error"));

        // Should not throw, just log error
        leaderboardScheduler.distributeDailyRewards();
    }

    @Test
    void distributeMonthlyRewards_alreadyDistributed_skips() {
        when(mongoTemplate.exists(any(), eq(LeaderboardReward.class))).thenReturn(true);

        leaderboardScheduler.distributeMonthlyRewards();

        verify(leaderboardService, never()).getTopUsers(any(), any(), anyInt());
    }

    @Test
    void distributeMonthlyRewards_success() {
        when(mongoTemplate.exists(any(), eq(LeaderboardReward.class))).thenReturn(false);

        List<LeaderboardEntry> topUsers = new ArrayList<>();
        LeaderboardEntry monthlyEntry = new LeaderboardEntry();
        monthlyEntry.setUserId("user1");
        monthlyEntry.setTotalCarbonSaved(100.0);
        topUsers.add(monthlyEntry);

        when(leaderboardService.getTopUsers(any(), any(), eq(10))).thenReturn(topUsers);

        User user1 = new User();
        user1.setUserid("user1");
        user1.setCurrentPoints(500);
        user1.setTotalPoints(2000);
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(user1));

        leaderboardScheduler.distributeMonthlyRewards();

        verify(userRepository).save(any(User.class));
        verify(mongoTemplate).save(any(LeaderboardReward.class));
    }

    // ==================== VipExpirationScheduler ====================
    @Test
    void checkVipExpiration_noExpiredVips() throws Exception {
        UserRepository vipUserRepo = mock(UserRepository.class);
        VipExpirationScheduler vipScheduler = new VipExpirationScheduler();
        setField(vipScheduler, "userRepository", vipUserRepo);

        when(vipUserRepo.findByVipIsActiveTrueAndVipExpiryDateBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());

        vipScheduler.checkVipExpiration();

        verify(vipUserRepo, never()).save(any(User.class));
    }

    @Test
    void checkVipExpiration_deactivatesExpiredVips() throws Exception {
        UserRepository vipUserRepo = mock(UserRepository.class);
        VipExpirationScheduler vipScheduler = new VipExpirationScheduler();
        setField(vipScheduler, "userRepository", vipUserRepo);

        User expiredUser = new User();
        expiredUser.setUserid("user1");
        User.Vip vip = new User.Vip();
        vip.setActive(true);
        expiredUser.setVip(vip);

        when(vipUserRepo.findByVipIsActiveTrueAndVipExpiryDateBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(expiredUser));

        vipScheduler.checkVipExpiration();

        verify(vipUserRepo).save(expiredUser);
        assertFalse(expiredUser.getVip().isActive());
    }

    @Test
    void checkVipExpiration_nullVip_skips() throws Exception {
        UserRepository vipUserRepo = mock(UserRepository.class);
        VipExpirationScheduler vipScheduler = new VipExpirationScheduler();
        setField(vipScheduler, "userRepository", vipUserRepo);

        User userWithNullVip = new User();
        userWithNullVip.setUserid("user2");
        userWithNullVip.setVip(null);

        when(vipUserRepo.findByVipIsActiveTrueAndVipExpiryDateBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(userWithNullVip));

        vipScheduler.checkVipExpiration();

        verify(vipUserRepo, never()).save(any());
    }

    @Test
    void checkVipExpiration_exceptionHandled() throws Exception {
        UserRepository vipUserRepo = mock(UserRepository.class);
        VipExpirationScheduler vipScheduler = new VipExpirationScheduler();
        setField(vipScheduler, "userRepository", vipUserRepo);

        User user = new User();
        user.setUserid("user3");
        User.Vip vip = new User.Vip();
        vip.setActive(true);
        user.setVip(vip);

        when(vipUserRepo.findByVipIsActiveTrueAndVipExpiryDateBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(user));
        when(vipUserRepo.save(any())).thenThrow(new RuntimeException("DB error"));

        // Should not throw
        vipScheduler.checkVipExpiration();
    }
}
