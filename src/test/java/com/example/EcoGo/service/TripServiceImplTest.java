package com.example.EcoGo.service;

import com.example.EcoGo.dto.PointsDto;
import com.example.EcoGo.dto.TripDto;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.interfacemethods.VipSwitchService;
import com.example.EcoGo.model.TransportMode;
import com.example.EcoGo.model.Trip;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.TransportModeRepository;
import com.example.EcoGo.repository.TripRepository;
import com.example.EcoGo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceImplTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransportModeRepository transportModeRepository;

    @Mock
    private PointsService pointsService;

    @Mock
    private VipSwitchService vipSwitchService;

    @InjectMocks
    private TripServiceImpl tripService;

    private User mockUser;
    private Trip mockTrip;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserid("testUser");
        mockUser.setTotalCarbon(100.0);
        mockUser.setCurrentPoints(100L);

        mockTrip = new Trip();
        mockTrip.setId("trip1");
        mockTrip.setUserId("testUser");
        mockTrip.setCarbonStatus("tracking");
    }

    @Test
    void completeTrip_success_refetchesUser() {
        TripDto.CompleteTripRequest request = new TripDto.CompleteTripRequest();
        request.endLng = 100.0;
        request.endLat = 10.0;
        request.distance = 5.0;
        request.detectedMode = "walk";
        request.transportModes = new ArrayList<>();
        TripDto.TransportSegmentDto segment = new TripDto.TransportSegmentDto();
        segment.mode = "walk";
        segment.subDistance = 5.0;
        request.transportModes.add(segment);

        TransportMode mode = new TransportMode();
        mode.setMode("walk");
        mode.setCarbonFactor(0.0); // Saves 100g/km (Car 100 - Walk 0)

        // Stubs
        when(tripRepository.findById("trip1")).thenReturn(Optional.of(mockTrip));
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(mode));
        when(vipSwitchService.isSwitchEnabled(anyString())).thenReturn(false);
        // when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));
        // // Standard mock behavior sufficient

        tripService.completeTrip("testUser", "trip1", request);

        // Verification:
        // 1. settle is called
        verify(pointsService).settle(eq("testUser"), any(PointsDto.SettleResult.class));

        // 2. findByUserid is called AT LEAST 2 times (start + refresh before save)
        // This validates that the fix to refetch the user is present.
        verify(userRepository, atLeast(2)).findByUserid("testUser");

        // 3. User is saved with updated carbon
        verify(userRepository).save(mockUser);
    }
}
