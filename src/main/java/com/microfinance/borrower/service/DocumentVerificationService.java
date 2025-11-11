package com.microfinance.borrower.service;

import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.borrower.entity.DocumentVerification;
import com.microfinance.borrower.repository.BorrowerDocumentRepository;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.borrower.repository.DocumentVerificationRepository;
import com.microfinance.common.config.DocumentConfig;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.microfinance.base.utils.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentVerificationService {

    private final DocumentVerificationRepository verificationRepository;
    private final BorrowerRepository borrowerRepository;
    private final BorrowerDocumentRepository borrowerDocumentRepository;
    private final BorrowerDocumentService borrowerDocumentService;
    private final SecurityUtils securityUtils; // Inject SecurityUtils

    @Transactional(readOnly = true)
    public List<DocumentVerificationDto> getDocumentVerifications(Long borrowerId) {
        return verificationRepository.findByBorrowerId(borrowerId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentVerificationDto getDocumentVerification(Long id) {
        DocumentVerification verification = verificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document verification not found with id: " + id));
        return convertToDto(verification);
    }

    @Transactional
    public DocumentVerificationDto createDocumentVerification(DocumentVerificationCreateRequest request) {
        // Validate borrower exists
        Borrower borrower = borrowerRepository.findById(request.getBorrowerId())
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + request.getBorrowerId()));

        // Validate borrower document exists if provided
        BorrowerDocument borrowerDocument = null;
        if (request.getBorrowerDocumentId() != null) {
            borrowerDocument = borrowerDocumentRepository.findById(request.getBorrowerDocumentId())
                    .orElseThrow(() -> new EntityNotFoundException("Borrower document not found with id: " + request.getBorrowerDocumentId()));
        }

        DocumentVerification verification = new DocumentVerification();
        verification.setBorrower(borrower);
        verification.setBorrowerDocument(borrowerDocument);
        verification.setDocumentType(request.getDocumentType());
        verification.setDocumentNumber(request.getDocumentNumber());
        verification.setIssuingAuthority(request.getIssuingAuthority());
        verification.setIssueDate(request.getIssueDate());
        verification.setExpiryDate(request.getExpiryDate());
        verification.setVerificationStatus(request.getVerificationStatus());
        verification.setVerifiedBy(request.getVerifiedBy());
        verification.setVerificationDate(request.getVerificationDate());
        verification.setVerificationNotes(request.getVerificationNotes());
        verification.setRejectionReason(request.getRejectionReason());
        verification.setAdditionalData(request.getAdditionalData());
        verification.setIsActive(request.getIsActive());
        verification.setCreatedBy(securityUtils.getCurrentUserId());

        DocumentVerification savedVerification = verificationRepository.save(verification);

        // Update corresponding borrower document status if exists
        if (borrowerDocument != null) {
            updateBorrowerDocumentStatus(borrowerDocument, request.getVerificationStatus());
        }

        log.info("Created document verification for borrower {}: {}", borrower.getFullName(), request.getDocumentType());
        return convertToDto(savedVerification);
    }

    @Transactional
    public DocumentVerificationDto updateDocumentVerification(Long id, DocumentVerificationUpdateRequest request) {
        DocumentVerification verification = verificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document verification not found with id: " + id));

        if (request.getDocumentType() != null) {
            verification.setDocumentType(request.getDocumentType());
        }
        if (request.getDocumentNumber() != null) {
            verification.setDocumentNumber(request.getDocumentNumber());
        }
        if (request.getIssuingAuthority() != null) {
            verification.setIssuingAuthority(request.getIssuingAuthority());
        }
        if (request.getIssueDate() != null) {
            verification.setIssueDate(request.getIssueDate());
        }
        if (request.getExpiryDate() != null) {
            verification.setExpiryDate(request.getExpiryDate());
        }
        if (request.getVerificationStatus() != null) {
            verification.setVerificationStatus(request.getVerificationStatus());
        }
        if (request.getVerifiedBy() != null) {
            verification.setVerifiedBy(request.getVerifiedBy());
        }
        if (request.getVerificationDate() != null) {
            verification.setVerificationDate(request.getVerificationDate());
        }
        if (request.getVerificationNotes() != null) {
            verification.setVerificationNotes(request.getVerificationNotes());
        }
        if (request.getRejectionReason() != null) {
            verification.setRejectionReason(request.getRejectionReason());
        }
        if (request.getAdditionalData() != null) {
            verification.setAdditionalData(request.getAdditionalData());
        }
        if (request.getIsActive() != null) {
            verification.setIsActive(request.getIsActive());
        }

        verification.setUpdatedBy(securityUtils.getCurrentUserId());

        DocumentVerification updatedVerification = verificationRepository.save(verification);

        // Update corresponding borrower document status if exists
        if (verification.getBorrowerDocument() != null && request.getVerificationStatus() != null) {
            updateBorrowerDocumentStatus(verification.getBorrowerDocument(), request.getVerificationStatus());
        }

        log.info("Updated document verification: {}", id);
        return convertToDto(updatedVerification);
    }

    @Transactional
    public void deleteDocumentVerification(Long id) {
        DocumentVerification verification = verificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document verification not found with id: " + id));
        verificationRepository.delete(verification);
        log.info("Deleted document verification: {}", id);
    }

    @Transactional
    public DocumentVerificationDto verifyDocument(Long id, DocumentVerificationActionRequest request) {
        DocumentVerification verification = verificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document verification not found with id: " + id));

        verification.setVerificationStatus(DocumentVerification.VerificationStatus.VERIFIED);
        verification.setVerifiedBy(request.getVerifiedBy());
        verification.setVerificationNotes(request.getVerificationNotes());
        verification.setVerificationDate(LocalDateTime.now());

        DocumentVerification updatedVerification = verificationRepository.save(verification);

        // Update corresponding borrower document status
        if (verification.getBorrowerDocument() != null) {
            updateBorrowerDocumentStatus(verification.getBorrowerDocument(), DocumentVerification.VerificationStatus.VERIFIED);
        }

        log.info("Verified document: {}", id);
        return convertToDto(updatedVerification);
    }

    @Transactional
    public DocumentVerificationDto rejectDocument(Long id, DocumentVerificationActionRequest request) {
        DocumentVerification verification = verificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document verification not found with id: " + id));

        verification.setVerificationStatus(DocumentVerification.VerificationStatus.REJECTED);
        verification.setVerifiedBy(request.getVerifiedBy());
        verification.setRejectionReason(request.getRejectionReason());
        verification.setVerificationDate(LocalDateTime.now());

        DocumentVerification updatedVerification = verificationRepository.save(verification);

        // Update corresponding borrower document status
        if (verification.getBorrowerDocument() != null) {
            updateBorrowerDocumentStatus(verification.getBorrowerDocument(), DocumentVerification.VerificationStatus.REJECTED);
        }

        log.info("Rejected document: {}", id);
        return convertToDto(updatedVerification);
    }

    @Transactional(readOnly = true)
    public List<DocumentVerificationDto> getDocumentVerificationsByStatus(Long borrowerId, DocumentVerification.VerificationStatus status) {
        return verificationRepository.findByBorrowerIdAndVerificationStatus(borrowerId, status)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentVerificationDto> getPendingDocumentVerifications(Long borrowerId) {
        return verificationRepository.findPendingByBorrowerId(borrowerId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentVerificationDto> getVerifiedDocumentVerifications(Long borrowerId) {
        return verificationRepository.findVerifiedByBorrowerId(borrowerId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long countActiveVerifiedDocuments(Long borrowerId) {
        return verificationRepository.countActiveVerifiedDocuments(borrowerId);
    }

    @Transactional
    public void markExpiredVerifications() {
        List<DocumentVerification> expiredVerifications = verificationRepository.findExpiredVerifications();
        for (DocumentVerification verification : expiredVerifications) {
            verification.setVerificationStatus(DocumentVerification.VerificationStatus.EXPIRED);
            verification.setIsActive(false);
            verificationRepository.save(verification);

            // Update corresponding borrower document status
            if (verification.getBorrowerDocument() != null) {
                updateBorrowerDocumentStatus(verification.getBorrowerDocument(), DocumentVerification.VerificationStatus.EXPIRED);
            }

            log.info("Marked document verification as expired: {}", verification.getId());
        }
    }

    private void updateBorrowerDocumentStatus(BorrowerDocument borrowerDocument, DocumentVerification.VerificationStatus status) {
        DocumentConfig.DocumentStatus documentStatus = mapVerificationStatusToDocumentStatus(status);
        borrowerDocumentService.updateDocumentStatus(
            borrowerDocument.getId(),
            documentStatus,
            securityUtils.getCurrentUserId(),//borrowerDocument.getVerifiedBy(),
            "Status updated from document verification"
        );
    }

    private DocumentConfig.DocumentStatus mapVerificationStatusToDocumentStatus(DocumentVerification.VerificationStatus status) {
        switch (status) {
            case VERIFIED:
                return DocumentConfig.DocumentStatus.VERIFIED;
            case REJECTED:
                return DocumentConfig.DocumentStatus.REJECTED;
            case EXPIRED:
                return DocumentConfig.DocumentStatus.EXPIRED;
            case PENDING:
            default:
                return DocumentConfig.DocumentStatus.PENDING;
        }
    }

    private DocumentVerificationDto convertToDto(DocumentVerification verification) {
        DocumentVerificationDto dto = new DocumentVerificationDto();
        dto.setId(verification.getId());
        dto.setBorrowerId(verification.getBorrower().getId());
        dto.setBorrowerName(verification.getBorrower().getFullName());
        
        if (verification.getBorrowerDocument() != null) {
            dto.setBorrowerDocumentId(verification.getBorrowerDocument().getId());
            dto.setBorrowerDocumentName(verification.getBorrowerDocument().getDocumentName());
        }
        
        dto.setDocumentType(verification.getDocumentType());
        dto.setDocumentNumber(verification.getDocumentNumber());
        dto.setIssuingAuthority(verification.getIssuingAuthority());
        dto.setIssueDate(verification.getIssueDate());
        dto.setExpiryDate(verification.getExpiryDate());
        dto.setVerificationStatus(verification.getVerificationStatus());
        dto.setVerifiedBy(verification.getVerifiedBy());
        dto.setVerificationDate(verification.getVerificationDate());
        dto.setVerificationNotes(verification.getVerificationNotes());
        dto.setRejectionReason(verification.getRejectionReason());
        dto.setAdditionalData(verification.getAdditionalData());
        dto.setIsActive(verification.getIsActive());
        dto.setCreatedAt(verification.getCreatedAt());
        dto.setUpdatedAt(verification.getUpdatedAt());
        
        return dto;
    }
}