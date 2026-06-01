package com.microfinance.loanapplications.service;

import com.microfinance.audit.service.AuditService;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.borrower.dto.BorrowerDocumentDto;
import com.microfinance.borrower.dto.BorrowerKycSummaryDto;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.borrower.service.BorrowerService;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.exception.ValidationException;
import com.microfinance.loanapplications.dto.*;
import com.microfinance.loanapplications.entity.LoanApplication;
import com.microfinance.loanapplications.mapper.LoanApplicationMapper;
import com.microfinance.loanapplications.repository.LoanApplicationRepository;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducts.repository.LoanProductRepository;
import com.microfinance.system.entity.Branch;
import com.microfinance.system.repository.BranchRepository;
import com.microfinance.system.service.SystemService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final BorrowerService borrowerService;
    private final DocumentComplianceService documentComplianceService;
    private final LoanApplicationMapper loanApplicationMapper;
    private final SystemService systemService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    private final LoanApplicationEnrichmentService enrichmentService; // Inject the new service

    @Autowired
    private BorrowerRepository borrowerRepository;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Autowired
    private BranchRepository branchRepository;




    @Override
    public LoanApplicationDto createApplication(CreateLoanApplicationDto dto, User currentUser) {
        log.info("Creating loan application for borrower: {}", dto.getBorrowerId());

        // Check document compliance before creating application
        DocumentComplianceSummary compliance = documentComplianceService
                .checkDocumentCompliance(dto.getBorrowerId(), dto.getLoanProductId());

        if (!compliance.getMeetsRequirements() && Boolean.TRUE.equals(dto.getSubmitForApproval())) {
            throw new ValidationException(
                "Cannot submit application: Document requirements not met. " +
                "Missing: " + compliance.getMissingDocumentTypes() +
                ", Pending: " + compliance.getPendingVerificationTypes() +
                ", Expired: " + compliance.getExpiredDocumentTypes()
            );
        }

        // Check KYC status
        BorrowerKycSummaryDto kycSummary = borrowerService.getBorrowerKycSummary(dto.getBorrowerId());
        if (!kycSummary.getKycComplete() && Boolean.TRUE.equals(dto.getSubmitForApproval())) {
            throw new ValidationException("Cannot submit application: Borrower KYC is not complete");
        }

        // Create the application
        LoanApplication application = createApplicationEntity(dto, currentUser);

        log.info("Loan application entity: {}", application);

        LoanApplication savedApplication = loanApplicationRepository.save(application);


        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }




        if (Objects.nonNull(savedApplication.getId())) {
            // Build audit message safely
            StringBuilder auditMessage = new StringBuilder()
                    .append("Loan Application of ID: ").append(savedApplication.getId());
            // Safe check for loan
            if (savedApplication.getLoan() != null) {
                auditMessage.append(" Loan No: ").append(savedApplication.getLoan().getLoanAccountNumber());
            } else {
                auditMessage.append(" (Loan not yet associated)");
            }
            auditMessage.append(" has been Created by: ").append(createdByName).append("-").append(createdById);

            auditService.masterAuditLogs(
                    savedApplication.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_ACTIVITY,
                    "LOAN_APPLICATION",
                    auditMessage.toString()
            );
        }


        //End Audit Section

        // Convert to DTO with document references
        return enrichWithDocumentInfo(savedApplication);
    }




    @Override
    public DocumentComplianceSummary checkDocumentCompliance(Long borrowerId, Long loanProductId) {
        return documentComplianceService.checkDocumentCompliance(borrowerId, loanProductId);
    }

    @Override
    public Boolean validateApplicationRequirements(Long borrowerId, Long loanProductId) {
        DocumentComplianceSummary compliance = checkDocumentCompliance(borrowerId, loanProductId);
        BorrowerKycSummaryDto kycSummary = borrowerService.getBorrowerKycSummary(borrowerId);

        return compliance.getMeetsRequirements() && kycSummary.getKycComplete();
    }

