package com.example.EcoGo.service.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockBusProviderTest {

    private MockBusProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockBusProvider();
    }

    // ===== Exact match =====
    @Test void getArrivals_COM2() {
        MockBusProvider.BusArrivalsResult r = provider.getArrivals("COM2", null);
        assertEquals("COM2", r.stopName());
        assertFalse(r.arrivals().isEmpty());
    }

    @Test void getArrivals_PGP() {
        MockBusProvider.BusArrivalsResult r = provider.getArrivals("PGP", null);
        assertEquals("PGP", r.stopName());
        assertEquals(3, r.arrivals().size());
    }

    @Test void getArrivals_UTown() {
        MockBusProvider.BusArrivalsResult r = provider.getArrivals("UTown", null);
        assertFalse(r.arrivals().isEmpty());
    }

    @Test void getArrivals_KR_MRT() {
        var r = provider.getArrivals("KR MRT", null);
        assertFalse(r.arrivals().isEmpty());
    }

    @Test void getArrivals_BIZ2() {
        var r = provider.getArrivals("BIZ2", null);
        assertEquals(2, r.arrivals().size());
    }

    @Test void getArrivals_TCOMS() {
        var r = provider.getArrivals("TCOMS", null);
        assertEquals(2, r.arrivals().size());
    }

    // ===== Chinese stops =====
    @Test void getArrivals_chinese_wuJieLu() {
        var r = provider.getArrivals("乌节路", null);
        assertFalse(r.arrivals().isEmpty());
    }

    @Test void getArrivals_chinese_binHaiWan() {
        var r = provider.getArrivals("滨海湾", null);
        assertFalse(r.arrivals().isEmpty());
    }

    @Test void getArrivals_chinese_laiFoShiFang() {
        var r = provider.getArrivals("莱佛士坊", null);
        assertFalse(r.arrivals().isEmpty());
    }

    @Test void getArrivals_chinese_niuCheShui() {
        var r = provider.getArrivals("牛车水", null);
        assertFalse(r.arrivals().isEmpty());
    }

    @Test void getArrivals_chinese_renMinGuangChang() {
        var r = provider.getArrivals("人民广场", null);
        assertFalse(r.arrivals().isEmpty());
    }

    // ===== English city stops =====
    @Test void getArrivals_Orchard() {
        var r = provider.getArrivals("Orchard", null);
        assertFalse(r.arrivals().isEmpty());
    }

    @Test void getArrivals_MarinaBay() {
        var r = provider.getArrivals("Marina Bay", null);
        assertFalse(r.arrivals().isEmpty());
    }

    @Test void getArrivals_Clementi() {
        var r = provider.getArrivals("Clementi", null);
        assertEquals(3, r.arrivals().size());
    }

    // ===== Null/blank stop → default (人民广场) =====
    @Test void getArrivals_nullStop() {
        var r = provider.getArrivals(null, null);
        assertEquals("人民广场", r.stopName());
        assertFalse(r.arrivals().isEmpty());
    }

    @Test void getArrivals_blankStop() {
        var r = provider.getArrivals("   ", null);
        assertEquals("人民广场", r.stopName());
    }

    // ===== Case-insensitive match =====
    @Test void getArrivals_caseInsensitive() {
        var r = provider.getArrivals("pgp", null);
        assertFalse(r.arrivals().isEmpty());
    }

    @Test void getArrivals_caseInsensitive_utown() {
        var r = provider.getArrivals("utown", null);
        assertFalse(r.arrivals().isEmpty());
    }

    // ===== Partial match =====
    @Test void getArrivals_partial() {
        var r = provider.getArrivals("Clem", null);
        assertFalse(r.arrivals().isEmpty());
    }

    // ===== Unknown stop → fallback generic =====
    @Test void getArrivals_unknownStop() {
        var r = provider.getArrivals("UnknownPlace123", null);
        assertEquals("UnknownPlace123", r.stopName());
        assertEquals(2, r.arrivals().size()); // Generic fallback: A1 + A2
    }

    // ===== Route filter =====
    @Test void getArrivals_routeFilter_match() {
        var r = provider.getArrivals("PGP", "A1");
        assertEquals(1, r.arrivals().size());
        assertEquals("A1", r.arrivals().get(0).get("route"));
    }

    @Test void getArrivals_routeFilter_noMatch_showsAll() {
        var r = provider.getArrivals("BIZ2", "D1");
        // D1 doesn't exist at BIZ2, so all routes shown
        assertEquals(2, r.arrivals().size());
    }

    @Test void getArrivals_routeFilter_caseInsensitive() {
        var r = provider.getArrivals("PGP", "a1");
        assertEquals(1, r.arrivals().size());
    }

    @Test void getArrivals_routeFilter_blank() {
        var r = provider.getArrivals("PGP", "  ");
        assertEquals(3, r.arrivals().size()); // No filter applied
    }

    @Test void getArrivals_routeFilter_null() {
        var r = provider.getArrivals("PGP", null);
        assertEquals(3, r.arrivals().size());
    }

    // ===== Arrival data fields =====
    @Test void arrival_containsExpectedFields() {
        var r = provider.getArrivals("COM2", null);
        Map<String, Object> first = r.arrivals().get(0);
        assertTrue(first.containsKey("route"));
        assertTrue(first.containsKey("direction"));
        assertTrue(first.containsKey("etaMinutes"));
        assertTrue(first.containsKey("status"));
    }

    // ===== BusArrivalsResult record =====
    @Test void busArrivalsResult_record() {
        var result = new MockBusProvider.BusArrivalsResult("test", List.of());
        assertEquals("test", result.stopName());
        assertTrue(result.arrivals().isEmpty());
    }
}
