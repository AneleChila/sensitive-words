package com.anele.sensitivewords.word;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TermNormalizer {
    public static final int MAX_LENGTH = 255;

    public String normalize(String input) {
        if (input == null || input.isBlank() || input.length() > MAX_LENGTH) throw invalid();
        String normalized = input.replaceAll("(?U)\\s+", " ").strip().toUpperCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.length() > MAX_LENGTH) throw invalid();
        return normalized;
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Term must contain 1 to 255 characters before and after normalization");
    }
}
