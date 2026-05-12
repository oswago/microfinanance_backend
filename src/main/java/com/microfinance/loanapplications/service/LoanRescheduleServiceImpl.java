package com.microfinance.loanapplications.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.loanapplications.dto.LoanDto;
import com.microfinance.loanapplications.dto.RescheduleRequestDto;
import com.microfinance.loanapplications.dto.disbursement.RescheduleApprovalDto;
import com.microfinance.loanapplications.dto.disbursement.RescheduleEligibilityDto;
import com.microfinance.loanapplications.dto.approval.ApprovalDecisionDto;
import com.microfinance.loanapplications.dto.rescheduling.RescheduleStatisticsDto;
import com.microfinance.loanapplications.dto.rescheduling.ApproveRejectRequestDto;
import com.microfinance.loanapplications.dto.rescheduling.CreateReschedulingRequestDto;
import com.microfinance.loanapplications.dto.rescheduling.EligibleLoanDto;
import com.microfinance.loanapplications.dto.rescheduling.ReschedulingDocumentDto;
import com.microfinance.loanapplications.entity.*;
import com.microfinance.loanapplications.mapper.EfficientLoanMapper;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanapplications.repository.LoanRescheduleRepository;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import com.microfinance.loanapplications.mapper.RescheduleMapper;
import com.microfinance.base.entity.User;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.BusinessException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.loanapplications.repository.ReschedulingDocumentRepository;
import com.microfinance.system.entity.Branch;
import com.microfinance.system.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoanRescheduleServiceImpl implements LoanRescheduleService {
    
    private final LoanRepository loanRepository;
    private final LoanRescheduleRepository loanRescheduleRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final EfficientLoanMapper loanMapper;
    private final RescheduleMapper rescheduleMapper;
    private final ReschedulingDocumentRepository reschedulingDocumentRepository;
    private final BranchRepository branchRepository;
    
    @Override
    public LoanDto rescheduleLoan(Long loanId, RescheduleRequestDto dto, User currentUser) {
        log.info("Rescheduling loan {} by user: {}", loanId, currentUser.getUsername());
        
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));
        
        // Validate rescheduling conditions
        validateRescheduleEligibility(loan);
        validateRescheduleRequest(loan, dto);
        
        // Create reschedule record
        LoanReschedule reschedule = createRescheduleRecord(loan, dto, currentUser);
        
        // Update loan terms
        updateLoanTerms(loan, dto);
        
        // Regenerate repayment schedule
        generateRepaymentSchedule(loan);
        
        Loan savedLoan = loanRepository.save(loan);
        loanRescheduleRepository.save(reschedule);
        
        log.info("Loan {} successfully rescheduled. New maturity date: {}", 
                loanId, dto.getNewMaturityDate());
        
        return loanMapper.toDto(savedLoan);
    }
    
    @Override
    public RescheduleApprovalDto submitRescheduleRequest(Long loanId, RescheduleRequestDto dto, User currentUser) {
        log.info("Submitting reschedule request for loan {} by user: {}", loanId, currentUser.getUsername());
        
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));
        
        // Validate eligibility
        validateRescheduleEligibility(loan);
        validateRescheduleRequest(loan, dto);
        
        // Create reschedule request (pending approval)
        LoanReschedule reschedule = new LoanReschedule();
        reschedule.setLoan(loan);
        reschedule.setOriginalMaturityDate(loan.getMaturityDate());
        reschedule.setNewMaturityDate(dto.getNewMaturityDate());
        reschedule.setExtensionMonths(calculateExtensionMonths(loan.getMaturityDate(), dto.getNewMaturityDate()));
        reschedule.setReason(dto.getReason());
        reschedule.setAdditionalNotes(dto.getAdditionalNotes());
        reschedule.setRequestedBy(currentUser);
        reschedule.setRequestDate(LocalDate.now());
        reschedule.setStatus(GeneralConfig.RescheduleStatus.PENDING_APPROVAL);
        
        // Calculate financial impact
        calculateFinancialImpact(reschedule, loan);
        
        LoanReschedule savedReschedule = loanRescheduleRepository.save(reschedule);
        
        log.info("Reschedule request submitted for loan {}. Request ID: {}", loanId, savedReschedule.getId());
        
        return rescheduleMapper.toApprovalDto(savedReschedule);
    }
    
    @Override
    public RescheduleApprovalDto approveReschedule(Long requestId, ApprovalDecisionDto dto, User approver) {
        log.info("Approving reschedule request {} by user: {}", requestId, approver.getUsername());
        
        LoanReschedule reschedule = loanRescheduleRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Reschedule request not found with id: " + requestId));
        
        if (reschedule.getStatus() != GeneralConfig.RescheduleStatus.PENDING_APPROVAL) {
            throw new BusinessException("Reschedule request is not pending approval");
        }
        
        Loan loan = reschedule.getLoan();
        
        // Update reschedule record
        reschedule.setStatus(GeneralConfig.RescheduleStatus.APPROVED);
        reschedule.setApprovedBy(approver);
        reschedule.setApprovalDate(LocalDate.now());
        reschedule.setApprovalNotes(dto.getComments());
        reschedule.setApprovalRole(dto.getApprovalRole());
        
        // Update loan terms
        updateLoanTerms(loan, rescheduleMapper.toRequestDto(reschedule));
        
        // Regenerate repayment schedule
        generateRepaymentSchedule(loan);
        
        loanRepository.save(loan);
        LoanReschedule savedReschedule = loanRescheduleRepository.save(reschedule);
        
        log.info("Reschedule request {} approved for loan {}", requestId, loan.getId());
        
        return rescheduleMapper.toApprovalDto(savedReschedule);
    }
    
    @Override
    public RescheduleApprovalDto rejectReschedule(Long requestId, ApprovalDecisionDto dto, User approver) {
        log.info("Rejecting reschedule request {} by user: {}", requestId, approver.getUsername());
        
        LoanReschedule reschedule = loanRescheduleRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Reschedule request not found with id: " + requestId));
        
        if (reschedule.getStatus() != GeneralConfig.RescheduleStatus.PENDING_APPROVAL) {
            throw new BusinessException("Reschedule request is not pending approval");
        }
        
        reschedule.setStatus(GeneralConfig.RescheduleStatus.REJECTED);
        reschedule.setApprovedBy(approver);
        reschedule.setApprovalDate(LocalDate.now());
        reschedule.setApprovalNotes(dto.getComments());
        reschedule.setRejectionReason(dto.getComments());
        
        LoanReschedule savedReschedule = loanRescheduleRepository.save(reschedule);
        
        log.info("Reschedule request {} rejected", requestId);
        
        return rescheduleMapper.toApprovalDto(savedReschedule);
    }
    
    @Override
    public List<RescheduleApprovalDto> getRescheduleHistory(Long loanId) {
        List<LoanReschedule> reschedules = loanRescheduleRepository.findByLoanIdOrderByRequestDateDesc(loanId);
        return reschedules.stream()
                .map(rescheduleMapper::toApprovalDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RescheduleApprovalDto> getPendingRescheduleRequests() {
        List<LoanReschedule> pendingRequests = loanRescheduleRepository
                .findByStatus(GeneralConfig.RescheduleStatus.PENDING_APPROVAL);
        return pendingRequests.stream()
                .map(rescheduleMapper::toApprovalDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public Page<RescheduleApprovalDto> getRescheduleRequestsByStatus(String status, Pageable pageable) {
        GeneralConfig.RescheduleStatus rescheduleStatus;
        try {
            rescheduleStatus = GeneralConfig.RescheduleStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid reschedule status: " + status);
        }
        
        Page<LoanReschedule> reschedules = loanRescheduleRepository.findByStatus(rescheduleStatus, pageable);
        return reschedules.map(rescheduleMapper::toApprovalDto);
    }
    
    @Override
    public RescheduleApprovalDto getRescheduleRequestById(Long requestId) {
        LoanReschedule reschedule = loanRescheduleRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Reschedule request not found with id: " + requestId));
        return rescheduleMapper.toApprovalDto(reschedule);
    }
    
    @Override
    public boolean canLoanBeRescheduled(Long loanId) {
        try {
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
            validateRescheduleEligibility(loan);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }
    
    @Override
    public RescheduleEligibilityDto checkRescheduleEligibility(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));
        
        RescheduleEligibilityDto eligibility = new RescheduleEligibilityDto();
        eligibility.setLoanId(loanId);
        eligibility.setLoanAccountNumber(loan.getLoanAccountNumber());
        eligibility.setCurrentStatus(loan.getStatus().name());
        
        try {
            validateRescheduleEligibility(loan);
            eligibility.setEligible(true);
            eligibility.setMessage("Loan is eligible for rescheduling");
        } catch (BusinessException e) {
            eligibility.setEligible(false);
            eligibility.setMessage(e.getMessage());
        }
        
        // Additional eligibility info
        eligibility.setDaysDelinquent(loan.getDaysDelinquent());
        eligibility.setOutstandingBalance(loan.getOutstandingBalance());
        eligibility.setCurrentMaturityDate(loan.getMaturityDate());
        eligibility.setMaxExtensionMonths(calculateMaxExtensionMonths(loan));
        
        return eligibility;
    }
    
    @Override
    public List<String> getValidRescheduleReasons() {
        return Arrays.asList(
            "BUSINESS_DOWNTURN",
            "HEALTH_ISSUES", 
            "NATURAL_DISASTER",
            "JOB_LOSS",
            "FAMILY_EMERGENCY",
            "SEASONAL_FLUCTUATION",
            "OTHER"
        );
    }
    
    // PRIVATE HELPER METHODS

    private void validateRescheduleEligibility(Loan loan) {
        // Check loan status
        if (loan.getStatus() != GeneralConfig.LoanStatus.ACTIVE &&
                loan.getStatus() != GeneralConfig.LoanStatus.DELINQUENT) {
            throw new BusinessException("Only active or delinquent loans can be rescheduled");
        }

        // Check if loan is already restructured too many times
        long previousReschedules = loanRescheduleRepository.countByLoanAndStatus(
                loan, GeneralConfig.RescheduleStatus.APPROVED);
        if (previousReschedules >= 3) {
            throw new BusinessException("Maximum reschedule limit (3) reached for this loan");
        }

        // Check if there's already a pending reschedule request
        boolean hasPendingRequest = loanRescheduleRepository.existsByLoanAndStatus(
                loan, GeneralConfig.RescheduleStatus.PENDING_APPROVAL);
        if (hasPendingRequest) {
            throw new BusinessException("There is already a pending reschedule request for this loan");
        }

        // Check if loan is too close to maturity
        long daysToMaturity = ChronoUnit.DAYS.between(LocalDate.now(), loan.getMaturityDate());
        if (daysToMaturity < 30) {
            throw new BusinessException("Cannot reschedule loan with less than 30 days to maturity");
        }

        // Check if loan has been recently rescheduled (within 6 months)
        Optional<LoanReschedule> latestReschedule = loanRescheduleRepository
                .findLatestApprovedReschedule(loan);
        if (latestReschedule.isPresent()) {
            LoanReschedule lastReschedule = latestReschedule.get();
            long monthsSinceLastReschedule = ChronoUnit.MONTHS.between(
                    lastReschedule.getApprovalDate(), LocalDate.now());
            if (monthsSinceLastReschedule < 6) {
                throw new BusinessException("Loan was recently rescheduled. Wait " +
                        (6 - monthsSinceLastReschedule) + " more months before requesting another reschedule");
            }
        }
    }
    
    private void validateRescheduleRequest(Loan loan, RescheduleRequestDto dto) {
        if (dto.getNewMaturityDate().isBefore(loan.getMaturityDate())) {
            throw new BusinessException("New maturity date must be after current maturity date");
        }
        
        if (dto.getNewMaturityDate().isAfter(LocalDate.now().plusYears(2))) {
            throw new BusinessException("Reschedule cannot extend beyond 2 years from today");
        }
        
        int extensionMonths = calculateExtensionMonths(loan.getMaturityDate(), dto.getNewMaturityDate());
        if (extensionMonths > 24) {
            throw new BusinessException("Maximum reschedule extension is 24 months");
        }
    }
    
    private LoanReschedule createRescheduleRecord(Loan loan, RescheduleRequestDto dto, User currentUser) {
        LoanReschedule reschedule = new LoanReschedule();
        reschedule.setLoan(loan);
        reschedule.setOriginalMaturityDate(loan.getMaturityDate());
        reschedule.setNewMaturityDate(dto.getNewMaturityDate());
        reschedule.setExtensionMonths(calculateExtensionMonths(loan.getMaturityDate(), dto.getNewMaturityDate()));
        reschedule.setReason(dto.getReason());
        reschedule.setAdditionalNotes(dto.getAdditionalNotes());
        reschedule.setRequestedBy(currentUser);
        reschedule.setRequestDate(LocalDate.now());
        reschedule.setStatus(GeneralConfig.RescheduleStatus.APPROVED); // Auto-approved for direct reschedule
        reschedule.setApprovedBy(currentUser);
        reschedule.setApprovalDate(LocalDate.now());
        
        calculateFinancialImpact(reschedule, loan);
        
        return reschedule;
    }
    
    private void updateLoanTerms(Loan loan, RescheduleRequestDto dto) {
        int extensionMonths = calculateExtensionMonths(loan.getMaturityDate(), dto.getNewMaturityDate());
        loan.setTenureMonths(loan.getTenureMonths() + extensionMonths);
        loan.setMaturityDate(dto.getNewMaturityDate());
        loan.setStatus(GeneralConfig.LoanStatus.RESTRUCTURED);
    }
    
    private void generateRepaymentSchedule(Loan loan) {
        // Clear existing future schedules
        List<RepaymentSchedule> futureSchedules = loan.getRepaymentSchedules().stream()
                .filter(schedule -> schedule.getStatus() == GeneralConfig.InstallmentStatus.PENDING)
                .collect(Collectors.toList());
        
        repaymentScheduleRepository.deleteAll(futureSchedules);
        
        // Generate new schedules (similar to your existing implementation)
        // This would regenerate the remaining repayment schedule
        // Implementation depends on your schedule generation logic
    }
    
    private int calculateExtensionMonths(LocalDate currentMaturity, LocalDate newMaturity) {
        return (int) ChronoUnit.MONTHS.between(currentMaturity, newMaturity);
    }
    
    private int calculateMaxExtensionMonths(Loan loan) {
        // Business logic for maximum extension
        // Could be based on loan product, borrower history, etc.
        return 24; // Default maximum
    }
    
    private void calculateFinancialImpact(LoanReschedule reschedule, Loan loan) {
        // Calculate the financial impact of rescheduling
        BigDecimal outstandingBalance = loan.getOutstandingBalance();
        BigDecimal monthlyInterestRate = loan.getInterestRate()
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        
        // Calculate new monthly payment
        BigDecimal newMonthlyPayment = calculateMonthlyPayment(
            outstandingBalance, monthlyInterestRate, reschedule.getExtensionMonths());
        
        reschedule.setOriginalMonthlyPayment(loan.getTotalDue().divide(
            BigDecimal.valueOf(loan.getTenureMonths()), 2, RoundingMode.HALF_UP));
        reschedule.setNewMonthlyPayment(newMonthlyPayment);
        reschedule.setOriginalTermMonths(loan.getTenureMonths());
        reschedule.setNewTermMonths(loan.getTenureMonths() + reschedule.getExtensionMonths());
    }


    // In your LoanRescheduleServiceImpl, add these methods:

    public RescheduleStatisticsDto getRescheduleStatistics(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(6);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        long totalRequests = loanRescheduleRepository.countByStatusAndRequestDateBetween(
                GeneralConfig.RescheduleStatus.PENDING_APPROVAL, startDate, endDate);
        long approvedRequests = loanRescheduleRepository.countByStatusAndRequestDateBetween(
                GeneralConfig.RescheduleStatus.APPROVED, startDate, endDate);
        long rejectedRequests = loanRescheduleRepository.countByStatusAndRequestDateBetween(
                GeneralConfig.RescheduleStatus.REJECTED, startDate, endDate);

        return RescheduleStatisticsDto.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalRequests(totalRequests)
                .approvedRequests(approvedRequests)
                .rejectedRequests(rejectedRequests)
                .approvalRate(approvedRequests > 0 ?
                        (double) approvedRequests / (approvedRequests + rejectedRequests) * 100 : 0)
                .build();
    }

    public List<LoanReschedule> getRecentRescheduleActivity(int days) {
        LocalDate sinceDate = LocalDate.now().minusDays(days);
        return loanRescheduleRepository.findRecentReschedules(sinceDate);
    }

    public boolean hasPendingRescheduleRequest(Long loanId) {
        return loanRescheduleRepository.existsByLoanIdAndStatus(
                loanId, GeneralConfig.RescheduleStatus.PENDING_APPROVAL);
    }

    public Optional<LoanReschedule> getLatestReschedule(Long loanId) {
        return loanRescheduleRepository.findLatestApprovedRescheduleByLoanId(loanId);
    }



    @Override
    public Page<RescheduleApprovalDto> getReschedulingRequests(String status, Long branchId,
                                                               LocalDate startDate, LocalDate endDate,
                                                               Pageable pageable) {
        log.info("Fetching rescheduling requests with filters");

        GeneralConfig.RescheduleStatus rescheduleStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                rescheduleStatus = GeneralConfig.RescheduleStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid status: " + status);
            }
        }

        Page<LoanReschedule> reschedules = loanRescheduleRepository.findWithFilters(
                rescheduleStatus, branchId, startDate, endDate, pageable);

        return reschedules.map(rescheduleMapper::toApprovalDto);
    }

    @Override
    public RescheduleStatisticsDto getReschedulingStatistics() {
        log.info("Getting rescheduling statistics");

        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);

        long pendingRequests = loanRescheduleRepository.countByStatus(
                GeneralConfig.RescheduleStatus.PENDING_APPROVAL);
        long underReview = loanRescheduleRepository.countByStatus(
                GeneralConfig.RescheduleStatus.UNDER_REVIEW);
        long approvedRequests = loanRescheduleRepository.countByStatus(
                GeneralConfig.RescheduleStatus.APPROVED);
        long rejectedRequests = loanRescheduleRepository.countByStatus(
                GeneralConfig.RescheduleStatus.REJECTED);

        long requestsThisMonth = loanRescheduleRepository.countByRequestDateBetween(
                startOfMonth, now);
        long requestsLastMonth = loanRescheduleRepository.countByRequestDateBetween(
                startOfLastMonth, startOfMonth.minusDays(1));

        // Calculate average processing time for last 30 days
        Double avgProcessingTime = loanRescheduleRepository.getAverageProcessingTime(
                now.minusDays(30), now);

        return RescheduleStatisticsDto.builder()
                .pendingRequests(pendingRequests)
                .underReview(underReview)
                .approvedRequests(approvedRequests)
                .rejectedRequests(rejectedRequests)
                .totalRequests(pendingRequests + underReview + approvedRequests + rejectedRequests)
                .requestsThisMonth(requestsThisMonth)
                .requestsLastMonth(requestsLastMonth)
                .averageProcessingTime(avgProcessingTime != null ? avgProcessingTime : 0)
                .approvalRate(approvedRequests + rejectedRequests > 0 ?
                        (double) approvedRequests / (approvedRequests + rejectedRequests) * 100 : 0)
                .build();
    }

    @Override
    public List<EligibleLoanDto> searchEligibleLoans(String searchTerm) {
        log.info(">>> Searching eligible loans with term: {}", searchTerm);

        List<Loan> loans;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            loans = loanRepository.searchEligibleForRescheduling(searchTerm.toLowerCase());
        } else {
            loans = loanRepository.findEligibleForRescheduling();
        }

        return loans.stream()
                .map(this::mapToEligibleLoanDto)
                .limit(20)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public RescheduleApprovalDto createReschedulingRequest(CreateReschedulingRequestDto requestDto,
                                                           List<MultipartFile> documents,
                                                           User currentUser) {
        log.info("Creating rescheduling request for loan: {}", requestDto.getLoanId());

        Loan loan = loanRepository.findById(requestDto.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        // Validate eligibility
        validateRescheduleEligibility(loan);

        // Create reschedule record
        LoanReschedule reschedule = new LoanReschedule();
        reschedule.setLoan(loan);
        reschedule.setRequestedBy(currentUser);
        reschedule.setRequestDate(LocalDate.now());
        reschedule.setStatus(GeneralConfig.RescheduleStatus.PENDING_APPROVAL);
        reschedule.setReason(requestDto.getReason());

        // Set current loan details
        reschedule.setOriginalTenureMonths(loan.getTenureMonths());
        reschedule.setOriginalMonthlyPayment(calculateCurrentMonthlyPayment(loan));

        // Set proposed changes based on request type
        setProposedTerms(reschedule, requestDto, loan);

        // Save request
        LoanReschedule savedReschedule = loanRescheduleRepository.save(reschedule);

        // Handle document uploads
        if (documents != null && !documents.isEmpty()) {
            saveDocuments(savedReschedule, documents, currentUser);
        }

        return rescheduleMapper.toApprovalDto(savedReschedule);
    }

    @Override
    @Transactional
    public RescheduleApprovalDto approveReschedulingRequest(Long requestId,
                                                            ApproveRejectRequestDto requestDto,
                                                            User approver) {
        log.info("Approving rescheduling request: {}", requestId);

        LoanReschedule reschedule = loanRescheduleRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (reschedule.getStatus() != GeneralConfig.RescheduleStatus.PENDING_APPROVAL &&
                reschedule.getStatus() != GeneralConfig.RescheduleStatus.UNDER_REVIEW) {
            throw new BusinessException("Only pending or under review requests can be approved");
        }

        // Convert LoanReschedule to RescheduleRequestDto for updateLoanTerms
        RescheduleRequestDto requestDtoForUpdate = convertToRequestDto(reschedule);

        // Update loan terms
        Loan loan = reschedule.getLoan();
        updateLoanTerms(loan, requestDtoForUpdate);

        // Update reschedule record
        reschedule.setStatus(GeneralConfig.RescheduleStatus.APPROVED);
        reschedule.setApprovedBy(approver);
        reschedule.setApprovalDate(LocalDate.now());
        reschedule.setApprovalNotes(requestDto.getComments());
        reschedule.setApprovalReference(requestDto.getApprovalReference());

        // Save changes
        loanRepository.save(loan);
        LoanReschedule savedReschedule = loanRescheduleRepository.save(reschedule);

        // Generate new repayment schedule
        generateRepaymentSchedule(loan);

        return rescheduleMapper.toApprovalDto(savedReschedule);
    }


    private RescheduleRequestDto convertToRequestDto(LoanReschedule reschedule) {
        RescheduleRequestDto dto = new RescheduleRequestDto();
        dto.setReason(reschedule.getReason());
        dto.setAdditionalNotes(reschedule.getAdditionalNotes());

        // Set maturity dates
        if (reschedule.getNewMaturityDate() != null) {
            dto.setNewMaturityDate(reschedule.getNewMaturityDate());
        } else if (reschedule.getNewTenureMonths() != null && reschedule.getLoan() != null) {
            // Calculate new maturity date from tenure
            LocalDate newMaturity = LocalDate.now().plusMonths(reschedule.getNewTenureMonths());
            dto.setNewMaturityDate(newMaturity);
        }

        // Set other fields as needed
        return dto;
    }


    @Override
    @Transactional
    public RescheduleApprovalDto rejectReschedulingRequest(Long requestId,
                                                           ApproveRejectRequestDto requestDto,
                                                           User approver) {
        log.info("Rejecting rescheduling request: {}", requestId);

        LoanReschedule reschedule = loanRescheduleRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (reschedule.getStatus() != GeneralConfig.RescheduleStatus.PENDING_APPROVAL &&
                reschedule.getStatus() != GeneralConfig.RescheduleStatus.UNDER_REVIEW) {
            throw new BusinessException("Only pending or under review requests can be rejected");
        }

        reschedule.setStatus(GeneralConfig.RescheduleStatus.REJECTED);
        reschedule.setApprovedBy(approver);
        reschedule.setApprovalDate(LocalDate.now());
        reschedule.setRejectionReason(requestDto.getComments());

        LoanReschedule savedReschedule = loanRescheduleRepository.save(reschedule);

        return rescheduleMapper.toApprovalDto(savedReschedule);
    }

    @Override
    @Transactional
    public RescheduleApprovalDto requestMoreInfo(Long requestId, String message, User currentUser) {
        log.info("Requesting more info for rescheduling request: {}", requestId);

        LoanReschedule reschedule = loanRescheduleRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        reschedule.setStatus(GeneralConfig.RescheduleStatus.UNDER_REVIEW);
        reschedule.setReviewedBy(currentUser);
        reschedule.setReviewComments(message);

        LoanReschedule savedReschedule = loanRescheduleRepository.save(reschedule);

        return rescheduleMapper.toApprovalDto(savedReschedule);
    }

    @Override
    @Transactional
    public RescheduleApprovalDto cancelReschedulingRequest(Long requestId, User currentUser) {
        log.info("Cancelling rescheduling request: {}", requestId);

        LoanReschedule reschedule = loanRescheduleRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (reschedule.getStatus() != GeneralConfig.RescheduleStatus.PENDING_APPROVAL &&
                reschedule.getStatus() != GeneralConfig.RescheduleStatus.UNDER_REVIEW) {
            throw new BusinessException("Only pending or under review requests can be cancelled");
        }

        reschedule.setStatus(GeneralConfig.RescheduleStatus.CANCELLED);
        reschedule.setReviewedBy(currentUser);

        LoanReschedule savedReschedule = loanRescheduleRepository.save(reschedule);

        return rescheduleMapper.toApprovalDto(savedReschedule);
    }

    @Override
    public byte[] generateReschedulingReport(LocalDate startDate, LocalDate endDate, Long branchId) {
        log.info("Generating rescheduling report");

        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(12);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<LoanReschedule> reschedules = loanRescheduleRepository.findWithFilters(
                null, branchId, startDate, endDate, pageable);

        try {
            return generateReschedulingPdfReport(reschedules.getContent(), startDate, endDate, branchId);
        } catch (Exception e) {
            log.error("Error generating report", e);
            throw new BusinessException("Failed to generate report");
        }
    }

    @Override
    public ReschedulingDocumentDto getDocument(Long documentId) {
        ReschedulingDocument document = reschedulingDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        return mapToDocumentDto(document);
    }

    @Override
    public byte[] downloadDocument(Long documentId) {
        ReschedulingDocument document = reschedulingDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        return document.getFileContent();
    }

    // ============== Helper Methods ==============

    private void setProposedTerms(LoanReschedule reschedule, CreateReschedulingRequestDto dto, Loan loan) {
        BigDecimal remainingBalance = loan.getOutstandingBalance();
        int remainingTerm = calculateRemainingTerm(loan);

        switch (dto.getRequestType()) {
            case "TENURE_EXTENSION":
                int newTerm = remainingTerm + (dto.getAdditionalMonths() != null ? dto.getAdditionalMonths() : 0);
                reschedule.setNewTenureMonths(newTerm);
                reschedule.setNewMonthlyPayment(calculateMonthlyPayment(
                        remainingBalance, loan.getInterestRate(), newTerm));
                break;

            case "PAYMENT_REDUCTION":
                if (dto.getReducedPayment() != null) {
                    reschedule.setNewMonthlyPayment(dto.getReducedPayment());
                    // Recalculate term based on reduced payment
                    int recalculatedTerm = recalculateTermFromPayment(
                            remainingBalance, loan.getInterestRate(), dto.getReducedPayment());
                    reschedule.setNewTenureMonths(recalculatedTerm);
                }
                break;

            case "INTEREST_RATE_ADJUSTMENT":
                if (dto.getProposedInterestRate() != null) {
                    reschedule.setProposedInterestRate(dto.getProposedInterestRate());
                    reschedule.setNewMonthlyPayment(calculateMonthlyPayment(
                            remainingBalance, dto.getProposedInterestRate(), remainingTerm));
                    reschedule.setNewTenureMonths(remainingTerm);
                }
                break;

            case "PAYMENT_HOLIDAY":
                if (dto.getHolidayMonths() != null) {
                    reschedule.setGracePeriodDays(dto.getHolidayMonths() * 30);
                    reschedule.setResumeDate(dto.getResumeDate());
                    reschedule.setNewTenureMonths(remainingTerm + dto.getHolidayMonths());
                    reschedule.setNewMonthlyPayment(calculateMonthlyPayment(
                            remainingBalance, loan.getInterestRate(), remainingTerm + dto.getHolidayMonths()));
                }
                break;
        }
    }

    private void saveDocuments(LoanReschedule reschedule, List<MultipartFile> documents, User currentUser) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        for (MultipartFile file : documents) {
            try {
                ReschedulingDocument document = new ReschedulingDocument();
                document.setLoanReschedule(reschedule); // Fixed: using setLoanReschedule
                document.setFileName(file.getOriginalFilename());
                document.setFileType(file.getContentType());
                document.setFileSize(file.getSize());
                document.setFileContent(file.getBytes());
                document.setUploadedBy(currentUser.getUsername());
                document.setUploadDate(LocalDateTime.now());

                reschedulingDocumentRepository.save(document);

                // Add to the reschedule's documents list
                if (reschedule.getDocuments() == null) {
                    reschedule.setDocuments(new ArrayList<>());
                }
                reschedule.getDocuments().add(document);

            } catch (Exception e) {
                log.error("Error saving document: {}", file.getOriginalFilename(), e);
                throw new BusinessException("Failed to save document: " + file.getOriginalFilename());
            }
        }
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        BigDecimal ratePlusOne = monthlyRate.add(BigDecimal.ONE);
        BigDecimal numerator = monthlyRate.multiply(ratePlusOne.pow(months));
        BigDecimal denominator = ratePlusOne.pow(months).subtract(BigDecimal.ONE);

        return principal.multiply(numerator).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private int recalculateTermFromPayment(BigDecimal principal, BigDecimal annualRate, BigDecimal payment) {
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        // Using formula: n = -log(1 - (P * r) / PMT) / log(1 + r)
        double r = monthlyRate.doubleValue();
        double p = principal.doubleValue();
        double pmt = payment.doubleValue();

        if (r == 0) {
            return (int) Math.ceil(p / pmt);
        }

        double n = -Math.log(1 - (p * r) / pmt) / Math.log(1 + r);
        return (int) Math.ceil(n);
    }

    private int calculateRemainingTerm(Loan loan) {
        long remainingMonths = ChronoUnit.MONTHS.between(LocalDate.now(), loan.getMaturityDate());
        return Math.max(0, (int) remainingMonths);
    }

    private BigDecimal calculateCurrentMonthlyPayment(Loan loan) {
        if (loan.getTotalDue() == null || loan.getTenureMonths() == null || loan.getTenureMonths() == 0) {
            return BigDecimal.ZERO;
        }
        return loan.getTotalDue().divide(BigDecimal.valueOf(loan.getTenureMonths()), 2, RoundingMode.HALF_UP);
    }

    private EligibleLoanDto mapToEligibleLoanDto(Loan loan) {
        Borrower borrower = loan.getBorrower();
        String displayName = loan.getLoanAccountNumber() + " - " +
                (borrower != null ? borrower.getFullName() : "Unknown");

        return EligibleLoanDto.builder()
                .id(loan.getId())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .borrowerName(borrower != null ? borrower.getFullName() : "Unknown")
                .borrowerId(borrower != null ? borrower.getBorrowerNumber() : null)
                .principalAmount(loan.getPrincipalAmount())
                .outstandingBalance(loan.getOutstandingBalance())
                .currentMonthlyPayment(calculateCurrentMonthlyPayment(loan))
                .remainingInstallments(calculateRemainingTerm(loan))
                .interestRate(loan.getInterestRate())
                .status(loan.getStatus() != null ? loan.getStatus().toString() : null)
                .daysOverdue(loan.getDaysDelinquent())
                .displayName(displayName)
                .build();
    }

    private ReschedulingDocumentDto mapToDocumentDto(ReschedulingDocument document) {
        return ReschedulingDocumentDto.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .uploadedBy(document.getUploadedBy())
                .uploadDate(document.getUploadDate())
                .downloadUrl("/api/loan-rescheduling/documents/" + document.getId() + "/download")
                .build();
    }

    private byte[] generateReschedulingPdfReport(List<LoanReschedule> reschedules,
                                                 LocalDate startDate, LocalDate endDate,
                                                 Long branchId) throws Exception {
        // PDF generation logic similar to other reports
        // ... implementation using iText ...
        return new byte[0]; // Placeholder
    }


    @Override
    public List<RescheduleApprovalDto> getReschedulingHistory(Long loanId) {
        log.info("Fetching rescheduling history for loan: {}", loanId);

        // Verify loan exists
        if (!loanRepository.existsById(loanId)) {
            throw new ResourceNotFoundException("Loan not found with id: " + loanId);
        }

        // Fetch all rescheduling requests for the loan, ordered by request date (newest first)
        List<LoanReschedule> reschedules = loanRescheduleRepository
                .findByLoanIdOrderByRequestDateDesc(loanId);

        if (reschedules.isEmpty()) {
            log.info("No rescheduling history found for loan: {}", loanId);
            return Collections.emptyList();
        }

        log.info("Found {} rescheduling records for loan: {}", reschedules.size(), loanId);

        // Convert to DTOs
        return reschedules.stream()
                .map(rescheduleMapper::toApprovalDto)
                .collect(Collectors.toList());
    }


    @Override
    public RescheduleApprovalDto getReschedulingRequestById(Long id) {
        log.info("Fetching rescheduling request with ID: {}", id);

        LoanReschedule reschedule = loanRescheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rescheduling request not found with id: " + id));

        log.debug("Found rescheduling request: {} for loan: {}",
                reschedule.getId(), reschedule.getLoan().getId());

        // Convert to DTO
        RescheduleApprovalDto dto = rescheduleMapper.toApprovalDto(reschedule);

        // Enhance with additional information
        enhanceRescheduleDto(dto, reschedule);

        return dto;
    }


    private void enhanceRescheduleDto(RescheduleApprovalDto dto, LoanReschedule reschedule) {
        if (dto == null || reschedule == null) {
            return;
        }

        // Add calculated fields
        if (reschedule.getRequestDate() != null && reschedule.getApprovalDate() != null) {
            long processingDays = ChronoUnit.DAYS.between(
                    reschedule.getRequestDate(), reschedule.getApprovalDate());
            dto.setProcessingDays((int) processingDays);
        }

        // Add approval status description
        if (reschedule.getStatus() != null) {
            switch (reschedule.getStatus()) {
                case PENDING_APPROVAL:
                    dto.setStatusDescription("Awaiting approval");
                    break;
                case UNDER_REVIEW:
                    dto.setStatusDescription("Under review by " +
                            (reschedule.getReviewedBy() != null ?
                                    reschedule.getReviewedBy().getFullName() : "credit team"));
                    break;
                case APPROVED:
                    dto.setStatusDescription("Approved on " +
                            (reschedule.getApprovalDate() != null ?
                                    reschedule.getApprovalDate().toString() : "unknown date"));
                    break;
                case REJECTED:
                    dto.setStatusDescription("Rejected: " +
                            (reschedule.getRejectionReason() != null ?
                                    reschedule.getRejectionReason() : "No reason provided"));
                    break;
                case CANCELLED:
                    dto.setStatusDescription("Cancelled by requester");
                    break;
            }
        }

        // Add financial impact summary
        if (reschedule.getOriginalMonthlyPayment() != null &&
                reschedule.getNewMonthlyPayment() != null) {
            BigDecimal paymentChange = reschedule.getNewMonthlyPayment()
                    .subtract(reschedule.getOriginalMonthlyPayment());
            dto.setMonthlyPaymentChange(paymentChange);
            dto.setMonthlyPaymentChangePercent(
                    paymentChange.divide(reschedule.getOriginalMonthlyPayment(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP)
            );
        }
    }


    @Override
    public byte[] generateAnalyticsReport(LocalDate startDate, LocalDate endDate, Long branchId) {
        log.info("Generating analytics report from {} to {} for branch: {}", startDate, endDate, branchId);

        // Set default date range if not provided
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(12);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Get statistics for the period
        RescheduleStatisticsDto statistics = getReschedulingStatistics();

        // Get all requests for the period
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<LoanReschedule> reschedules = loanRescheduleRepository.findWithFilters(
                null, branchId, startDate, endDate, pageable);

        try {
            return generateAnalyticsPdfReport(reschedules.getContent(), statistics, startDate, endDate, branchId);
        } catch (Exception e) {
            log.error("Error generating analytics report", e);
            throw new BusinessException("Failed to generate analytics report");
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////
    @Override
    public byte[] generateSingleReschedulingReport(Long requestId) {
        log.info("Generating rescheduling report for request ID: {}", requestId);

        // Fetch the specific rescheduling request with all related data
        LoanReschedule reschedule = loanRescheduleRepository.findByIdWithAllDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescheduling request not found with id: " + requestId));

        try {
            return generateSingleReschedulePdfReport(reschedule);
        } catch (Exception e) {
            log.error("Error generating single rescheduling report for request {}", requestId, e);
            throw new BusinessException("Failed to generate report for request: " + requestId);
        }
    }

    private byte[] generateSingleReschedulePdfReport(LoanReschedule reschedule) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Add title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("RESCHEDULING REQUEST REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));

        // Request Information
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

        document.add(new Paragraph("REQUEST INFORMATION", headerFont));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Request ID: " + reschedule.getId(), normalFont));
        document.add(new Paragraph("Status: " + (reschedule.getStatus() != null ? reschedule.getStatus().toString() : "N/A"), normalFont));
        document.add(new Paragraph("Request Date: " + (reschedule.getRequestDate() != null ? reschedule.getRequestDate().toString() : "N/A"), normalFont));
        document.add(new Paragraph("Reason: " + (reschedule.getReason() != null ? reschedule.getReason() : "N/A"), normalFont));

        document.add(new Paragraph(" "));

        // Loan Information
        Loan loan = reschedule.getLoan();
        if (loan != null) {
            document.add(new Paragraph("LOAN INFORMATION", headerFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Loan Account: " + loan.getLoanAccountNumber(), normalFont));

            if (loan.getBorrower() != null) {
                Borrower borrower = loan.getBorrower();
                document.add(new Paragraph("Borrower: " + borrower.getFirstName() + " " +
                        (borrower.getLastName() != null ? borrower.getLastName() : ""), normalFont));
                document.add(new Paragraph("Borrower ID: " + borrower.getBorrowerNumber(), normalFont));
            }

            document.add(new Paragraph("Principal Amount: " + formatCurrency(loan.getPrincipalAmount()), normalFont));
            document.add(new Paragraph("Outstanding Balance: " + formatCurrency(loan.getOutstandingBalance()), normalFont));
            document.add(new Paragraph("Interest Rate: " + loan.getInterestRate() + "%", normalFont));
            document.add(new Paragraph("Original Tenure: " + loan.getTenureMonths() + " months", normalFont));
            document.add(new Paragraph("Original Maturity Date: " +
                    (loan.getMaturityDate() != null ? loan.getMaturityDate().toString() : "N/A"), normalFont));

            document.add(new Paragraph(" "));
        }

        // Rescheduling Details
        document.add(new Paragraph("RESCHEDULING DETAILS", headerFont));
        document.add(new Paragraph(" "));

        // Create a table for comparison
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Table headers
        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        addTableCell(table, "Metric", tableHeaderFont);
        addTableCell(table, "Current", tableHeaderFont);
        addTableCell(table, "Proposed", tableHeaderFont);
        addTableCell(table, "Change", tableHeaderFont);

        // Monthly Payment
        addTableCell(table, "Monthly Payment", normalFont);
        addTableCell(table, formatCurrency(reschedule.getOriginalMonthlyPayment()), normalFont);
        addTableCell(table, formatCurrency(reschedule.getNewMonthlyPayment()), normalFont);

        BigDecimal paymentChange = reschedule.getNewMonthlyPayment() != null && reschedule.getOriginalMonthlyPayment() != null ?
                reschedule.getNewMonthlyPayment().subtract(reschedule.getOriginalMonthlyPayment()) : BigDecimal.ZERO;
        String paymentChangeStr = formatCurrency(paymentChange) +
                (paymentChange.compareTo(BigDecimal.ZERO) > 0 ? " ↑" : paymentChange.compareTo(BigDecimal.ZERO) < 0 ? " ↓" : "");
        addTableCell(table, paymentChangeStr, normalFont);

        // Term/Months
        addTableCell(table, "Term (Months)", normalFont);
        addTableCell(table, String.valueOf(reschedule.getOriginalTermMonths() != null ? reschedule.getOriginalTermMonths() : ""), normalFont);
        addTableCell(table, String.valueOf(reschedule.getNewTermMonths() != null ? reschedule.getNewTermMonths() : ""), normalFont);

        Integer termChange = reschedule.getNewTermMonths() != null && reschedule.getOriginalTermMonths() != null ?
                reschedule.getNewTermMonths() - reschedule.getOriginalTermMonths() : 0;
        String termChangeStr = termChange + (termChange > 0 ? " ↑" : termChange < 0 ? " ↓" : "");
        addTableCell(table, termChangeStr, normalFont);

        // Maturity Date
        addTableCell(table, "Maturity Date", normalFont);
        addTableCell(table, reschedule.getOriginalMaturityDate() != null ? reschedule.getOriginalMaturityDate().toString() : "N/A", normalFont);
        addTableCell(table, reschedule.getNewMaturityDate() != null ? reschedule.getNewMaturityDate().toString() : "N/A", normalFont);

        String extensionStr = reschedule.getExtensionMonths() != null ?
                reschedule.getExtensionMonths() + " months" : "0 months";
        addTableCell(table, extensionStr, normalFont);

        document.add(table);

        // Approval Details (if available)
        if (reschedule.getStatus() == GeneralConfig.RescheduleStatus.APPROVED ||
                reschedule.getStatus() == GeneralConfig.RescheduleStatus.REJECTED) {

            document.add(new Paragraph(" "));
            document.add(new Paragraph("APPROVAL DETAILS", headerFont));
            document.add(new Paragraph(" "));

            if (reschedule.getApprovedBy() != null) {
                document.add(new Paragraph("Action By: " +
                        reschedule.getApprovedBy().getFirstName() + " " +
                        (reschedule.getApprovedBy().getLastName() != null ? reschedule.getApprovedBy().getLastName() : ""), normalFont));
            }

            document.add(new Paragraph("Action Date: " +
                    (reschedule.getApprovalDate() != null ? reschedule.getApprovalDate().toString() : "N/A"), normalFont));

            if (reschedule.getStatus() == GeneralConfig.RescheduleStatus.REJECTED &&
                    reschedule.getRejectionReason() != null) {
                document.add(new Paragraph("Rejection Reason: " + reschedule.getRejectionReason(), normalFont));
            }

            if (reschedule.getApprovalNotes() != null) {
                document.add(new Paragraph("Notes: " + reschedule.getApprovalNotes(), normalFont));
            }
        }

        // Footer
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);
        document.add(new Paragraph("Report generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")), footerFont));
        document.add(new Paragraph("This is a system-generated report.", footerFont));

        document.close();
        return baos.toByteArray();
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "KES0.00";
        return "KES" + amount.setScale(2, RoundingMode.HALF_UP).toString();
    }


    private byte[] generateAnalyticsPdfReport(List<LoanReschedule> reschedules,
                                              RescheduleStatisticsDto statistics,
                                              LocalDate startDate,
                                              LocalDate endDate,
                                              Long branchId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("RESCHEDULING ANALYTICS REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));

        // Report Info
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

        document.add(new Paragraph("Report Period: " + startDate + " to " + endDate, normalFont));
        if (branchId != null) {
            String branchName = getBranchName(branchId);
            document.add(new Paragraph("Branch: " + branchName, normalFont));
        } else {
            document.add(new Paragraph("Branch: All Branches", normalFont));
        }
        document.add(new Paragraph("Generated On: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")), normalFont));
        document.add(new Paragraph(" "));

        // Summary Statistics
        document.add(new Paragraph("SUMMARY STATISTICS", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
        document.add(new Paragraph(" "));

        // Create a table for summary
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(70);
        summaryTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addSummaryRow(summaryTable, "Total Requests:", String.valueOf(statistics.getTotalRequests()), boldFont, normalFont);
        addSummaryRow(summaryTable, "Pending:", String.valueOf(statistics.getPendingRequests()), boldFont, normalFont);
        addSummaryRow(summaryTable, "Under Review:", String.valueOf(statistics.getUnderReview()), boldFont, normalFont);
        addSummaryRow(summaryTable, "Approved:", String.valueOf(statistics.getApprovedRequests()), boldFont, normalFont);
        addSummaryRow(summaryTable, "Rejected:", String.valueOf(statistics.getRejectedRequests()), boldFont, normalFont);
        addSummaryRow(summaryTable, "This Month:", String.valueOf(statistics.getRequestsThisMonth()), boldFont, normalFont);
        addSummaryRow(summaryTable, "Last Month:", String.valueOf(statistics.getRequestsLastMonth()), boldFont, normalFont);
        addSummaryRow(summaryTable, "Approval Rate:", String.format("%.1f%%", statistics.getApprovalRate()), boldFont, normalFont);
        addSummaryRow(summaryTable, "Avg Processing Time:", String.format("%.1f days", statistics.getAverageProcessingTime() / 86400), boldFont, normalFont);

        document.add(summaryTable);
        document.add(new Paragraph(" "));

        // Status Distribution
        document.add(new Paragraph("STATUS DISTRIBUTION", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
        document.add(new Paragraph(" "));

        PdfPTable statusTable = new PdfPTable(2);
        statusTable.setWidthPercentage(60);
        statusTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addStatusRow(statusTable, "Pending", statistics.getPendingRequests(), statistics.getTotalRequests(), normalFont);
        addStatusRow(statusTable, "Under Review", statistics.getUnderReview(), statistics.getTotalRequests(), normalFont);
        addStatusRow(statusTable, "Approved", statistics.getApprovedRequests(), statistics.getTotalRequests(), normalFont);
        addStatusRow(statusTable, "Rejected", statistics.getRejectedRequests(), statistics.getTotalRequests(), normalFont);

        document.add(statusTable);
        document.add(new Paragraph(" "));

        // Reasons Breakdown
        if (!reschedules.isEmpty()) {
            document.add(new Paragraph("REASONS FOR RESCHEDULING", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
            document.add(new Paragraph(" "));

            // Count reasons
            Map<String, Integer> reasonCounts = new HashMap<>();
            for (LoanReschedule reschedule : reschedules) {
                String reason = reschedule.getReason() != null ? reschedule.getReason() : "Not Specified";
                reasonCounts.put(reason, reasonCounts.getOrDefault(reason, 0) + 1);
            }

            PdfPTable reasonTable = new PdfPTable(3);
            reasonTable.setWidthPercentage(100);

            // Table headers
            Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            addTableCell(reasonTable, "Reason", tableHeaderFont);
            addTableCell(reasonTable, "Count", tableHeaderFont);
            addTableCell(reasonTable, "Percentage", tableHeaderFont);

            // Table data
            int total = reschedules.size();
            for (Map.Entry<String, Integer> entry : reasonCounts.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / total;
                addTableCell(reasonTable, entry.getKey(), normalFont);
                addTableCell(reasonTable, String.valueOf(entry.getValue()), normalFont);
                addTableCell(reasonTable, String.format("%.1f%%", percentage), normalFont);
            }

            document.add(reasonTable);
            document.add(new Paragraph(" "));
        }

        // Recent Requests (Top 10)
        if (!reschedules.isEmpty()) {
            document.add(new Paragraph("RECENT RESCHEDULING REQUESTS", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
            document.add(new Paragraph(" "));

            // Sort by request date descending
            List<LoanReschedule> recentRequests = reschedules.stream()
                    .sorted((a, b) -> b.getRequestDate().compareTo(a.getRequestDate()))
                    .limit(10)
                    .collect(Collectors.toList());

            PdfPTable recentTable = new PdfPTable(5);
            recentTable.setWidthPercentage(100);
            recentTable.setWidths(new float[]{1.5f, 2f, 2f, 1.5f, 1.5f});

            // Table headers
            Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            addTableCell(recentTable, "ID", tableHeaderFont);
            addTableCell(recentTable, "Loan Account", tableHeaderFont);
            addTableCell(recentTable, "Borrower", tableHeaderFont);
            addTableCell(recentTable, "Request Date", tableHeaderFont);
            addTableCell(recentTable, "Status", tableHeaderFont);

            // Table data
            for (LoanReschedule req : recentRequests) {
                addTableCell(recentTable, String.valueOf(req.getId()), normalFont);
                addTableCell(recentTable, req.getLoan() != null ? req.getLoan().getLoanAccountNumber() : "N/A", normalFont);

                String borrowerName = "N/A";
                if (req.getLoan() != null && req.getLoan().getBorrower() != null) {
                    Borrower borrower = req.getLoan().getBorrower();
                    borrowerName = borrower.getFirstName() + " " +
                            (borrower.getLastName() != null ? borrower.getLastName() : "");
                }
                addTableCell(recentTable, borrowerName, normalFont);

                addTableCell(recentTable, req.getRequestDate() != null ? req.getRequestDate().toString() : "N/A", normalFont);
                addTableCell(recentTable, req.getStatus() != null ? req.getStatus().toString() : "N/A", normalFont);
            }

            document.add(recentTable);
        }

        // Footer
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);
        document.add(new Paragraph("This is a system-generated analytics report.", footerFont));

        document.close();
        return baos.toByteArray();
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void addStatusRow(PdfPTable table, String status, long count, long total, Font font) {
        double percentage = total > 0 ? (count * 100.0) / total : 0;

        PdfPCell statusCell = new PdfPCell(new Phrase(status, font));
        statusCell.setBorder(PdfPCell.NO_BORDER);
        statusCell.setPadding(5);
        table.addCell(statusCell);

        String value = count + " (" + String.format("%.1f", percentage) + "%)";
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }


    private String getBranchName(Long branchId) {
        Optional<Branch> branch=branchRepository.findById(branchId);
        String branch_name="Branch " + branchId;
        if(branch.isPresent()){
            branch_name= branch.get().getName();
        }
        return branch_name;
    }


}