package com.example.aidatingagentbackend.engine;

import java.util.Set;

public record MessageSignals(
        String normalizedMessage,
        Set<MessageSignalType> types
) {

    public boolean has(MessageSignalType type) {
        return types != null && types.contains(type);
    }

    public boolean hasAny(MessageSignalType... candidates) {
        if (types == null || candidates == null) {
            return false;
        }
        for (MessageSignalType candidate : candidates) {
            if (types.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
