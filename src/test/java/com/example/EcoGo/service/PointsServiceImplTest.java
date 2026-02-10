package com.example.EcoGo.service;

import com.example.EcoGo.dto.PointsDto;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.BadgeService;
import com.example.EcoGo.model.TransportMode;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserPointsLog;
import com.example.EcoGo.repository.TransportModeRepository;
import com.example.EcoGo.repository.UserPointsLogRepository;
import com.example.EcoGo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPointsLogRepository pointsLogRepository;

    @Mock
    private TransportModeRepository transportModeRepository;

    @Mock
    private BadgeService badgeService;

    @InjectMocks
    private PointsServiceImpl pointsService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserid("testUser");
        mockUser.setCurrentPoints(100L);
        mockUser.setTotalPoints(100L);
        mockUser.setTotalCarbon(10.0);
    }

    @Test
    void adjustPoints_add_success() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(pointsLogRepository.save(any(UserPointsLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserPointsLog log = pointsService.adjustPoints("testUser", 50, "trip", "Trip points", null, null);

        assertEquals(150L, mockUser.getCurrentPoints());
        assertEquals(150L, mockUser.getTotalPoints()); // Trip adds to total
        assertEquals(50, log.getPoints());
        verify(userRepository).save(mockUser);
    }

    @Test
    void adjustPoints_deduct_success() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(pointsLogRepository.save(any(UserPointsLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserPointsLog log = pointsService.adjustPoints("testUser", -50, "redeem", "Redeem", null, null);

        assertEquals(50L, mockUser.getCurrentPoints());
        assertEquals(100L, mockUser.getTotalPoints()); // Redeem doesn't change total points
        assertEquals(-50, log.getPoints());
    }

    @Test
    void adjustPoints_insufficientFunds() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));

        assertThrows(BusinessException.class,
                () -> pointsService.adjustPoints("testUser", -200, "redeem", "Redeem", null, null));
    }

    @Test
    void getCurrentPoints_success() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));

        PointsDto.CurrentPointsResponse response = pointsService.getCurrentPoints("testUser");

        assertEquals(100L, response.currentPoints);
        assertEquals(100L, response.totalPoints);
    }

    @Test
    void calculatePoints_success() {
        TransportMode mode = new TransportMode();
        mode.setMode("walk");
        mode.setCarbonFactor(0.0);

        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(mode));

        // car (100) - walk (0) = 100g/km. 5km * 100 = 500g. 500 * 10 = 5000 points.
        long points = pointsService.calculatePoints("walk", 5.0);

        assertEquals(5000L, points);
    }

    @Test
    void settleTrip_success() {
        PointsDto.SettleTripRequest request = new PointsDto.SettleTripRequest();
        request.tripId = "trip1";
        request.detectedMode = "walk";
        request.distance = 2.0;
        request.carbonSaved = 200;
        request.isGreenTrip = true;

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        TransportMode mode = new TransportMode();
        mode.setMode("walk");
        mode.setCarbonFactor(0.0);
        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(mode));
        when(pointsLogRepository.save(any(UserPointsLog.class))).thenAnswer(i -> i.getArgument(0));

        pointsService.settleTrip("testUser", request);

        // assertNotNull(result); // method is void
        // assertEquals(2000L, result.points); // cannot check return value
        verify(userRepository, times(2)).save(mockUser);
    }

    @Test
    void settleTrip_userNotFound_duringStatsUpdate() {
        PointsDto.SettleTripRequest request = new PointsDto.SettleTripRequest();
        request.detectedMode = "walk";
        request.distance = 1.0;

        TransportMode mode = new TransportMode();
        mode.setMode("walk");
        when(transportModeRepository.findByMode("walk")).thenReturn(java.util.Optional.of(mode));

        // First call (adjustPoints) -> Found, Second call (updateStats) -> Not Found
        when(userRepository.findByUserid("testUser"))
                .thenReturn(java.util.Optional.of(mockUser))
                .thenReturn(java.util.Optional.empty());

        // Mock save for adjustPoints
        when(pointsLogRepository.save(any(UserPointsLog.class))).thenAnswer(i -> i.getArgument(0));

        assertThrows(BusinessException.class, () -> pointsService.settleTrip("testUser", request));
    }

    @Test
    void getFacultyTotalPoints_userNotFound() {
        when(userRepository.findByUserid("unknownUser")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> pointsService.getFacultyTotalPoints("unknownUser"));
    }
}
