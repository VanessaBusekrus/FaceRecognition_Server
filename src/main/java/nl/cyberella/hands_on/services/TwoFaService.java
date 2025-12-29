package nl.cyberella.hands_on.services;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import nl.cyberella.hands_on.models.User;
import nl.cyberella.hands_on.dto.twofa.EnableResponse;
import nl.cyberella.hands_on.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service // @Service annotation so it can be injected into other services/controllers
public class TwoFaService implements nl.cyberella.hands_on.services.interfaces.ITwoFaService {

    private final UserRepository userRepository;
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    public TwoFaService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Enable 2FA for the provided User instance. This method mutates and saves
     * the user by storing a temporary secret and returns the secret + otpauth URL.
     * This avoids repeated database lookups when the caller already has the user.
     */
    public EnableResponse enable2fa(User user) throws Exception {
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey(); // contains the raw string secret
        user.setTempTwoFactorSecret(secret); // store temp secret
        userRepository.save(user); // save user with temp secret

        String issuer = "hands_on";
        String otpAuth = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                URLEncoder.encode(issuer, StandardCharsets.UTF_8),
                URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8),
                URLEncoder.encode(secret, StandardCharsets.UTF_8),
                URLEncoder.encode(issuer, StandardCharsets.UTF_8));

        return new EnableResponse(secret, otpAuth);
    }

    // ID-based variant to avoid email lookups from the frontend
    public boolean verifySetup(User user, int token) {
        String secret = user.getTempTwoFactorSecret();
        if (secret == null) return false;
        boolean ok = gAuth.authorize(secret, token); // verify against temp secret
        if (ok) {
            user.setTwoFactorSecret(secret);
            user.setTwoFactorEnabled(true);
            user.setTempTwoFactorSecret(null);
            userRepository.save(user);
        }
        return ok;
    }

    public boolean verify(User user, int token) {
        String secret = user.getTwoFactorSecret();
        if (secret == null) return false;
        return gAuth.authorize(secret, token);
    }

    // secret: manual entry (base32). otpauth_url: standard otpauth URL.
    // EnableResponse is defined in nl.cyberella.hands_on.dto.twofa.EnableResponse

    public Optional<User> findUserById(Integer id) {
        return userRepository.findById(id);
    }
}
