package nl.cyberella.hands_on.audit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the AuditLogger component.
 *
 * These tests attach a lightweight in-memory Logback appender to the
 * `nl.cyberella.audit` logger so we can capture log events and assert on
 * their contents without touching the filesystem.
 *
 * Why tests use a TestAppender:
 * - Keeps tests fast and deterministic (no file IO).
 * - Exercises the same log formatting code used in production (templates).
 * - Avoids relying on Spring / full application context; the AuditLogger is
 *   instantiated directly for isolation.
 */
public class AuditLoggerTest {

    // The underlying Logback logger for audit events; we attach a test appender to it.
    private Logger auditLogbackLogger;
    // In-memory appender that collects emitted logging events for assertions.
    private TestAppender appender;
    // The class under test. Instantiated directly rather than via Spring.
    private AuditLogger auditLogger;

    /**
     * Simple in-test appender that collects logging events into a list.
     * We keep this minimal: it only stores the events so tests can inspect
     * formatted messages or argument arrays as needed.
     */
    static class TestAppender extends AppenderBase<ILoggingEvent> {
        final List<ILoggingEvent> events = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
        }
    }

    /**
     * Attach a TestAppender to the audit logger before every test. This ensures
     * each test runs in isolation and can inspect only the events it caused.
     */
    @BeforeEach
    void setup() {
        auditLogbackLogger = (Logger) LoggerFactory.getLogger("nl.cyberella.audit");
        appender = new TestAppender();
        appender.setContext(auditLogbackLogger.getLoggerContext());
        appender.start();
        auditLogbackLogger.addAppender(appender);

        // instantiate the AuditLogger directly to keep tests unit-scoped
        auditLogger = new AuditLogger();
    }

    /**
     * Detach and stop the test appender after each test to avoid leaking state
     * into other tests.
     */
    @AfterEach
    void tearDown() {
        auditLogbackLogger.detachAppender(appender);
        appender.stop();
    }

    /**
     * Verifies that when a reason is provided (as an AuditReason enum) the
     * logger includes the reason name in the formatted audit message.
     */
    @Test
    void auditSignInAttempt_includesReasonName_whenProvided() {
        auditLogger.auditSignInAttempt(123, false, AuditReason.PASSWORD_MISMATCH);

        // ensure an event was logged and inspect the formatted message
        assertFalse(appender.events.isEmpty(), "No audit events captured");
        String msg = appender.events.get(appender.events.size() - 1).getFormattedMessage();
        assertTrue(msg.contains("SIGNIN_ATTEMPT"));
        assertTrue(msg.contains("id=123"));
        assertTrue(msg.contains("reason=PASSWORD_MISMATCH"));
    }

    /**
     * Verifies that when the reason parameter is null the logger omits the
     * `reason=` token and that a null user id is rendered as `unknown` to avoid
     * accidentally logging PII.
     */
    @Test
    void auditRegisterAttempt_omitsReason_whenNull() {
        auditLogger.auditRegisterAttempt(null, false, null);

        assertFalse(appender.events.isEmpty());
        String msg = appender.events.get(appender.events.size() - 1).getFormattedMessage();
        assertTrue(msg.contains("REGISTER_ATTEMPT"));
        assertTrue(msg.contains("id=unknown"));
        assertFalse(msg.contains("reason="));
    }

    /**
     * Verifies that the update-profile audit includes the success flag and the
     * reason when provided.
     */
    @Test
    void auditUpdateProfileAttempt_includesSuccess_andReason() {
        auditLogger.auditUpdateProfileAttempt(7, true, AuditReason.SUCCESS);

        assertFalse(appender.events.isEmpty());
        String msg = appender.events.get(appender.events.size() - 1).getFormattedMessage();
        assertTrue(msg.contains("UPDATEPROFILE_ATTEMPT"));
        assertTrue(msg.contains("id=7"));
        assertTrue(msg.contains("success=true"));
        assertTrue(msg.contains("reason=SUCCESS"));
    }
}
