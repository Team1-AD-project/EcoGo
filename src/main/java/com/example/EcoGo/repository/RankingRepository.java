package com.example.EcoGo.repository;

import com.example.EcoGo.model.Ranking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RankingRepository extends MongoRepository<Ranking, String> {
    Page<Ranking> findByPeriodOrderByRankAsc(String period, Pageable pageable);
    Page<Ranking> findByPeriodAndNicknameContainingIgnoreCaseOrderByRankAsc(String period, String nickname, Pageable pageable);

    // For statistics
    List<Ranking> findByPeriod(String period);
    List<Ranking> findByPeriodAndNicknameContainingIgnoreCase(String period, String nickname);
}
