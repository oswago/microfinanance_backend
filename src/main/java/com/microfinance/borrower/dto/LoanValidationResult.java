package com.microfinance.borrower.dto;

import com.microfinance.borrower.dto.ValidationResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoanValidationResult extends ValidationResult {
    private Long loanApplicationId;
    private Long borrowerId;
    private Long loanProductId;
    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private Boolean meetsAllCriteria;
    private Integer creditScore;
    private String riskRating;

    public LoanValidationResult() {
        super();
    }

    // Specific loan validation methods
    public void addAmountValidationError(BigDecimal minAmount, BigDecimal maxAmount) {
        addError(String.format("Loan amount must be between %s and %s", minAmount, maxAmount));
    }

    public void addTenureValidationError(Integer minTenure, Integer maxTenure) {
        addError(String.format("Loan tenure must be between %d and %d months", minTenure, maxTenure));
    }

    public void addCreditScoreWarning(Integer minimumScore) {
        addWarning(String.format("Credit score (%d) is below recommended minimum (%d)", creditScore, minimumScore));
    }

    public void addCollateralRequirement(String requirement) {
        addInfoMessage("Collateral requirement: " + requirement);
    }
}