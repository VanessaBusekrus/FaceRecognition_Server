package nl.cyberella.hands_on.dto.twofa;

/**
 * DTO returned by the 2FA enable flow.
 * - secret: manual base32 secret the user can type into an authenticator app
 * - otpauth_url: otpauth:// URL suitable for QR code generation
 */
public record EnableResponse(String secret, String otpauth_url) {}
