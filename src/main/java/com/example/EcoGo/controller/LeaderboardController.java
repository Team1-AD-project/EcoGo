package com.example.EcoGo.controller;

import com.example.EcoGo.dto.LeaderboardStatsDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.LeaderboardInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class LeaderboardController {
    private static final Logger logger = LoggerFactory.getLogger(LeaderboardController.class);

    @Autowired
    private LeaderboardInterface leaderboardService;

    // === Web Endpoints (Admin) ===

    @GetMapping("/web/leaderboards/periods")
    public ResponseMessage<List<String>> getWebAvailablePeriods() {
        logger.info("[WEB] Fetching all available leaderboard periods");
        List<String> periods = leaderboardService.getAvailablePeriods();
        return ResponseMessage.success(periods);
    }

    @GetMapping("/web/leaderboards/rankings")
    public ResponseMessage<LeaderboardStatsDto> getWebRankingsByPeriod(
            @RequestParam String period,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("[WEB] Fetching rankings for period: {} with search: '{}'", period, name);
        LeaderboardStatsDto statsDto = leaderboardService.getRankingsAndStatsByPeriod(period, name, page, size);
        return ResponseMessage.success(statsDto);
    }

    // === Mobile Endpoints ===

    @GetMapping("/mobile/leaderboards/periods")
    public ResponseMessage<List<String>> getMobileAvailablePeriods() {
        logger.info("[Mobile] Fetching all available leaderboard periods");
        List<String> periods = leaderboardService.getAvailablePeriods();
        return ResponseMessage.success(periods);
    }

    @GetMapping("/mobile/leaderboards/rankings")
    public ResponseMessage<LeaderboardStatsDto> getMobileRankingsByPeriod(
            @RequestParam String period,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("[Mobile] Fetching rankings for period: {} with search: '{}'", period, name);
        LeaderboardStatsDto statsDto = leaderboardService.getRankingsAndStatsByPeriod(period, name, page, size);
        return ResponseMessage.success(statsDto);
    }
}
