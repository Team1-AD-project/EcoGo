package com.example.EcoGo.service;

import com.example.EcoGo.dto.PointsDto;
import com.example.EcoGo.dto.TripDto;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceImplTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TransportModeRepository transportModeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PointsService pointsService;
    @Mock
    private VipSwitchService vipSwitchService;

    @InjectMocks
    private TripServiceImpl tripService;

    private User testUser;
    private Trip testTrip;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserid("user1");
        testUser.setCurrentPoints(500);
        testUser.setTotalCarbon(50.0);

        testTrip = new Trip();
        testTrip.setId("trip1");
        testTrip.setUserId("user1");
        testTrip.setCarbonStatus("tracking");
        testTrip.setStartTime(LocalDateTime.now());
        testTrip.setCreatedAt(LocalDateTime.now());
        testTrip.setStartPoint(new Trip.GeoPoint(116.0, 39.0));
        testTrip.setStartLocation(new Trip.LocationDetail("Address A", "Place A", "Zone A"));
    }

    // ---------- helpers ----------

    private TripDto.StartTripRequest buildStartRequest() {
        TripDto.StartTripRequest req = new TripDto.StartTripRequest();
        req.startLng = 116.0;
        req.startLat = 39.0;
        req.startAddress = "Address A";
        req.startPlaceName = "Place A";
        req.startCampusZone = "Zone A";
        return req;
    }

    private TripDto.CompleteTripRequest buildCompleteRequest() {
        TripDto.CompleteTripRequest req = new TripDto.CompleteTripRequest();
        req.endLng = 116.1;
        req.endLat = 39.1;
        req.endAddress = "Address B";
        req.endPlaceName = "Place B";
        req.endCampusZone = "Zone B";
        req.distance = 2.5;
        req.detectedMode = "walk";
        req.mlConfidence = 0.95;
        req.isGreenTrip = true;

        TripDto.TransportSegmentDto seg = new TripDto.TransportSegmentDto();
        seg.mode = "walk";
        seg.subDistance = 2.5;
        seg.subDuration = 30;
        req.transportModes = List.of(seg);

        return req;
    }

    // ========== startTrip ==========

    @Test
    void startTrip_success() {
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        Trip result = tripService.startTrip("user1", buildStartRequest());

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("user1", result.getUserId());
        assertEquals("tracking", result.getCarbonStatus());
        assertEquals(116.0, result.getStartPoint().getLng());
        assertEquals(39.0, result.getStartPoint().getLat());
        assertEquals("Address A", result.getStartLocation().getAddress());
        assertEquals("Place A", result.getStartLocation().getPlaceName());
        assertEquals("Zone A", result.getStartLocation().getCampusZone());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void startTrip_userNotFound() {
        when(userRepository.findByUserid("nonexistent")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.startTrip("nonexistent", buildStartRequest()));
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    // ========== completeTrip ==========

    @Test
    void completeTrip_success_nonVip() {
        TransportMode walkMode = new TransportMode("1", "walk", "Walking", 0.2, "icon", 1, true);

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(walkMode));
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(false);
        when(pointsService.formatTripDescription(anyString(), anyString(), anyDouble())).thenReturn("Place A -> Place B (2.5km)");
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        Trip result = tripService.completeTrip("user1", "trip1", buildCompleteRequest());

        assertEquals("completed", result.getCarbonStatus());
        assertNotNull(result.getEndPoint());
        assertNotNull(result.getEndLocation());
        assertEquals(0.5, result.getCarbonSaved(), 0.01);
        assertEquals(10, result.getPointsGained());
        verify(pointsService).settle(eq("user1"), any(PointsDto.SettleResult.class));
        verify(userRepository).save(testUser);
    }

    @Test
    void completeTrip_success_vipDoublePoints() {
        User.Vip vip = new User.Vip();
        vip.setActive(true);
        testUser.setVip(vip);

        TransportMode walkMode = new TransportMode("1", "walk", "Walking", 0.2, "icon", 1, true);

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(walkMode));
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(true);
        when(pointsService.formatTripDescription(anyString(), anyString(), anyDouble())).thenReturn("desc");
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        Trip result = tripService.completeTrip("user1", "trip1", buildCompleteRequest());

        assertEquals(20, result.getPointsGained());
    }

    @Test
    void completeTrip_tripNotFound() {
        when(tripRepository.findById("nonexistent")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.completeTrip("user1", "nonexistent", buildCompleteRequest()));
        assertEquals(ErrorCode.TRIP_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void completeTrip_noPermission() {
        testTrip.setUserId("otherUser");
        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.completeTrip("user1", "trip1", buildCompleteRequest()));
        assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    void completeTrip_wrongStatus() {
        testTrip.setCarbonStatus("completed");
        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.completeTrip("user1", "trip1", buildCompleteRequest()));
        assertEquals(ErrorCode.TRIP_STATUS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void completeTrip_noTransportModes() {
        TripDto.CompleteTripRequest req = buildCompleteRequest();
        req.transportModes = null;

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(false);
        when(pointsService.formatTripDescription(anyString(), anyString(), anyDouble())).thenReturn("desc");
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        Trip result = tripService.completeTrip("user1", "trip1", req);

        assertEquals(0.0, result.getCarbonSaved());
        assertEquals(0, result.getPointsGained());
    }

    @Test
    void completeTrip_unknownTransportMode_skipsCarbon() {
        TripDto.CompleteTripRequest req = buildCompleteRequest();
        TripDto.TransportSegmentDto seg = new TripDto.TransportSegmentDto();
        seg.mode = "unknown_mode";
        seg.subDistance = 5.0;
        seg.subDuration = 20;
        req.transportModes = List.of(seg);

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(transportModeRepository.findByMode("unknown_mode")).thenReturn(Optional.empty());
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(false);
        when(pointsService.formatTripDescription(any(), anyString(), anyDouble())).thenReturn("desc");
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        Trip result = tripService.completeTrip("user1", "trip1", req);

        // Unknown mode => carbonFactor not applied
        assertEquals(0.0, result.getCarbonSaved());
    }

    @Test
    void completeTrip_multipleTransportSegments() {
        TripDto.CompleteTripRequest req = buildCompleteRequest();
        TripDto.TransportSegmentDto seg1 = new TripDto.TransportSegmentDto();
        seg1.mode = "walk";
        seg1.subDistance = 1.0;
        seg1.subDuration = 15;
        TripDto.TransportSegmentDto seg2 = new TripDto.TransportSegmentDto();
        seg2.mode = "bus";
        seg2.subDistance = 3.0;
        seg2.subDuration = 10;
        req.transportModes = List.of(seg1, seg2);

        TransportMode walkMode = new TransportMode("1", "walk", "Walking", 0.2, "icon", 1, true);
        TransportMode busMode = new TransportMode("2", "bus", "Bus", 0.1, "icon", 2, true);

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(walkMode));
        when(transportModeRepository.findByMode("bus")).thenReturn(Optional.of(busMode));
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(false);
        when(pointsService.formatTripDescription(any(), anyString(), anyDouble())).thenReturn("desc");
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        Trip result = tripService.completeTrip("user1", "trip1", req);

        // walk: 0.2 * 1.0 = 0.2, bus: 0.1 * 3.0 = 0.3, total = 0.5
        assertEquals(0.5, result.getCarbonSaved(), 0.01);
        assertEquals(2, result.getTransportModes().size());
    }

    @Test
    void completeTrip_withPolylinePoints() {
        TripDto.CompleteTripRequest req = buildCompleteRequest();
        TripDto.GeoPointDto p1 = new TripDto.GeoPointDto();
        p1.lng = 116.0;
        p1.lat = 39.0;
        TripDto.GeoPointDto p2 = new TripDto.GeoPointDto();
        p2.lng = 116.05;
        p2.lat = 39.05;
        TripDto.GeoPointDto p3 = new TripDto.GeoPointDto();
        p3.lng = 116.1;
        p3.lat = 39.1;
        req.polylinePoints = List.of(p1, p2, p3);

        TransportMode walkMode = new TransportMode("1", "walk", "Walking", 0.2, "icon", 1, true);

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(walkMode));
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(false);
        when(pointsService.formatTripDescription(any(), anyString(), anyDouble())).thenReturn("desc");
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        Trip result = tripService.completeTrip("user1", "trip1", req);

        assertNotNull(result.getPolylinePoints());
        assertEquals(3, result.getPolylinePoints().size());
        assertEquals(116.05, result.getPolylinePoints().get(1).getLng(), 0.01);
    }

    @Test
    void completeTrip_nullPolylinePoints() {
        TripDto.CompleteTripRequest req = buildCompleteRequest();
        req.polylinePoints = null;

        TransportMode walkMode = new TransportMode("1", "walk", "Walking", 0.2, "icon", 1, true);

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(walkMode));
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(false);
        when(pointsService.formatTripDescription(any(), anyString(), anyDouble())).thenReturn("desc");
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        Trip result = tripService.completeTrip("user1", "trip1", req);

        assertNull(result.getPolylinePoints());
    }

    @Test
    void completeTrip_zeroCarbonSaved_doesNotUpdateTotalCarbon() {
        TripDto.CompleteTripRequest req = buildCompleteRequest();
        req.transportModes = null; // no transport => carbonSaved = 0

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(false);
        when(pointsService.formatTripDescription(any(), anyString(), anyDouble())).thenReturn("desc");
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        tripService.completeTrip("user1", "trip1", req);

        // userRepository.save should only be called once (for saving the trip)
        // but not for updating totalCarbon since carbonSaved is 0
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void completeTrip_updatesTotalCarbon() {
        TransportMode walkMode = new TransportMode("1", "walk", "Walking", 0.2, "icon", 1, true);

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(walkMode));
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(false);
        when(pointsService.formatTripDescription(anyString(), anyString(), anyDouble())).thenReturn("desc");
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        tripService.completeTrip("user1", "trip1", buildCompleteRequest());

        assertEquals(50.5, testUser.getTotalCarbon(), 0.01);
        verify(userRepository).save(testUser);
    }

    @Test
    void completeTrip_nullStartLocation_handledInDescription() {
        testTrip.setStartLocation(null);
        TransportMode walkMode = new TransportMode("1", "walk", "Walking", 0.2, "icon", 1, true);

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(walkMode));
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(false);
        when(pointsService.formatTripDescription(isNull(), anyString(), anyDouble())).thenReturn("desc");
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        Trip result = tripService.completeTrip("user1", "trip1", buildCompleteRequest());

        assertNotNull(result);
        verify(pointsService).formatTripDescription(isNull(), eq("Place B"), eq(2.5));
    }

    @Test
    void completeTrip_vipActiveButSwitchDisabled_noDoublePoints() {
        User.Vip vip = new User.Vip();
        vip.setActive(true);
        testUser.setVip(vip);

        TransportMode walkMode = new TransportMode("1", "walk", "Walking", 0.2, "icon", 1, true);

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(walkMode));
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(false); // switch off
        when(pointsService.formatTripDescription(any(), anyString(), anyDouble())).thenReturn("desc");
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        Trip result = tripService.completeTrip("user1", "trip1", buildCompleteRequest());

        // VIP active but switch disabled => no doubling
        assertEquals(10, result.getPointsGained());
    }

    @Test
    void completeTrip_vipNullButSwitchEnabled_noDoublePoints() {
        testUser.setVip(null);

        TransportMode walkMode = new TransportMode("1", "walk", "Walking", 0.2, "icon", 1, true);

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(walkMode));
        when(userRepository.findByUserid("user1")).thenReturn(Optional.of(testUser));
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(true); // switch on
        when(pointsService.formatTripDescription(any(), anyString(), anyDouble())).thenReturn("desc");
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        Trip result = tripService.completeTrip("user1", "trip1", buildCompleteRequest());

        // No VIP => no doubling
        assertEquals(10, result.getPointsGained());
    }

    // ========== cancelTrip ==========

    @Test
    void cancelTrip_success() {
        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        tripService.cancelTrip("user1", "trip1");

        assertEquals("canceled", testTrip.getCarbonStatus());
        assertNotNull(testTrip.getEndTime());
        verify(tripRepository).save(testTrip);
    }

    @Test
    void cancelTrip_tripNotFound() {
        when(tripRepository.findById("nonexistent")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.cancelTrip("user1", "nonexistent"));
        assertEquals(ErrorCode.TRIP_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void cancelTrip_noPermission() {
        testTrip.setUserId("otherUser");
        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.cancelTrip("user1", "trip1"));
        assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    void cancelTrip_wrongStatus() {
        testTrip.setCarbonStatus("completed");
        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.cancelTrip("user1", "trip1"));
        assertEquals(ErrorCode.TRIP_STATUS_ERROR.getCode(), ex.getCode());
    }

    // ========== getTripById ==========

    @Test
    void getTripById_success() {
        testTrip.setDistance(2.5);
        testTrip.setCarbonSaved(0.5);
        testTrip.setPointsGained(50);
        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));

        TripDto.TripResponse result = tripService.getTripById("user1", "trip1");

        assertNotNull(result);
        assertEquals("trip1", result.id);
        assertEquals("user1", result.userId);
        assertEquals(2.5, result.distance);
    }

    @Test
    void getTripById_notFound() {
        when(tripRepository.findById("nonexistent")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.getTripById("user1", "nonexistent"));
        assertEquals(ErrorCode.TRIP_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void getTripById_noPermission() {
        testTrip.setUserId("otherUser");
        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.getTripById("user1", "trip1"));
        assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex.getCode());
    }

    // ========== getUserTrips ==========

    @Test
    void getUserTrips_success() {
        Trip trip2 = new Trip();
        trip2.setId("trip2");
        trip2.setUserId("user1");
        trip2.setCarbonStatus("completed");
        trip2.setStartTime(LocalDateTime.now());

        when(tripRepository.findByUserIdOrderByCreatedAtDesc("user1")).thenReturn(List.of(testTrip, trip2));

        List<TripDto.TripSummaryResponse> result = tripService.getUserTrips("user1");

        assertEquals(2, result.size());
    }

    @Test
    void getUserTrips_empty() {
        when(tripRepository.findByUserIdOrderByCreatedAtDesc("user1")).thenReturn(List.of());

        List<TripDto.TripSummaryResponse> result = tripService.getUserTrips("user1");

        assertTrue(result.isEmpty());
    }

    // ========== getCurrentTrip ==========

    @Test
    void getCurrentTrip_exists() {
        when(tripRepository.findByUserIdAndCarbonStatus("user1", "tracking")).thenReturn(List.of(testTrip));

        TripDto.TripResponse result = tripService.getCurrentTrip("user1");

        assertNotNull(result);
        assertEquals("trip1", result.id);
    }

    @Test
    void getCurrentTrip_none() {
        when(tripRepository.findByUserIdAndCarbonStatus("user1", "tracking")).thenReturn(List.of());

        TripDto.TripResponse result = tripService.getCurrentTrip("user1");

        assertNull(result);
    }

    // ========== getAllTrips ==========

    @Test
    void getAllTrips_success() {
        Trip trip2 = new Trip();
        trip2.setId("trip2");
        trip2.setUserId("user2");
        trip2.setCarbonStatus("completed");

        when(tripRepository.findAll()).thenReturn(List.of(testTrip, trip2));

        List<TripDto.TripSummaryResponse> result = tripService.getAllTrips();

        assertEquals(2, result.size());
    }

    @Test
    void getAllTrips_empty() {
        when(tripRepository.findAll()).thenReturn(List.of());

        List<TripDto.TripSummaryResponse> result = tripService.getAllTrips();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllTrips_dbException() {
        when(tripRepository.findAll()).thenThrow(new RuntimeException("DB error"));

        List<TripDto.TripSummaryResponse> result = tripService.getAllTrips();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllTrips_conversionException_skipsFailedTrip() {
        Trip badTrip = mock(Trip.class);
        when(badTrip.getId()).thenReturn("bad");
        // Make getStartPoint throw to simulate conversion failure
        when(badTrip.getUserId()).thenReturn("u1");
        when(badTrip.getDetectedMode()).thenReturn("walk");
        when(badTrip.getCarbonStatus()).thenReturn("completed");
        when(badTrip.getStartTime()).thenReturn(LocalDateTime.now());
        when(badTrip.getStartPoint()).thenThrow(new RuntimeException("conversion error"));

        when(tripRepository.findAll()).thenReturn(List.of(testTrip, badTrip));

        List<TripDto.TripSummaryResponse> result = tripService.getAllTrips();

        // Good trip succeeds, bad trip is skipped
        assertEquals(1, result.size());
    }

    // ========== getTripsByUser ==========

    @Test
    void getTripsByUser_success() {
        testTrip.setDistance(2.5);
        testTrip.setCarbonSaved(0.5);
        when(tripRepository.findByUserIdOrderByCreatedAtDesc("user1")).thenReturn(List.of(testTrip));

        List<TripDto.TripResponse> result = tripService.getTripsByUser("user1");

        assertEquals(1, result.size());
        assertEquals("trip1", result.get(0).id);
    }

    @Test
    void getTripsByUser_empty() {
        when(tripRepository.findByUserIdOrderByCreatedAtDesc("userX")).thenReturn(List.of());

        List<TripDto.TripResponse> result = tripService.getTripsByUser("userX");

        assertTrue(result.isEmpty());
    }

    @Test
    void getTripsByUser_dbException() {
        when(tripRepository.findByUserIdOrderByCreatedAtDesc("user1"))
                .thenThrow(new RuntimeException("DB error"));

        List<TripDto.TripResponse> result = tripService.getTripsByUser("user1");

        assertTrue(result.isEmpty());
    }

    @Test
    void getTripsByUser_conversionException_skipsFailedTrip() {
        Trip badTrip = mock(Trip.class);
        when(badTrip.getId()).thenReturn("bad");
        when(badTrip.getUserId()).thenReturn("user1");
        when(badTrip.getStartTime()).thenReturn(LocalDateTime.now());
        when(badTrip.getStartPoint()).thenThrow(new RuntimeException("conversion error"));

        when(tripRepository.findByUserIdOrderByCreatedAtDesc("user1")).thenReturn(List.of(testTrip, badTrip));

        List<TripDto.TripResponse> result = tripService.getTripsByUser("user1");

        assertEquals(1, result.size());
    }

    // ========== convertToResponse (full coverage via getTripById) ==========

    @Test
    void getTripById_fullConversion() {
        testTrip.setDistance(3.0);
        testTrip.setCarbonSaved(0.6);
        testTrip.setPointsGained(60);
        testTrip.setDetectedMode("bike");
        testTrip.setMlConfidence(0.9);
        testTrip.setGreenTrip(true);
        testTrip.setEndPoint(new Trip.GeoPoint(116.1, 39.1));
        testTrip.setEndLocation(new Trip.LocationDetail("Address B", "Place B", "Zone B"));
        testTrip.setEndTime(LocalDateTime.now());

        Trip.TransportSegment seg = new Trip.TransportSegment("bike", 3.0, 15);
        testTrip.setTransportModes(List.of(seg));

        Trip.GeoPoint p1 = new Trip.GeoPoint(116.0, 39.0);
        Trip.GeoPoint p2 = new Trip.GeoPoint(116.05, 39.05);
        testTrip.setPolylinePoints(List.of(p1, p2));

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));

        TripDto.TripResponse result = tripService.getTripById("user1", "trip1");

        assertEquals("trip1", result.id);
        assertEquals("user1", result.userId);
        assertEquals(3.0, result.distance);
        assertEquals(0.6, result.carbonSaved);
        assertEquals(60, result.pointsGained);
        assertEquals("bike", result.detectedMode);
        assertEquals(0.9, result.mlConfidence, 0.01);
        assertTrue(result.isGreenTrip);
        assertNotNull(result.startPoint);
        assertNotNull(result.endPoint);
        assertEquals(116.1, result.endPoint.lng, 0.01);
        assertEquals(39.1, result.endPoint.lat, 0.01);
        assertNotNull(result.startLocation);
        assertNotNull(result.endLocation);
        assertEquals("Address B", result.endLocation.address);
        assertEquals("Place B", result.endLocation.placeName);
        assertEquals("Zone B", result.endLocation.campusZone);
        assertEquals(1, result.transportModes.size());
        assertEquals("bike", result.transportModes.get(0).mode);
        assertEquals(3.0, result.transportModes.get(0).subDistance, 0.01);
        assertEquals(15, result.transportModes.get(0).subDuration);
        assertEquals(2, result.polylinePoints.size());
    }

    @Test
    void getTripById_nullEndPointAndEndLocation() {
        testTrip.setEndPoint(null);
        testTrip.setEndLocation(null);
        testTrip.setTransportModes(null);
        testTrip.setPolylinePoints(null);

        when(tripRepository.findById("trip1")).thenReturn(Optional.of(testTrip));

        TripDto.TripResponse result = tripService.getTripById("user1", "trip1");

        assertNull(result.endPoint);
        assertNull(result.endLocation);
        assertNull(result.transportModes);
        assertNull(result.polylinePoints);
    }

    // ========== convertToSummary (full coverage via getUserTrips) ==========

    @Test
    void getUserTrips_fullSummaryConversion() {
        testTrip.setDistance(3.0);
        testTrip.setCarbonSaved(0.6);
        testTrip.setPointsGained(60);
        testTrip.setDetectedMode("bike");
        testTrip.setGreenTrip(true);
        testTrip.setEndPoint(new Trip.GeoPoint(116.1, 39.1));
        testTrip.setEndLocation(new Trip.LocationDetail("Address B", "Place B", "Zone B"));
        testTrip.setEndTime(LocalDateTime.now());

        when(tripRepository.findByUserIdOrderByCreatedAtDesc("user1")).thenReturn(List.of(testTrip));

        List<TripDto.TripSummaryResponse> result = tripService.getUserTrips("user1");

        assertEquals(1, result.size());
        TripDto.TripSummaryResponse summary = result.get(0);
        assertEquals("trip1", summary.id);
        assertEquals("user1", summary.userId);
        assertEquals("bike", summary.detectedMode);
        assertEquals(3.0, summary.distance, 0.01);
        assertEquals(0.6, summary.carbonSaved, 0.01);
        assertEquals(60, summary.pointsGained);
        assertTrue(summary.isGreenTrip);
        assertNotNull(summary.startPoint);
        assertNotNull(summary.endPoint);
        assertEquals(116.1, summary.endPoint.lng, 0.01);
        assertEquals("Place A", summary.startPlaceName);
        assertEquals("Place B", summary.endPlaceName);
    }

    @Test
    void getUserTrips_nullStartAndEndLocations_inSummary() {
        testTrip.setStartLocation(null);
        testTrip.setEndLocation(null);
        testTrip.setStartPoint(null);
        testTrip.setEndPoint(null);

        when(tripRepository.findByUserIdOrderByCreatedAtDesc("user1")).thenReturn(List.of(testTrip));

        List<TripDto.TripSummaryResponse> result = tripService.getUserTrips("user1");

        assertEquals(1, result.size());
        TripDto.TripSummaryResponse summary = result.get(0);
        assertNull(summary.startPoint);
        assertNull(summary.endPoint);
        assertNull(summary.startPlaceName);
        assertNull(summary.endPlaceName);
    }
}
