package com.microfinance.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom exception for handling validation errors in the application.
 * This exception should be thrown when business rule validations fail.
 */
@Getter
public class ValidationException extends RuntimeException {
    
    private final String errorCode;
    private final List<ValidationError> errors;
    private final HttpStatus httpStatus;
    
    /**
     * Constructor with a simple error message
     */
    public ValidationException(String message) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
        this.errors = new ArrayList<>();
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    /**
     * Constructor with error message and error code
     */
    public ValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.errors = new ArrayList<>();
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    /**
     * Constructor with error message and specific HTTP status
     */
    public ValidationException(String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
        this.errors = new ArrayList<>();
        this.httpStatus = httpStatus;
    }
    
    /**
     * Constructor with error message, error code, and HTTP status
     */
    public ValidationException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.errors = new ArrayList<>();
        this.httpStatus = httpStatus;
    }
    
    /**
     * Constructor with a list of validation errors
     */
    public ValidationException(List<ValidationError> errors) {
        super("Validation failed");
        this.errorCode = "VALIDATION_ERROR";
        this.errors = errors;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    /**
     * Constructor with Spring BindingResult for form validation errors
     */
    public ValidationException(BindingResult bindingResult) {
        super("Form validation failed");
        this.errorCode = "FORM_VALIDATION_ERROR";
        this.errors = new ArrayList<>();
        this.httpStatus = HttpStatus.BAD_REQUEST;
        
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            this.errors.add(new ValidationError(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getCode()
            ));
        }
    }
    
    /**
     * Static factory method for common validation scenarios
     */
    public static ValidationException of(String message) {
        return new ValidationException(message);
    }
    
    public static ValidationException of(String message, String errorCode) {
        return new ValidationException(message, errorCode);
    }
    
    public static ValidationException fieldError(String field, String message) {
        List<ValidationError> errors = List.of(new ValidationError(field, message));
        return new ValidationException(errors);
    }
    
    public static ValidationException fieldError(String field, String message, String errorCode) {
        List<ValidationError> errors = List.of(new ValidationError(field, message, errorCode));
        return new ValidationException(errors);
    }
    
    /**
     * Check if there are any validation errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Add a validation error to the exception
     */
    public void addError(ValidationError error) {
        this.errors.add(error);
    }
    
    public void addError(String field, String message) {
        this.errors.add(new ValidationError(field, message));
    }
    
    public void addError(String field, String message, String errorCode) {
        this.errors.add(new ValidationError(field, message, errorCode));
    }
    
    /**
     * Inner class to represent individual validation errors
     */
    @Getter
    public static class ValidationError {
        private final String field;
        private final String message;
        private final String errorCode;
        private final Object rejectedValue;
        
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
            this.errorCode = "FIELD_ERROR";
            this.rejectedValue = null;
        }
        
        public ValidationError(String field, String message, String errorCode) {
            this.field = field;
            this.message = message;
            this.errorCode = errorCode;
            this.rejectedValue = null;
        }
        
        public ValidationError(String field, String message, String errorCode, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.errorCode = errorCode;
            this.rejectedValue = rejectedValue;
        }
        
        @Override
        public String toString() {
            return String.format("ValidationError{field='%s', message='%s', errorCode='%s'}", 
                field, message, errorCode);
        }
    }
    
    /**
     * Common validation error codes
     */
    public static final class ErrorCodes {
        public static final String REQUIRED_FIELD = "REQUIRED_FIELD";
        public static final String INVALID_FORMAT = "INVALID_FORMAT";
        public static final String MIN_VALUE = "MIN_VALUE";
        public static final String MAX_VALUE = "MAX_VALUE";
        public static final String MIN_LENGTH = "MIN_LENGTH";
        public static final String MAX_LENGTH = "MAX_LENGTH";
        public static final String UNIQUE_CONSTRAINT = "UNIQUE_CONSTRAINT";
        public static final String INVALID_DATE = "INVALID_DATE";
        public static final String FUTURE_DATE = "FUTURE_DATE";
        public static final String PAST_DATE = "PAST_DATE";
        public static final String INVALID_EMAIL = "INVALID_EMAIL";
        public static final String INVALID_PHONE = "INVALID_PHONE";
        public static final String INVALID_AMOUNT = "INVALID_AMOUNT";
        public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
        public static final String LOAN_LIMIT_EXCEEDED = "LOAN_LIMIT_EXCEEDED";
        public static final String BORROWER_INACTIVE = "BORROWER_INACTIVE";
        public static final String KYC_REQUIRED = "KYC_REQUIRED";
        
        private ErrorCodes() {
            // Constants class
        }
    }
    
    @Override
    public String toString() {
        if (hasErrors()) {
            return String.format("ValidationException{errorCode='%s', errors=%s}", errorCode, errors);
        }
        return String.format("ValidationException{message='%s', errorCode='%s'}", getMessage(), errorCode);
    }
}