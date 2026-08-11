package com.example.aidatingagentbackend.prompt;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class KoreanAddressResolver {

    public Address resolve(Integer userAge, String userGender, Integer characterAge, String characterGender) {
        Gender user = Gender.from(userGender);
        Gender character = Gender.from(characterGender);
        if (userAge == null || characterAge == null || characterAge >= userAge
                || user == Gender.UNKNOWN || character == Gender.UNKNOWN) {
            return Address.none();
        }

        String term = switch (character) {
            case MALE -> user == Gender.FEMALE ? "누나" : "형";
            case FEMALE -> user == Gender.FEMALE ? "언니" : "오빠";
            case UNKNOWN -> null;
        };
        return new Address(term, true);
    }

    public record Address(String term, boolean shouldUseKinshipTerm) {
        static Address none() {
            return new Address(null, false);
        }
    }

    private enum Gender {
        MALE, FEMALE, UNKNOWN;

        static Gender from(String value) {
            if (value == null || value.isBlank()) return UNKNOWN;
            return switch (value.strip().toUpperCase(Locale.ROOT)) {
                case "MALE", "M", "MAN", "남", "남성", "남자" -> MALE;
                case "FEMALE", "F", "WOMAN", "여", "여성", "여자" -> FEMALE;
                default -> UNKNOWN;
            };
        }
    }
}
