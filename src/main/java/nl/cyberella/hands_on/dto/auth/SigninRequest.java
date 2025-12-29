package nl.cyberella.hands_on.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SigninRequest(
    @NotBlank(message = "email required") @Email(message = "invalid email") String email,
    @NotBlank(message = "password required") String password
) {
}
