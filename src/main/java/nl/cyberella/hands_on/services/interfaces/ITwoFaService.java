package nl.cyberella.hands_on.services.interfaces;

import nl.cyberella.hands_on.models.User;
import nl.cyberella.hands_on.dto.twofa.EnableResponse;

import java.util.Optional;

/*/
ITwoFaService defines the 2FA workflow:
- enable2fa → start 2FA setup for a user.
- verifySetup → confirm temporary secret during setup.
- verify → validate tokens during normal login.
- findUserById → safely fetch users by ID.
*/

public interface ITwoFaService {
    EnableResponse enable2fa(User user) throws Exception;
    boolean verifySetup(User user, int token);
    boolean verify(User user, int token);
    Optional<User> findUserById(Integer id);
}
