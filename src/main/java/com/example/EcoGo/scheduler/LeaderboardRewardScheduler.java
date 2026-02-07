package com.example.EcoGo.scheduler;

import com.example.EcoGo.dto.LeaderboardEntry;
import com.example.EcoGo.interfacemethods.LeaderboardInterface;
import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.model.LeaderboardReward;
import com.example.EcoGo.repository.LeaderboardRewardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Component
public class LeaderboardRewardScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LeaderboardRewardScheduler.class);

    @Autowired
    private LeaderboardInterface leaderboardService;

    @Autowired
    private PointsService pointsService;

    @Autowired
    private LeaderboardRewardRepository rewardRepository;

    /**
     * Daily reward: runs at 00:05 every day, rewards top 10 for YESTERDAY.
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void distributeDailyRewards() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String periodKey = yesterday.toString();

        if (rewardRepository.existsByTypeAndPeriodKey("DAILY", periodKey)) {
            logger.info("Daily rewards for {} already distributed. Skipping.", periodKey);
            return;
        }

        logger.info("Distributing daily leaderboard rewards for {}", periodKey);
        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = yesterday.plusDays(1).atStartOfDay();

        List<LeaderboardEntry> topUsers = leaderboardService.getTopUsers(start, end, 10);
        distributeRewards(topUsers, "DAILY", periodKey);
    }

    /**
     * Monthly reward: runs at 00:10 on the 1st of each month, rewards top 10 for LAST MONTH.
     */
    @Scheduled(cron = "0 10 0 1 * ?")
    public void distributeMonthlyRewards() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        String periodKey = lastMonth.toString();

        if (rewardRepository.existsByTypeAndPeriodKey("MONTHLY", periodKey)) {
            logger.info("Monthly rewards for {} already distributed. Skipping.", periodKey);
            return;
        }

        logger.info("Distributing monthly leaderboard rewards for {}", periodKey);
        LocalDateTime start = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime end = lastMonth.atEndOfMonth().plusDays(1).atStartOfDay();

        List<LeaderboardEntry> topUsers = leaderboardService.getTopUsers(start, end, 10);
        distributeRewards(topUsers, "MONTHLY", periodKey);
    }

    private void distributeRewards(List<LeaderboardEntry> topUsers, String type, String periodKey) {
        for (int i = 0; i < topUsers.size(); i++) {
            int rank = i + 1;
            // Daily: 100→10 pts (×10), Monthly: 1000→100 pts (×100)
            long multiplier = "DAILY".equals(type) ? 10L : 100L;
            long points = (11 - rank) * multiplier;
            LeaderboardEntry entry = topUsers.get(i);

            try {
                String description = String.format("Leaderboard %s Rank #%d reward (%s)", type, rank, periodKey);
                pointsService.adjustPoints(
                        entry.getUserId(),
                        points,
                        "leaderboard",
                        description,
                        null,
                        null
                );

                LeaderboardReward reward = new LeaderboardReward();
                reward.setType(type);
                reward.setPeriodKey(periodKey);
                reward.setUserId(entry.getUserId());
                reward.setRank(rank);
                reward.setPointsAwarded(points);
                reward.setCarbonSaved(entry.getTotalCarbonSaved());
                reward.setDistributedAt(LocalDateTime.now());
                rewardRepository.save(reward);

                logger.info("Awarded {} points to user {} (Rank #{}) for {} {}",
                        points, entry.getUserId(), rank, type, periodKey);
            } catch (Exception e) {
                logger.error("Failed to reward user {} (Rank #{}) for {} {}: {}",
                        entry.getUserId(), rank, type, periodKey, e.getMessage());
            }
        }
        logger.info("Finished {} rewards for {}: {} users rewarded", type, periodKey, topUsers.size());
    }
}
