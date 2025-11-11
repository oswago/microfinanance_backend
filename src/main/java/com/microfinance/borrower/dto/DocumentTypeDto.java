package com.microfinance.borrower.dto;

import lombok.Data;

@Data
public class DocumentTypeDto {
    private String code;
    private String displayName;
    private String category;
    private boolean required;
    private String description;
    
    // For frontend grouping
    public String getCategoryLabel() {
        switch (category) {
            case "IDENTITY": return "Identity Documents";
            case "ADDRESS": return "Address Proof";
            case "INCOME": return "Income Proof";
            case "EMPLOYMENT": return "Employment Documents";
            case "COLLATERAL": return "Collateral Documents";
            case "PERSONAL": return "Personal Documents";
            default: return "Other Documents";
        }
    }

}