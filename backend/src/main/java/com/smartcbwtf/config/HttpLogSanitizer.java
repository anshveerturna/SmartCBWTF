package com.smartcbwtf.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class HttpLogSanitizer {

    private static final int MAX_CORRELATION_ID_LENGTH = 128;
    private static final int MAX_LOG_VALUE_LENGTH = 256;
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._~:/+=,@-]+");
    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "access_token",
            "authorization",
            "client_secret",
            "code_challenge",
            "code_verifier",
            "code",
            "id_token",
            "key",
            "password",
            "refresh_token",
            "secret",
            "session_state",
            "state",
            "token");

    private HttpLogSanitizer() {
    }

    static String correlationIdOrNull(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null || trimmed.length() > MAX_CORRELATION_ID_LENGTH) {
            return null;
        }
        if (!SAFE_CORRELATION_ID.matcher(trimmed).matches() || containsControlCharacter(trimmed)) {
            return null;
        }
        return trimmed;
    }

    static String headerForLog(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return truncate(replaceControlCharacters(trimmed), MAX_LOG_VALUE_LENGTH);
    }

    static String queryForAudit(String queryString) {
        String trimmed = trimToNull(queryString);
        if (trimmed == null) {
            return null;
        }

        StringBuilder output = new StringBuilder();
        String[] parts = trimmed.split("&", -1);
        for (String part : parts) {
            if (output.length() > 0) {
                output.append('&');
            }

            int equalsIndex = part.indexOf('=');
            String rawKey = equalsIndex >= 0 ? part.substring(0, equalsIndex) : part;
            String rawValue = equalsIndex >= 0 ? part.substring(equalsIndex + 1) : null;
            String safeKey = truncate(replaceControlCharacters(rawKey), MAX_LOG_VALUE_LENGTH);

            output.append(safeKey);
            if (rawValue != null) {
                output.append('=');
                output.append(isSensitiveQueryKey(rawKey)
                        ? "[REDACTED]"
                        : truncate(replaceControlCharacters(rawValue), MAX_LOG_VALUE_LENGTH));
            }
        }
        return truncate(output.toString(), MAX_LOG_VALUE_LENGTH);
    }

    private static boolean isSensitiveQueryKey(String rawKey) {
        String decoded = decode(rawKey).toLowerCase(Locale.ENGLISH);
        if (SENSITIVE_QUERY_KEYS.contains(decoded)) {
            return true;
        }
        return decoded.contains("password")
                || decoded.contains("secret")
                || decoded.endsWith("token")
                || decoded.endsWith("key");
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean containsControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return true;
            }
        }
        return false;
    }

    private static String replaceControlCharacters(String value) {
        StringBuilder output = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            output.append(c < 0x20 || c == 0x7F ? ' ' : c);
        }
        return output.toString();
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
