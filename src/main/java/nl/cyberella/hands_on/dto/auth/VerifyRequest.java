package nl.cyberella.hands_on.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;

public record VerifyRequest(
    @Email(message = "invalid email") String email,
    @NotNull(message = "userId required") Integer userId,
    @NotBlank(message = "token required") String token
) {
}
