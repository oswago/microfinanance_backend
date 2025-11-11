package com.microfinance.borrower.service;

import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.borrower.repository.BorrowerDocumentRepository;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.system.entity.Branch;
import com.microfinance.system.repository.BranchRepository;
import com.microfinance.system.service.SystemService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.microfinance.base.utils.SecurityUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BorrowerServiceImpl implements BorrowerService {

    private final BorrowerActivityService borrowerActivityService;
    private final SecurityUtils securityUtils; // Inject SecurityUtils


    @Override
    public BorrowerCreditAssessmentDto assessCreditworthiness(Long borrowerId) {
        return null;
    }

    @Override
    public List<BorrowerDto> getBorrowersEligibleForLoan(Long loanProductId) {
        return List.of();
    }

    @Override
    public Boolean isBorrowerEligibleForLoan(Long borrowerId, Long loanProductId) {
        return null;
    }

    @Override
    public BorrowerKycSummaryDto getKycSummary(Long borrowerId) {
        return null;
    }

    @Override
    public List<BorrowerDocumentDto> getMissingRequiredDocuments(Long borrowerId) {
        return List.of();
    }

    @Override
    public Boolean isKycComplete(Long borrowerId) {
        return null;
    }

    @Override
    public BorrowerPortfolioSummaryDto getPortfolioSummary(Long borrowerId) {
        return null;
    }

    @Override
    public List<BorrowerActivityDto> getRecentActivities(Long borrowerId) {
        return List.of();
    }

    @Override
    public BorrowerDto assignToGroup(Long borrowerId, Long groupId) {
        return null;
    }

    @Override
    public BorrowerDto removeFromGroup(Long borrowerId) {
        return null;
    }

    @Override
    public BorrowerActivityDto logActivity(BorrowerActivityDto activityDto) {
        return null;
    }

    @Override
    public Page<BorrowerActivityDto> getBorrowerActivities(Long borrowerId, Pageable pageable) {
        return null;
    }

    @Override
    public Page<BorrowerActivityDto> searchActivities(ActivitySearchCriteria criteria, Pageable pageable) {
        return null;
    }

    @Override
    public BorrowerActivitySummaryDto getActivitySummary(Long borrowerId, LocalDate startDate, LocalDate endDate) {
        return null;
    }

    @Override
    public List<BorrowerActivityDto.TimelineGroup> getActivityTimeline(Long borrowerId, int days) {
        return List.of();
    }

    @Override
    public List<BorrowerActivityDto> getRecentActivities(Long borrowerId, int limit) {
        return List.of();
    }

    private final BorrowerDocumentRepository borrowerDocumentRepository;
    private final BorrowerRepository borrowerRepository;
    private final BranchRepository branchRepository;
    private final SystemService systemService;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file.max-size:5242880}") // 5MB default
    private long maxFileSize;

    @Value("${app.file.allowed-types:image/jpeg,image/png,image/jpg,application/pdf}")
    private String allowedFileTypes;

    
    @Override
    @Transactional(readOnly = true)
    public Page<BorrowerDto> getAllBorrowers(Pageable pageable) {
        return borrowerRepository.findAll(pageable).map(this::convertToDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<BorrowerDto> getBorrowersByBranch(Long branchId, Pageable pageable) {
        return borrowerRepository.findByBranchId(branchId, pageable).map(this::convertToDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<BorrowerDto> searchBorrowers(String search, Pageable pageable) {
        return borrowerRepository.searchBorrowers(search, pageable).map(this::convertToDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BorrowerDto getBorrowerById(Long id) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + id));
        return convertToDto(borrower);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BorrowerDto getBorrowerByNumber(String borrowerNumber) {
        Borrower borrower = borrowerRepository.findByBorrowerNumber(borrowerNumber)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with number: " + borrowerNumber));
        return convertToDto(borrower);
    }
    
    @Override
    @Transactional
    public BorrowerDto createBorrower(BorrowerDto borrowerDto, Long createdBy) {
        // Validate unique constraints
        if (borrowerRepository.existsByPhoneNumber(borrowerDto.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists: " + borrowerDto.getPhoneNumber());
        }
        
        if (borrowerDto.getIdentificationNumber() != null && 
            borrowerRepository.existsByIdentificationNumber(borrowerDto.getIdentificationNumber())) {
            throw new IllegalArgumentException("Identification number already exists: " + borrowerDto.getIdentificationNumber());
        }
        
        Borrower borrower = convertToEntity(borrowerDto);
        
        // Generate borrower number
        String borrowerNumber = systemService.getNextNumber("BORROWER");
        borrower.setBorrowerNumber(borrowerNumber);
        
        // Set branch
        if (borrowerDto.getBranchId() != null) {
            Branch branch = branchRepository.findById(borrowerDto.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found with id: " + borrowerDto.getBranchId()));
            borrower.setBranch(branch);
        }
        
        borrower.setCreatedBy(createdBy);
        borrower.setStatus(Borrower.BorrowerStatus.ACTIVE);
        borrower.setKycStatus(Borrower.KycStatus.PENDING);
        
        Borrower savedBorrower = borrowerRepository.save(borrower);
        log.info("Created new borrower: {} with number: {}", savedBorrower.getFullName(), borrowerNumber);
        
        return convertToDto(savedBorrower);
    }
    
    @Override
    @Transactional
    public BorrowerDto updateBorrower(Long id, BorrowerDto borrowerDto) {
        Borrower existingBorrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + id));
        
        // Update fields
        existingBorrower.setFirstName(borrowerDto.getFirstName());
        existingBorrower.setLastName(borrowerDto.getLastName());
        existingBorrower.setMiddleName(borrowerDto.getMiddleName());
        existingBorrower.setGender(borrowerDto.getGender());
        existingBorrower.setDateOfBirth(borrowerDto.getDateOfBirth());
        existingBorrower.setPhoneNumber(borrowerDto.getPhoneNumber());
        existingBorrower.setEmail(borrowerDto.getEmail());
        existingBorrower.setAddress(borrowerDto.getAddress());
        existingBorrower.setCity(borrowerDto.getCity());
        existingBorrower.setState(borrowerDto.getState());
        existingBorrower.setCountry(borrowerDto.getCountry());
        existingBorrower.setPostalCode(borrowerDto.getPostalCode());
        existingBorrower.setMaritalStatus(borrowerDto.getMaritalStatus());
        existingBorrower.setOccupation(borrowerDto.getOccupation());
        existingBorrower.setEmployer(borrowerDto.getEmployer());
        existingBorrower.setMonthlyIncome(borrowerDto.getMonthlyIncome());
        existingBorrower.setEmergencyContactName(borrowerDto.getEmergencyContactName());
        existingBorrower.setEmergencyContactPhone(borrowerDto.getEmergencyContactPhone());
        existingBorrower.setEmergencyContactRelationship(borrowerDto.getEmergencyContactRelationship());
        existingBorrower.setNationality(borrowerDto.getNationality());
        existingBorrower.setIdentificationType(borrowerDto.getIdentificationType());
        existingBorrower.setIdentificationNumber(borrowerDto.getIdentificationNumber());
        existingBorrower.setNotes(borrowerDto.getNotes());
        
        // Update branch if changed
        if (borrowerDto.getBranchId() != null && 
            (existingBorrower.getBranch() == null || !existingBorrower.getBranch().getId().equals(borrowerDto.getBranchId()))) {
            Branch branch = branchRepository.findById(borrowerDto.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found with id: " + borrowerDto.getBranchId()));
            existingBorrower.setBranch(branch);
        }
        
        Borrower updatedBorrower = borrowerRepository.save(existingBorrower);
        log.info("Updated borrower: {} with id: {}", updatedBorrower.getFullName(), id);
        
        return convertToDto(updatedBorrower);
    }
    
    @Override
    @Transactional
    public void deleteBorrower(Long id) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + id));
        
        // Soft delete - set status to INACTIVE
        borrower.setStatus(Borrower.BorrowerStatus.INACTIVE);
        borrowerRepository.save(borrower);
        
        log.info("Soft deleted borrower: {} with id: {}", borrower.getFullName(), id);
    }
    
    @Override
    @Transactional
    public BorrowerDto updateBorrowerStatus(Long id, Borrower.BorrowerStatus status) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + id));
        
        borrower.setStatus(status);
        Borrower updatedBorrower = borrowerRepository.save(borrower);
        
        log.info("Updated borrower status to {} for borrower: {} with id: {}", status, borrower.getFullName(), id);
        
        return convertToDto(updatedBorrower);
    }
    
    @Override
    @Transactional
    public BorrowerDto updateKycStatus(Long id, Borrower.KycStatus kycStatus, Long verifiedBy, String notes) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + id));
        
        borrower.setKycStatus(kycStatus);
        borrower.setKycVerifiedBy(verifiedBy);
        borrower.setKycVerifiedAt(LocalDateTime.now());
        
        Borrower updatedBorrower = borrowerRepository.save(borrower);
        
        log.info("Updated KYC status to {} for borrower: {} with id: {}", kycStatus, borrower.getFullName(), id);
        
        return convertToDto(updatedBorrower);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BorrowerDto> getBorrowersByGroup(Long groupId) {
        return borrowerRepository.findByGroupId(groupId, Pageable.unpaged())
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long getBorrowerCountByBranch(Long branchId) {
        return borrowerRepository.countActiveBorrowersByBranch(branchId);
    }

    
    // Helper methods for entity-DTO conversion
    private BorrowerDto convertToDto(Borrower borrower) {
        BorrowerDto dto = new BorrowerDto();
        dto.setId(borrower.getId());
        dto.setBorrowerNumber(borrower.getBorrowerNumber());
        dto.setFirstName(borrower.getFirstName());
        dto.setLastName(borrower.getLastName());
        dto.setMiddleName(borrower.getMiddleName());
        dto.setGender(borrower.getGender());
        dto.setDateOfBirth(borrower.getDateOfBirth());
        dto.setPhoneNumber(borrower.getPhoneNumber());
        dto.setEmail(borrower.getEmail());
        dto.setAddress(borrower.getAddress());
        dto.setCity(borrower.getCity());
        dto.setState(borrower.getState());
        dto.setCountry(borrower.getCountry());
        dto.setPostalCode(borrower.getPostalCode());
        dto.setMaritalStatus(borrower.getMaritalStatus());
        dto.setOccupation(borrower.getOccupation());
        dto.setEmployer(borrower.getEmployer());
        dto.setMonthlyIncome(borrower.getMonthlyIncome());
        dto.setStatus(borrower.getStatus());
        dto.setKycStatus(borrower.getKycStatus());
        dto.setKycVerifiedAt(borrower.getKycVerifiedAt());
        dto.setEmergencyContactName(borrower.getEmergencyContactName());
        dto.setEmergencyContactPhone(borrower.getEmergencyContactPhone());
        dto.setEmergencyContactRelationship(borrower.getEmergencyContactRelationship());
        dto.setNationality(borrower.getNationality());
        dto.setIdentificationType(borrower.getIdentificationType());
        dto.setIdentificationNumber(borrower.getIdentificationNumber());
        dto.setNotes(borrower.getNotes());
        dto.setFullName(borrower.getFullName());
        
        if (borrower.getBranch() != null) {
            dto.setBranchId(borrower.getBranch().getId());
            dto.setBranchName(borrower.getBranch().getName());
        }
        
        if (borrower.getGroup() != null) {
            dto.setGroupId(borrower.getGroup().getId());
            dto.setGroupName(borrower.getGroup().getGroupName());
        }
        
        return dto;
    }
    
    private Borrower convertToEntity(BorrowerDto dto) {
        Borrower borrower = new Borrower();
        borrower.setFirstName(dto.getFirstName());
        borrower.setLastName(dto.getLastName());
        borrower.setMiddleName(dto.getMiddleName());
        borrower.setGender(dto.getGender());
        borrower.setDateOfBirth(dto.getDateOfBirth());
        borrower.setPhoneNumber(dto.getPhoneNumber());
        borrower.setEmail(dto.getEmail());
        borrower.setAddress(dto.getAddress());
        borrower.setCity(dto.getCity());
        borrower.setState(dto.getState());
        borrower.setCountry(dto.getCountry());
        borrower.setPostalCode(dto.getPostalCode());
        borrower.setMaritalStatus(dto.getMaritalStatus());
        borrower.setOccupation(dto.getOccupation());
        borrower.setEmployer(dto.getEmployer());
        borrower.setMonthlyIncome(dto.getMonthlyIncome());
        borrower.setEmergencyContactName(dto.getEmergencyContactName());
        borrower.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        borrower.setEmergencyContactRelationship(dto.getEmergencyContactRelationship());
        borrower.setNationality(dto.getNationality());
        borrower.setIdentificationType(dto.getIdentificationType());
        borrower.setIdentificationNumber(dto.getIdentificationNumber());
        borrower.setNotes(dto.getNotes());
        
        return borrower;
    }

    public BorrowerSummaryDto convertToSummaryDto(Borrower borrower) {
        BorrowerSummaryDto dto = new BorrowerSummaryDto();
        dto.setId(borrower.getId());
        dto.setBorrowerNumber(borrower.getBorrowerNumber());
        dto.setFirstName(borrower.getFirstName());
        dto.setLastName(borrower.getLastName());
        dto.setMiddleName(borrower.getMiddleName());
        dto.setFullName(borrower.getFullName());
        dto.setPhoneNumber(borrower.getPhoneNumber());
        dto.setEmail(borrower.getEmail());
        dto.setStatus(borrower.getStatus());
        dto.setKycStatus(borrower.getKycStatus());
        dto.setKycVerifiedAt(borrower.getKycVerifiedAt());
        dto.setDateOfBirth(borrower.getDateOfBirth());
        dto.setOccupation(borrower.getOccupation());
        dto.setMonthlyIncome(borrower.getMonthlyIncome());
        dto.setIdentificationNumber(borrower.getIdentificationNumber());
        dto.setCreatedAt(borrower.getCreatedAt());

        if (borrower.getBranch() != null) {
            dto.setBranchName(borrower.getBranch().getName());
        }

        if (borrower.getGroup() != null) {
            dto.setGroupName(borrower.getGroup().getGroupName());
        }

        return dto;
    }

    public List<BorrowerSummaryDto> convertToSummaryDtoList(List<Borrower> borrowers) {
        return borrowers.stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BorrowerSummaryDto getBorrowerSummaryById(Long id) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + id));
        return convertToSummaryDto(borrower);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BorrowerSummaryDto> getAllBorrowerSummaries(Pageable pageable) {
        return borrowerRepository.findAll(pageable).map(this::convertToSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowerSummaryDto> getBorrowerSummariesByGroup(Long groupId) {
        return borrowerRepository.findByGroupId(groupId, Pageable.unpaged())
                .stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }


    //************************DocumentUploads related methods**********************************************************/
    @Transactional
    @Override
    public BorrowerDocumentDto uploadDocument(Long borrowerId, DocumentConfig.DocumentType documentType,
                                              String documentName, MultipartFile file, String description) {
        // Validate borrower exists
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + borrowerId));
        // Validate file
        validateFile(file);
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // Generate unique filename
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String uniqueFileName = generateUniqueFileName(documentType, fileExtension);
            Path filePath = uploadPath.resolve(uniqueFileName);

            // Save file to filesystem
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Create and save document record
            BorrowerDocument document = createBorrowerDocument(borrower, documentType, documentName,
                    description, file, uniqueFileName, filePath.toString());

            BorrowerDocument savedDocument = borrowerDocumentRepository.save(document);

            log.info("Document uploaded successfully for borrower {}: {}",
                    borrower.getFullName(), savedDocument.getDocumentName());

            return convertToDocumentDto(savedDocument);

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

    @Override
    @Transactional
    public void removeDocument(Long documentId) {
        BorrowerDocument document = borrowerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));

        try {
            // Delete file from filesystem
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", document.getFilePath());
            }

            // Delete record from database
            borrowerDocumentRepository.delete(document);
            log.info("Removed document record: {}", document.getDocumentName());

        } catch (IOException ex) {
            log.error("Failed to delete file: {}", document.getFilePath(), ex);
            throw new RuntimeException("Failed to delete document file", ex);
        }
    }

    // Additional document-related methods
    @Override
    @Transactional(readOnly = true)
    public List<BorrowerDocumentDto> getBorrowerDocuments(Long borrowerId) {
        return borrowerDocumentRepository.findByBorrowerId(borrowerId)
                .stream()
                .map(this::convertToDocumentDto)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    @Override
    public BorrowerDocumentDto getDocumentById(Long documentId) {
        BorrowerDocument document = borrowerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));
        return convertToDocumentDto(document);
    }


    @Transactional
    @Override
    public BorrowerDocumentDto updateDocumentStatus(Long documentId, DocumentConfig.DocumentStatus status,
                                                    Long verifiedBy, String verificationNotes) {
        BorrowerDocument document = borrowerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));

        document.setStatus(status);
        document.setVerifiedBy(verifiedBy);
        document.setVerificationNotes(verificationNotes);

        if (status == DocumentConfig.DocumentStatus.VERIFIED) {
            document.setVerifiedAt(LocalDateTime.now());
        }

        BorrowerDocument updatedDocument = borrowerDocumentRepository.save(document);
        log.info("Updated document status to {} for document: {}", status, document.getDocumentName());

        return convertToDocumentDto(updatedDocument);
    }

    @Transactional(readOnly = true)
    @Override
    public List<BorrowerDocumentDto> getDocumentsByType(Long borrowerId, DocumentConfig.DocumentType documentType) {
        return borrowerDocumentRepository.findByBorrowerIdAndDocumentType(borrowerId, documentType)
                .stream()
                .map(this::convertToDocumentDto)
                .collect(Collectors.toList());
    }

    // Helper method for DTO conversion
    private BorrowerDocumentDto convertToDocumentDto(BorrowerDocument document) {
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

    @Override
    @Transactional
    public BulkKycVerificationResponse bulkUpdateKycStatus(BulkKycVerificationRequest request) {
        Long performedBy =securityUtils.getCurrentUserId();
        BulkKycVerificationResponse response = new BulkKycVerificationResponse();
        response.setPerformedBy(performedBy);
        response.setPerformedByName(securityUtils.getCurrentUsername()); // You'll need to implement this

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

                // Log activity
                borrowerActivityService.logQuickActivity(
                        borrowerId,
                        getKycActivityType(request.getKycStatus()),
                        String.format("KYC status updated to %s. Notes: %s",
                                request.getKycStatus().name(),
                                specificNotes != null ? specificNotes : "No notes provided"),
                        performedBy,
                        "KYC_BULK_UPDATE",
                        null
                );

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

    @Override
    @Transactional
    public List<BorrowerDto> bulkUpdateKycStatus(List<Long> borrowerIds, Borrower.KycStatus kycStatus,
                                                 Long verifiedBy, String notes) {
        List<BorrowerDto> updatedBorrowers = new ArrayList<>();

        for (Long borrowerId : borrowerIds) {
            try {
                BorrowerDto updatedBorrower = updateKycStatus(borrowerId, kycStatus, verifiedBy, notes);
                updatedBorrowers.add(updatedBorrower);
            } catch (Exception e) {
                log.warn("Failed to update KYC status for borrower {}: {}", borrowerId, e.getMessage());
                // Continue with other borrowers
            }
        }

        return updatedBorrowers;
    }

    @Override
    @Transactional
    public BulkKycVerificationResponse bulkKycRejection(List<Long> borrowerIds, String rejectionReason, Long rejectedBy) {
        BulkKycVerificationRequest request = new BulkKycVerificationRequest();
        request.setBorrowerIds(borrowerIds);
        request.setKycStatus(Borrower.KycStatus.REJECTED);
        request.setVerificationNotes(rejectionReason);

        return bulkUpdateKycStatus(request);
    }

    @Override
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

    // Helper method to send bulk notifications
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

    // Helper method to get current user name (implement based on your security setup)
    private String getCurrentUserName() {
        try {
            // This depends on your security implementation
            // Example: return securityUtils.getCurrentUserPrincipal().getUsername();
            return "System User"; // Placeholder
        } catch (Exception e) {
            return "Unknown User";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowerDto> getBorrowersEligibleForKycUpdate(Borrower.KycStatus currentStatus, Boolean documentsUploaded) {
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

        return borrowers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private boolean hasAllRequiredDocuments(Long borrowerId) {
        // Implement logic to check if borrower has all required KYC documents
        // This would typically query your document repository
        // Placeholder implementation:
        //Each Loan Product has the associated required documents, so ona ecan check agains the Loan product
        return true;
    }


}