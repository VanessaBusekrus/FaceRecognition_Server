package nl.cyberella.hands_on.services;

import nl.cyberella.hands_on.models.Login;
import nl.cyberella.hands_on.models.User;
import nl.cyberella.hands_on.dto.auth.SigninResult;
import nl.cyberella.hands_on.repositories.LoginRepository;
import nl.cyberella.hands_on.repositories.UserRepository;
import nl.cyberella.hands_on.services.interfaces.IAuthService;

import lombok.extern.slf4j.Slf4j;
import nl.cyberella.hands_on.audit.AuditLogger;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service // @Service annotation so it can be injected into other services/controllers
@Slf4j
public class AuthService implements IAuthService {


    // Repositories and utilities used by the service. These are injected
    // via constructor-based dependency injection (preferred in Spring).
    private final UserRepository userRepository;
    private final LoginRepository loginRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    /**
     * Construct the service with required dependencies.
     * Spring will provide the concrete beans for the repositories and
     * password encoder when creating this service.
     */
    public AuthService(UserRepository userRepository, LoginRepository loginRepository, BCryptPasswordEncoder passwordEncoder, AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.loginRepository = loginRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogger = auditLogger;
    }
    
    @Override
    public User register(String name, String email, String password) {
        // If a user with the same email already exists, signal failure by
        // returning null (controller handles this case and returns 400).
        if (userRepository.findByEmail(email).isPresent()) return null;

        // Create a new User entity and populate non-sensitive fields.
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setJoined(LocalDate.now());
        u.setEntries(0);

        // Persist the user. If the entity's id is null JPA will INSERT a new
        // row and populate generated values (e.g. the primary key) on `u`.
        // If an id is present JPA will perform an update/merge. After save
        // the returned/managed entity is tracked in the persistence context
        // so further changes within the same transaction will be persisted.
        userRepository.save(u);

        // Create the login record that stores the password hash separately.
        // This keeps authentication data in a dedicated table (Login).
        // -> creating a new Login entity instance (a plain Java object that will hold the credentials of email and the hased password) -> will then be persisted using the loginRepository
        Login l = new Login();
        l.setEmail(email);

        // Hash the raw password before saving. Never store plaintext passwords.
        l.setHash(passwordEncoder.encode(password));
        loginRepository.save(l);

        // Return the created user entity to the caller (controller will
        // convert/serialize it for the client).
        // -> Could also return a DTO if we wanted to avoid returning the full entity.
        return u;
    }

    @Override
    public SigninResult signin(String email, String password) {
        // Basic null-checks for safety — return null to indicate failure.
        if (email == null || password == null) return null;

        // Look up the Login record by email (login table stores the hashed password).
        var loginOpt = loginRepository.findById(email);
        if (loginOpt.isEmpty()) {
            // No login row — try to map to user id for non-PII logging
            Integer uid = null;
            try {
                var uOpt = userRepository.findByEmail(email);
                if (uOpt.isPresent()) uid = uOpt.get().getId();
            } catch (Exception ignore) {}
            log.warn("Authentication failed: no login record for id={}", uid == null ? "unknown" : uid);
            try { auditLogger.auditSignInAttempt(uid, false, nl.cyberella.hands_on.audit.AuditReason.NO_LOGIN_RECORD); } catch (Exception ignore) {}
            return null;
        }

        // Verify the provided password against the stored hash.
        boolean valid = passwordEncoder.matches(password, loginOpt.get().getHash());
        if (!valid) {
            Integer uid = null;
            try {
                var uOpt = userRepository.findByEmail(email);
                if (uOpt.isPresent()) uid = uOpt.get().getId();
            } catch (Exception ignore) {}
            log.warn("Authentication failed: password mismatch for id={}", uid == null ? "unknown" : uid);
            try { auditLogger.auditSignInAttempt(uid, false, nl.cyberella.hands_on.audit.AuditReason.PASSWORD_MISMATCH); } catch (Exception ignore) {}
            return null;
        }

        // Fetch the user profile associated with this email. It contains
        // user metadata (name, entries, 2FA flags) but not the password hash.
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("Authentication failed: user profile not found for email lookup (id=unknown)");
            try { auditLogger.auditSignInAttempt(null, false, nl.cyberella.hands_on.audit.AuditReason.USER_PROFILE_MISSING); } catch (Exception ignore) {}
            return null;
        }
        var user = userOpt.get();

        // If the user has 2FA enabled, return a SigninResult indicating that
        // second-factor verification is required. We include the userId so the
        // frontend can continue the 2FA flow without exposing sensitive data.
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            try { auditLogger.auditSignInAttempt(user.getId(), true, nl.cyberella.hands_on.audit.AuditReason.TWO_FA_REQUIRED); } catch (Exception ignore) {}
            return new SigninResult(true, user.getId(), null);
        }

        // Successful signin without 2FA — return the authenticated user.
    try { auditLogger.auditSignInAttempt(user.getId(), true, nl.cyberella.hands_on.audit.AuditReason.SUCCESS); } catch (Exception ignore) {}
        return new SigninResult(false, null, user);
    }

}
