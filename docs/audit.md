# Audit logging and AuditReason

This document explains the `AuditReason` enum and how audit logging works in this project.

Location
- Enum: `src/main/java/nl/cyberella/hands_on/audit/AuditReason.java`
- Audit logger: `src/main/java/nl/cyberella/hands_on/audit/AuditLogger.java`
- Logback config: `src/main/resources/logback.xml` (writes `nl.cyberella.audit` to `audit.log`).

AuditReason values

- `NO_LOGIN_RECORD` — No login row exists for the supplied email.
- `PASSWORD_MISMATCH` — Password verification failed.
- `USER_PROFILE_MISSING` — User profile not found for email lookup.
- `TWO_FA_REQUIRED` — User has 2FA enabled and requires a second-factor step.
- `SUCCESS` — Operation succeeded.
- `UNKNOWN` — Generic unknown/fallback reason.

Why use the enum?

- Strong typing: compile-time guarantees and clearer tests.
- Central documentation of allowed reasons.
- Safer refactors: IDE/compile-time help when renaming values.

Best practices

- Audit entries MUST NOT include PII (emails, passwords). Use user IDs or `unknown`.
- Keep the enum stable: these values are emitted into audit logs consumed by operators/SIEMs.
- Consider structured JSON audit output for easier downstream parsing — the enum pairs well with that.

Example usage

```java
auditLogger.auditSignInAttempt(userId, false, AuditReason.PASSWORD_MISMATCH);
```
