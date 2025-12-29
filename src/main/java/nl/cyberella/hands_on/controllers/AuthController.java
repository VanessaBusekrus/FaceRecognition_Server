package nl.cyberella.hands_on.controllers;

import nl.cyberella.hands_on.dto.auth.RegisterRequest;
import nl.cyberella.hands_on.dto.auth.SigninRequest;
import nl.cyberella.hands_on.models.User;
import nl.cyberella.hands_on.dto.user.UserMapper;
import nl.cyberella.hands_on.dto.user.UserResponse;
import nl.cyberella.hands_on.models.UpdateProfileRequest;
import nl.cyberella.hands_on.services.UserService;
import nl.cyberella.hands_on.services.interfaces.IUserService;
import nl.cyberella.hands_on.utils.PasswordValidator;
import nl.cyberella.hands_on.services.interfaces.IAuthService;
import nl.cyberella.hands_on.audit.AuditLogger;
import lombok.extern.slf4j.Slf4j;
import nl.cyberella.hands_on.controllers.interfaces.IAuthController;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;


import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class AuthController implements IAuthController {
    // Services that this controller depends on
    private final IAuthService authService;
    private final IUserService userService;
    private final UserService userServiceImpl;
    private final AuditLogger auditLogger;

    // Constructor injection for the services. Whenever an AuthController gets created, this constructor is called.
    // Calling the interfaces here, so the controller is decoupled from the specific implementation
    public AuthController(IAuthService authService, IUserService userService, UserService userServiceImpl, AuditLogger auditLogger) {
        this.authService = authService;
        this.userService = userService;
        this.userServiceImpl = userServiceImpl;
        this.auditLogger = auditLogger;
    }

    /**
     * GET /profile/{id}
     * Fetch user profile by ID and return it as a UserResponse object (-> returns ResponseEntity,
     * containing any type of data (user info or error))
     */
    @GetMapping("/profile/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Integer id) { // @PathVariable annotation = taking te value from the {id} part of the URL and passing it as the id parameter to this method
        // var u = u will have the same type as whatever userService.findById(id) returns 
        // userService = the service object injected into the controller -> contains methods for working with user 
        // (e.g., finding by ID, saving, updating, etc.) 
        var u = userService.findById(id);
        if (u.isPresent()) {
            // Getting the user object from the Optional u.get()
            // Passing it to UserMapper.from() to convert it into a UserResponse object
            // Storing the result in the variable resp
            // This is done to not send the full user entity directly to the frontend 
            // User Response contains only safe data (name, email, phone) in a strcutured way
            UserResponse resp = UserMapper.from(u.get());
            return ResponseEntity.ok(resp);
        }
        // If user not found, return 404 error
        throw new EntityNotFoundException("User not found");
    }

    /**
     * PUT /updateprofile
     * Update user profile information such as name, phone, and email.
     */
    @PutMapping("/updateprofile")
    // @Valid annotation ensures that the object follows any validation annotations 
    // defined in the class (like @NotNull, @Email, etc.) -> see models/UpdateProfileRequest.java
    // @RequestBody UpdateProfileRequest req -> binds the JSON sent in the request body to a Java object req
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        if (req.getId() == null) throw new IllegalArgumentException("ID is required");

        // Audit: attempt to update profile for given ID (will be marked success later)
        Integer attemptId = req.getId();

        var userOpt = userService.findById(req.getId());
        if (userOpt.isEmpty()) {
            // Audit failure: no user to update
            try { auditLogger.auditUpdateProfileAttempt(attemptId, false); } catch (Exception ignore) {}
            throw new EntityNotFoundException("User not found");
        }
        var user = userOpt.get(); // extracting the User object with userOpt.get()

        // Update name if provided
        if (req.getName() != null) {
            if (req.getName().isBlank()) throw new IllegalArgumentException("Name cannot be empty");
            user.setName(req.getName());
        }

        // Update phone if provided
        if (req.getPhone() != null) {
            String phone = req.getPhone();
            // Simple regex to allow digits, spaces, +, -, parentheses
            if (!phone.matches("^[0-9 +()\\-]{6,20}$")) throw new IllegalArgumentException("Invalid phone format");
            user.setPhone(phone);
        }

        // Update email if provided
        if (req.getEmail() != null) {
            try {
                boolean ok = userService.changeEmail(user, req.getEmail());
                if (!ok) throw new IllegalArgumentException("Email already in use");
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid email");
            }
        }

        userService.save(user); // Save the updated user
        // Audit success
        try {
            auditLogger.auditUpdateProfileAttempt(attemptId, true);
        } catch (Exception ignore) {}
        UserResponse resp = UserMapper.from(user);
        return ResponseEntity.ok(resp);
    }

    /**
     * POST /register
     * Register a new user.
     */
    @PostMapping({"/register"})
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest body) {
        // Basic input validation
        if (body.name() == null || body.name().isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (body.email() == null || body.email().isBlank() || !body.email().contains("@")) {
            throw new IllegalArgumentException("Valid email is required.");
        }
        if (body.password() == null || body.password().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        // Validate password strength using PasswordValidator
        PasswordValidator.ValidationResult validation = PasswordValidator.validate(body.password());
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Password invalid: " + String.join(", ", validation.getErrors()));
        }

        String normalizedEmail = body.email().trim().toLowerCase(); // Normalize email
        // Application-level attempt log for registration — avoid logging email (PII)
        User u = authService.register(body.name(), normalizedEmail, body.password()); // Call service to create user
        if (u == null) {
            try { auditLogger.auditRegisterAttempt(null, false); } catch (Exception ignore) {}
            throw new IllegalArgumentException("Unable to register. Please verify your credentials.");
        }
        try { auditLogger.auditRegisterAttempt(u.getId(), true); } catch (Exception ignore) {}
            UserResponse resp = UserMapper.from(u);
            return ResponseEntity.ok(resp);
        }

    /**
     * POST /signin
     * Login a user with email and password.
     */
    @PostMapping({"/signin"})
    public ResponseEntity<?> signin(@Valid @RequestBody SigninRequest body) {
        // Basic input validation
        if (body.email() == null || body.email().isBlank() || body.password() == null || body.password().isBlank()) {
            throw new IllegalArgumentException("Email and password are required");
        }

        String normalizedEmail = body.email().trim().toLowerCase();
        // Authentication attempt (AuthService will perform auditing and reason reporting)

        var res = authService.signin(normalizedEmail, body.password()); // Call service to authenticate

        if (res == null) {
            // Authentication failed — AuthService records an audit entry with the reason.
            throw new BadCredentialsException("Invalid email or password");
        }

        // If credentials are valid but 2FA is required — treat credentials check as successful
    if (res.requiresTwoFactor()) {
        // AuthService records audit for 2FA-required signins.
        return ResponseEntity.ok(java.util.Map.of(
            "requiresTwoFactor", true,
            "userID", res.userId()
        ));
    }

        UserResponse resp = UserMapper.from(res.user()); // Take the full user from res.user(), convert it into a safe format using UserMapper.from(), and store it in resp to return to the frontend
        return ResponseEntity.ok(resp);
    }

    /**
     * VULNERABLE ENDPOINT - FOR EDUCATIONAL PURPOSES ONLY
     * 
     * GET /api/search?name=xxx
     * 
     * This endpoint demonstrates a SQL injection vulnerability by passing
     * unsanitized user input directly to a method that constructs SQL
     * queries using string concatenation.
     * 
     * SECURITY FLAW: The 'name' parameter from the HTTP request is passed
     * directly to searchByNameVulnerable() which concatenates it into raw SQL.
     * 
     * Example attack:
     *   GET /api/search?name=' OR '1'='1
     *   This would execute: SELECT * FROM users WHERE name LIKE '%' OR '1'='1%'
     *   Result: Returns ALL users in the database instead of filtering by name
     * 
     * CodeQL should detect the tainted data flow from @RequestParam to the SQL query.
     * 
     * NEVER USE THIS PATTERN IN PRODUCTION CODE!
     * 
     * @param name the search term from HTTP request (VULNERABLE)
     * @return list of users (or all users if exploited)
     */
    @GetMapping("/api/search")
    public ResponseEntity<?> searchUsersVulnerable(@RequestParam String name) {
        // VULNERABLE: User input flows directly from HTTP request to SQL query
        List<User> users = userServiceImpl.searchByNameVulnerable(name);
        return ResponseEntity.ok(users);
    }
}
