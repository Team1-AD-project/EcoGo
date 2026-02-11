package com.example.EcoGo.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecommendationDtoTest {

    // ===== RecommendationResponseDto =====
    @Test void responseDto_default() {
        RecommendationResponseDto dto = new RecommendationResponseDto();
        assertNull(dto.getText());
        assertNull(dto.getTag());
    }

    @Test void responseDto_allArgs() {
        RecommendationResponseDto dto = new RecommendationResponseDto("Go green!", "eco");
        assertEquals("Go green!", dto.getText());
        assertEquals("eco", dto.getTag());
    }

    @Test void responseDto_setters() {
        RecommendationResponseDto dto = new RecommendationResponseDto();
        dto.setText("Walk more");
        dto.setTag("health");
        assertEquals("Walk more", dto.getText());
        assertEquals("health", dto.getTag());
    }

    // ===== RecommendationRequestDto =====
    @Test void requestDto_default() {
        RecommendationRequestDto dto = new RecommendationRequestDto();
        assertNull(dto.getDestination());
    }

    @Test void requestDto_setter() {
        RecommendationRequestDto dto = new RecommendationRequestDto();
        dto.setDestination("UTown");
        assertEquals("UTown", dto.getDestination());
    }
}
