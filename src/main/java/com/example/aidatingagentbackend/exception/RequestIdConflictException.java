package com.example.aidatingagentbackend.exception;

public class RequestIdConflictException extends RuntimeException {
    public RequestIdConflictException(String message) {
        super(message);
    }
}
