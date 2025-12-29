package nl.cyberella.hands_on.controllers;

import nl.cyberella.hands_on.dto.auth.UserIdRequest;
import nl.cyberella.hands_on.dto.auth.VerifyRequest;
import nl.cyberella.hands_on.models.User;
import org.springframework.http.ResponseEntity;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Optional;
import nl.cyberella.hands_on.services.interfaces.ITwoFaService;
import nl.cyberella.hands_on.controllers.interfaces.ITwoFaController;

@RestController
public class TwoFaController implements ITwoFaController {

    private final ITwoFaService twoFaService;

    public TwoFaController(ITwoFaService twoFaService) {
        this.twoFaService = twoFaService;
    }

    @PostMapping("/enable-2fa")
    public ResponseEntity<?> enable(@Valid @RequestBody UserIdRequest body) throws Exception {
    Optional<User> userOpt = twoFaService.findUserById(body.userId());
        if (userOpt.isEmpty()) {
            throw new EntityNotFoundException("user not found");
        }

        // Generate a temporary secret and an otpauth URL for the user. The
        // frontend should show the otpauth_url as a QR code or let the user
        // copy the secret into their authenticator app.
        // Delegate to the user-based enable2fa overload to avoid another lookup
        var resp = twoFaService.enable2fa(userOpt.get());
        if (resp == null) {
            // Something went wrong during secret generation or save.
            throw new IllegalArgumentException("failed to enable 2FA");
        }

    // Return the manual secret and otpauth URL so the frontend can
    // render the QR code client-side and/or show the manual entry.
    return ResponseEntity.ok(java.util.Map.of(
        "manualEntry", resp.secret(),
        "otpauth_url", resp.otpauth_url()
    ));
    }

    @PostMapping("/verify-2fa-setup")
    public ResponseEntity<?> verifySetup(@Valid @RequestBody VerifyRequest body) {
        // After the user adds the temporary secret to their authenticator app,
        // the frontend will prompt them for a one-time token to verify setup.
        // We verify the token using the temporary secret stored on the user,
        // and if valid we promote the temporary secret to the permanent one.
        // Prefer ID-based verification when the frontend provides userId.
    Optional<User> userOpt = twoFaService.findUserById(body.userId());
        if (userOpt.isEmpty()) {
            // Return 400 for a bad request where the provided user id doesn't exist.
            throw new EntityNotFoundException("user not found");
        }

        int token;
        try {
            token = Integer.parseInt(body.token());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid token format");
        }

        boolean ok;
            ok = twoFaService.verifySetup(userOpt.get(), token);
        if (!ok) {
            throw new IllegalArgumentException("invalid token");
        }
        // Return a small JSON payload so the frontend can call response.json()
        return ResponseEntity.ok(java.util.Map.of("success", true));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyRequest body) {
        // This endpoint is used when a user must prove possession of their
        // authenticator (for example, during signin when 2FA is required).
        // It checks the provided token against the stored permanent secret.
    Optional<User> userOpt = twoFaService.findUserById(body.userId());
        if (userOpt.isEmpty()) {
            // Return 400 for a bad request where the provided user id doesn't exist.
            throw new EntityNotFoundException("user not found");
        }
        
        int token;
        try {
            token = Integer.parseInt(body.token());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid token format");
        }
        boolean ok;
        ok = twoFaService.verify(userOpt.get(), token);
        if (!ok) {
            throw new IllegalArgumentException("invalid token");
        }
        var user = userOpt.get();
        return ResponseEntity.ok(java.util.Map.of("user", java.util.Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "entries", user.getEntries(),
                "joined", user.getJoined(),
                "two_factor_enabled", user.getTwoFactorEnabled()
        )));
    }
}