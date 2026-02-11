package com.example.EcoGo.dto.chatbot;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BookingDetailDtoTest {

    @Test void defaultConstructor() {
        BookingDetailDto dto = new BookingDetailDto();
        assertNull(dto.getBookingId());
        assertNull(dto.getTripId());
        assertNull(dto.getFromName());
        assertNull(dto.getToName());
        assertNull(dto.getDepartAt());
        assertEquals(0, dto.getPassengers());
        assertNull(dto.getStatus());
        assertNull(dto.getCreatedAt());
    }

    @Test void allArgsConstructor() {
        Instant now = Instant.now();
        BookingDetailDto dto = new BookingDetailDto(
                "bk_1", "trip_1", "COM3", "UTown",
                "2026-02-11T10:00:00", 3, "confirmed", now);
        assertEquals("bk_1", dto.getBookingId());
        assertEquals("trip_1", dto.getTripId());
        assertEquals("COM3", dto.getFromName());
        assertEquals("UTown", dto.getToName());
        assertEquals("2026-02-11T10:00:00", dto.getDepartAt());
        assertEquals(3, dto.getPassengers());
        assertEquals("confirmed", dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test void settersAndGetters() {
        BookingDetailDto dto = new BookingDetailDto();
        dto.setBookingId("bk_2");
        dto.setTripId("trip_2");
        dto.setFromName("PGP");
        dto.setToName("CLB");
        dto.setDepartAt("2026-02-11T12:00");
        dto.setPassengers(2);
        dto.setStatus("pending");
        Instant now = Instant.now();
        dto.setCreatedAt(now);

        assertEquals("bk_2", dto.getBookingId());
        assertEquals("trip_2", dto.getTripId());
        assertEquals("PGP", dto.getFromName());
        assertEquals("CLB", dto.getToName());
        assertEquals("2026-02-11T12:00", dto.getDepartAt());
        assertEquals(2, dto.getPassengers());
        assertEquals("pending", dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test void nullFields() {
        BookingDetailDto dto = new BookingDetailDto(null, null, null, null, null, 0, null, null);
        assertNull(dto.getBookingId());
        assertNull(dto.getTripId());
    }
}
