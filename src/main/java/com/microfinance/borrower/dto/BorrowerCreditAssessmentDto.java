package com.microfinance.borrower.dto;

import com.microfinance.borrower.entity.Borrower;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BorrowerCreditAssessmentDto {
    private Long borrowerId;
    private String borrowerName;
    private Integer creditScore;
    private Borrower.RiskRating riskRating;
    private BigDecimal recommendedLoanLimit;
    private List<String> strengths;
    private List<String> weaknesses;
    private Boolean eligibleForLoan;
    private String assessmentNotes;
}