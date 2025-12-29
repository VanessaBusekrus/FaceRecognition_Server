package nl.cyberella.hands_on.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Signin2FARequest(
    @NotNull(message = "userID required") Integer userID,
    @NotBlank(message = "code required") String code
) {
}
