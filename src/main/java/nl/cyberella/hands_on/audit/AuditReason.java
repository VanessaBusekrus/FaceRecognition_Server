package nl.cyberella.hands_on.audit;

/**
 * Strongly-typed audit reason codes used in audit logs.
 * Keep values stable because they are emitted to audit logs consumed by operators/SIEM.
 */
public enum AuditReason {
    NO_LOGIN_RECORD,
    PASSWORD_MISMATCH,
    USER_PROFILE_MISSING,
    TWO_FA_REQUIRED,
    SUCCESS,
    UNKNOWN
}
