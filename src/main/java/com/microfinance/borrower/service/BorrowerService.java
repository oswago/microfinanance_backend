package com.microfinance.borrower.service;

import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.common.config.DocumentConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BorrowerService {
    
    Page<BorrowerDto> getAllBorrowers(Pageable pageable);
    
    Page<BorrowerDto> getBorrowersByBranch(Long branchId, Pageable pageable);
    
    Page<BorrowerDto> searchBorrowers(String search, Pageable pageable);
    
    BorrowerDto getBorrowerById(Long id);
    
    BorrowerDto getBorrowerByNumber(String borrowerNumber);
    
    BorrowerDto createBorrower(BorrowerDto borrowerDto, Long createdBy);
    
    BorrowerDto updateBorrower(Long id, BorrowerDto borrowerDto);
    
    void deleteBorrower(Long id);
    
    BorrowerDto updateBorrowerStatus(Long id, Borrower.BorrowerStatus status);
    
    BorrowerDto updateKycStatus(Long id, Borrower.KycStatus kycStatus, Long verifiedBy, String notes);
    
    List<BorrowerDto> getBorrowersByGroup(Long groupId);
    
    Long getBorrowerCountByBranch(Long branchId);

    //************************DocumentUploads related methods**********************************************************/

    void removeDocument(Long documentId);

    BorrowerSummaryDto getBorrowerSummaryById(Long id);

    Page<BorrowerSummaryDto> getAllBorrowerSummaries(Pageable pageable);

    List<BorrowerSummaryDto> getBorrowerSummariesByGroup(Long groupId);

    BorrowerDocumentDto uploadDocument(Long borrowerId,
                                       DocumentConfig.DocumentType documentType,
                                       String documentName,
                                       MultipartFile file,
                                       String description);

    // Additional document-related methods
    @Transactional(readOnly = true)
    List<BorrowerDocumentDto> getBorrowerDocuments(Long borrowerId);

    @Transactional(readOnly = true)
    BorrowerDocumentDto getDocumentById(Long documentId);

    @Transactional
    BorrowerDocumentDto updateDocumentStatus(Long documentId, DocumentConfig.DocumentStatus status,
                                             Long verifiedBy, String verificationNotes);

    @Transactional(readOnly = true)
    List<BorrowerDocumentDto> getDocumentsByType(Long borrowerId, DocumentConfig.DocumentType documentType);
    // Credit assessment methods
    BorrowerCreditAssessmentDto assessCreditworthiness(Long borrowerId);
    List<BorrowerDto> getBorrowersEligibleForLoan(Long loanProductId);
    Boolean isBorrowerEligibleForLoan(Long borrowerId, Long loanProductId);

    // KYC workflow methods
    BorrowerKycSummaryDto getKycSummary(Long borrowerId);
    List<BorrowerDocumentDto> getMissingRequiredDocuments(Long borrowerId);
    Boolean isKycComplete(Long borrowerId);

    // Portfolio management
    BorrowerPortfolioSummaryDto getPortfolioSummary(Long borrowerId);
    List<BorrowerActivityDto> getRecentActivities(Long borrowerId);

    // Group management
    BorrowerDto assignToGroup(Long borrowerId, Long groupId);
    BorrowerDto removeFromGroup(Long borrowerId);

    // Activity tracking methods
    BorrowerActivityDto logActivity(BorrowerActivityDto activityDto);
    Page<BorrowerActivityDto> getBorrowerActivities(Long borrowerId, Pageable pageable);
    Page<BorrowerActivityDto> searchActivities(ActivitySearchCriteria criteria, Pageable pageable);
    BorrowerActivitySummaryDto getActivitySummary(Long borrowerId, LocalDate startDate, LocalDate endDate);
    List<BorrowerActivityDto.TimelineGroup> getActivityTimeline(Long borrowerId, int days);
    List<BorrowerActivityDto> getRecentActivities(Long borrowerId, int limit);

    // Helper method for common activity logging
    default BorrowerActivityDto logStandardActivity(Long borrowerId, BorrowerActivityDto.ActivityType activityType,
                                                    String description, Long performedBy, String referenceType, Long referenceId) {
        BorrowerActivityDto activity = new BorrowerActivityDto();
        activity.setBorrowerId(borrowerId);
        activity.setActivityType(activityType);
        activity.setDescription(description);
        activity.setActivityDate(LocalDateTime.now());
        activity.setPerformedBy(performedBy);
        activity.setReferenceType(referenceType);
        activity.setReferenceId(referenceId);
        return logActivity(activity);
    }

    // Bulk operations
    BulkKycVerificationResponse bulkUpdateKycStatus(BulkKycVerificationRequest request);
    List<BorrowerDto> bulkUpdateKycStatus(List<Long> borrowerIds, Borrower.KycStatus kycStatus,
                                          Long verifiedBy, String notes);
    BulkKycVerificationResponse bulkKycRejection(List<Long> borrowerIds, String rejectionReason, Long rejectedBy);
    BulkKycVerificationResponse bulkKycVerification(List<Long> borrowerIds, Long verifiedBy);

    // In BorrowerService interface
    List<BorrowerDto> getBorrowersEligibleForKycUpdate(Borrower.KycStatus currentStatus, Boolean documentsUploaded);

}