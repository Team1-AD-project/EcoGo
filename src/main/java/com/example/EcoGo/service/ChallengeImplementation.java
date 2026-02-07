package com.example.EcoGo.service;

import com.example.EcoGo.dto.UserChallengeProgressDTO;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.ChallengeInterface;
import com.example.EcoGo.model.Challenge;
import com.example.EcoGo.model.Trip;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserChallengeProgress;
import com.example.EcoGo.repository.ChallengeRepository;
import com.example.EcoGo.repository.TripRepository;
import com.example.EcoGo.repository.UserChallengeProgressRepository;
import com.example.EcoGo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChallengeImplementation implements ChallengeInterface {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private UserChallengeProgressRepository userChallengeProgressRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Challenge> getAllChallenges() {
        List<Challenge> challenges = challengeRepository.findAll();
        // 实时计算每个挑战的参与人数
        for (Challenge challenge : challenges) {
            long participantCount = userChallengeProgressRepository.countByChallengeId(challenge.getId());
            challenge.setParticipants((int) participantCount);
        }
        return challenges;
    }

    @Override
    public Challenge getChallengeById(String id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));
    }

    @Override
    public Challenge createChallenge(Challenge challenge) {
        challenge.setCreatedAt(LocalDateTime.now());
        challenge.setUpdatedAt(LocalDateTime.now());
        if (challenge.getStatus() == null) challenge.setStatus("ACTIVE");
        if (challenge.getParticipants() == null) challenge.setParticipants(0);
        if (challenge.getIcon() == null) challenge.setIcon("🏆");
        return challengeRepository.save(challenge);
    }

    @Override
    public Challenge updateChallenge(String id, Challenge challenge) {
        Challenge existing = challengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        if (challenge.getTitle() != null) existing.setTitle(challenge.getTitle());
        if (challenge.getDescription() != null) existing.setDescription(challenge.getDescription());
        if (challenge.getType() != null) existing.setType(challenge.getType());
        if (challenge.getTarget() != null) existing.setTarget(challenge.getTarget());
        if (challenge.getReward() != null) existing.setReward(challenge.getReward());
        if (challenge.getBadge() != null) existing.setBadge(challenge.getBadge());
        if (challenge.getIcon() != null) existing.setIcon(challenge.getIcon());
        if (challenge.getStatus() != null) existing.setStatus(challenge.getStatus());
        if (challenge.getStartTime() != null) existing.setStartTime(challenge.getStartTime());
        if (challenge.getEndTime() != null) existing.setEndTime(challenge.getEndTime());
        existing.setUpdatedAt(LocalDateTime.now());

        return challengeRepository.save(existing);
    }

    @Override
    public void deleteChallenge(String id) {
        if (!challengeRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        // 同时删除相关的用户参与记录
        userChallengeProgressRepository.deleteByChallengeId(id);
        challengeRepository.deleteById(id);
    }

    @Override
    public List<Challenge> getChallengesByStatus(String status) {
        return challengeRepository.findByStatus(status);
    }

    @Override
    public List<Challenge> getChallengesByType(String type) {
        return challengeRepository.findByType(type);
    }

    @Override
    public List<Challenge> getChallengesByUserId(String userId) {
        // 从UserChallengeProgress获取用户参与的挑战ID列表
        List<UserChallengeProgress> userProgress = userChallengeProgressRepository.findByUserId(userId);
        List<String> challengeIds = userProgress.stream()
                .map(UserChallengeProgress::getChallengeId)
                .collect(Collectors.toList());

        // 获取对应的挑战
        return challengeRepository.findAllById(challengeIds);
    }

    @Override
    public UserChallengeProgress joinChallenge(String challengeId, String userId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        // 检查挑战是否有效
        if (!"ACTIVE".equals(challenge.getStatus())) {
            throw new BusinessException(ErrorCode.CHALLENGE_NOT_ACTIVE);
        }

        // 检查是否已过期
        if (challenge.getEndTime() != null && challenge.getEndTime().isBefore(LocalDateTime.now())) {
            challenge.setStatus("EXPIRED");
            challengeRepository.save(challenge);
            throw new BusinessException(ErrorCode.CHALLENGE_EXPIRED);
        }

        // 检查是否已参加
        if (userChallengeProgressRepository.existsByChallengeIdAndUserId(challengeId, userId)) {
            throw new BusinessException(ErrorCode.CHALLENGE_ALREADY_JOINED);
        }

        // 创建用户参与记录
        UserChallengeProgress progress = new UserChallengeProgress();
        progress.setChallengeId(challengeId);
        progress.setUserId(userId);
        progress.setStatus("IN_PROGRESS");
        progress.setJoinedAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());

        UserChallengeProgress saved = userChallengeProgressRepository.save(progress);

        // 更新挑战参与人数
        challenge.setParticipants(challenge.getParticipants() + 1);
        challenge.setUpdatedAt(LocalDateTime.now());
        challengeRepository.save(challenge);

        return saved;
    }

    @Override
    public void leaveChallenge(String challengeId, String userId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        UserChallengeProgress progress = userChallengeProgressRepository
                .findByChallengeIdAndUserId(challengeId, userId)
                .orElse(null);

        if (progress != null) {
            userChallengeProgressRepository.delete(progress);

            // 更新挑战参与人数
            challenge.setParticipants(Math.max(0, challenge.getParticipants() - 1));
            challenge.setUpdatedAt(LocalDateTime.now());
            challengeRepository.save(challenge);
        }
    }

    @Override
    public List<UserChallengeProgressDTO> getChallengeParticipantsWithProgress(String challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        List<UserChallengeProgress> participants = userChallengeProgressRepository.findByChallengeId(challengeId);
        List<UserChallengeProgressDTO> result = new ArrayList<>();

        for (UserChallengeProgress participant : participants) {
            UserChallengeProgressDTO dto = buildProgressDTO(participant, challenge);
            result.add(dto);
        }

        return result;
    }

    @Override
    public UserChallengeProgressDTO getUserChallengeProgress(String challengeId, String userId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        UserChallengeProgress progress = userChallengeProgressRepository
                .findByChallengeIdAndUserId(challengeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        return buildProgressDTO(progress, challenge);
    }

    /**
     * 构建用户挑战进度DTO，包含从Trip表计算的实时进度
     */
    private UserChallengeProgressDTO buildProgressDTO(UserChallengeProgress progress, Challenge challenge) {
        UserChallengeProgressDTO dto = new UserChallengeProgressDTO();
        dto.setId(progress.getId());
        dto.setChallengeId(progress.getChallengeId());
        dto.setUserId(progress.getUserId());
        dto.setJoinedAt(progress.getJoinedAt());
        dto.setCompletedAt(progress.getCompletedAt());
        dto.setRewardClaimed(progress.getRewardClaimed());
        dto.setTarget(challenge.getTarget());

        // 查询用户信息
        User user = userRepository.findByUserid(progress.getUserId()).orElse(null);
        if (user != null) {
            dto.setUserNickname(user.getNickname());
            dto.setUserEmail(user.getEmail());
            dto.setUserAvatar(user.getAvatar());
        } else {
            dto.setUserNickname("Unknown User");
            dto.setUserEmail(null);
            dto.setUserAvatar(null);
        }

        // 从Trip表计算进度（使用当月范围）
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusNanos(1);
        Double current = calculateProgressFromTrips(
                progress.getUserId(),
                challenge.getType(),
                monthStart,
                monthEnd
        );
        dto.setCurrent(current);

        // 计算进度百分比
        Double target = challenge.getTarget();
        if (target != null && target > 0) {
            dto.setProgressPercent(Math.min(100.0, (current / target) * 100));
        } else {
            dto.setProgressPercent(0.0);
        }

        // 判断是否已完成
        if (target != null && current >= target) {
            dto.setStatus("COMPLETED");
            // 如果之前是IN_PROGRESS，更新为COMPLETED并发放奖励
            if ("IN_PROGRESS".equals(progress.getStatus())) {
                progress.setStatus("COMPLETED");
                progress.setCompletedAt(LocalDateTime.now());
                progress.setUpdatedAt(LocalDateTime.now());
                progress.setRewardClaimed(true);
                userChallengeProgressRepository.save(progress);
                dto.setCompletedAt(progress.getCompletedAt());
                dto.setRewardClaimed(true);

                // 发放积分奖励
                if (challenge.getReward() != null && challenge.getReward() > 0 && user != null) {
                    user.setCurrentPoints(user.getCurrentPoints() + challenge.getReward());
                    user.setTotalPoints(user.getTotalPoints() + challenge.getReward());
                    userRepository.save(user);
                }
            }
        } else {
            dto.setStatus(progress.getStatus());
        }

        return dto;
    }

    /**
     * 从Trip表计算用户在指定时间范围内的进度
     *
     * @param userId    用户ID
     * @param type      挑战类型
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 进度值
     */
    private Double calculateProgressFromTrips(String userId, String type, LocalDateTime startTime, LocalDateTime endTime) {
        // 查询用户在时间范围内的绿色出行记录
        List<Trip> trips = tripRepository.findByUserIdAndIsGreenTripAndCarbonStatusAndStartTimeBetween(
                userId, true, "completed", startTime, endTime
        );

        switch (type) {
            case "GREEN_TRIPS_COUNT":
                // 绿色出行次数
                return (double) trips.size();

            case "GREEN_TRIPS_DISTANCE":
                // 绿色出行总距离（米）
                return trips.stream()
                        .mapToDouble(Trip::getDistance)
                        .sum();

            case "CARBON_SAVED":
                // 碳排放减少量（克）
                return trips.stream()
                        .mapToDouble(Trip::getCarbonSaved)
                        .sum();

            default:
                return 0.0;
        }
    }
}
