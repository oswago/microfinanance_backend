package com.microfinance.borrower.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ValidationResult {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private List<String> infoMessages;

    // Constructors
    public ValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.infoMessages = new ArrayList<>();
    }

    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.infoMessages = new ArrayList<>();
    }

    public ValidationResult(boolean valid, List<String> errors, List<String> warnings, List<String> infoMessages) {
        this.valid = valid;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        this.infoMessages = infoMessages != null ? infoMessages : new ArrayList<>();
    }

    // Factory methods for common scenarios
    public static ValidationResult valid() {
        return new ValidationResult(true, new ArrayList<>());
    }

    public static ValidationResult valid(String infoMessage) {
        ValidationResult result = new ValidationResult(true, new ArrayList<>());
        result.addInfoMessage(infoMessage);
        return result;
    }

    public static ValidationResult invalid(String error) {
        ValidationResult result = new ValidationResult(false, new ArrayList<>());
        result.addError(error);
        return result;
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    // Helper methods for adding messages
    public void addError(String error) {
        if (error != null && !error.trim().isEmpty()) {
            this.errors.add(error);
            this.valid = false; // Adding an error automatically makes validation invalid
        }
    }

    public void addErrors(List<String> errors) {
        if (errors != null) {
            this.errors.addAll(errors);
            if (!errors.isEmpty()) {
                this.valid = false;
            }
        }
    }

    public void addWarning(String warning) {
        if (warning != null && !warning.trim().isEmpty()) {
            this.warnings.add(warning);
        }
    }

    public void addWarnings(List<String> warnings) {
        if (warnings != null) {
            this.warnings.addAll(warnings);
        }
    }

    public void addInfoMessage(String infoMessage) {
        if (infoMessage != null && !infoMessage.trim().isEmpty()) {
            this.infoMessages.add(infoMessage);
        }
    }

    public void addInfoMessages(List<String> infoMessages) {
        if (infoMessages != null) {
            this.infoMessages.addAll(infoMessages);
        }
    }

    // Check methods
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean hasInfoMessages() {
        return !infoMessages.isEmpty();
    }

    public boolean hasMessages() {
        return hasErrors() || hasWarnings() || hasInfoMessages();
    }

    // Get first error (useful for single error scenarios)
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    // Get all messages combined
    public List<String> getAllMessages() {
        List<String> allMessages = new ArrayList<>();
        allMessages.addAll(errors);
        allMessages.addAll(warnings);
        allMessages.addAll(infoMessages);
        return allMessages;
    }

    // Merge multiple validation results
    public static ValidationResult merge(ValidationResult... results) {
        ValidationResult merged = new ValidationResult(true, new ArrayList<>());
        
        for (ValidationResult result : results) {
            if (result != null) {
                merged.addErrors(result.getErrors());
                merged.addWarnings(result.getWarnings());
                merged.addInfoMessages(result.getInfoMessages());
                
                // If any result is invalid, the merged result is invalid
                if (!result.isValid()) {
                    merged.setValid(false);
                }
            }
        }
        
        return merged;
    }

    // Create a copy of this validation result
    public ValidationResult copy() {
        return new ValidationResult(
            this.valid,
            new ArrayList<>(this.errors),
            new ArrayList<>(this.warnings),
            new ArrayList<>(this.infoMessages)
        );
    }

    // Convert to string for logging/debugging
    @Override
    public String toString() {
        return String.format("ValidationResult{valid=%s, errors=%d, warnings=%d, infoMessages=%d}",
                valid, errors.size(), warnings.size(), infoMessages.size());
    }

    // Detailed string representation
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Validation Result: ").append(valid ? "VALID" : "INVALID").append("\n");
        
        if (!errors.isEmpty()) {
            sb.append("Errors:\n");
            for (String error : errors) {
                sb.append("  - ").append(error).append("\n");
            }
        }
        
        if (!warnings.isEmpty()) {
            sb.append("Warnings:\n");
            for (String warning : warnings) {
                sb.append("  - ").append(warning).append("\n");
            }
        }
        
        if (!infoMessages.isEmpty()) {
            sb.append("Info:\n");
            for (String info : infoMessages) {
                sb.append("  - ").append(info).append("\n");
            }
        }
        
        return sb.toString();
    }
}