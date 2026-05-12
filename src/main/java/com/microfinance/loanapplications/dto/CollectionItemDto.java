package com.microfinance.loanapplications.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
class CollectionItemDto {
    private String loanAccountNumber;
    private String borrowerName;
    private BigDecimal amountCollected;
    private String paymentMethod;
}