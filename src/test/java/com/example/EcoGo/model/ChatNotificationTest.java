package com.example.EcoGo.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ChatNotificationTest {

    @Test void defaultConstructor() {
        ChatNotification n = new ChatNotification();
        assertNull(n.getId());
        assertNull(n.getNotificationId());
        assertNull(n.getUserId());
        assertNull(n.getType());
        assertNull(n.getTitle());
        assertNull(n.getBody());
        assertFalse(n.isRead());
        assertNull(n.getCreatedAt());
    }

    @Test void settersAndGetters() {
        ChatNotification n = new ChatNotification();
        n.setId("id_1");
        n.setNotificationId("nt_001");
        n.setUserId("u_001");
        n.setType("profile_updated");
        n.setTitle("Profile Updated");
        n.setBody("Your profile has been updated.");
        n.setRead(true);
        Instant now = Instant.now();
        n.setCreatedAt(now);

        assertEquals("id_1", n.getId());
        assertEquals("nt_001", n.getNotificationId());
        assertEquals("u_001", n.getUserId());
        assertEquals("profile_updated", n.getType());
        assertEquals("Profile Updated", n.getTitle());
        assertEquals("Your profile has been updated.", n.getBody());
        assertTrue(n.isRead());
        assertEquals(now, n.getCreatedAt());
    }
}
