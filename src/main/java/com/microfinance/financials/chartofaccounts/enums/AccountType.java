package com.microfinance.financials.chartofaccounts.enums;

// AccountType enum
public enum AccountType {
    ASSET("Asset"),
    LIABILITY("Liability"),
    EQUITY("Equity"),
    INCOME("Income"),
    EXPENSE("Expense");
    
    private final String displayName;
    
    AccountType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}