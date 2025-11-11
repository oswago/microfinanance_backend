package com.microfinance.borrower.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GroupMemberSummaryDto {
    private Long memberId;
    private String borrowerNumber;
    private String fullName;
    private String phoneNumber;
    private String email;
    
    // Membership Information
    private Long groupId;
    private String groupName;
    private String membershipStatus; // ACTIVE, INACTIVE, SUSPENDED
    private Boolean isGroupLeader;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joinedDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate membershipStartDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate membershipEndDate;
    
    // Personal Information
    private String occupation;
    private BigDecimal monthlyIncome;
    private String city;
    private String identificationNumber;
    
    // KYC Status
    private String kycStatus;
    private LocalDateTime kycVerifiedAt;
    private Boolean kycComplete;
    
    // Loan Portfolio
    private Integer totalLoansTaken;
    private Integer activeLoans;
    private Integer completedLoans;
    private Integer defaultedLoans;
    
    private BigDecimal totalBorrowedAmount;
    private BigDecimal totalRepaidAmount;
    private BigDecimal outstandingBalance;
    private BigDecimal overdueAmount;
    
    // Savings Information
    private BigDecimal savingsBalance;
    private BigDecimal totalSavingsDeposits;
    private BigDecimal totalSavingsWithdrawals;
    
    // Performance Metrics
    private BigDecimal repaymentRate;
    private String repaymentBehavior; // EXCELLENT, GOOD, AVERAGE, POOR
    private Integer daysSinceLastRepayment;
    private Integer consecutiveOnTimeRepayments;
    
    // Risk Assessment
    private String riskRating;
    private Integer creditScore;
    private Boolean isBlacklisted;
    private String blacklistReason;
    
    // Meeting Attendance
    private Integer meetingsAttended;
    private Integer meetingsMissed;
    private BigDecimal attendanceRate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastMeetingAttended;
    
    // Guarantee Information
    private Integer activeGuarantees;
    private Integer totalGuaranteesProvided;
    private BigDecimal totalGuaranteedAmount;
    
    // Eligibility Status
    private Boolean eligibleForNewLoans;
    private String eligibilityReason;
    private BigDecimal recommendedLoanLimit;
    
    // Contact Information
    private String emergencyContactName;
    private String emergencyContactPhone;
    
    // Additional Metrics
    private Integer loanApplicationsPending;
    private Integer activeGuarantorRoles;
    private String memberSince; // Formatted string e.g., "2 years, 3 months"

    // Helper methods
    public BigDecimal getRepaymentRatePercentage() {
        return repaymentRate != null ? repaymentRate.multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
    }
    
    public BigDecimal getAttendanceRatePercentage() {
        return attendanceRate != null ? attendanceRate.multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
    }
    
    public Integer getTotalMeetings() {
        return (meetingsAttended != null ? meetingsAttended : 0) + 
               (meetingsMissed != null ? meetingsMissed : 0);
    }
    
    public BigDecimal getAverageLoanAmount() {
        return totalLoansTaken > 0 ? 
            totalBorrowedAmount.divide(BigDecimal.valueOf(totalLoansTaken), 2, BigDecimal.ROUND_HALF_UP) : 
            BigDecimal.ZERO;
    }
    
    public String getMembershipDuration() {
        if (membershipStartDate == null) return "N/A";
        
        LocalDate now = LocalDate.now();
        long years = java.time.temporal.ChronoUnit.YEARS.between(membershipStartDate, now);
        long months = java.time.temporal.ChronoUnit.MONTHS.between(membershipStartDate, now) % 12;
        
        if (years > 0) {
            return months > 0 ? years + " years, " + months + " months" : years + " years";
        } else {
            return months + " months";
        }
    }
}