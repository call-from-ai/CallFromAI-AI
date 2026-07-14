package com.example.aidatingagentbackend.exception;

public class ProactivePolicyRejectedException extends RuntimeException {
    public ProactivePolicyRejectedException(String message) {
        super(message);
    }
}
