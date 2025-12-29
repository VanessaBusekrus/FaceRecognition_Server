package nl.cyberella.hands_on.dto.auth;

// Imports for validation annotations
import jakarta.validation.constraints.Email; // ensures the field is a valid email format
import jakarta.validation.constraints.NotBlank; // ensures the field is not null, empty, or just whitespace

/**
 * DTO used by the registration endpoint. Converted to a record to
 * represent an immutable data carrier for request bodies.
 * Acts as a clean, safe way to pass data from HTTP request → backend controller
 * 
 * A record is used because:
 * - Simple and immutable, perfect for request bodies.
 * - Spring can automatically deserialize JSON into this record when a registration request is received.
 */

// Declaring a class with final fields and automatic constructor, getters, equals, hashCode, and toString.
// For example, name will have a getter method name().
public record RegisterRequest(
	@NotBlank(message = "name required") String name,
	@NotBlank(message = "email required") @Email(message = "invalid email") String email,
	@NotBlank(message = "password required") String password
) {
}
