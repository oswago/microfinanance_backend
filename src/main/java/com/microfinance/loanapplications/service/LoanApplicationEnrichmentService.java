package com.microfinance.loanapplications.service;

import com.microfinance.borrower.dto.BorrowerDocumentDto;
import com.microfinance.borrower.dto.BorrowerKycSummaryDto;
import com.microfinance.borrower.service.BorrowerService;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.dto.BorrowerDocumentReferenceDto;
import com.microfinance.loanapplications.dto.DocumentComplianceSummary;
import com.microfinance.loanapplications.dto.LoanApplicationDto;
import com.microfinance.loanapplications.entity.LoanApplication;
import com.microfinance.loanapplications.mapper.LoanApplicationMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanApplicationEnrichmentService {
    
    private final LoanApplicationMapper loanApplicationMapper;
    private final BorrowerService borrowerService;
    private final DocumentComplianceService documentComplianceService;
    @Autowired
    private EntityManager entityManager;
    
    /**
     * Enrich DTO with document info - NO TRANSACTION ANNOTATION to avoid rollback issues
     * Each inner call handles its own transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public LoanApplicationDto enrichWithDocumentInfo(LoanApplication application) {
        log.debug("Enriching application: {}", application.getId());

        // Start with basic DTO
        LoanApplicationDto dto = loanApplicationMapper.toDto(application);

        try {
            // Add borrower document references
            List<BorrowerDocumentDto> borrowerDocuments = borrowerService
                    .getBorrowerDocuments(application.getBorrower().getId());

            List<BorrowerDocumentReferenceDto> documentReferences = documentComplianceService
                    .convertToReferenceDtos(borrowerDocuments);
            dto.setBorrowerDocuments(documentReferences);

        } catch (Exception e) {
            log.error("Failed to get borrower documents for application {}: {}",
                    application.getId(), e.getMessage());
            // Continue with partially enriched DTO
        }



        try {
            // Add KYC summary
            BorrowerKycSummaryDto kycSummary = borrowerService
                    .getBorrowerKycSummary(application.getBorrower().getId());
            dto.setBorrowerKycSummary(kycSummary);

        } catch (Exception e) {
            log.error("Failed to get KYC summary for application {}: {}",
                    application.getId(), e.getMessage());
            // Continue with partially enriched DTO
        }



        try {
            // Add document compliance
            Long loanProductId = application.getLoanProduct() != null ?
                    application.getLoanProduct().getId() : null;

            DocumentComplianceSummary compliance = documentComplianceService
                    .checkDocumentCompliance(application.getBorrower().getId(), loanProductId);
            dto.setDocumentCompliance(compliance);

        } catch (Exception e) {
            log.error("Failed to get document compliance for application {}: {}",
                    application.getId(), e.getMessage());
            // Continue with partially enriched DTO
        }



        log.error("====Enrichment withDocumentInfo: {}",dto);
        return dto;
    }


}