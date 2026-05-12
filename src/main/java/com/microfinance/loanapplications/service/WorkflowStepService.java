package com.microfinance.loanapplications.service;

import com.microfinance.borrower.entity.DocumentVerification;
import com.microfinance.borrower.enums.KycWorkflowStep;
import com.microfinance.borrower.repository.BorrowerDocumentRepository;
import com.microfinance.borrower.repository.DocumentVerificationRepository;
import com.microfinance.common.config.DocumentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class WorkflowStepService {
    
    @Autowired
    private BorrowerDocumentRepository borrowerDocumentRepository;
    
    @Autowired
    private DocumentVerificationRepository documentVerificationRepository;
    
    @Autowired
    private LoanProductDocumentService loanProductDocumentService;
    
    /**
     * Unified method to check if a workflow step is completed
     */
    public boolean isStepCompleted(KycWorkflowStep step, Long borrowerId) {
        switch (step) {
            case UPLOAD_ID_PROOF:
                return hasDocumentsUploaded(borrowerId, "IDENTITY");
            case UPLOAD_ADDRESS_PROOF:
                return hasDocumentsUploaded(borrowerId, "ADDRESS");
            case UPLOAD_INCOME_PROOF:
                return hasDocumentsUploaded(borrowerId, "INCOME");
            case UPLOAD_PHOTOGRAPH:
                return hasDocumentsUploaded(borrowerId, "PERSONAL");
            case VERIFY_ID_PROOF:
                return areDocumentsVerified(borrowerId, "IDENTITY");
            case VERIFY_ADDRESS_PROOF:
                return areDocumentsVerified(borrowerId, "ADDRESS");
            case VERIFY_INCOME_PROOF:
                return areDocumentsVerified(borrowerId, "INCOME");
            case VERIFY_PHOTOGRAPH:
                return areDocumentsVerified(borrowerId, "PERSONAL");
            default:
                return isNonDocumentStepCompleted(step, borrowerId);
        }
    }
    
    /**
     * Check if documents of a specific category are uploaded
     */

    private boolean hasDocumentsUploaded(Long borrowerId, String category) {
        Set<DocumentConfig.DocumentType> requiredTypes = getRequiredDocumentTypesByCategory(borrowerId, category);
        log.info("hasDocumentsUploaded Start for borrower: {}, Category: {} ", borrowerId,category);

        if (requiredTypes.isEmpty()) return true;
        
        List<String> typeStrings = requiredTypes.stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        log.info("hasDocumentsUploaded Start for borrower: {}, TypeString: {} ", borrowerId,typeStrings);
                
        return borrowerDocumentRepository.existsByBorrowerIdAndDocumentTypeInImp(borrowerId, typeStrings);
    }


    /**
     * Check if documents of a specific category are verified
      */
    private boolean areDocumentsVerified(Long borrowerId, String category) {
        Set<DocumentConfig.DocumentType> requiredTypes = getRequiredDocumentTypesByCategory(borrowerId, category);
        log.info("areDocumentsVerified Start for borrower: {}, Category: {} requiredTypes{} ", borrowerId,category,requiredTypes);
        if (requiredTypes.isEmpty()) return true;

        // Convert Set to List of DocumentType (not String)
        List<DocumentConfig.DocumentType> typeList = new ArrayList<>(requiredTypes);

        return documentVerificationRepository.existsByBorrowerIdAndDocumentTypeInAndVerificationStatus(
                borrowerId, typeList, DocumentVerification.VerificationStatus.VERIFIED);
    }


    /**
     * Get required document types filtered by category
     */
    private Set<DocumentConfig.DocumentType> getRequiredDocumentTypesByCategory(Long borrowerId, String category) {
        log.info("getRequiredDocumentTypesByCategory Start for borrower: {}, Category: {} ", borrowerId,category);
        return loanProductDocumentService.getRequiredDocumentTypes(borrowerId).stream()
                .filter(type -> type.getCategory().equals(category))
                .collect(Collectors.toSet());

    }



    private boolean isNonDocumentStepCompleted(KycWorkflowStep step, Long borrowerId) {

        log.info("isNonDocumentStepCompleted Start for borrower: {}, Steps: {} ", borrowerId,step);
        // Implement logic for non-document steps
        // This could check against a workflow step completion table
        return false;
    }
}