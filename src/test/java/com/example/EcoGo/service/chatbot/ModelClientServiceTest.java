package com.example.EcoGo.service.chatbot;

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

class ModelClientServiceTest {

    private ModelClientService service;
    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() throws Exception {
        service = new ModelClientService();

        // Inject mock HttpClient
        mockHttpClient = mock(HttpClient.class);
        Field f = ModelClientService.class.getDeclaredField("httpClient");
        f.setAccessible(true);
        f.set(service, mockHttpClient);

        // Set config fields
        setField("modelBaseUrl", "http://localhost:9000");
        setField("modelApiKey", "test-key");
        setField("modelName", "test-model");
        setField("modelEnabled", true);
    }

    private void setField(String name, Object value) throws Exception {
        Field f = ModelClientService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    // ===== isEnabled =====
    @Test void isEnabled_true() { assertTrue(service.isEnabled()); }

    @Test void isEnabled_false() throws Exception {
        setField("modelEnabled", false);
        assertFalse(service.isEnabled());
    }

    // ===== callModelForTool: disabled =====
    @Test void callModelForTool_disabled_returnsNull() throws Exception {
        setField("modelEnabled", false);
        assertNull(service.callModelForTool("hello"));
    }

    // ===== callModelForTool: success with tool call =====
    @Test
    @SuppressWarnings("unchecked")
    void callModelForTool_withToolCall() throws Exception {
        String responseBody = "{\"choices\":[{\"message\":{\"content\":\"ok\",\"tool_calls\":[{\"function\":{\"name\":\"create_booking\",\"arguments\":\"{\\\"fromName\\\":\\\"PGP\\\",\\\"toName\\\":\\\"CLB\\\"}\"}}]}}]}";
        mockApiResponse(200, responseBody);
        ModelClientService.ModelResult result = service.callModelForTool("book PGP to CLB");
        assertNotNull(result);
        assertNotNull(result.toolCall());
        assertEquals("create_booking", result.toolCall().name());
        assertEquals("PGP", result.toolCall().arguments().get("fromName"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void callModelForTool_withoutToolCall() throws Exception {
        String responseBody = "{\"choices\":[{\"message\":{\"content\":\"Hello! How can I help?\"}}]}";
        mockApiResponse(200, responseBody);
        ModelClientService.ModelResult result = service.callModelForTool("hello");
        assertNotNull(result);
        assertNull(result.toolCall());
        assertEquals("Hello! How can I help?", result.text());
    }

    @Test
    @SuppressWarnings("unchecked")
    void callModelForTool_non200_returnsNull() throws Exception {
        mockApiResponse(500, "error");
        assertNull(service.callModelForTool("hello"));
    }

    @Test void callModelForTool_exception_returnsNull() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any())).thenThrow(new RuntimeException("timeout"));
        assertNull(service.callModelForTool("hello"));
    }

    // ===== parseModelResponse via reflection =====
    @Test void parseModelResponse_withToolCall() throws Exception {
        String body = "{\"choices\":[{\"message\":{\"content\":\"\",\"tool_calls\":[{\"function\":{\"name\":\"get_bus_arrivals\",\"arguments\":\"{\\\"stopName\\\":\\\"COM3\\\"}\"}}]}}]}";
        ModelClientService.ModelResult result = invokeParseModelResponse(body);
        assertNotNull(result);
        assertNotNull(result.toolCall());
        assertEquals("get_bus_arrivals", result.toolCall().name());
    }

    @Test void parseModelResponse_noToolCalls() throws Exception {
        String body = "{\"choices\":[{\"message\":{\"content\":\"plain text response\"}}]}";
        ModelClientService.ModelResult result = invokeParseModelResponse(body);
        assertNotNull(result);
        assertNull(result.toolCall());
        assertEquals("plain text response", result.text());
    }

    @Test void parseModelResponse_emptyToolCalls() throws Exception {
        String body = "{\"choices\":[{\"message\":{\"content\":\"hello\",\"tool_calls\":[]}}]}";
        ModelClientService.ModelResult result = invokeParseModelResponse(body);
        assertNotNull(result);
        assertNull(result.toolCall());
    }

    @Test void parseModelResponse_nullFunctionName() throws Exception {
        String body = "{\"choices\":[{\"message\":{\"content\":\"\",\"tool_calls\":[{\"function\":{\"arguments\":\"{}\"}}]}}]}";
        ModelClientService.ModelResult result = invokeParseModelResponse(body);
        assertNotNull(result);
        assertNull(result.toolCall());
    }

    @Test void parseModelResponse_invalidJson() throws Exception {
        ModelClientService.ModelResult result = invokeParseModelResponse("not json");
        assertNull(result);
    }

    @Test void parseModelResponse_emptyChoices() throws Exception {
        String body = "{\"choices\":[]}";
        ModelClientService.ModelResult result = invokeParseModelResponse(body);
        assertNotNull(result);
        assertNull(result.toolCall());
    }

    @Test void parseModelResponse_updateUserToolCall() throws Exception {
        String body = "{\"choices\":[{\"message\":{\"content\":\"\",\"tool_calls\":[{\"function\":{\"name\":\"update_user\",\"arguments\":\"{\\\"userId\\\":\\\"u_001\\\",\\\"patch\\\":{\\\"nickname\\\":\\\"New\\\"}}\"}}]}}]}";
        ModelClientService.ModelResult result = invokeParseModelResponse(body);
        assertNotNull(result.toolCall());
        assertEquals("update_user", result.toolCall().name());
    }

    // ===== URL normalization =====
    @Test
    @SuppressWarnings("unchecked")
    void callModelForTool_trailingSlash() throws Exception {
        setField("modelBaseUrl", "http://localhost:9000/");
        String responseBody = "{\"choices\":[{\"message\":{\"content\":\"hi\"}}]}";
        mockApiResponse(200, responseBody);
        assertNotNull(service.callModelForTool("hello"));
    }

    // ===== Records =====
    @Test void modelResult_record() {
        var r = new ModelClientService.ModelResult("txt", null);
        assertEquals("txt", r.text());
        assertNull(r.toolCall());
    }

    @Test void toolCall_record() {
        var tc = new ModelClientService.ToolCall("fn", java.util.Map.of("k", "v"));
        assertEquals("fn", tc.name());
        assertEquals("v", tc.arguments().get("k"));
    }

    // ===== Helpers =====
    private ModelClientService.ModelResult invokeParseModelResponse(String body) throws Exception {
        Method m = ModelClientService.class.getDeclaredMethod("parseModelResponse", String.class);
        m.setAccessible(true);
        return (ModelClientService.ModelResult) m.invoke(service, body);
    }

    @SuppressWarnings("unchecked")
    private void mockApiResponse(int statusCode, String body) throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(statusCode);
        when(mockResponse.body()).thenReturn(body);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);
    }
}
