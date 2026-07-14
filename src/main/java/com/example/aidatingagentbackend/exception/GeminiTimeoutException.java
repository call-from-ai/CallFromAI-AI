package com.example.aidatingagentbackend.exception;

public class GeminiTimeoutException extends GeminiCallException {
    public GeminiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
