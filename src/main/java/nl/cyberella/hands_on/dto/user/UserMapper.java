package nl.cyberella.hands_on.dto.user;

import nl.cyberella.hands_on.models.User;

/**
 * Small mapper from JPA entity to REST response model.
 * 
 * UserMapper is a static utility to convert User → UserResponse.
 * Helps separate internal entity models from API responses.
 * Keeps the API safe and prevents accidental exposure of sensitive data.
 */
public final class UserMapper {

    private UserMapper() {}

    public static UserResponse from(User u) {
        if (u == null) return null;
        return new UserResponse( // new UserResponse(...) = > Converts the JPA User entity to a DTO (UserResponse). Only includes safe fields that you want to expose via the API.
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getJoined(),
                u.getEntries(),
                u.getPhone(),
                u.getTwoFactorEnabled()
        );
    }
}
