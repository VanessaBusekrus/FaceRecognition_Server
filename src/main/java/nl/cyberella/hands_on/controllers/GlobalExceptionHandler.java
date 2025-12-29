package nl.cyberella.hands_on.controllers;

import nl.cyberella.hands_on.dto.error.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/*
GlobalExceptionHandler centralizes error handling.
Converts exceptions into structured JSON with proper HTTP status codes.
Handles:
- Validation errors → 400
- Malformed JSON → 400
- Illegal arguments → 400
- Entity not found → 404
- Authentication failures → 401
- Database conflicts → 409
- Any other exceptions → 500
Logs errors with appropriate severity (debug, warn, info, error). 


How it works in practice:
1. A controller method throws an exception (e.g., IllegalArgumentException).
2. Spring detects the exception and looks for a matching @ExceptionHandler method in GlobalExceptionHandler
3. The corresponding method is invoked, which:
   - Logs the error
   - Constructs an ErrorResponse object with details
   - Returns a ResponseEntity with the ErrorResponse and appropriate HTTP status code
4. Spring serializes the ErrorResponse to JSON and sends it back to the client.
*/

/*
RestControllerAdvice:
1. Detects exceptions thrown by any controller
2. Converts them into JSON responses
3. Applies globally across the application
*/
@RestControllerAdvice // Applies globally to all controllers. It catches specific exceptions thrown anywhere in the controllers or services and converts them into clean, consistent JSON responses.
@Slf4j
public class GlobalExceptionHandler {

    // Get current timestamp
    private String now() {
        return LocalDateTime.now().toString();
    }

    /**
     * Sanitize user-controlled input before logging to prevent log injection attacks.
     * Removes control characters, normalizes whitespace and replaces any remaining
     * non-printable characters to ensure the value cannot break log structure.
     *
     * @param input the user-controlled string (e.g., URI, parameter)
     * @return sanitized string safe for logging
     */
    private String sanitizeForLog(String input) {
        if (input == null) {
            return "<null>";
        }
        // Remove all control characters (includes \r, \n, \t and others)
        String sanitized = input.replaceAll("\\p{Cntrl}", "");
        // Normalize any remaining whitespace to a single space
        sanitized = sanitized.replaceAll("\\s+", " ");
        // Replace any remaining non-printable characters with an underscore
        sanitized = sanitized.replaceAll("[^\\p{Print}]", "_");
        return sanitized;
    }

    /*
    Constructs an ErrorResponse object, which will be returned as JSON.
    fieldErrors is optional and used for validation errors.

    Generic error builder -> Creates consistent JSON response objects

    */
    private ErrorResponse build(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(status, error, message, path, now(), fieldErrors);
    }

    /* Each method is annotated with @ExceptionHandler for a specific exception type. */

    /* Validation errors:
     * Triggered when @Valid fails on request bodies.
     * Collects field-specific errors into a map.
     * Returns HTTP 400 Bad Request with details about which fields failed.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
            .stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));

        ErrorResponse body = build(HttpStatus.BAD_REQUEST.value(), "Bad Request", "Validation failed", req.getRequestURI(), fieldErrors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /* Malformed JSON:
     * Triggered if the JSON body cannot be parsed.
     * Returns HTTP 400 Bad Request with a generic message: “Malformed JSON request”.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
    log.warn("Malformed JSON request", ex);
        ErrorResponse body = build(HttpStatus.BAD_REQUEST.value(), "Bad Request", "Malformed JSON request", req.getRequestURI(), null);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /* Illegal arguments:
     * Handles invalid input parameters (like blank name, invalid email).
     * Returns HTTP 400 Bad Request with the exception’s message.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
    log.warn("Illegal argument: {}", ex.getMessage());
        ErrorResponse body = build(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), req.getRequestURI(), null);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /* Entity not found:
     * Handles cases where a requested entity (like User) does not exist.
     * Returns HTTP 404 Not Found with the exception’s message.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex, HttpServletRequest req) {
    log.info("Entity not found: {}", ex.getMessage());
        ErrorResponse body = build(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), req.getRequestURI(), null);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /* Authentication failed:
     * Handles authentication errors.
     * Returns HTTP 401 Unauthorized with the exception’s message.
     */
    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
    log.info("Authentication failed: {}", ex.getMessage());
        ErrorResponse body = build(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", ex.getMessage(), req.getRequestURI(), null);
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    /* Data integrity violation:
     * Handles database constraint violations (like unique key violations).
     * Returns HTTP 409 Conflict with a generic message.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex, HttpServletRequest req) {
    log.warn("Data integrity violation: {}", ex.getMessage());
        ErrorResponse body = build(HttpStatus.CONFLICT.value(), "Conflict", "Data integrity violation", req.getRequestURI(), null);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /* Catch-all for other exceptions
     * Handles any unanticipated exceptions.
     * Returns HTTP 500 Internal Server Error with a generic message.
     * Logs the full exception for debugging.
     * URI is sanitized to prevent log injection attacks.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest req) {
        // Sanitize URI to prevent log injection (CWE-117)
        String safeUri = sanitizeForLog(req.getRequestURI());
        log.error("Unhandled exception for request {}", safeUri, ex);
        ErrorResponse body = build(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", "An unexpected error occurred", req.getRequestURI(), null);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
