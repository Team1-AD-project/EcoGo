package com.example.EcoGo.service.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NusBusProviderTest {

    private NusBusProvider provider;
    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() throws Exception {
        provider = new NusBusProvider();

        // Inject mock HttpClient via reflection
        mockHttpClient = mock(HttpClient.class);
        Field httpField = NusBusProvider.class.getDeclaredField("httpClient");
        httpField.setAccessible(true);
        httpField.set(provider, mockHttpClient);

        // Inject credentials
        setField("username", "testUser");
        setField("password", "testPass");
    }

    private void setField(String name, Object value) throws Exception {
        Field f = NusBusProvider.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(provider, value);
    }

    // ===== isRouteName =====
    @Test void isRouteName_A1() { assertTrue(NusBusProvider.isRouteName("A1")); }
    @Test void isRouteName_D2() { assertTrue(NusBusProvider.isRouteName("D2")); }
    @Test void isRouteName_K() { assertTrue(NusBusProvider.isRouteName("K")); }
    @Test void isRouteName_caseInsensitive() { assertTrue(NusBusProvider.isRouteName("a1")); }
    @Test void isRouteName_BTC() { assertTrue(NusBusProvider.isRouteName("BTC")); }
    @Test void isRouteName_invalid() { assertFalse(NusBusProvider.isRouteName("COM3")); }
    @Test void isRouteName_null() { assertFalse(NusBusProvider.isRouteName(null)); }
    @Test void isRouteName_empty() { assertFalse(NusBusProvider.isRouteName("")); }
    @Test void isRouteName_L() { assertTrue(NusBusProvider.isRouteName("L")); }
    @Test void isRouteName_E() { assertTrue(NusBusProvider.isRouteName("E")); }
    @Test void isRouteName_A1E() { assertTrue(NusBusProvider.isRouteName("A1E")); }
    @Test void isRouteName_D2E() { assertTrue(NusBusProvider.isRouteName("D2E")); }

    // ===== getArrivals with mock HTTP =====
    @Test void getArrivals_nullStop_defaultsToUtown() throws Exception {
        mockApiResponse(200, buildApiResponse("UTOWN", List.of(
                buildShuttle("D1", "UTOWN-D1-S", List.of(Map.of("eta", 3, "plate", "ABC"))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals(null, null);
        assertNotNull(result);
        assertEquals("UTOWN", result.stopName());
        assertFalse(result.arrivals().isEmpty());
    }

    @Test void getArrivals_blankStop_defaultsToUtown() throws Exception {
        mockApiResponse(200, buildApiResponse("UTOWN", List.of(
                buildShuttle("D2", "UTOWN-D2-E", List.of(Map.of("eta", 5))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("  ", null);
        assertEquals("UTOWN", result.stopName());
    }

    @Test void getArrivals_exactMatch_COM3() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttle("A1", "COM3-A1-S", List.of(Map.of("eta", 2, "plate", "SG123", "ts", "10:00"))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertEquals("COM3", result.stopName());
        assertFalse(result.arrivals().isEmpty());
        assertEquals("A1", result.arrivals().get(0).get("route"));
    }

    @Test void getArrivals_chineseAlias() throws Exception {
        mockApiResponse(200, buildApiResponse("UTOWN", List.of(
                buildShuttle("D1", "UTOWN-D1-S", List.of(Map.of("eta", 4))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("大学城", null);
        assertNotNull(result);
    }

    @Test void getArrivals_caseInsensitive() throws Exception {
        mockApiResponse(200, buildApiResponse("UTOWN", List.of(
                buildShuttle("D2", "UTOWN-D2-S", List.of(Map.of("eta", 6))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("utown", null);
        assertNotNull(result);
    }

    @Test void getArrivals_partialMatch() throws Exception {
        mockApiResponse(200, buildApiResponse("KR-MRT", List.of(
                buildShuttle("A1", "KR-MRT-A1-S", List.of(Map.of("eta", 3))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("Kent Ridge MRT", null);
        assertNotNull(result);
    }

    @Test void getArrivals_routeAsStop_detectsAndSetsDefault() throws Exception {
        // When user passes a route name ("D1") as stop → should default to UTOWN
        mockApiResponse(200, buildApiResponse("UTOWN", List.of(
                buildShuttle("D1", "UTOWN-D1-S", List.of(Map.of("eta", 2))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("D1", null);
        assertNotNull(result);
        assertTrue(result.stopName().contains("D1"));
    }

    @Test void getArrivals_withRouteFilter() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttle("A1", "COM3-A1-S", List.of(Map.of("eta", 2))),
                buildShuttle("D2", "COM3-D2-E", List.of(Map.of("eta", 5))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", "A1");
        assertTrue(result.stopName().contains("A1"));
        // Route filter applied
        for (Map<String, Object> a : result.arrivals()) {
            assertEquals("A1", a.get("route"));
        }
    }

    @Test void getArrivals_routeFilter_noMatch_showsAll() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttle("A1", "COM3-A1-S", List.of(Map.of("eta", 2))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", "D2");
        // D2 filter doesn't match any → shows all
        assertFalse(result.arrivals().isEmpty());
    }

    @Test void getArrivals_apiReturns_non200() throws Exception {
        mockApiResponse(500, "Internal Server Error");
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertTrue(result.arrivals().isEmpty());
    }

    @Test void getArrivals_apiThrows() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any())).thenThrow(new RuntimeException("Connection refused"));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertTrue(result.arrivals().isEmpty());
    }

    @Test void getArrivals_emptyShuttles() throws Exception {
        mockApiResponse(200, "{\"ShuttleServiceResult\":{\"shuttles\":[]}}");
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertTrue(result.arrivals().isEmpty());
    }

    @Test void getArrivals_nullShuttleServiceResult() throws Exception {
        mockApiResponse(200, "{\"ShuttleServiceResult\":null}");
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertTrue(result.arrivals().isEmpty());
    }

    @Test void getArrivals_nullShuttles() throws Exception {
        mockApiResponse(200, "{\"ShuttleServiceResult\":{\"shuttles\":null}}");
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertTrue(result.arrivals().isEmpty());
    }

    @Test void getArrivals_shuttleWithBlankName() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttle("", "COM3-X-S", List.of(Map.of("eta", 3))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertTrue(result.arrivals().isEmpty());
    }

    @Test void getArrivals_shuttleWithNullName() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttleRaw(null, "COM3-X-S", null, null, null, null, null))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertTrue(result.arrivals().isEmpty());
    }

    @Test void getArrivals_etaArriving() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttle("A1", "COM3-A1-S", List.of(Map.of("eta", 1))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertEquals("arriving", result.arrivals().get(0).get("status"));
    }

    @Test void getArrivals_etaOnTime() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttle("A1", "COM3-A1-S", List.of(Map.of("eta", 5))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertEquals("on_time", result.arrivals().get(0).get("status"));
    }

    @Test void getArrivals_etaScheduled() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttle("A1", "COM3-A1-S", List.of(Map.of("eta", 15))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertEquals("scheduled", result.arrivals().get(0).get("status"));
    }

    @Test void getArrivals_negativeEta_skipped() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttle("A1", "COM3-A1-S", List.of(Map.of("eta", -1))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertTrue(result.arrivals().isEmpty());
    }

    @Test void getArrivals_nullEta_skipped() throws Exception {
        String json = "{\"ShuttleServiceResult\":{\"shuttles\":[{\"name\":\"A1\",\"busstopcode\":\"COM3-A1-S\",\"_etas\":[{\"eta\":null}]}]}}";
        mockApiResponse(200, json);
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertTrue(result.arrivals().isEmpty());
    }

    // ===== Legacy fields (fallback) =====
    @Test void getArrivals_legacyFields() throws Exception {
        String json = buildApiResponse("COM3", List.of(
                buildShuttleRaw("D2", "COM3-D2-S", null, "3", "SG111", "7", "SG222")));
        mockApiResponse(200, json);
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertEquals(2, result.arrivals().size());
    }

    @Test void getArrivals_legacyFields_negativeEta() throws Exception {
        String json = buildApiResponse("COM3", List.of(
                buildShuttleRaw("D2", "COM3-D2-E", null, "-1", null, "5", null)));
        mockApiResponse(200, json);
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertEquals(1, result.arrivals().size());
    }

    @Test void getArrivals_legacyFields_blankEta() throws Exception {
        String json = buildApiResponse("COM3", List.of(
                buildShuttleRaw("D2", "COM3-D2-E", null, "", null, "-", null)));
        mockApiResponse(200, json);
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertTrue(result.arrivals().isEmpty());
    }

    @Test void getArrivals_legacyFields_invalidNumber() throws Exception {
        String json = buildApiResponse("COM3", List.of(
                buildShuttleRaw("D2", "COM3-D2-E", null, "abc", null, "xyz", null)));
        mockApiResponse(200, json);
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertTrue(result.arrivals().isEmpty());
    }

    // ===== Direction parsing =====
    @Test void getArrivals_directionStart() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttle("A1", "COM3-A1-S", List.of(Map.of("eta", 5))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertEquals("start", result.arrivals().get(0).get("direction"));
    }

    @Test void getArrivals_directionEnd() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttle("A1", "COM3-A1-E", List.of(Map.of("eta", 5))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertEquals("end", result.arrivals().get(0).get("direction"));
    }

    @Test void getArrivals_directionLoop() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttle("A1", "COM3-A1", List.of(Map.of("eta", 5))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertEquals("loop", result.arrivals().get(0).get("direction"));
    }

    @Test void getArrivals_noBusstopcode() throws Exception {
        mockApiResponse(200, buildApiResponse("COM3", List.of(
                buildShuttle("A1", null, List.of(Map.of("eta", 5))))));
        NusBusProvider.BusArrivalsResult result = provider.getArrivals("COM3", null);
        assertEquals("loop", result.arrivals().get(0).get("direction"));
    }

    // ===== resolveStopCode via reflection =====
    @Test void resolveStopCode_exact() throws Exception {
        assertEquals("PGP", invokeResolveStopCode("PGP"));
    }

    @Test void resolveStopCode_caseInsensitive() throws Exception {
        assertEquals("PGP", invokeResolveStopCode("pgp"));
    }

    @Test void resolveStopCode_partial() throws Exception {
        assertEquals("KR-MRT", invokeResolveStopCode("Kent Ridge"));
    }

    @Test void resolveStopCode_unknown_fallback() throws Exception {
        assertEquals("UNKNOWN-PLACE", invokeResolveStopCode("Unknown Place"));
    }

    @Test void resolveStopCode_chineseAlias() throws Exception {
        assertEquals("BIZ2", invokeResolveStopCode("商学院"));
    }

    @Test void resolveStopCode_englishAlias() throws Exception {
        assertEquals("CLB", invokeResolveStopCode("Central Library"));
    }

    // ===== BusArrivalsResult record =====
    @Test void busArrivalsResult_record() {
        var r = new NusBusProvider.BusArrivalsResult("COM3", List.of(Map.of("route", "A1")));
        assertEquals("COM3", r.stopName());
        assertEquals(1, r.arrivals().size());
    }

    // ===== Helpers =====
    private String invokeResolveStopCode(String input) throws Exception {
        Method m = NusBusProvider.class.getDeclaredMethod("resolveStopCode", String.class);
        m.setAccessible(true);
        return (String) m.invoke(provider, input);
    }

    @SuppressWarnings("unchecked")
    private void mockApiResponse(int statusCode, String body) throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(statusCode);
        when(mockResponse.body()).thenReturn(body);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);
    }

    private String buildApiResponse(String stopName, List<String> shuttleJsons) {
        return "{\"ShuttleServiceResult\":{\"name\":\"" + stopName + "\",\"shuttles\":["
                + String.join(",", shuttleJsons) + "]}}";
    }

    private String buildShuttle(String name, String busstopcode, List<Map<String, Object>> etas) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"name\":\"").append(name).append("\"");
        if (busstopcode != null) sb.append(",\"busstopcode\":\"").append(busstopcode).append("\"");
        if (etas != null) {
            sb.append(",\"_etas\":[");
            List<String> etaJsons = new ArrayList<>();
            for (Map<String, Object> eta : etas) {
                StringBuilder e = new StringBuilder("{");
                List<String> fields = new ArrayList<>();
                if (eta.containsKey("eta")) fields.add("\"eta\":" + eta.get("eta"));
                if (eta.containsKey("plate")) fields.add("\"plate\":\"" + eta.get("plate") + "\"");
                if (eta.containsKey("ts")) fields.add("\"ts\":\"" + eta.get("ts") + "\"");
                e.append(String.join(",", fields));
                e.append("}");
                etaJsons.add(e.toString());
            }
            sb.append(String.join(",", etaJsons));
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    private String buildShuttleRaw(String name, String busstopcode, List<Map<String, Object>> etas,
                                    String arrivalTime, String arrivalTimePlate,
                                    String nextArrivalTime, String nextArrivalTimePlate) {
        StringBuilder sb = new StringBuilder("{");
        List<String> fields = new ArrayList<>();
        if (name != null) fields.add("\"name\":\"" + name + "\"");
        else fields.add("\"name\":null");
        if (busstopcode != null) fields.add("\"busstopcode\":\"" + busstopcode + "\"");
        if (etas != null) {
            // If etas is provided, use it; otherwise legacy fields
            sb.append("\"_etas\":[");
        } else {
            // No _etas field → triggers legacy fallback
        }
        if (arrivalTime != null) fields.add("\"arrivalTime\":\"" + arrivalTime + "\"");
        if (arrivalTimePlate != null) fields.add("\"arrivalTime_veh_plate\":\"" + arrivalTimePlate + "\"");
        if (nextArrivalTime != null) fields.add("\"nextArrivalTime\":\"" + nextArrivalTime + "\"");
        if (nextArrivalTimePlate != null) fields.add("\"nextArrivalTime_veh_plate\":\"" + nextArrivalTimePlate + "\"");
        sb.append(String.join(",", fields));
        sb.append("}");
        return sb.toString();
    }
}
