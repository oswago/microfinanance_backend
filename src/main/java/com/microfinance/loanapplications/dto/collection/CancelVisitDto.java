// dto/collection/CancelVisitDto.java
package com.microfinance.loanapplications.dto.collection;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelVisitDto {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}