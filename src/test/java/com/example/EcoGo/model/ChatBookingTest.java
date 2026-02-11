package com.example.EcoGo.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ChatBookingTest {

    @Test void defaultConstructor() {
        ChatBooking b = new ChatBooking();
        assertNull(b.getId());
        assertNull(b.getBookingId());
        assertNull(b.getTripId());
        assertNull(b.getUserId());
        assertNull(b.getFromName());
        assertNull(b.getToName());
        assertNull(b.getDepartAt());
        assertEquals(0, b.getPassengers());
        assertNull(b.getStatus());
        assertNull(b.getCreatedAt());
    }

    @Test void settersAndGetters() {
        ChatBooking b = new ChatBooking();
        b.setId("id_1");
        b.setBookingId("bk_1");
        b.setTripId("trip_1");
        b.setUserId("u_001");
        b.setFromName("COM3");
        b.setToName("UTown");
        b.setDepartAt("2026-02-11T10:00:00");
        b.setPassengers(3);
        b.setStatus("confirmed");
        Instant now = Instant.now();
        b.setCreatedAt(now);

        assertEquals("id_1", b.getId());
        assertEquals("bk_1", b.getBookingId());
        assertEquals("trip_1", b.getTripId());
        assertEquals("u_001", b.getUserId());
        assertEquals("COM3", b.getFromName());
        assertEquals("UTown", b.getToName());
        assertEquals("2026-02-11T10:00:00", b.getDepartAt());
        assertEquals(3, b.getPassengers());
        assertEquals("confirmed", b.getStatus());
        assertEquals(now, b.getCreatedAt());
    }
}
