package com.example.EcoGo.controller;

import com.example.EcoGo.dto.LeaderboardStatsDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.LeaderboardInterface;
import com.example.EcoGo.utils.JwtUtils;
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

    @Autowired
    private JwtUtils jwtUtils;

    // === Web Endpoints (protected by SecurityConfig: ROLE_ADMIN required) ===

    @GetMapping("/web/leaderboards/periods")
    public ResponseMessage<List<String>> getWebAvailablePeriods(
            @RequestHeader("Authorization") String authHeader) {
        String adminId = extractUserId(authHeader);
        logger.info("[WEB] Admin {} fetching all available leaderboard periods", adminId);
        List<String> periods = leaderboardService.getAvailablePeriods();
        return ResponseMessage.success(periods);
    }

    @GetMapping("/web/leaderboards/rankings")
    public ResponseMessage<LeaderboardStatsDto> getWebRankingsByPeriod(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String period,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String adminId = extractUserId(authHeader);
        logger.info("[WEB] Admin {} fetching rankings for period: {} with search: '{}'", adminId, period, name);
        LeaderboardStatsDto statsDto = leaderboardService.getRankingsAndStatsByPeriod(period, name, page, size);
        return ResponseMessage.success(statsDto);
    }

    // === Mobile Endpoints (protected by SecurityConfig: authenticated required) ===

    @GetMapping("/mobile/leaderboards/periods")
    public ResponseMessage<List<String>> getMobileAvailablePeriods(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        logger.info("[Mobile] User {} fetching all available leaderboard periods", userId);
        List<String> periods = leaderboardService.getAvailablePeriods();
        return ResponseMessage.success(periods);
    }

    @GetMapping("/mobile/leaderboards/rankings")
    public ResponseMessage<LeaderboardStatsDto> getMobileRankingsByPeriod(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String period,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = extractUserId(authHeader);
        logger.info("[Mobile] User {} fetching rankings for period: {} with search: '{}'", userId, period, name);
        LeaderboardStatsDto statsDto = leaderboardService.getRankingsAndStatsByPeriod(period, name, page, size);
        return ResponseMessage.success(statsDto);
    }

    // ========== Helper ==========

    private String extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtils.getUserIdFromToken(token);
    }
}
