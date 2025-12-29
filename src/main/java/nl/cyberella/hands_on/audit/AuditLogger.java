package nl.cyberella.hands_on.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simple audit logger component. Writes messages to the dedicated audit logger
 * configured in logback.xml (logger name: nl.cyberella.audit).
 *
 * Audit entries MUST NOT contain PII. Only include user identifiers (IDs) and
 * generic event names/outcomes.
 */
@Component
public class AuditLogger {

    private final Logger auditLogger = LoggerFactory.getLogger("nl.cyberella.audit");

    public void auditSignInAttempt(Integer userId, boolean success) {
        // userId may be null if unknown — write a placeholder to avoid logging PII
        String id = userId == null ? "unknown" : String.valueOf(userId);
        auditLogger.info("SIGNIN_ATTEMPT id={} success={}", id, success);
    }

    public void auditSignInAttempt(Integer userId, boolean success, AuditReason reason) {
        String id = userId == null ? "unknown" : String.valueOf(userId);
        if (reason == null) {
            auditLogger.info("SIGNIN_ATTEMPT id={} success={}", id, success);
        } else {
            auditLogger.info("SIGNIN_ATTEMPT id={} success={} reason={}", id, success, reason.name());
        }
    }

    public void auditUpdateProfileAttempt(Integer userId, boolean success) {
        String id = userId == null ? "unknown" : String.valueOf(userId);
        auditLogger.info("UPDATEPROFILE_ATTEMPT id={} success={}", id, success);
    }

    public void auditUpdateProfileAttempt(Integer userId, boolean success, AuditReason reason) {
        String id = userId == null ? "unknown" : String.valueOf(userId);
        if (reason == null) {
            auditLogger.info("UPDATEPROFILE_ATTEMPT id={} success={}", id, success);
        } else {
            auditLogger.info("UPDATEPROFILE_ATTEMPT id={} success={} reason={}", id, success, reason.name());
        }
    }

    public void auditRegisterAttempt(Integer userId, boolean success) {
        String id = userId == null ? "unknown" : String.valueOf(userId);
        auditLogger.info("REGISTER_ATTEMPT id={} success={}", id, success);
    }

    public void auditRegisterAttempt(Integer userId, boolean success, AuditReason reason) {
        String id = userId == null ? "unknown" : String.valueOf(userId);
        if (reason == null) {
            auditLogger.info("REGISTER_ATTEMPT id={} success={}", id, success);
        } else {
            auditLogger.info("REGISTER_ATTEMPT id={} success={} reason={}", id, success, reason.name());
        }
    }
}
