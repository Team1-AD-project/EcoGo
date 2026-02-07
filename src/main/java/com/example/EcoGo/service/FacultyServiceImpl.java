package com.example.EcoGo.service;

import com.example.EcoGo.dto.FacultyStatsDto;
import com.example.EcoGo.model.Faculty;
import com.example.EcoGo.model.Trip;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.FacultyRepository;
import com.example.EcoGo.repository.TripRepository;
import com.example.EcoGo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FacultyServiceImpl {

    private final FacultyRepository facultyRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public FacultyServiceImpl(FacultyRepository facultyRepository, TripRepository tripRepository,
            UserRepository userRepository) {
        this.facultyRepository = facultyRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }

    public List<String> getAllFacultyNames() {
        return facultyRepository.findAll().stream()
                .map(Faculty::getName)
                .collect(Collectors.toList());
    }

    public List<FacultyStatsDto.CarbonResponse> getMonthlyFacultyCarbonStats() {
        // 1. Determine current month range
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        LocalDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);

        // 2. Fetch all trips in this range
        List<Trip> trips = tripRepository.findByStartTimeBetween(startOfMonth, endOfMonth);

        // 3. Aggregate by Faculty
        // Strategy:
        // - If trip has 'faculty', use it.
        // - If trip 'faculty' is null, look up user's faculty (fallback for old data).
        // Optimization: Cache user faculties to avoid N+1 queries if possible, or just
        // accept N+1 for fallback.
        // Since we are creating a new feature, assuming most new trips have faculty or
        // we accept the cost for now.
        // Better: Fetch all users involved in these trips if needed.

        Map<String, Long> facultyCarbonMap = new HashMap<>();

        // Initialize map with all known faculties (so even 0 carbon faculties appear)
        List<String> allFaculties = getAllFacultyNames();
        for (String f : allFaculties) {
            facultyCarbonMap.put(f, 0L);
        }

        // Cache for user faculties to minimize DB hits
        Map<String, String> userFacultyCache = new HashMap<>();

        for (Trip trip : trips) {
            String faculty = trip.getFaculty();

            // Fallback to User look up if faculty missing
            if (faculty == null) {
                if (userFacultyCache.containsKey(trip.getUserId())) {
                    faculty = userFacultyCache.get(trip.getUserId());
                } else {
                    // Fetch user
                    // Note: This could be slow for many trips.
                    // Optimization: We could fetch all relevant users in one go using
                    // 'findByUserIdIn',
                    // but for simplicity/MVP we do this.
                    // User suggested: "trips里面也有个faculty的字段...如果你觉得能优化性能我可以加上" -> We ADDED it.
                    // So we expect new data to have it. Old data might be missed or slow.
                    // Let's implement fallback but be aware.
                    try {
                        User user = userRepository.findByUserid(trip.getUserId()).orElse(null);
                        if (user != null) {
                            faculty = user.getFaculty();
                            userFacultyCache.put(trip.getUserId(), faculty);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }

            if (faculty != null && facultyCarbonMap.containsKey(faculty)) {
                long current = facultyCarbonMap.get(faculty);
                facultyCarbonMap.put(faculty, current + trip.getCarbonSaved());
            }
        }

        // 4. Convert to DTO list
        List<FacultyStatsDto.CarbonResponse> response = new ArrayList<>();
        for (Map.Entry<String, Long> entry : facultyCarbonMap.entrySet()) {
            response.add(new FacultyStatsDto.CarbonResponse(entry.getKey(), entry.getValue()));
        }

        return response;
    }
}
