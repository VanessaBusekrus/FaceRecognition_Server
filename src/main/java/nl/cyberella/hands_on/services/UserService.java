package nl.cyberella.hands_on.services;

import nl.cyberella.hands_on.models.User;
import nl.cyberella.hands_on.repositories.UserRepository;
import nl.cyberella.hands_on.repositories.LoginRepository;
import nl.cyberella.hands_on.models.Login;
import nl.cyberella.hands_on.services.interfaces.IUserService;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.Optional;

/*
UserService is the main user management service.
Implements IUserService and provides concrete database operations.
Key responsibilities:
- Fetch users (findById)
- Increment image entries (incrementEntries)
- Create new users (createUser)
- Save/update users (save)
- Change email safely, including migrating login credentials (changeEmail)
Uses Spring Data JPA repositories and transaction management to ensure consistency.
*/

@Service // @Service annotation so it can be injected into other services/controllers
@Slf4j
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final LoginRepository loginRepository;

    public UserService(UserRepository userRepository, LoginRepository loginRepository) {
        this.userRepository = userRepository;
        this.loginRepository = loginRepository;
    }

    public Optional<User> findById(Integer id) {
        // Calling the user repository here because it is the data access layer (=> talks to the DB)
        var res = userRepository.findById(id);
        if (res.isEmpty()) {
            log.info("UserService.findById returned 0 results for id={}", id);
        }
        return res;
    }

    public Optional<User> findByEmail(String email) {
        var res = userRepository.findByEmail(email);
        if (res.isEmpty()) {
            log.info("UserService.findByEmail returned 0 results for email (id unknown)");
        }
        return res;
    }

    @Transactional
    public Integer incrementEntries(Integer id, int faceCount) {
        // Validate input to prevent integer overflow
        if (faceCount < 0) {
            throw new IllegalArgumentException("faceCount must be non-negative");
        }
        if (faceCount > 1000) {
            throw new IllegalArgumentException("faceCount exceeds maximum allowed value");
        }
        
        Optional<User> u = userRepository.findById(id);
        if (u.isEmpty()) return null;
        User user = u.get();
        if (user.getEntries() == null) user.setEntries(0);
        
        int currentEntries = user.getEntries();
        
        // Guard against integer overflow using Math.addExact (throws ArithmeticException on overflow)
        try {
            int newEntries = Math.addExact(currentEntries, faceCount);
            user.setEntries(newEntries);
        } catch (ArithmeticException e) {
            log.warn("Integer overflow prevented for user id={}, current={}, increment={}", 
                     id, currentEntries, faceCount);
            // Cap at Integer.MAX_VALUE instead of wrapping to negative
            user.setEntries(Integer.MAX_VALUE);
        }
        
        userRepository.save(user);
        return user.getEntries();
    }

    public User createUser(String name, String email) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setJoined(LocalDate.now());
        u.setEntries(0);
        u.setTwoFactorEnabled(false);
        return userRepository.save(u);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    /*
     * Change the user's email. Returns true on success, false if the new email is already taken.
     * This will also migrate the login row (copy hash to new email and delete old login) when present.
    

    Without @Transactional, the find and save are separate operations.
    If something goes wrong after updating entries but before saving, you might end up with partial or inconsistent data.
    With @Transactional, either all changes happen, or none happen → this is what “atomic update” means.
    */
    @Transactional
    public boolean changeEmail(User user, String newEmail) {
        if (newEmail == null || newEmail.isBlank() || !newEmail.contains("@")) throw new IllegalArgumentException("invalid email");
        if (userRepository.findByEmail(newEmail).isPresent()) return false;
        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        userRepository.save(user);

        // Migrate login row if exists
        var oldLoginOpt = loginRepository.findById(oldEmail);
        if (oldLoginOpt.isPresent()) {
            Login oldLogin = oldLoginOpt.get();
            Login newLogin = new Login();
            newLogin.setEmail(newEmail);
            newLogin.setHash(oldLogin.getHash());
            loginRepository.save(newLogin);
            loginRepository.delete(oldLogin);
        }
        return true;
    }
}
