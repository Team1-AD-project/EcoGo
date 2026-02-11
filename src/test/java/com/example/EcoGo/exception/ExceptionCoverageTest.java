package com.example.EcoGo.exception;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for BusinessException, GlobalExceptionHandler, and CustomErrorController.
 */
class ExceptionCoverageTest {

    // ==================== BusinessException ====================
    @Test
    void businessException_withErrorCode() {
        BusinessException ex = new BusinessException(ErrorCode.USER_NOT_FOUND);
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void businessException_withArgs() {
        BusinessException ex = new BusinessException(ErrorCode.PARAM_ERROR, "email is invalid");
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("email is invalid"));
    }

    @Test
    void businessException_differentCodes() {
        BusinessException ex1 = new BusinessException(ErrorCode.NOT_LOGIN);
        assertEquals(ErrorCode.NOT_LOGIN.getCode(), ex1.getCode());

        BusinessException ex2 = new BusinessException(ErrorCode.NO_PERMISSION);
        assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex2.getCode());
    }

    // ==================== GlobalExceptionHandler ====================
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessException() {
        BusinessException ex = new BusinessException(ErrorCode.USER_NOT_FOUND);
        ResponseMessage<Object> resp = handler.handlerBusinessException(ex);
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), resp.getCode());
        assertEquals("User not found", resp.getMessage());
        assertNull(resp.getData());
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleConstraintViolationException() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Field must not be null");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseMessage<Object> resp = handler.handlerConstraintViolationException(ex);
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), resp.getCode());
        assertEquals("Field must not be null", resp.getMessage());
    }

    @Test
    void handleMethodArgumentNotValidException() {
        BindingResult bindingResult = mock(BindingResult.class);
        ObjectError error = new ObjectError("field", "must not be blank");
        when(bindingResult.getAllErrors()).thenReturn(List.of(error));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseMessage<Object> resp = handler.handlerMethodArgumentNotValidException(ex);
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), resp.getCode());
        assertEquals("must not be blank", resp.getMessage());
    }

    @Test
    void handleSystemException() {
        Exception ex = new RuntimeException("Unexpected error");
        ResponseMessage<Object> resp = handler.handlerSystemException(ex);
        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), resp.getCode());
    }

    @Test
    void handleAccessDeniedException() {
        org.springframework.security.access.AccessDeniedException ex =
                new org.springframework.security.access.AccessDeniedException("Forbidden");
        ResponseMessage<Object> resp = handler.handlerAccessDeniedException(ex);
        assertEquals(ErrorCode.NO_PERMISSION.getCode(), resp.getCode());
    }

    // ==================== CustomErrorController ====================
    private final CustomErrorController errorController = new CustomErrorController();

    @Test
    void handleError_404() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(404);

        ResponseMessage<Void> resp = errorController.handleError(request);
        assertEquals(404, resp.getCode());
        assertEquals("Resource Not Found", resp.getMessage());
    }

    @Test
    void handleError_400() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(400);

        ResponseMessage<Void> resp = errorController.handleError(request);
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), resp.getCode());
    }

    @Test
    void handleError_500() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(500);

        ResponseMessage<Void> resp = errorController.handleError(request);
        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), resp.getCode());
    }

    @Test
    void handleError_unknownStatus() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(503);

        ResponseMessage<Void> resp = errorController.handleError(request);
        assertEquals(503, resp.getCode());
        assertTrue(resp.getMessage().contains("503"));
    }

    @Test
    void handleError_nullStatus() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(null);

        ResponseMessage<Void> resp = errorController.handleError(request);
        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), resp.getCode());
        assertEquals("Unknown Error", resp.getMessage());
    }
}
