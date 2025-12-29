package nl.cyberella.hands_on.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PasswordValidator {

    public static class ValidationResult {
        private final boolean isValid;
        private final List<String> errors;

        public ValidationResult(boolean isValid, List<String> errors) {
            this.isValid = isValid;
            this.errors = errors;
        }

        public boolean isValid() { return isValid; }
        public List<String> getErrors() { return errors; }
    }

    public static ValidationResult validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isBlank()) {
            errors.add("password required");
            return new ValidationResult(false, errors);
        }

        if (password.length() < 8) errors.add("at least 8 characters");
        if (!Pattern.compile("[A-Z]").matcher(password).find()) errors.add("at least one uppercase letter");
        if (!Pattern.compile("[a-z]").matcher(password).find()) errors.add("at least one lowercase letter");
        if (!Pattern.compile("\\d").matcher(password).find()) errors.add("at least one number");
        if (!Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find()) errors.add("at least one special character (!@#$%^&*(),.?\":{}|<>)");

        return new ValidationResult(errors.isEmpty(), errors);
    }
}
