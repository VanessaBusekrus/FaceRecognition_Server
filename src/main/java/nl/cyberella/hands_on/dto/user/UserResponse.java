package nl.cyberella.hands_on.dto.user;

import java.time.LocalDate;

/**
 * DTO (Date Transfer Object) to send data to the frontend
 * 
 * REST model returned by the webservice for user profile data.
 *
 * This record intentionally excludes sensitive fields such as password
 * hashes and two-factor secrets to avoid accidental exposure.
 */
public record UserResponse(
        Integer id,
        String name,
        String email,
        LocalDate joined,
        Integer entries,
        String phone,
        Boolean twoFactorEnabled
) {}
