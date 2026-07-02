package com.smartcbwtf.config;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    @Test
    void malformedRequestBodyReturnsBadRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/location/update");

        var response = handler.handleUnreadableMessage(
                new HttpMessageNotReadableException("bad json"),
                request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed request body", response.getBody().getMessage());
        assertEquals("/api/location/update", response.getBody().getPath());
    }

    @Test
    void constraintViolationsReturnBadRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/master-data/hcfs");

        var response = handler.handleConstraintViolation(
                new ConstraintViolationException("bad query", Set.of()),
                request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertEquals("/api/admin/master-data/hcfs", response.getBody().getPath());
        assertTrue(response.getBody().getDetails().containsKey("errors"));
    }

    @Test
    void responseStatusExceptionsPreserveIntendedStatus() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/hcfs/id/approve");

        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.GONE,
                        "This legacy workflow endpoint has been retired."),
                request);

        assertEquals(HttpStatus.GONE, response.getStatusCode());
        assertEquals("This legacy workflow endpoint has been retired.", response.getBody().getMessage());
        assertEquals("/api/hcfs/id/approve", response.getBody().getPath());
    }
}
