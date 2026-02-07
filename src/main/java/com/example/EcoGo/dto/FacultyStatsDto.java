package com.example.EcoGo.dto;

public class FacultyStatsDto {

    public static class PointsResponse {
        public String faculty;
        public Long totalPoints;

        public PointsResponse(String faculty, Long totalPoints) {
            this.faculty = faculty;
            this.totalPoints = totalPoints;
        }
    }

    public static class CarbonResponse {
        public String faculty;
        public Long totalCarbon;

        public CarbonResponse(String faculty, Long totalCarbon) {
            this.faculty = faculty;
            this.totalCarbon = totalCarbon;
        }
    }
}
