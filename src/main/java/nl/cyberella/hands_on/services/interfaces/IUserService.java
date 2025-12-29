package nl.cyberella.hands_on.services.interfaces;

import nl.cyberella.hands_on.models.User;

import java.util.Optional;

/**
IUserService is a service interface for user operations.
Provides common user management functions:
- Find user by ID
- Increment entries
- Create new user
- Save user
- Change email
 */
public interface IUserService {
    Optional<User> findById(Integer id);
    Optional<User> findByEmail(String email);
    Integer incrementEntries(Integer id, int faceCount);
    User createUser(String name, String email);
    User save(User user);
    boolean changeEmail(User user, String newEmail);
}
