package com.microfinance.loanapplications.dto.repayment;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodBreakdownDto {
    
    /**
     * The payment method code
     */
    private String paymentMethod;
    
    /**
     * Human-readable display name
     */
    private String displayName;
    
    /**
     * Total amount collected
     */
    private BigDecimal amount;
    
    /**
     * Number of transactions
     */
    private Long transactionCount;
    
    /**
     * Percentage of total collection
     */
    private Double percentage;
    
    /**
     * Average transaction amount for this method
     */
    private BigDecimal averageAmount;
    
    /**
     * Minimum transaction amount
     */
    private BigDecimal minAmount;
    
    /**
     * Maximum transaction amount
     */
    private BigDecimal maxAmount;
    
    /**
     * Icon class for UI (e.g., pi pi-money-bill, pi pi-credit-card)
     */
    private String icon;
    
    /**
     * Color code for UI
     */
    private String color;
    
    // Helper method to set icon based on payment method
    public static String getIconForMethod(String method) {
        if (method == null) return "pi pi-question";
        
        switch (method) {
            case "CASH":
                return "pi pi-money-bill";
            case "BANK_TRANSFER":
                return "pi pi-building";
            case "MOBILE_MONEY":
                return "pi pi-mobile";
            case "CHEQUE":
                return "pi pi-file";
            case "CARD":
                return "pi pi-credit-card";
            case "DIRECT_DEBIT":
                return "pi pi-sync";
            default:
                return "pi pi-dollar";
        }
    }
    
    // Helper method to set color based on payment method
    public static String getColorForMethod(String method) {
        if (method == null) return "#6c757d";
        
        switch (method) {
            case "CASH":
                return "#10b981"; // Green
            case "BANK_TRANSFER":
                return "#3b82f6"; // Blue
            case "MOBILE_MONEY":
                return "#8b5cf6"; // Purple
            case "CHEQUE":
                return "#f59e0b"; // Orange
            case "CARD":
                return "#ec4899"; // Pink
            case "DIRECT_DEBIT":
                return "#6b7280"; // Gray
            default:
                return "#6c757d";
        }
    }
}