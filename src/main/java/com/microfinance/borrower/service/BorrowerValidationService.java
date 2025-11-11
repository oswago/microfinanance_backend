package com.microfinance.borrower.service;

import com.microfinance.borrower.dto.BorrowerDto;
import com.microfinance.borrower.dto.ValidationResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class BorrowerValidationService {
    
    public ValidationResult validateBorrowerCreation(BorrowerDto borrowerDto) {
        List<String> errors = new ArrayList<>();
        // Age validation (must be 18+)
        if (borrowerDto.getDateOfBirth() != null) {
            if (ChronoUnit.YEARS.between(borrowerDto.getDateOfBirth(), LocalDate.now()) < 18) {
                errors.add("Borrower must be at least 18 years old");
            }
        }
        
        // Income validation
        if (borrowerDto.getMonthlyIncome() != null && borrowerDto.getMonthlyIncome() <= 0) {
            errors.add("Monthly income must be positive");
        }
        
        // Phone number format validation
        if (!isValidPhoneNumber(borrowerDto.getPhoneNumber())) {
            errors.add("Invalid phone number format");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    public boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("\\d{10,15}");
    }
}