/*
   @Override
    @Transactional(readOnly = true)
    public LoanApplicationDto getApplicationById(Long id) {
        log.info("Fetching loan application by ID: {}", id);

        LoanApplication application = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + id));
        // First get the basic DTO
        LoanApplicationDto dto = loanApplicationMapper.toDto(application);
        // Then enrich with document info in a separate try-catch
        try {
            // Call enrichment service (which now properly handles errors)
            LoanApplicationDto enrichedDto = enrichmentService.enrichWithDocumentInfo(application);
            if (enrichedDto != null) {
                dto = enrichedDto;
            }
        } catch (Exception e) {
            log.error("Failed to enrich application {}: {}", id, e.getMessage(), e);
            // Return basic DTO without enrichment
        }
       log.info("=== Fetching loan dto: {}", dto);
        return dto;
    }
*/

    @Override
    public LoanApplicationDto getApplicationById(Long id) {
        log.info("Fetching loan application by ID: {}", id);

        LoanApplication application = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + id));

        LoanApplicationDto enrichedDto = null;

        try {
            enrichedDto = enrichmentService.enrichWithDocumentInfo(application);
            log.info("=== Enrichment successful, documents: {}, KYC: {}, Compliance: {}",
                    enrichedDto.getBorrowerDocuments() != null ? enrichedDto.getBorrowerDocuments().size() : 0,
                    enrichedDto.getBorrowerKycSummary() != null,
                    enrichedDto.getDocumentCompliance() != null);
        } catch (UnexpectedRollbackException e) {
            // This is the key - the enrichment worked but transaction rolled back
            log.warn("Transaction rolled back but enrichment data was built. Attempting to recover...");
            // Try to get the enriched DTO from the enrichment service again
            // or build it manually
            try {
                // Re-run enrichment but without transaction propagation
                enrichedDto = buildEnrichedDtoManually(application);
            } catch (Exception ex) {
                log.error("Recovery failed: {}", ex.getMessage());
                enrichedDto = loanApplicationMapper.toDto(application);
            }
        } catch (Exception e) {
            log.error("Failed to enrich application: {}", e.getMessage(), e);
            enrichedDto = loanApplicationMapper.toDto(application);
        }

        // Ensure we never return null
        if (enrichedDto == null) {
            enrichedDto = loanApplicationMapper.toDto(application);
        }

        log.info("=== Returning DTO with documents: {}, KYC: {}, Compliance: {}",
                enrichedDto.getBorrowerDocuments() != null ? enrichedDto.getBorrowerDocuments().size() : 0,
                enrichedDto.getBorrowerKycSummary() != null,
                enrichedDto.getDocumentCompliance() != null);

        return enrichedDto;
    }

    // Manual enrichment without transaction
    private LoanApplicationDto buildEnrichedDtoManually(LoanApplication application) {
        log.info("Building enriched DTO manually for application: {}", application.getId());

        LoanApplicationDto dto = loanApplicationMapper.toDto(application);

        Long borrowerId = application.getBorrower().getId();
        Long loanProductId = application.getLoanProduct() != null ? application.getLoanProduct().getId() : null;

        // Fetch documents directly
        try {
            List<BorrowerDocumentDto> borrowerDocuments = borrowerService.getBorrowerDocuments(borrowerId);
            if (borrowerDocuments != null && !borrowerDocuments.isEmpty()) {
                List<BorrowerDocumentReferenceDto> documentReferences = documentComplianceService
                        .convertToReferenceDtos(borrowerDocuments);
                dto.setBorrowerDocuments(documentReferences);
            }
        } catch (Exception e) {
            log.error("Manual doc fetch failed: {}", e.getMessage());
            dto.setBorrowerDocuments(new ArrayList<>());
        }

        // Fetch KYC directly
        try {
            BorrowerKycSummaryDto kycSummary = borrowerService.getBorrowerKycSummary(borrowerId);
            dto.setBorrowerKycSummary(kycSummary);
        } catch (Exception e) {
            log.error("Manual KYC fetch failed: {}", e.getMessage());
        }

        // Fetch compliance directly
        try {
            DocumentComplianceSummary compliance = documentComplianceService
                    .checkDocumentCompliance(borrowerId, loanProductId);
            dto.setDocumentCompliance(compliance);
        } catch (Exception e) {
            log.error("Manual compliance fetch failed: {}", e.getMessage());
            DocumentComplianceSummary fallback = new DocumentComplianceSummary();
            fallback.setMeetsRequirements(false);
            dto.setDocumentCompliance(fallback);
        }

        return dto;
    }


    @Override
    @Transactional(readOnly = true)
    public LoanApplicationDto getApplicationByNumber(String applicationNumber) {
        log.info("Fetching loan application by number: {}", applicationNumber);

        LoanApplication application = loanApplicationRepository.findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with number: " + applicationNumber));

        return enrichWithDocumentInfo(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanApplicationDto> getApplicationsByBorrower(Long borrowerId) {
        log.info("Fetching loan applications for borrower: {}", borrowerId);

        List<LoanApplication> applications = loanApplicationRepository.findByBorrowerId(borrowerId);

        return applications.stream()
                .map(this::enrichWithDocumentInfo)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public Page<LoanApplicationDto> getApplicationsByStatus(String status, Pageable pageable) {
        log.info("Fetching loan applications with status: {}", status);

        Page<LoanApplication> applications;
        if (status != null && !status.trim().isEmpty()) {
            try {
                // Split by comma and handle multiple statuses
                String[] statusArray = status.split(",");

                if (statusArray.length == 1) {
                    // Single status
                    GeneralConfig.LoanApplicationStatus applicationStatus =
                            GeneralConfig.LoanApplicationStatus.valueOf(statusArray[0].trim().toUpperCase());
                    applications = loanApplicationRepository.findByStatus(applicationStatus, pageable);
                } else {
                    // Multiple statuses
                    List<GeneralConfig.LoanApplicationStatus> statuses = Arrays.stream(statusArray)
                            .map(s -> GeneralConfig.LoanApplicationStatus.valueOf(s.trim().toUpperCase()))
                            .collect(Collectors.toList());
                    applications = loanApplicationRepository.findByStatusIn(statuses, pageable);
                }
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid application status: " + status);
            }
        } else {
            applications = loanApplicationRepository.findAll(pageable);
        }

        // Process each application individually - errors in one won't affect others
        List<LoanApplicationDto> enrichedDtos = new ArrayList<>();

        for (LoanApplication application : applications.getContent()) {
            try {
                // Each enrichment runs in its own transaction
                LoanApplicationDto enrichedDto = enrichmentService.enrichWithDocumentInfo(application);
                enrichedDtos.add(enrichedDto);
            } catch (Exception e) {
                log.error("Failed to enrich application {}: {}", application.getId(), e.getMessage());
                // Add basic DTO without enrichment as fallback
                LoanApplicationDto basicDto = loanApplicationMapper.toDto(application);
                enrichedDtos.add(basicDto);
            }
        }

        // Return as Page
        return new PageImpl<>(enrichedDtos, pageable, applications.getTotalElements());
    }


    @Override
    @Transactional(readOnly = true)
    public List<LoanApplicationDto> getDraftApplications(User currentUser) {
        log.info("Fetching draft applications for user: {}", currentUser.getUsername());

        List<LoanApplication> drafts = loanApplicationRepository.findByStatusAndCreatedBy(
                GeneralConfig.LoanApplicationStatus.DRAFT, currentUser.getId());

        return drafts.stream()
                .map(this::enrichWithDocumentInfo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanApplicationDto> getPendingApprovals() {
        log.info("Fetching pending approval applications");

        List<LoanApplication> pendingApplications = loanApplicationRepository.findByStatusIn(
                List.of(GeneralConfig.LoanApplicationStatus.SUBMITTED, GeneralConfig.LoanApplicationStatus.UNDER_REVIEW));

        return pendingApplications.stream()
                .map(this::enrichWithDocumentInfo)
                .collect(Collectors.toList());
    }

    @Override
    public LoanApplicationDto submitForApproval(Long id, SubmitApplicationDto dto, User currentUser) {
        log.info("Submitting application {} for approval by user: {}", id, currentUser.getUsername());

        LoanApplication application = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + id));

        // Validate application can be submitted
        validateApplicationForSubmission(application, currentUser);

        // Update application status and details
        application.setStatus(GeneralConfig.LoanApplicationStatus.SUBMITTED);
        application.setSubmittedDate(LocalDateTime.now());
        application.setSubmittedBy(currentUser.getUsername());
        application.setPurpose(dto.getPurpose());
        application.setAdditionalNotes(dto.getAdditionalNotes());

        LoanApplication savedApplication = loanApplicationRepository.save(application);

        log.info("Application {} successfully submitted for approval", id);

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedApplication.getId())) {
            auditService.masterAuditLogs(
                    savedApplication.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_ACTIVITY,
                    "LOAN_APPLICATION",
                    "Loan Application of ID:"+savedApplication.getId()+" Loan No:"+savedApplication.getLoan().getLoanAccountNumber()+  " has been Submitted for APPROVAL Created by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        return enrichWithDocumentInfo(savedApplication);
    }

    @Override
    public LoanApplicationDto updateApplication(Long id, CreateLoanApplicationDto dto, User currentUser) {
        log.info("Updating application {} by user: {}", id, currentUser.getUsername());

        LoanApplication application = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + id));

        // Validate application can be updated
        if (application.getStatus() != GeneralConfig.LoanApplicationStatus.DRAFT) {
            throw new ValidationException("Only draft applications can be updated");
        }

        // Validate user has permission to update this application
        if (!application.getCreatedById().equals(currentUser.getId())) {
            throw new ValidationException("You can only update applications created by you");
        }

        // Fetch borrower and loan product entities
        Borrower borrower = borrowerRepository.findById(dto.getBorrowerId())
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found: " + dto.getBorrowerId()));

        LoanProduct loanProduct = loanProductRepository.findById(dto.getLoanProductId())
                .orElseThrow(() -> new EntityNotFoundException("Loan product not found: " + dto.getLoanProductId()));

        // Get branch (use existing branch or current user's branch)
        Branch branch = application.getBranch();
        if (branch == null) {
            branch = branchRepository.findById(1L) // Default branch
                    .orElseThrow(() -> new EntityNotFoundException("Default branch not found"));
        }

        // Update application fields with entities
        updateApplicationEntity(application, dto, borrower, loanProduct, branch, currentUser);
        LoanApplication savedApplication = loanApplicationRepository.save(application);

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }


        if (Objects.nonNull(savedApplication.getId())) {
            // Build audit message safely
            StringBuilder auditMessage = new StringBuilder()
                    .append("Loan Application of ID: ").append(savedApplication.getId());

            // Safe check for loan
            if (savedApplication.getLoan() != null) {
                auditMessage.append(" Loan No: ").append(savedApplication.getLoan().getLoanAccountNumber());
            } else {
                auditMessage.append(" (Loan not yet associated)");
            }

            auditMessage.append(" has been UPDATED by: ").append(createdByName).append("-").append(createdById);

            auditService.masterAuditLogs(
                    savedApplication.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_ACTIVITY,
                    "LOAN_APPLICATION",
                    auditMessage.toString()
            );
        }
        //End Audit Section


        return enrichWithDocumentInfo(savedApplication);
    }



    @Override
    public LoanApplicationDto cancelApplication(Long id, String reason, User currentUser) {
        log.info("Cancelling application {} by user: {}", id, currentUser.getUsername());

        LoanApplication application = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + id));

        // Validate application can be cancelled
        if (application.getStatus().isTerminalState()) {
            throw new ValidationException("Cannot cancel application in terminal state: " + application.getStatus());
        }

        // Update application status
        application.setStatus(GeneralConfig.LoanApplicationStatus.CANCELLED);
        application.setCancelledAt(LocalDateTime.now());
        application.setCancelledBy(currentUser.getUsername());
        application.setCancellationReason(reason);

        LoanApplication savedApplication = loanApplicationRepository.save(application);

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedApplication.getId())) {
            auditService.masterAuditLogs(
                    savedApplication.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_ACTIVITY,
                    "LOAN_APPLICATION",
                    "Loan Application of ID:"+savedApplication.getId()+" Loan No:"+savedApplication.getLoan().getLoanAccountNumber()+  " has been CANCELLED by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        log.info("Application {} successfully cancelled", id);
        return enrichWithDocumentInfo(savedApplication);
    }

    @Override
    public void deleteDraftApplication(Long id, User currentUser) {
        log.info("Deleting draft application {} by user: {}", id, currentUser.getUsername());

        LoanApplication application = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + id));

        // Validate application can be deleted
        if (application.getStatus() != GeneralConfig.LoanApplicationStatus.DRAFT) {
            throw new ValidationException("Only draft applications can be deleted");
        }

        // Validate user has permission to delete this application
        if (!application.getCreatedById().equals(currentUser.getId())) {
            throw new ValidationException("You can only delete applications created by you");
        }


        loanApplicationRepository.delete(application);

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(application.getId())) {
            auditService.masterAuditLogs(
                    application.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_ACTIVITY,
                    "LOAN_APPLICATION",
                    "Draft Application of ID:"+application.getId()+" Loan No:"+application.getLoan().getLoanAccountNumber()+  " has been DELETED by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        log.info("Draft application {} successfully deleted", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationStatsDto getApplicationStatistics(Long branchId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating application statistics for branch: {}, period: {} to {}", branchId, startDate, endDate);

        // If no dates provided, use default period (last 30 days)
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Get statistics using the new repository methods
        Long totalApplications = loanApplicationRepository.countByCreatedAtBetween(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        Long draftCount = loanApplicationRepository.countByStatusAndCreatedAtBetween(
                GeneralConfig.LoanApplicationStatus.DRAFT, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        Long submittedCount = loanApplicationRepository.countByStatusAndCreatedAtBetween(
                GeneralConfig.LoanApplicationStatus.SUBMITTED, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        Long approvedCount = loanApplicationRepository.countByStatusAndCreatedAtBetween(
                GeneralConfig.LoanApplicationStatus.APPROVED, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        Long rejectedCount = loanApplicationRepository.countByStatusAndCreatedAtBetween(
                GeneralConfig.LoanApplicationStatus.REJECTED, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        // Use the new time-based methods
        Long newApplicationsToday = loanApplicationRepository.countApplicationsCreatedToday();
        Long newApplicationsThisWeek = loanApplicationRepository.countApplicationsCreatedThisWeek();
        Long newApplicationsThisMonth = loanApplicationRepository.countApplicationsCreatedThisMonth();

        // Get financial statistics
        BigDecimal totalAppliedAmount = loanApplicationRepository.sumAppliedAmountByPeriod(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        // Calculate approval rate
        double approvalRate = (submittedCount + approvedCount + rejectedCount) > 0 ?
                (double) approvedCount / (submittedCount + approvedCount + rejectedCount) * 100 : 0;

        return ApplicationStatsDto.builder()
                .reportDate(LocalDate.now())
                .periodType("CUSTOM")
                .startDate(startDate)
                .endDate(endDate)
                .branchId(branchId)
                .totalCounts(ApplicationStatsDto.TotalCounts.builder()
                        .totalApplications(totalApplications)
                        .newApplicationsToday(newApplicationsToday)
                        .newApplicationsThisWeek(newApplicationsThisWeek)
                        .newApplicationsThisMonth(newApplicationsThisMonth)
                        .growthRate(calculateGrowthRate(newApplicationsThisMonth))
                        .build())
                .statusCounts(ApplicationStatsDto.StatusCounts.builder()
                        .draftCount(draftCount)
                        .pendingApprovalCount(submittedCount)
                        .approvedCount(approvedCount)
                        .rejectedCount(rejectedCount)
                        .cancelledCount(0L)
                        .disbursedCount(0L)
                        .build())
                .financialStats(ApplicationStatsDto.FinancialStats.builder()
                        .totalAppliedAmount(totalAppliedAmount != null ? totalAppliedAmount : BigDecimal.ZERO)
                        .averageApplicationAmount(calculateAverageAmount(totalAppliedAmount, totalApplications))
                        .minApplicationAmount(BigDecimal.ZERO)
                        .maxApplicationAmount(BigDecimal.ZERO)
                        .totalApprovedAmount(BigDecimal.ZERO)
                        .totalDisbursedAmount(BigDecimal.ZERO)
                        .build())
                .approvalStats(ApplicationStatsDto.ApprovalStats.builder()
                        .totalPendingApproval(submittedCount)
                        .totalApproved(approvedCount)
                        .totalRejected(rejectedCount)
                        .averageApprovalTime(0.0)
                        .build())
                .build();
    }

    private Double calculateGrowthRate(Long currentMonthCount) {
        // Simple growth calculation - you can enhance this
        // For now, return a placeholder or implement your growth logic
        return 0.0;
    }

    private BigDecimal calculateAverageAmount(BigDecimal totalAmount, Long count) {
        if (totalAmount == null || count == null || count == 0) {
            return BigDecimal.ZERO;
        }
        return totalAmount.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
    }
    // PRIVATE HELPER METHODS

    private LoanApplication createApplicationEntity(CreateLoanApplicationDto dto, User currentUser) {
        LoanApplication application = new LoanApplication();

        // Fetch entities
        Borrower borrower = borrowerRepository.findById(dto.getBorrowerId())
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found: " + dto.getBorrowerId()));

        LoanProduct loanProduct = loanProductRepository.findById(dto.getLoanProductId())
                .orElseThrow(() -> new EntityNotFoundException("Loan product not found: " + dto.getLoanProductId()));

        Branch branch = systemService.getBranchForUser(currentUser);
        if (branch == null) {  // ✅ Use == null, not .equals(null)
            Optional<Branch> fetchBranch = branchRepository.findById(dto.getBranchId());
            if (fetchBranch.isPresent()) {
                branch = fetchBranch.get();
            }
        }
        // Set fields
        application.setBorrower(borrower);
        application.setLoanProduct(loanProduct);
        application.setBranch(branch);
        application.setAppliedAmount(dto.getAppliedAmount());
        application.setTenureMonths(dto.getTenureMonths());
        application.setTenureUnit(dto.getTenureUnit());
        application.setPurpose(dto.getPurpose());
        application.setPurposeCategory(dto.getPurposeCategory());
        application.setAdditionalNotes(dto.getAdditionalNotes());
        application.setCreatedBy(currentUser.getId());
        application.setCreatedByUser(currentUser);
        application.setInsuranceFee(dto.getInsuranceFee());
        application.setProcessingFee(dto.getProcessingFee());
        application.setTermsAccepted(true);
        application.setStatus(GeneralConfig.LoanApplicationStatus.DRAFT);
        application.setApplicationNumber(generateApplicationNumber());
        return application;
    }


    private void updateApplicationEntity(LoanApplication application,
                                         CreateLoanApplicationDto dto,
                                         Borrower borrower,
                                         LoanProduct loanProduct,
                                         Branch branch,
                                         User currentUser) {
        // Update basic fields
        application.setAppliedAmount(dto.getAppliedAmount());
        application.setTenureMonths(dto.getTenureMonths());
        application.setTenureUnit(dto.getTenureUnit());
        application.setPurpose(dto.getPurpose());
        application.setAdditionalNotes(dto.getAdditionalNotes());

        // Ensure entities are set
        application.setBorrower(borrower);
        application.setLoanProduct(loanProduct);
        application.setBranch(branch);

        // Update audit fields
        application.setUpdatedBy(currentUser.getId());
        application.setUpdatedByUser(currentUser);
        application.setUpdatedAt(LocalDateTime.now());

        // If submitting for approval
        if (Boolean.TRUE.equals(dto.getSubmitForApproval())) {
            application.setStatus(GeneralConfig.LoanApplicationStatus.PENDING_APPROVAL);
            application.setSubmittedDate(LocalDateTime.now());
            application.setSubmittedBy(currentUser.getUsername());
        }
    }

    private void validateApplicationForSubmission(LoanApplication application, User currentUser) {
        // Check if application is in draft status
        if (application.getStatus() != GeneralConfig.LoanApplicationStatus.DRAFT) {
            throw new ValidationException("Only draft applications can be submitted for approval");
        }

        // Check if user has permission to submit this application
        if (!application.getCreatedById().equals(currentUser.getId())) {
            throw new ValidationException("You can only submit applications created by you");
        }

        // Check document compliance
        DocumentComplianceSummary compliance = documentComplianceService.checkDocumentCompliance(
                application.getBorrower().getId(), application.getLoanProduct().getId());

        if (!compliance.getMeetsRequirements()) {
            throw new ValidationException(
                    "Cannot submit application: Document requirements not met. " +
                            "Missing: " + compliance.getMissingDocumentTypes() +
                            ", Pending: " + compliance.getPendingVerificationTypes() +
                            ", Expired: " + compliance.getExpiredDocumentTypes()
            );
        }

        // Check KYC status
        BorrowerKycSummaryDto kycSummary = borrowerService.getBorrowerKycSummary(application.getBorrower().getId());
        if (!kycSummary.getKycComplete()) {
            throw new ValidationException("Cannot submit application: Borrower KYC is not complete");
        }
    }

    private String generateApplicationNumber() {
        return systemService.getNextNumber("LOAN_APP");
    }

    private LoanApplicationDto enrichWithDocumentInfo(LoanApplication application) {
        LoanApplicationDto dto = loanApplicationMapper.toDto(application);

        log.info(">>> Add borrower document references START  ") ;

        // Add borrower document references
        List<BorrowerDocumentDto> borrowerDocuments = borrowerService
                .getBorrowerDocuments(application.getBorrower().getId());

        log.info(">>> Add borrowerDocuments references section : {} ",borrowerDocuments) ;

        List<BorrowerDocumentReferenceDto> documentReferences = documentComplianceService
                .convertToReferenceDtos(borrowerDocuments);
        dto.setBorrowerDocuments(documentReferences);

        log.info(">>> Add documentReferences references section: {}  ",documentReferences ) ;

        // Add KYC summary
        BorrowerKycSummaryDto kycSummary = borrowerService
                .getBorrowerKycSummary(application.getBorrower().getId());
        dto.setBorrowerKycSummary(kycSummary);

        log.info("Add kycSummary  section: {}  ",kycSummary ) ;

        // Add document compliance
        DocumentComplianceSummary compliance = documentComplianceService
                .checkDocumentCompliance(application.getBorrower().getId(), application.getLoanProduct().getId());
        dto.setDocumentCompliance(compliance);

        log.info("Add compliance  section: {}  ",compliance ) ;

        return dto;
    }



}