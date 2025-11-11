package com.microfinance.borrower.service;

import com.microfinance.base.repository.UserRepository;
import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.borrower.entity.KycWorkflow;
import com.microfinance.borrower.entity.KycWorkflowStepStatus;
import com.microfinance.borrower.enums.KycWorkflowState;
import com.microfinance.borrower.repository.BorrowerDocumentRepository;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.borrower.repository.KycWorkflowRepository;
import com.microfinance.borrower.repository.KycWorkflowStepStatusRepository;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.common.service.DocumentConfigService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.base.config.FileStorageProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BorrowerDocumentService {

    private final BorrowerDocumentRepository documentRepository;
    private final BorrowerRepository borrowerRepository;
    private final SecurityUtils securityUtils;
    private final FileStorageProperties fileStorageProperties;
    private final UserRepository userRepository;
    private final KycWorkflowRepository kycWorkflowRepository;
    private final KycWorkflowStepStatusRepository kycWorkflowStepStatusRepository;
    private final DocumentConfigService documentConfigService;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file.max-size:5242880}") // 5MB default
    private long maxFileSize;

    @Value("${app.file.allowed-types:image/jpeg,image/png,image/jpg,application/pdf}")
    private String allowedFileTypes;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create upload directory!", ex);
        }
    }

    //************************DocumentUploads related methods**********************************************************/

    @Transactional
    public BorrowerDocumentDto uploadDocument(Long borrowerId, DocumentConfig.DocumentType documentType,
                                              String documentName, MultipartFile file, String description) {
        // Validate borrower exists
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + borrowerId));
        // Validate file
        validateFile(file);
        try {
            // Generate unique filename
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String uniqueFileName = generateUniqueFileName(documentType, fileExtension);
            Path filePath = fileStorageLocation.resolve(uniqueFileName);

            // Save file to filesystem
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Create and save document record
            BorrowerDocument document = createBorrowerDocument(borrower, documentType, documentName,
                    description, file, uniqueFileName, filePath.toString());

            BorrowerDocument savedDocument = documentRepository.save(document);

            log.info("Document uploaded successfully for borrower {}: {}",
                    borrower.getFullName(), savedDocument.getDocumentName());

            return convertToDto(savedDocument);

        } catch (IOException ex) {
            log.error("Failed to upload document for borrower {}: {}", borrowerId, ex.getMessage());
            throw new RuntimeException("Failed to upload document: " + ex.getMessage(), ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedFileType(contentType)) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + allowedFileTypes);
        }

        // Additional validation for specific file types
        if (contentType.startsWith("image/")) {
            validateImageFile(file);
        }
    }

    private boolean isAllowedFileType(String contentType) {
        String[] allowedTypes = allowedFileTypes.split(",");
        for (String allowedType : allowedTypes) {
            if (contentType.equals(allowedType.trim())) {
                return true;
            }
        }
        return false;
    }

    private void validateImageFile(MultipartFile file) {
        try {
            // Basic image validation - you could add more sophisticated checks
            if (file.getSize() == 0) {
                throw new IllegalArgumentException("Image file appears to be corrupted");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid image file: " + e.getMessage());
        }
    }

    private String generateUniqueFileName(DocumentConfig.DocumentType documentType, String fileExtension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomId = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s_%s%s",
                documentType.name().toLowerCase(),
                timestamp,
                randomId,
                fileExtension);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".dat";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private BorrowerDocument createBorrowerDocument(Borrower borrower, DocumentConfig.DocumentType documentType,
                                                    String documentName, String description, MultipartFile file,
                                                    String fileName, String filePath) {
        BorrowerDocument document = new BorrowerDocument();
        document.setBorrower(borrower);
        document.setDocumentType(DocumentConfig.DocumentType.valueOf(String.valueOf(documentType)));
        document.setDocumentName(documentName);
        document.setDescription(description);
        document.setFilePath(filePath);
        document.setFileName(fileName);
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStatus(DocumentConfig.DocumentStatus.PENDING);
        document.setCreatedAt(LocalDateTime.now());
        // Set expiry date for certain document types
        if (documentType == DocumentConfig.DocumentType.PASSPORT ||
                documentType == DocumentConfig.DocumentType.NATIONAL_ID) {
            document.setExpiryDate(LocalDateTime.now().plusYears(5).toLocalDate());
        }

        return document;
    }

    @Transactional
    public void removeDocument(Long documentId) {
        BorrowerDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));

        try {
            // Delete file from filesystem
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", document.getFilePath());
            }

            // Delete record from database
            documentRepository.delete(document);
            log.info("Removed document record: {}", document.getDocumentName());

        } catch (IOException ex) {
            log.error("Failed to delete file: {}", document.getFilePath(), ex);
            throw new RuntimeException("Failed to delete document file", ex);
        }
    }
