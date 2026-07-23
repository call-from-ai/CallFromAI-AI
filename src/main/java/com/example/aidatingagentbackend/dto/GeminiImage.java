package com.example.aidatingagentbackend.dto;

import java.util.Arrays;

public record GeminiImage(byte[] data, String mimeType) {

    public GeminiImage {
        data = data == null ? new byte[0] : Arrays.copyOf(data, data.length);
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("image mime type is required");
        }
    }

    @Override
    public byte[] data() {
        return Arrays.copyOf(data, data.length);
    }
}
