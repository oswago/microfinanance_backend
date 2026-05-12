// dto/collection/CancelNoticeDto.java
package com.microfinance.loanapplications.dto.collection;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelNoticeDto {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}