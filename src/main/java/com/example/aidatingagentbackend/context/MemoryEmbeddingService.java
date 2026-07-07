package com.example.aidatingagentbackend.context;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
public class MemoryEmbeddingService {

    private static final int DIMENSIONS = 128;

    private static final List<SemanticAlias> SEMANTIC_ALIASES = List.of(
            new SemanticAlias("gift", List.of("선물", "생일", "기념일", "챙겨", "축하")),
            new SemanticAlias("breakup", List.of("헤어", "이별", "끝내", "그만 만나", "관계 종료")),
            new SemanticAlias("apology", List.of("미안", "사과", "잘못", "화해", "용서")),
            new SemanticAlias("affection", List.of("사랑", "좋아", "보고 싶", "그리워", "애정")),
            new SemanticAlias("stress", List.of("힘들", "지쳤", "피곤", "스트레스", "고생")),
            new SemanticAlias("date", List.of("데이트", "만나", "같이", "함께", "카페", "영화")),
            new SemanticAlias("trust", List.of("믿", "솔직", "진심", "약속", "거짓말")),
            new SemanticAlias("jealousy", List.of("질투", "다른 사람", "전 애인", "전남친", "전여친"))
    );

    public double[] embed(String text) {
        double[] vector = new double[DIMENSIONS];
        if (!StringUtils.hasText(text)) {
            return vector;
        }

        String normalized = text.toLowerCase();
        Arrays.stream(normalized.split("[^a-z0-9가-힣]+"))
                .filter(token -> token.length() >= 2)
                .forEach(token -> addToken(vector, token, 1.0));

        SEMANTIC_ALIASES.stream()
                .filter(alias -> alias.matches(normalized))
                .forEach(alias -> addToken(vector, alias.key(), 2.0));

        normalize(vector);
        return vector;
    }

    public String serialize(double[] vector) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(vector[i]);
        }

        return builder.toString();
    }

    public double[] deserialize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String[] parts = value.split(",");
        if (parts.length != DIMENSIONS) {
            return null;
        }

        double[] vector = new double[DIMENSIONS];
        try {
            for (int i = 0; i < parts.length; i++) {
                vector[i] = Double.parseDouble(parts[i]);
            }
        } catch (NumberFormatException exception) {
            return null;
        }

        return vector;
    }

    public double cosineSimilarity(double[] left, double[] right) {
        if (left == null || right == null || left.length != right.length) {
            return 0.0;
        }

        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private void addToken(double[] vector, String token, double weight) {
        int index = Math.floorMod(hash(token), vector.length);
        vector[index] += weight;
    }

    private int hash(String token) {
        byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
        int hash = 0;
        for (byte current : bytes) {
            hash = 31 * hash + current;
        }

        return hash;
    }

    private void normalize(double[] vector) {
        double norm = 0.0;
        for (double value : vector) {
            norm += value * value;
        }
        if (norm == 0.0) {
            return;
        }

        double divisor = Math.sqrt(norm);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / divisor;
        }
    }

    private record SemanticAlias(String key, List<String> keywords) {

        boolean matches(String text) {
            return keywords.stream().anyMatch(text::contains);
        }
    }
}
