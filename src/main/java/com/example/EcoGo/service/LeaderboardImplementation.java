package com.example.EcoGo.service;

import com.example.EcoGo.dto.LeaderboardStatsDto;
import com.example.EcoGo.interfacemethods.LeaderboardInterface;
import com.example.EcoGo.model.Ranking;
import com.example.EcoGo.repository.RankingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LeaderboardImplementation implements LeaderboardInterface {

    @Autowired
    private RankingRepository rankingRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<Ranking> getRankingsByPeriod(String period, String name, Pageable pageable) {
        if (name != null && !name.isEmpty()) {
            return rankingRepository.findByPeriodAndNicknameContainingIgnoreCaseOrderByRankAsc(period, name, pageable);
        } else {
            return rankingRepository.findByPeriodOrderByRankAsc(period, pageable);
        }
    }

    @Override
    public LeaderboardStatsDto getRankingsAndStatsByPeriod(String period, String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        boolean hasSearchName = name != null && !name.isEmpty();

        // 1. Get the paginated results
        Page<Ranking> rankingsPage = hasSearchName
                ? rankingRepository.findByPeriodAndNicknameContainingIgnoreCaseOrderByRankAsc(period, name, pageable)
                : rankingRepository.findByPeriodOrderByRankAsc(period, pageable);

        // 2. Get all rankings for the period (without pagination) to calculate stats
        List<Ranking> allRankingsForPeriod = rankingRepository.findByPeriod(period);

        long totalCarbonSaved = 0;
        long totalVipUsers = 0;

        for (Ranking ranking : allRankingsForPeriod) {
            totalCarbonSaved += ranking.getCarbonSaved();
            if (ranking.isVip()) {
                totalVipUsers++;
            }
        }

        return new LeaderboardStatsDto(rankingsPage, totalCarbonSaved, totalVipUsers);
    }

    @Override
    public List<String> getAvailablePeriods() {
        return mongoTemplate.query(Ranking.class)
                .distinct("period")
                .as(String.class)
                .all();
    }
}
