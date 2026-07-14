package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.ErrorResponse;
import com.example.aidatingagentbackend.exception.GeminiCallException;
import com.example.aidatingagentbackend.exception.GeminiTimeoutException;
import com.example.aidatingagentbackend.exception.ProactivePolicyRejectedException;
import com.example.aidatingagentbackend.exception.RequestIdConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsIntegrationErrorsAndIncludesRequestId() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chat");
        request.addHeader(RequestIdSupport.HEADER, "req-9");

        assertStatus(handler.gatewayTimeout(new GeminiTimeoutException("timeout", null), request), HttpStatus.GATEWAY_TIMEOUT);
        assertStatus(handler.badGateway(new GeminiCallException("failed", null), request), HttpStatus.BAD_GATEWAY);
        assertStatus(handler.conflict(new RequestIdConflictException("conflict"), request), HttpStatus.CONFLICT);
        assertStatus(handler.proactiveRejected(new ProactivePolicyRejectedException("rejected"), request), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private void assertStatus(ResponseEntity<ErrorResponse> response, HttpStatus status) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().requestId()).isEqualTo("req-9");
    }
}
