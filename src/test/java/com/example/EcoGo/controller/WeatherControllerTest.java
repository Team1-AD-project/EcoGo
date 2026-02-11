package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for WeatherController.
 */
class WeatherControllerTest {

    private WeatherService weatherService;
    private WeatherController controller;

    @BeforeEach
    void setUp() throws Exception {
        weatherService = mock(WeatherService.class);
        controller = new WeatherController();

        Field f = WeatherController.class.getDeclaredField("weatherService");
        f.setAccessible(true);
        f.set(controller, weatherService);
    }

    @Test
    void getCurrentWeather_success() {
        Map<String, Object> weatherData = Map.of(
                "temperature", 28L,
                "humidity", "75%",
                "condition", "Clear"
        );
        when(weatherService.getWeather(anyDouble(), anyDouble())).thenReturn(weatherData);

        ResponseMessage<Object> resp = controller.getCurrentWeather();
        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData());
    }

    @Test
    void getCurrentWeather_exception() {
        when(weatherService.getWeather(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("API error"));

        ResponseMessage<Object> resp = controller.getCurrentWeather();
        assertEquals(500, resp.getCode());
        assertTrue(resp.getMessage().contains("获取天气信息失败"));
    }
}
