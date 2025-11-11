package com.microfinance.base.utils;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator {
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
    );
    
    public boolean isValid(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }
    
    public String getPasswordRequirements() {
        return "Password must contain at least 8 characters, including uppercase, lowercase, digit, and special character";
    }
}