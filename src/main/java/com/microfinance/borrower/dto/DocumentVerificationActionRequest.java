package com.microfinance.borrower.dto;

import lombok.Data;

@Data
public class DocumentVerificationActionRequest {
    private String verifiedBy;
    private String verificationNotes;
    private String rejectionReason;
}