/*
    // Additional document-related methods
    @Transactional(readOnly = true)
    public List<BorrowerDocumentDto> getBorrowerDocuments(Long borrowerId) {
        return documentRepository.findByBorrowerId(borrowerId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }*/
@Transactional(readOnly = true)
public List<BorrowerDocumentDto> getBorrowerDocuments(Long borrowerId) {
    List<BorrowerDocument> documents = documentRepository.findByBorrowerId(borrowerId);

    if (documents.isEmpty()) {
        return new ArrayList<>();
    }

    // Batch fetch user names to avoid N+1 queries
    Set<Long> verifierIds = documents.stream()
            .map(BorrowerDocument::getVerifiedBy)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    log.debug("Found {} documents with {} unique verifiers", documents.size(), verifierIds.size());

    // Use the helper method that returns Map directly
    Map<Long, String> verifierNames = userRepository.findUserNamesMapByIds(verifierIds);

    log.debug("Successfully fetched {} verifier names", verifierNames.size());

    // Convert documents to DTOs and set verifier names
    return documents.stream()
            .map(document -> {
                BorrowerDocumentDto dto = convertToDto(document);

                // Set verifier name if document was verified by someone
                if (document.getVerifiedBy() != null) {
                    String verifierName = verifierNames.get(document.getVerifiedBy());
                    dto.setVerifiedByName(verifierName != null ? verifierName : "Unknown User");
                }
                return dto;
            })
            .collect(Collectors.toList());
}



    @Transactional(readOnly = true)
    public BorrowerDocumentDto getDocumentById(Long documentId) {
        BorrowerDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));
        return convertToDto(document);
    }

    @Transactional
    public BorrowerDocumentDto updateDocumentStatus(Long documentId, DocumentConfig.DocumentStatus status,
                                                    Long verifiedBy, String verificationNotes) {
        BorrowerDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));

        document.setStatus(status);
        document.setVerifiedBy(verifiedBy);
        document.setVerificationNotes(verificationNotes);

        if (status == DocumentConfig.DocumentStatus.VERIFIED) {
            document.setVerifiedAt(LocalDateTime.now());
        }

        BorrowerDocument updatedDocument = documentRepository.save(document);
        log.info("Updated document status to {} for document: {}", status, document.getDocumentName());

        return convertToDto(updatedDocument);
    }

    @Transactional(readOnly = true)
    public List<BorrowerDocumentDto> getDocumentsByType(Long borrowerId, DocumentConfig.DocumentType documentType) {
        return documentRepository.findByBorrowerIdAndDocumentType(borrowerId, documentType)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Helper method for DTO conversion
    private BorrowerDocumentDto convertToDto(BorrowerDocument document) {
        BorrowerDocumentDto dto = new BorrowerDocumentDto();
        dto.setId(document.getId());
        dto.setBorrowerId(document.getBorrower().getId());
        dto.setDocumentType(String.valueOf(document.getDocumentType()));
        dto.setDocumentName(document.getDocumentName());
        dto.setDescription(document.getDescription());
        dto.setFilePath(document.getFilePath());
        dto.setFileName(document.getFileName());
        dto.setFileType(document.getFileType());
        dto.setFileSize(document.getFileSize());
        dto.setStatus(document.getStatus());
        dto.setVerifiedAt(document.getVerifiedAt());
        dto.setVerifiedBy(document.getVerifiedBy());
        dto.setVerificationNotes(document.getVerificationNotes());
        dto.setExpiryDate(document.getExpiryDate());
        dto.setBorrowerName(document.getBorrower().getFullName());
        dto.setCreatedAt(document.getCreatedAt());
        return dto;
    }

    //************************KYC Bulk Operations**********************************************************/

    @Transactional
    public BulkKycVerificationResponse bulkUpdateKycStatus(BulkKycVerificationRequest request) {
        Long performedBy = securityUtils.getCurrentUserId();
        BulkKycVerificationResponse response = new BulkKycVerificationResponse();
        response.setPerformedBy(performedBy);
        response.setPerformedByName(securityUtils.getCurrentUsername());

        // Validate request
        if (!request.isValid()) {
            throw new IllegalArgumentException("Invalid bulk KYC verification request");
        }
        // Process each borrower
        for (Long borrowerId : request.getBorrowerIds()) {
            try {
                Borrower borrower = borrowerRepository.findById(borrowerId)
                        .orElseThrow(() -> new RuntimeException("Borrower not found with id: " + borrowerId));

                String previousStatus = borrower.getKycStatus().name();

                // Get specific notes for this borrower if provided
                String specificNotes = request.getVerificationNotes();
                if (request.getBorrowerDetails() != null) {
                    specificNotes = request.getBorrowerDetails().stream()
                            .filter(detail -> detail.getBorrowerId().equals(borrowerId))
                            .map(BulkKycVerificationRequest.BorrowerVerificationDetail::getSpecificNotes)
                            .findFirst()
                            .orElse(request.getVerificationNotes());
                }

                // Update KYC status
                borrower.setKycStatus(request.getKycStatus());
                borrower.setKycVerifiedBy(performedBy);
                borrower.setKycVerifiedAt(LocalDateTime.now());

                Borrower updatedBorrower = borrowerRepository.save(borrower);

                // Add to response
                BulkKycVerificationResponse.BorrowerUpdateResult result =
                        BulkKycVerificationResponse.BorrowerUpdateResult.success(
                                borrowerId,
                                borrower.getFullName(),
                                borrower.getBorrowerNumber(),
                                previousStatus,
                                request.getKycStatus().name()
                        );
                response.addSuccessResult(result);

                log.info("Bulk KYC update: Borrower {} status changed from {} to {}",
                        borrowerId, previousStatus, request.getKycStatus().name());

            } catch (Exception e) {
                log.error("Failed to update KYC status for borrower {}: {}", borrowerId, e.getMessage());

                BulkKycVerificationResponse.BorrowerUpdateResult result =
                        BulkKycVerificationResponse.BorrowerUpdateResult.failure(
                                borrowerId,
                                "Unknown", // We don't have borrower name due to exception
                                "Unknown",
                                e.getMessage()
                        );
                response.addFailureResult(result);
            }
        }

        response.generateSummary();

        // Send notifications if requested
        if (request.getSendNotification() && request.isVerificationAction()) {
            sendBulkKycNotifications(request.getBorrowerIds(), request.getNotificationTemplate());
        }

        log.info("Bulk KYC verification completed: {}", response.getSummary());
        return response;
    }

    @Transactional
    public BulkKycVerificationResponse bulkKycRejection(List<Long> borrowerIds, String rejectionReason, Long rejectedBy) {
        BulkKycVerificationRequest request = new BulkKycVerificationRequest();
        request.setBorrowerIds(borrowerIds);
        request.setKycStatus(Borrower.KycStatus.REJECTED);
        request.setVerificationNotes(rejectionReason);

        return bulkUpdateKycStatus(request);
    }

    @Transactional
    public BulkKycVerificationResponse bulkKycVerification(List<Long> borrowerIds, Long verifiedBy) {
        BulkKycVerificationRequest request = new BulkKycVerificationRequest();
        request.setBorrowerIds(borrowerIds);
        request.setKycStatus(Borrower.KycStatus.VERIFIED);
        request.setVerificationNotes("Bulk verification completed");
        request.setSendNotification(true);
        request.setNotificationTemplate("KYC_VERIFIED");

        return bulkUpdateKycStatus(request);
    }

    public Boolean isKycComplete(Long borrowerId) {
        Optional<KycWorkflow> workflow = kycWorkflowRepository.findByBorrowerId(borrowerId);
        return workflow.map(w -> w.getCurrentState() == KycWorkflowState.VERIFIED).orElse(false);
    }

    public BorrowerKycSummaryDto getKycSummary(Long borrowerId) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + borrowerId));

        BorrowerKycSummaryDto summary = new BorrowerKycSummaryDto();
        summary.setBorrowerId(borrowerId);
        summary.setBorrowerName(borrower.getFullName());

        // Get document statistics
        List<BorrowerDocument> documents = documentRepository.findByBorrowerId(borrowerId);
        long documentsUploaded = documents.size();
        long documentsVerified = documents.stream()
                .filter(doc -> doc.getStatus() == DocumentConfig.DocumentStatus.VERIFIED)
                .count();
        long documentsPending = documents.stream()
                .filter(doc -> doc.getStatus() == DocumentConfig.DocumentStatus.PENDING)
                .count();

        // Get KYC required documents
       // Set<DocumentConfig.DocumentType> kycDocuments = DocumentConfig.DocumentUtils.getKYCRequiredDocuments();
        Set<DocumentConfig.DocumentType> kycDocuments = documentConfigService.getRequiredDocumentsForUseCase("FULL_KYC");
        long documentsRequired = kycDocuments.size();

        summary.setDocumentsUploaded((int) documentsUploaded);
        summary.setDocumentsVerified((int) documentsVerified);
        summary.setDocumentsPending((int) documentsPending);
        summary.setDocumentsRequired((int) documentsRequired);

        // Calculate completion percentage
        double completionPercentage = documentsRequired > 0 ?
                (double) documentsVerified / documentsRequired * 100 : 0;
        summary.setKycCompletionPercentage((int) Math.round(completionPercentage));

        // Get KYC workflow status
        Optional<KycWorkflow> workflow = kycWorkflowRepository.findByBorrowerId(borrowerId);
        if (workflow.isPresent()) {
            KycWorkflow kycWorkflow = workflow.get();
            summary.setCurrentState(kycWorkflow.getCurrentState());
            summary.setCurrentStep(kycWorkflow.getCurrentStep());
            summary.setStartedAt(kycWorkflow.getStartedAt());
            summary.setCompletedAt(kycWorkflow.getCompletedAt());
            summary.setAssignedOfficerName(kycWorkflow.getAssignedOfficerName());

            // Get pending steps
            List<KycWorkflowStepStatus> pendingSteps = kycWorkflowStepStatusRepository
                    .findPendingStepsByWorkflowId(kycWorkflow.getId());
            summary.setPendingStepsCount(pendingSteps.size());

            // Calculate estimated completion
            if (kycWorkflow.getEstimatedCompletionDate() != null) {
                summary.setEstimatedCompletionDate(kycWorkflow.getEstimatedCompletionDate());
            }
        } else {
            summary.setCurrentState(KycWorkflowState.NOT_STARTED);
            summary.setPendingStepsCount(0);
        }

        return summary;
    }

    // Helper method to determine activity type based on KYC status
    private com.microfinance.borrower.dto.BorrowerActivityDto.ActivityType getKycActivityType(Borrower.KycStatus kycStatus) {
        switch (kycStatus) {
            case VERIFIED:
                return com.microfinance.borrower.dto.BorrowerActivityDto.ActivityType.BORROWER_KYC_VERIFIED;
            case REJECTED:
                return com.microfinance.borrower.dto.BorrowerActivityDto.ActivityType.BORROWER_KYC_REJECTED;
            case PENDING:
                return com.microfinance.borrower.dto.BorrowerActivityDto.ActivityType.BORROWER_KYC_INITIATED;
            case EXPIRED:
                return com.microfinance.borrower.dto.BorrowerActivityDto.ActivityType.BORROWER_KYC_EXPIRED;
            default:
                return com.microfinance.borrower.dto.BorrowerActivityDto.ActivityType.BORROWER_UPDATED;
        }
    }

    // Helper method to send bulk notifications - PLACEHOLDER (no implementation code in original)
    private void sendBulkKycNotifications(List<Long> borrowerIds, String template) {
        try {
            // Implementation depends on your notification service
            // This could send SMS, email, or push notifications
            log.info("Sending KYC notifications to {} borrowers with template: {}",
                    borrowerIds.size(), template);
            // Example implementation:
            // notificationService.sendBulkNotification(borrowerIds, template, "KYC_VERIFICATION");
        } catch (Exception e) {
            log.error("Failed to send bulk KYC notifications: {}", e.getMessage());
        }
    }

    // PLACEHOLDER ONLY - This method had no implementation in the original
    @Transactional(readOnly = true)
    public List<BorrowerDto> getBorrowersEligibleForKycUpdate(Borrower.KycStatus currentStatus, Boolean documentsUploaded) {
        // PLACEHOLDER: This method had no implementation in the original BorrowerService
        // Typically, borrowers with pending KYC and all documents uploaded are eligible
        List<Borrower> borrowers;

        if (currentStatus != null) {
            borrowers = borrowerRepository.findByKycStatus(currentStatus);
        } else {
            // Default: get borrowers with pending KYC
            borrowers = borrowerRepository.findByKycStatus(Borrower.KycStatus.PENDING);
        }

        // Filter by documents uploaded if requested
        if (documentsUploaded != null && documentsUploaded) {
            borrowers = borrowers.stream()
                    .filter(borrower -> hasAllRequiredDocuments(borrower.getId()))
                    .collect(Collectors.toList());
        }

        // PLACEHOLDER: Convert to DTO - would need converter method
        // return borrowers.stream()
        //         .map(this::convertToBorrowerDto)
        //         .collect(Collectors.toList());

        return new ArrayList<>(); // Temporary return
    }

    // PLACEHOLDER ONLY - This method had no implementation in the original
    private boolean hasAllRequiredDocuments(Long borrowerId) {
        // PLACEHOLDER: This method had no implementation in the original BorrowerService
        // Implement logic to check if borrower has all required KYC documents
        // This would typically query your document repository
        // Each Loan Product has the associated required documents, so one can check against the Loan product
        return true;
    }

    //************************Additional Document Methods**********************************************************/

    // Overloaded method with expiry date
    @Transactional
    public BorrowerDocumentDto uploadDocument(Long borrowerId, DocumentConfig.DocumentType documentType,
                                              String documentName, MultipartFile file,
                                              String description, LocalDate expiryDate) {
        BorrowerDocumentDto document = uploadDocument(borrowerId, documentType, documentName, file, description);

        // Update expiry date if provided
        if (expiryDate != null) {
            BorrowerDocument existingDocument = documentRepository.findById(document.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Document not found"));
            existingDocument.setExpiryDate(expiryDate);
            documentRepository.save(existingDocument);
            document.setExpiryDate(expiryDate);
        }

        return document;
    }

    @Transactional
    public BorrowerDocumentDto updateDocumentStatus(Long documentId, DocumentConfig.DocumentStatus status,
                                                    String verificationNotes) {
        return updateDocumentStatus(documentId, status, securityUtils.getCurrentUserId(), verificationNotes);
    }

    @Transactional
    public BorrowerDocumentDto verifyDocument(Long documentId, String verificationNotes) {
        return updateDocumentStatus(documentId, DocumentConfig.DocumentStatus.VERIFIED, verificationNotes);
    }

    @Transactional
    public BorrowerDocumentDto rejectDocument(Long documentId, String verificationNotes) {
        return updateDocumentStatus(documentId, DocumentConfig.DocumentStatus.REJECTED, verificationNotes);
    }

    @Transactional(readOnly = true)
    public List<BorrowerDocumentDto> getDocumentsByStatus(Long borrowerId, DocumentConfig.DocumentStatus status) {
        List<BorrowerDocument> documents = documentRepository.findByBorrowerIdAndStatus(borrowerId, status);
        return documents.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BorrowerDocumentDto> getPendingDocuments(Long borrowerId) {
        List<BorrowerDocument> documents = documentRepository.findPendingDocumentsByBorrower(borrowerId);
        return documents.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BorrowerDocumentDto> getExpiredDocuments() {
        List<BorrowerDocument> documents = documentRepository.findExpiredDocuments();
        return documents.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long countVerifiedDocumentsByType(Long borrowerId, DocumentConfig.DocumentType documentType) {
        return documentRepository.countVerifiedDocumentsByType(borrowerId, documentType);
    }

    // Utility Methods
    public boolean hasRequiredDocuments(Long borrowerId, List<DocumentConfig.DocumentType> requiredTypes) {
        for (DocumentConfig.DocumentType documentType : requiredTypes) {
            Long count = documentRepository.countVerifiedDocumentsByType(borrowerId, documentType);
            if (count == 0) {
                return false;
            }
        }
        return true;
    }

    public List<DocumentConfig.DocumentType> getMissingDocumentTypes(Long borrowerId, List<DocumentConfig.DocumentType> requiredTypes) {
        return requiredTypes.stream()
                .filter(documentType -> {
                    Long count = documentRepository.countVerifiedDocumentsByType(borrowerId, documentType);
                    return count == 0;
                })
                .collect(Collectors.toList());
    }

    // Document Expiry Management
    @Transactional
    public void markExpiredDocuments() {
        List<BorrowerDocument> expiredDocuments = documentRepository.findExpiredDocuments();
        for (BorrowerDocument document : expiredDocuments) {
            if (document.getStatus() != DocumentConfig.DocumentStatus.EXPIRED) {
                document.setStatus(DocumentConfig.DocumentStatus.EXPIRED);
                documentRepository.save(document);
                log.info("Marked document as expired: {} (ID: {})", document.getDocumentName(), document.getId());
            }
        }
    }

    // Bulk Operations
    @Transactional
    public int bulkUpdateDocumentStatus(List<Long> documentIds, DocumentConfig.DocumentStatus status,
                                        String verificationNotes) {
        int updatedCount = 0;
        Long currentUserId = securityUtils.getCurrentUserId();

        for (Long documentId : documentIds) {
            try {
                updateDocumentStatus(documentId, status, currentUserId, verificationNotes);
                updatedCount++;
            } catch (Exception e) {
                log.warn("Failed to update document {}: {}", documentId, e.getMessage());
            }
        }

        log.info("Bulk updated {} documents to status: {}", updatedCount, status);
        return updatedCount;
    }
}