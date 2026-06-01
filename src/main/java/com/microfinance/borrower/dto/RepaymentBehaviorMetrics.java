package com.microfinance.borrower.dto;

import lombok.Data;

@Data
    public class RepaymentBehaviorMetrics {
    private  int totalLoans;
    private int completedLoans;
    private   int activeLoans;
    private   int defaultedLoans;
    private double completionRate;
    private double onTimeRate;
    private   double earlyRate;
    private    double lateRate;
    private double defaultRate;
    private    double avgLateDays;
    private double paymentReliabilityScore;
    private   String riskLevel;


    }