package nl.cyberella.hands_on.services;

import nl.cyberella.hands_on.audit.AuditLogger;
import nl.cyberella.hands_on.dto.auth.SigninResult;
import nl.cyberella.hands_on.models.Login;
import nl.cyberella.hands_on.models.User;
import nl.cyberella.hands_on.repositories.LoginRepository;
import nl.cyberella.hands_on.repositories.UserRepository;
// import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import nl.cyberella.hands_on.audit.AuditReason;

// AuthServiceTest tests AuthService in isolation by mocking its dependencies (UserRepository, LoginRepository, BCryptPasswordEncoder, AuditLogger)
@ExtendWith(MockitoExtension.class) // Enable Mockito in JUnit 5
public class AuthServiceTest {

    @Mock // Mocked dependency
    private UserRepository userRepository;

    @Mock
    private LoginRepository loginRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks // Injecting the mocks into the service under test (AuthService)
    private AuthService authService;

    private final String testEmail = "test@example.com";
    private final String testPassword = "Hello@1234!";

    // Commented out setup method as not needed currently
    // @BeforeEach // runs before each test method to set up a clean test environment
    // void setup() {
    //     // initialize or reset common test data here
    // }

    // Arrange-Act-Assert pattern used in tests
    // Tests for register method
    @Test
    void register_success_createsUserAndLogin() {
        // 1. Arrange: set up mock behavior for this test case
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty()); // Simulate that no existing user has this email (email is available)
        when(passwordEncoder.encode(testPassword)).thenReturn("hashedpw"); // Simulate password hashing — the encoder will return this fake hash

        // 2. Act: call the method under test, in this case register
        doAnswer(inv -> {     // Simulate JPA's save() assigning an auto-generated ID when saving a User
            User u = inv.getArgument(0);
            u.setId(42); // pretend the database assigned ID = 42
            return u;
        }).when(userRepository).save(any(User.class)); // simulate saving user, setting a mock ID

        // Simulate saving a Login object — just return the same object that was passed in
        when(loginRepository.save(any(Login.class))).thenAnswer(inv -> inv.getArgument(0));

        // register the user with test data
        User created = authService.register("Tester", testEmail, testPassword);

        // 3. Assert: verify results and interactions
        // Check that a User object was returned and contains expected values
        assertNotNull(created); // user should be created
        assertEquals("Tester", created.getName()); // verify name
        assertEquals(testEmail, created.getEmail()); // verify email
        assertNotNull(created.getJoined()); // verify joined date is set
        assertEquals(0, created.getEntries()); // verify initial entries is 0

        // Verify repository interactions
        verify(userRepository, times(1)).findByEmail(testEmail); // verify email uniqueness check
        verify(userRepository, times(1)).save(any(User.class)); // verify user save called once, hence user created once only
        // Capture the Login object passed to loginRepository.save() to inspect its values
        ArgumentCaptor<Login> loginCaptor = ArgumentCaptor.forClass(Login.class); // to capture object passed to collaborators (e.g., to inspect the Login object passed to loginRepository.save) / verify login save called once with correct data
        verify(loginRepository, times(1)).save(loginCaptor.capture()); // login saved once
        // Verify that the Login record was created correctly
        assertEquals(testEmail, loginCaptor.getValue().getEmail()); // verify login email matches
        assertEquals("hashedpw", loginCaptor.getValue().getHash()); // verify login password hash matches
    }

    // Tests for register method when email already exists
    @Test
    void register_fails_whenEmailExists() {
        // 1. Arrange: simulate a user that already exists in the database
        User existing = new User();
        existing.setEmail(testEmail);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existing));

        // 2. Act: attempt to register a new user with the same email
        User created = authService.register("X", testEmail, "Hello@12345!");

        // 3. Assert: verify that registration failed and nothing was saved
        assertNull(created);
        // Verify that email lookup was performed once
        verify(userRepository, times(1)).findByEmail(testEmail);
        // Verify that no new user or login was saved
        verify(userRepository, never()).save(any(User.class));
        verify(loginRepository, never()).save(any(Login.class));
    }

    // Tests for signin method when inputs are null
    @Test
    void signin_nullInputs_returnsNull() {
        // Arrange not needed as no dependencies are called with null inputs
        // Act & Assert: calling signin() with null values should safely return null
        assertNull(authService.signin(null, null));
        assertNull(authService.signin(testEmail, null));
        assertNull(authService.signin(null, testPassword));
    }

    // Tests for signin method when no login record exists
    @Test
    void signin_noLoginRecord_auditedAndReturnsNull() {
        // 1. Arrange: set up mocks to simulate a user exists but no login record
        // Simulate that there is no Login entry for the given email
        when(loginRepository.findById(testEmail)).thenReturn(Optional.empty());
        // Simulate that the user exists in the UserRepository with ID = 7
        User u = new User();
        u.setId(7);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(u));

        // Act & Assert — attempt to sign in with the test email and password and verify it returns null
        assertNull(authService.signin(testEmail, testPassword));

        // Verify that loginRepository was queried exactly once
        verify(loginRepository, times(1)).findById(testEmail);

        // Verify that an audit entry was created for the failed sign-in attempt
        // Arguments: user ID = 7, success = false, reason = NO_LOGIN_RECORD
        verify(auditLogger, times(1)).auditSignInAttempt(eq(7), eq(false), eq(AuditReason.NO_LOGIN_RECORD));
    }

    // Tests for signin method when password does not match
    @Test
    void signin_passwordMismatch_auditedAndReturnsNull() {
        Login l = new Login(testEmail, "storedhash");
        when(loginRepository.findById(testEmail)).thenReturn(Optional.of(l));
        when(passwordEncoder.matches(eq(testPassword), anyString())).thenReturn(false);
        User u = new User(); u.setId(9);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(u));

        assertNull(authService.signin(testEmail, testPassword));

    verify(auditLogger, times(1)).auditSignInAttempt(eq(9), eq(false), eq(AuditReason.PASSWORD_MISMATCH));
    }

    // Tests for signin method when successful without 2FA
    @Test
    void signin_success_without2fa_returnsUser() {
        Login l = new Login(testEmail, "storedhash");
        when(loginRepository.findById(testEmail)).thenReturn(Optional.of(l));
        when(passwordEncoder.matches(eq(testPassword), anyString())).thenReturn(true);
        User u = new User();
        u.setId(11);
        u.setTwoFactorEnabled(false);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(u));

        SigninResult res = authService.signin(testEmail, testPassword);

        assertNotNull(res);
        assertFalse(res.requiresTwoFactor());
        assertNull(res.userId());
        assertNotNull(res.user());
        assertEquals(11, res.user().getId());

    verify(auditLogger, times(1)).auditSignInAttempt(eq(11), eq(true), eq(AuditReason.SUCCESS));
    }

    // Tests for signin method when successful but 2FA is required
    @Test
    void signin_twoFactorRequired_returnsSigninResultFlag() {
        Login l = new Login(testEmail, "storedhash");
        when(loginRepository.findById(testEmail)).thenReturn(Optional.of(l));
        when(passwordEncoder.matches(eq(testPassword), anyString())).thenReturn(true);
        User u = new User();
        u.setId(13);
        u.setTwoFactorEnabled(true);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(u));

        SigninResult res = authService.signin(testEmail, testPassword);

        assertNotNull(res);
        assertTrue(res.requiresTwoFactor());
        assertEquals(13, res.userId());
        assertNull(res.user());

    verify(auditLogger, times(1)).auditSignInAttempt(eq(13), eq(true), eq(AuditReason.TWO_FA_REQUIRED));
    }

}
