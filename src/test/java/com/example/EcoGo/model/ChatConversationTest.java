package com.example.EcoGo.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ChatConversationTest {

    // ===== ChatConversation =====
    @Test void defaultConstructor() {
        ChatConversation c = new ChatConversation();
        assertNull(c.getId());
        assertNull(c.getConversationId());
        assertNull(c.getUserId());
        assertNull(c.getCreatedAt());
        assertNull(c.getUpdatedAt());
        assertNotNull(c.getMessages());
        assertNotNull(c.getState());
    }

    @Test void settersAndGetters() {
        ChatConversation c = new ChatConversation();
        c.setId("id_1");
        c.setConversationId("c_123");
        c.setUserId("u_001");
        Instant now = Instant.now();
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        List<ChatConversation.Message> msgs = new ArrayList<>();
        msgs.add(new ChatConversation.Message("user", "hello"));
        c.setMessages(msgs);
        ChatConversation.ConversationState state = new ChatConversation.ConversationState();
        state.setIntent("booking");
        c.setState(state);

        assertEquals("id_1", c.getId());
        assertEquals("c_123", c.getConversationId());
        assertEquals("u_001", c.getUserId());
        assertEquals(now, c.getCreatedAt());
        assertEquals(now, c.getUpdatedAt());
        assertEquals(1, c.getMessages().size());
        assertEquals("booking", c.getState().getIntent());
    }

    // ===== Message =====
    @Test void message_default() {
        ChatConversation.Message m = new ChatConversation.Message();
        assertNull(m.getRole());
        assertNull(m.getText());
        assertNull(m.getTimestamp());
        assertNotNull(m.getUiActions());
    }

    @Test void message_twoArgConstructor() {
        ChatConversation.Message m = new ChatConversation.Message("assistant", "hi");
        assertEquals("assistant", m.getRole());
        assertEquals("hi", m.getText());
        assertNotNull(m.getTimestamp());
    }

    @Test void message_setters() {
        ChatConversation.Message m = new ChatConversation.Message();
        m.setRole("user");
        m.setText("hello");
        Instant t = Instant.now();
        m.setTimestamp(t);
        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(Map.of("type", "SUGGESTIONS"));
        m.setUiActions(actions);

        assertEquals("user", m.getRole());
        assertEquals("hello", m.getText());
        assertEquals(t, m.getTimestamp());
        assertEquals(1, m.getUiActions().size());
    }

    // ===== ConversationState =====
    @Test void state_default() {
        ChatConversation.ConversationState s = new ChatConversation.ConversationState();
        assertNull(s.getIntent());
        assertNotNull(s.getPartialData());
    }

    @Test void state_setters() {
        ChatConversation.ConversationState s = new ChatConversation.ConversationState();
        s.setIntent("bus");
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        s.setPartialData(data);

        assertEquals("bus", s.getIntent());
        assertEquals("value", s.getPartialData().get("key"));
    }
}
