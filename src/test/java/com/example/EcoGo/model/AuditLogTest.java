package com.example.EcoGo.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    @Test void defaultConstructor() {
        AuditLog a = new AuditLog();
        assertNull(a.getId());
        assertNull(a.getAuditId());
        assertNull(a.getActorUserId());
        assertNull(a.getTargetUserId());
        assertNull(a.getAction());
        assertNotNull(a.getDetails());
        assertNull(a.getCreatedAt());
    }

    @Test void settersAndGetters() {
        AuditLog a = new AuditLog();
        a.setId("id_1");
        a.setAuditId("au_001");
        a.setActorUserId("admin_001");
        a.setTargetUserId("u_002");
        a.setAction("update_user");
        Map<String, Object> details = new HashMap<>();
        details.put("nickname", "NewName");
        a.setDetails(details);
        Instant now = Instant.now();
        a.setCreatedAt(now);

        assertEquals("id_1", a.getId());
        assertEquals("au_001", a.getAuditId());
        assertEquals("admin_001", a.getActorUserId());
        assertEquals("u_002", a.getTargetUserId());
        assertEquals("update_user", a.getAction());
        assertEquals("NewName", a.getDetails().get("nickname"));
        assertEquals(now, a.getCreatedAt());
    }
}
