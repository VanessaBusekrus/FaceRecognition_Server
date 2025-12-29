package nl.cyberella.hands_on.dto.auth;

import jakarta.validation.constraints.NotNull;

public record UserIdRequest(@NotNull(message = "userId required") Integer userId) {
}
