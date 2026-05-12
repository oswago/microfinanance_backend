// BulkDisbursementRequestDto.java
package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDisbursementRequestDto {
    
    @NotEmpty(message = "At least one loan must be selected")
    private List<Long> loanIds;
    
    @NotNull(message = "Disbursement date is required")
    private LocalDate disbursementDate;
    
    @NotNull(message = "Disbursement method is required")
    private String disbursementMethod;
    
    private String batchReference;
    
    private String notes;
}