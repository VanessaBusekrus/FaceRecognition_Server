package nl.cyberella.hands_on.dto.error;

import java.util.Map;

/**
 * Generic error response returned by the application's global exception handler.
 * Uses a record to keep the response compact and immutable.
 */
public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    String timestamp,
    Map<String, String> fieldErrors
) {}
