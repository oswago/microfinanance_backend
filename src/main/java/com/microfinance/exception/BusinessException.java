package com.microfinance.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom exception for handling business logic errors in the application.
 * This exception should be thrown when business rules or constraints are violated.
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    /**
     * Constructor with a simple error message
     */
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    /**
     * Constructor with error message and error code
     */
    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    /**
     * Constructor with error message and specific HTTP status
     */
    public BusinessException(String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.httpStatus = httpStatus;
    }
    
    /**
     * Constructor with error message, error code, and HTTP status
     */
    public BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    /**
     * Static factory methods for common business scenarios
     */
    public static BusinessException of(String message) {
        return new BusinessException(message);
    }
    
    public static BusinessException of(String message, String errorCode) {
        return new BusinessException(message, errorCode);
    }
    
    public static BusinessException notFound(String resource) {
        return new BusinessException(resource + " not found", "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    
    public static BusinessException unauthorized(String message) {
        return new BusinessException(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }
    
    public static BusinessException forbidden(String message) {
        return new BusinessException(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }
    
    public static BusinessException conflict(String message) {
        return new BusinessException(message, "CONFLICT", HttpStatus.CONFLICT);
    }
    
    /**
     * Common business error codes
     */
    public static final class ErrorCodes {
        public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String FORBIDDEN = "FORBIDDEN";
        public static final String CONFLICT = "CONFLICT";
        public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
        public static final String LOAN_ALREADY_DISBURSED = "LOAN_ALREADY_DISBURSED";
        public static final String APPLICATION_NOT_DRAFT = "APPLICATION_NOT_DRAFT";
        public static final String APPLICATION_NOT_PENDING = "APPLICATION_NOT_PENDING";
        public static final String INVALID_TRANSITION = "INVALID_TRANSITION";
        public static final String MAX_LOANS_EXCEEDED = "MAX_LOANS_EXCEEDED";
        public static final String DELINQUENT_BORROWER = "DELINQUENT_BORROWER";
        public static final String KYC_NOT_VERIFIED = "KYC_NOT_VERIFIED";
        
        private ErrorCodes() {
            // Constants class
        }
    }
    
    @Override
    public String toString() {
        return String.format("BusinessException{message='%s', errorCode='%s', httpStatus=%s}", 
            getMessage(), errorCode, httpStatus);
    }
}