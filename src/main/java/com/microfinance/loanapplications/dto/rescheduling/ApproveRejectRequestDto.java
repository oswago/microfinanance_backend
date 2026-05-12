// ApproveRejectRequestDto.java
package com.microfinance.loanapplications.dto.rescheduling;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRejectRequestDto {
    
    @Size(max = 500, message = "Comments cannot exceed 500 characters")
    private String comments;
    
    private Long reviewedBy;
    private String approvalReference;
}