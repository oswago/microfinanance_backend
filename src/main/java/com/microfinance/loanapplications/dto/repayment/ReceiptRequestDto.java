package com.microfinance.loanapplications.dto.repayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptRequestDto {
    private Long repaymentId;
    private Long installmentId;
    private Long loanId;
    private String receiptType;

    @Builder.Default
    private Boolean includeQrCode = false;  // ✅ Default to false

    @Builder.Default
    private String format = "PDF";          // ✅ Default to PDF
}