package com.example.EcoGo.dto.chatbot;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ChatResponseDtoTest {

    // ===== ChatResponseDto constructors & methods =====

    @Test void defaultConstructor() {
        ChatResponseDto dto = new ChatResponseDto();
        assertNotNull(dto.getServerTimestamp());
        assertNotNull(dto.getUiActions());
    }

    @Test void convIdAndTextConstructor() {
        ChatResponseDto dto = new ChatResponseDto("c1", "Hello");
        assertEquals("c1", dto.getConversationId());
        assertEquals("Hello", dto.getAssistant().getText());
        assertNotNull(dto.getServerTimestamp());
    }

    @Test void settersAndGetters() {
        ChatResponseDto dto = new ChatResponseDto();
        dto.setConversationId("c2");
        dto.setAssistant(new ChatResponseDto.AssistantMessage("text"));
        dto.setUiActions(new ArrayList<>());
        Instant now = Instant.now();
        dto.setServerTimestamp(now);

        assertEquals("c2", dto.getConversationId());
        assertEquals("text", dto.getAssistant().getText());
        assertEquals(now, dto.getServerTimestamp());
    }

    @Test void withUiAction() {
        ChatResponseDto dto = new ChatResponseDto("c1", "t");
        ChatResponseDto.UiAction action = new ChatResponseDto.UiAction("DEEPLINK", Map.of("url", "http://x"));
        dto.withUiAction(action);
        assertEquals(1, dto.getUiActions().size());
        assertEquals("DEEPLINK", dto.getUiActions().get(0).getType());
    }

    @Test void withSuggestions() {
        ChatResponseDto dto = new ChatResponseDto("c1", "t");
        dto.withSuggestions(List.of("opt1", "opt2"));
        assertEquals(1, dto.getUiActions().size());
        assertEquals("SUGGESTIONS", dto.getUiActions().get(0).getType());
        assertTrue(dto.getUiActions().get(0).getPayload().containsKey("options"));
    }

    @Test void withDeeplink() {
        ChatResponseDto dto = new ChatResponseDto("c1", "t");
        dto.withDeeplink("ecogo://trip/123");
        assertEquals(1, dto.getUiActions().size());
        assertEquals("DEEPLINK", dto.getUiActions().get(0).getType());
        assertEquals("ecogo://trip/123", dto.getUiActions().get(0).getPayload().get("url"));
    }

    @Test void withShowForm() {
        ChatResponseDto dto = new ChatResponseDto("c1", "t");
        List<Map<String, Object>> fields = List.of(Map.of("key", "name", "label", "Name"));
        dto.withShowForm("form1", "Title", fields);
        assertEquals(1, dto.getUiActions().size());
        assertEquals("SHOW_FORM", dto.getUiActions().get(0).getType());
        assertEquals("form1", dto.getUiActions().get(0).getPayload().get("formId"));
    }

    @Test void withShowConfirm() {
        ChatResponseDto dto = new ChatResponseDto("c1", "t");
        dto.withShowConfirm("Confirm?", "Are you sure?");
        assertEquals(1, dto.getUiActions().size());
        assertEquals("SHOW_CONFIRM", dto.getUiActions().get(0).getType());
        assertEquals("Confirm?", dto.getUiActions().get(0).getPayload().get("title"));
    }

    @Test void chainedBuilders() {
        ChatResponseDto dto = new ChatResponseDto("c1", "t")
                .withSuggestions(List.of("A"))
                .withDeeplink("ecogo://x")
                .withShowConfirm("T", "B");
        assertEquals(3, dto.getUiActions().size());
    }

    // ===== AssistantMessage =====
    @Test void assistantMessage_default() {
        ChatResponseDto.AssistantMessage msg = new ChatResponseDto.AssistantMessage();
        assertNull(msg.getText());
        assertNotNull(msg.getCitations());
    }

    @Test void assistantMessage_textOnly() {
        ChatResponseDto.AssistantMessage msg = new ChatResponseDto.AssistantMessage("text");
        assertEquals("text", msg.getText());
    }

    @Test void assistantMessage_textAndCitations() {
        List<ChatResponseDto.Citation> citations = List.of(new ChatResponseDto.Citation("T", "S", "Sn"));
        ChatResponseDto.AssistantMessage msg = new ChatResponseDto.AssistantMessage("text", citations);
        assertEquals("text", msg.getText());
        assertEquals(1, msg.getCitations().size());
    }

    @Test void assistantMessage_nullCitations() {
        ChatResponseDto.AssistantMessage msg = new ChatResponseDto.AssistantMessage("text", null);
        assertNotNull(msg.getCitations());
        assertTrue(msg.getCitations().isEmpty());
    }

    @Test void assistantMessage_setters() {
        ChatResponseDto.AssistantMessage msg = new ChatResponseDto.AssistantMessage();
        msg.setText("new");
        msg.setCitations(List.of(new ChatResponseDto.Citation()));
        assertEquals("new", msg.getText());
        assertEquals(1, msg.getCitations().size());
    }

    // ===== Citation =====
    @Test void citation_default() {
        ChatResponseDto.Citation c = new ChatResponseDto.Citation();
        assertNull(c.getTitle());
        assertNull(c.getSource());
        assertNull(c.getSnippet());
    }

    @Test void citation_allArgs() {
        ChatResponseDto.Citation c = new ChatResponseDto.Citation("Title", "Source", "Snippet");
        assertEquals("Title", c.getTitle());
        assertEquals("Source", c.getSource());
        assertEquals("Snippet", c.getSnippet());
    }

    @Test void citation_setters() {
        ChatResponseDto.Citation c = new ChatResponseDto.Citation();
        c.setTitle("T");
        c.setSource("S");
        c.setSnippet("Sn");
        assertEquals("T", c.getTitle());
        assertEquals("S", c.getSource());
        assertEquals("Sn", c.getSnippet());
    }

    // ===== UiAction =====
    @Test void uiAction_default() {
        ChatResponseDto.UiAction a = new ChatResponseDto.UiAction();
        assertNull(a.getType());
        assertNotNull(a.getPayload());
    }

    @Test void uiAction_allArgs() {
        ChatResponseDto.UiAction a = new ChatResponseDto.UiAction("DEEPLINK", Map.of("url", "x"));
        assertEquals("DEEPLINK", a.getType());
        assertEquals("x", a.getPayload().get("url"));
    }

    @Test void uiAction_nullPayload() {
        ChatResponseDto.UiAction a = new ChatResponseDto.UiAction("TEST", null);
        assertNotNull(a.getPayload());
        assertTrue(a.getPayload().isEmpty());
    }

    @Test void uiAction_setters() {
        ChatResponseDto.UiAction a = new ChatResponseDto.UiAction();
        a.setType("SHOW_FORM");
        a.setPayload(Map.of("key", "val"));
        assertEquals("SHOW_FORM", a.getType());
        assertEquals("val", a.getPayload().get("key"));
    }
}
