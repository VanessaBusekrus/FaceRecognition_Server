package nl.cyberella.hands_on.dto.auth;

import nl.cyberella.hands_on.models.User; // importing the user entity from models

/*
 * SigninResult is a simple, immutable DTO for sign-in responses.
 * It tells the frontend:
 * - Does the user need 2FA?
 * - Which user is signing in?
 * - Optionally provides the user object for convenience.
 * 
 * Using a record keeps it concise, immutable, and type-safe
 */

public record SigninResult(boolean requiresTwoFactor, Integer userId, User user) {}
