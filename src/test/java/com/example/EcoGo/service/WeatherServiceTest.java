package com.example.EcoGo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for WeatherService.
 */
class WeatherServiceTest {

    private RestTemplate restTemplate;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() throws Exception {
        restTemplate = mock(RestTemplate.class);
        weatherService = new WeatherService();

        // Inject mocks via reflection
        setField(weatherService, "restTemplate", restTemplate);
        setField(weatherService, "apiKey", "test-api-key");
        setField(weatherService, "weatherUrl", "https://api.openweathermap.org/data/2.5/weather");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void getWeather_success() {
        // Mock weather API response
        Map<String, Object> mainData = new HashMap<>();
        mainData.put("temp", 28.5);
        mainData.put("humidity", 75);

        Map<String, Object> weatherItem = new HashMap<>();
        weatherItem.put("main", "Clear");
        weatherItem.put("description", "clear sky");
        weatherItem.put("icon", "01d");

        Map<String, Object> weatherResponse = new HashMap<>();
        weatherResponse.put("main", mainData);
        weatherResponse.put("weather", List.of(weatherItem));

        // Mock pollution API response
        Map<String, Object> pollutionMain = new HashMap<>();
        pollutionMain.put("aqi", 2);

        Map<String, Object> pollutionItem = new HashMap<>();
        pollutionItem.put("main", pollutionMain);

        Map<String, Object> pollutionResponse = new HashMap<>();
        pollutionResponse.put("list", List.of(pollutionItem));

        when(restTemplate.getForObject(contains("/weather"), eq(Map.class))).thenReturn(weatherResponse);
        when(restTemplate.getForObject(contains("/air_pollution"), eq(Map.class))).thenReturn(pollutionResponse);

        Map<String, Object> result = weatherService.getWeather(1.3521, 103.8198);

        assertNotNull(result);
        assertEquals(29L, result.get("temperature")); // Math.round(28.5) = 29
        assertEquals("75%", result.get("humidity"));
        assertEquals("Clear", result.get("condition"));
        assertEquals("clear sky", result.get("description"));
        assertEquals("01d", result.get("icon"));
        assertEquals(2, result.get("aqiLevel"));
    }

    @Test
    void getWeather_nullWeatherData() {
        when(restTemplate.getForObject(contains("/weather"), eq(Map.class))).thenReturn(null);
        when(restTemplate.getForObject(contains("/air_pollution"), eq(Map.class))).thenReturn(null);

        Map<String, Object> result = weatherService.getWeather(1.35, 103.82);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getWeather_nullMainData() {
        Map<String, Object> weatherResponse = new HashMap<>();
        weatherResponse.put("main", null);
        weatherResponse.put("weather", null);

        when(restTemplate.getForObject(contains("/weather"), eq(Map.class))).thenReturn(weatherResponse);
        when(restTemplate.getForObject(contains("/air_pollution"), eq(Map.class))).thenReturn(null);

        Map<String, Object> result = weatherService.getWeather(1.35, 103.82);
        assertNotNull(result);
    }

    @Test
    void getWeather_emptyWeatherList() {
        Map<String, Object> mainData = new HashMap<>();
        mainData.put("temp", 25.0);
        mainData.put("humidity", 60);

        Map<String, Object> weatherResponse = new HashMap<>();
        weatherResponse.put("main", mainData);
        weatherResponse.put("weather", List.of()); // empty list

        when(restTemplate.getForObject(contains("/weather"), eq(Map.class))).thenReturn(weatherResponse);
        when(restTemplate.getForObject(contains("/air_pollution"), eq(Map.class))).thenReturn(null);

        Map<String, Object> result = weatherService.getWeather(1.35, 103.82);
        assertNotNull(result);
        assertEquals(25L, result.get("temperature"));
        assertNull(result.get("condition")); // no weather items
    }

    @Test
    void getWeather_nullPollutionList() {
        Map<String, Object> weatherResponse = new HashMap<>();
        Map<String, Object> mainData = new HashMap<>();
        mainData.put("temp", 30.0);
        mainData.put("humidity", 80);
        weatherResponse.put("main", mainData);
        weatherResponse.put("weather", List.of());

        Map<String, Object> pollutionResponse = new HashMap<>();
        pollutionResponse.put("list", List.of()); // empty list

        when(restTemplate.getForObject(contains("/weather"), eq(Map.class))).thenReturn(weatherResponse);
        when(restTemplate.getForObject(contains("/air_pollution"), eq(Map.class))).thenReturn(pollutionResponse);

        Map<String, Object> result = weatherService.getWeather(1.35, 103.82);
        assertNotNull(result);
        assertNull(result.get("aqiLevel"));
    }

    @Test
    void getWeather_apiException_throwsRuntimeException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("API timeout"));

        assertThrows(RuntimeException.class, () -> weatherService.getWeather(1.35, 103.82));
    }
}
