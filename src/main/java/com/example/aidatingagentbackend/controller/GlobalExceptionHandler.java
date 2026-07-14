package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.ErrorResponse;
import com.example.aidatingagentbackend.exception.RequestIdConflictException;
import com.example.aidatingagentbackend.exception.GeminiCallException;
import com.example.aidatingagentbackend.exception.GeminiTimeoutException;
import com.example.aidatingagentbackend.exception.ProactivePolicyRejectedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(GeminiTimeoutException.class)
    public ResponseEntity<ErrorResponse> gatewayTimeout(GeminiTimeoutException exception, HttpServletRequest request) {
        return response(HttpStatus.GATEWAY_TIMEOUT, exception.getMessage(), request);
    }

    @ExceptionHandler(GeminiCallException.class)
    public ResponseEntity<ErrorResponse> badGateway(GeminiCallException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler(ProactivePolicyRejectedException.class)
    public ResponseEntity<ErrorResponse> proactiveRejected(
            ProactivePolicyRejectedException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler(RequestIdConflictException.class)
    public ResponseEntity<ErrorResponse> conflict(RequestIdConflictException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> status(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return response(status, exception.getReason(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> internalError(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(),
                RequestIdSupport.resolve(request)));
    }
}
