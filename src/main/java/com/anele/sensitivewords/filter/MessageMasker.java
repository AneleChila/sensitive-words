package com.anele.sensitivewords.filter;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Literal, longest-first matching. The original message is never modified before matching. */
@Component
public class MessageMasker {
    private static final String TOKEN_CHAR = "[\\p{L}\\p{M}\\p{N}_]";

    public String mask(String message, List<String> words) {
        if (words.isEmpty()) return message;
        String alternatives = words.stream().filter(w -> !w.isBlank()).distinct()
            .sorted(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()))
            .map(word -> Arrays.stream(word.split(" "))
                .map(Pattern::quote).collect(Collectors.joining("\\s+")))
            .collect(Collectors.joining("|"));
        if (alternatives.isEmpty()) return message;
        Pattern pattern = Pattern.compile("(?<!" + TOKEN_CHAR + ")(?:" + alternatives + ")(?!" + TOKEN_CHAR + ")",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(message);
        StringBuilder result = new StringBuilder(message.length());
        while (matcher.find()) {
            // One star per non-whitespace Unicode code point in the matched span.
            String replacement = matcher.group().codePoints()
                .mapToObj(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp)
                    ? new String(Character.toChars(cp)) : "*")
                .collect(Collectors.joining());
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
