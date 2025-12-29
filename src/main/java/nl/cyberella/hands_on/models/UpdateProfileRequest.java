package nl.cyberella.hands_on.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

/*
 * UpdateProfileRequest is a DTO for updating user profiles.
 * Only id is required; other fields are optional.
 * Uses Lombok for boilerplate and Jakarta validation for safe input.
 * Works with Spring controllers via @RequestBody and @Valid.
 */

@Data // generates getters, setters, toString(), equals(), hashCode()
@NoArgsConstructor // generates a no-argument constructor
public class UpdateProfileRequest {
    @NotNull(message = "id required")
    private Integer id;

    // name, phone and email are optional on update; keep nullable but if present controller will validate content
    private String name;

    private String phone;

    @Email(message = "invalid email")
    private String email;
}
