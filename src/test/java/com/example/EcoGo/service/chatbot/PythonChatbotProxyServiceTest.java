package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.dto.chatbot.ChatResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PythonChatbotProxyServiceTest {

    private PythonChatbotProxyService service;
    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() throws Exception {
        service = new PythonChatbotProxyService();

        mockHttpClient = mock(HttpClient.class);
        Field f = PythonChatbotProxyService.class.getDeclaredField("httpClient");
        f.setAccessible(true);
        f.set(service, mockHttpClient);

        setField("pythonBaseUrl", "http://localhost:8000");
        setField("pythonEnabled", true);
        setField("timeoutSeconds", 5);
    }

    private void setField(String name, Object value) throws Exception {
        Field f = PythonChatbotProxyService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    // ===== isEnabled =====
    @Test void isEnabled_true() { assertTrue(service.isEnabled()); }

    @Test void isEnabled_false() throws Exception {
        setField("pythonEnabled", false);
        assertFalse(service.isEnabled());
    }

    // ===== forwardChat: disabled =====
    @Test void forwardChat_disabled() throws Exception {
        setField("pythonEnabled", false);
        assertNull(service.forwardChat("u1", "user", "c1", "hi"));
    }

    // ===== forwardChat: success =====
    @Test
    @SuppressWarnings("unchecked")
    void forwardChat_success() throws Exception {
        String responseBody = "{\"conversationId\":\"c_123\",\"assistant\":{\"text\":\"Hello!\",\"citations\":[{\"title\":\"T\",\"source\":\"S\",\"snippet\":\"Sn\"}]},\"uiActions\":[{\"type\":\"SUGGESTIONS\",\"payload\":{\"options\":[\"opt1\"]}}]}";
        mockApiResponse(200, responseBody);
        ChatResponseDto result = service.forwardChat("u1", "user", "c1", "hello");
        assertNotNull(result);
        assertEquals("c_123", result.getConversationId());
        assertEquals("Hello!", result.getAssistant().getText());
        assertEquals(1, result.getAssistant().getCitations().size());
        assertEquals("T", result.getAssistant().getCitations().get(0).getTitle());
        assertEquals(1, result.getUiActions().size());
        assertEquals("SUGGESTIONS", result.getUiActions().get(0).getType());
    }

    @Test
    @SuppressWarnings("unchecked")
    void forwardChat_noCitations() throws Exception {
        String responseBody = "{\"assistant\":{\"text\":\"Hi there\"}}";
        mockApiResponse(200, responseBody);
        ChatResponseDto result = service.forwardChat("u1", "user", "c1", "hi");
        assertNotNull(result);
        assertEquals("c1", result.getConversationId());
        assertEquals("Hi there", result.getAssistant().getText());
    }

    @Test
    @SuppressWarnings("unchecked")
    void forwardChat_noUiActions() throws Exception {
        String responseBody = "{\"conversationId\":\"c_456\",\"assistant\":{\"text\":\"Response\"}}";
        mockApiResponse(200, responseBody);
        ChatResponseDto result = service.forwardChat("u1", "user", "c1", "hi");
        assertNotNull(result);
        assertTrue(result.getUiActions().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void forwardChat_non200() throws Exception {
        mockApiResponse(500, "error");
        assertNull(service.forwardChat("u1", "user", "c1", "hi"));
    }

    @Test void forwardChat_exception() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any())).thenThrow(new RuntimeException("connection refused"));
        assertNull(service.forwardChat("u1", "user", "c1", "hi"));
    }

    // ===== parsePythonResponse via reflection =====
    @Test void parsePythonResponse_full() throws Exception {
        String body = "{\"conversationId\":\"c_999\",\"assistant\":{\"text\":\"Full response\",\"citations\":[{\"title\":\"Title1\",\"source\":\"src1\",\"snippet\":\"snip1\"}]},\"uiActions\":[{\"type\":\"DEEPLINK\",\"payload\":{\"url\":\"https://example.com\"}}]}";
        ChatResponseDto result = invokeParsePythonResponse(body, "fallback");
        assertNotNull(result);
        assertEquals("c_999", result.getConversationId());
        assertEquals("Full response", result.getAssistant().getText());
        assertEquals(1, result.getAssistant().getCitations().size());
        assertEquals(1, result.getUiActions().size());
        assertEquals("DEEPLINK", result.getUiActions().get(0).getType());
    }

    @Test void parsePythonResponse_noConversationId_fallback() throws Exception {
        String body = "{\"assistant\":{\"text\":\"No conv id\"}}";
        ChatResponseDto result = invokeParsePythonResponse(body, "fallback_id");
        assertEquals("fallback_id", result.getConversationId());
    }

    @Test void parsePythonResponse_emptyAssistant() throws Exception {
        String body = "{\"assistant\":{}}";
        ChatResponseDto result = invokeParsePythonResponse(body, "c1");
        assertNotNull(result);
        assertEquals("", result.getAssistant().getText());
    }

    @Test void parsePythonResponse_invalidJson() throws Exception {
        ChatResponseDto result = invokeParsePythonResponse("not json", "c1");
        assertNull(result);
    }

    @Test void parsePythonResponse_uiActionWithEmptyPayload() throws Exception {
        String body = "{\"assistant\":{\"text\":\"Hi\"},\"uiActions\":[{\"type\":\"SUGGESTIONS\"}]}";
        ChatResponseDto result = invokeParsePythonResponse(body, "c1");
        assertNotNull(result);
        assertEquals(1, result.getUiActions().size());
    }

    @Test void parsePythonResponse_multipleCitations() throws Exception {
        String body = "{\"assistant\":{\"text\":\"hi\",\"citations\":[{\"title\":\"T1\",\"source\":\"S1\",\"snippet\":\"Sn1\"},{\"title\":\"T2\",\"source\":\"S2\",\"snippet\":\"Sn2\"}]}}";
        ChatResponseDto result = invokeParsePythonResponse(body, "c1");
        assertEquals(2, result.getAssistant().getCitations().size());
    }

    // ===== URL normalization =====
    @Test
    @SuppressWarnings("unchecked")
    void forwardChat_trailingSlash() throws Exception {
        setField("pythonBaseUrl", "http://localhost:8000/");
        String responseBody = "{\"assistant\":{\"text\":\"OK\"}}";
        mockApiResponse(200, responseBody);
        assertNotNull(service.forwardChat("u1", "user", "c1", "hello"));
    }

    // ===== Helpers =====
    private ChatResponseDto invokeParsePythonResponse(String body, String fallbackConvId) throws Exception {
        Method m = PythonChatbotProxyService.class.getDeclaredMethod("parsePythonResponse", String.class, String.class);
        m.setAccessible(true);
        return (ChatResponseDto) m.invoke(service, body, fallbackConvId);
    }

    @SuppressWarnings("unchecked")
    private void mockApiResponse(int statusCode, String body) throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(statusCode);
        when(mockResponse.body()).thenReturn(body);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);
    }
}
