package com.microfinance.borrower.dto;

import com.microfinance.common.config.DocumentConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.microfinance.common.dto.DocumentTypeDto;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycRequirementsDto {
    private Set<DocumentTypeDto> requiredDocuments;
    private Set<KycWorkflowStepStatusDto> workflowSteps;
}