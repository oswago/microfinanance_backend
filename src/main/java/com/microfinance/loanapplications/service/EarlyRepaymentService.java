package com.microfinance.loanapplications.service;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.microfinance.audit.service.AuditService;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.borrower.service.BorrowerService;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.BusinessException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.loanapplications.dto.earlyrepayment.*;
import com.microfinance.loanapplications.dto.earlyrepayment.EligibleLoanDto;
import com.microfinance.loanapplications.entity.*;
import com.microfinance.loanapplications.repository.*;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducts.repository.LoanProductRepository;
import com.microfinance.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;


import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@RequiredArgsConstructor
public class EarlyRepaymentService {

    private final EarlyRepaymentRepository earlyRepaymentRepository;
    private final LoanRepository loanRepository;
    private final LoanProductRepository loanProductRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final BorrowerRepository borrowerRepository;
    private final UserRepository userRepository;
    private final SystemService systemService;
    private final PdfGenerationService pdfGenerationService;
    private final BorrowerService borrowerService;

    private final SecurityUtils securityUtils;
    private final AuditService auditService;



    @Transactional
    public EarlyRepaymentRequestDto createEarlyRepaymentRequest(
            CreateEarlyRepaymentRequestDto requestDto, User currentUser) {

        log.info("Creating early repayment request for loan: {}", requestDto.getLoanId());

        // Validate loan
        Loan loan = loanRepository.findById(requestDto.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + requestDto.getLoanId()));

        // Check if loan is eligible for early repayment //uncomment later on after testing.
        validateLoanForEarlyRepayment(loan);

        // Check for existing pending request
        List<EarlyRepaymentRequest> existingRequests = earlyRepaymentRepository.findByLoanId(loan.getId());
        boolean hasPending = existingRequests.stream()
                .anyMatch(r -> r.getStatus() == GeneralConfig.EarlyRepaymentStatus.PENDING ||
                               r.getStatus() == GeneralConfig.EarlyRepaymentStatus.UNDER_REVIEW);
        if (hasPending) {
            throw new BusinessException("Loan already has a pending early repayment request");
        }

        // Calculate early repayment amounts
        EarlyRepaymentCalculationDto calculation = calculateEarlyRepayment(loan.getId(), null);


        // Generate Early RepaymentNumber
        String earlyRepaymentNumber = systemService.getNextNumber("EARLY_REPAYMENT");

        EarlyRepaymentRequest request=null;
        try {

        // Create request
        request = EarlyRepaymentRequest.builder()
                .requestNumber(earlyRepaymentNumber)
                .loan(loan)
                .borrower(loan.getBorrower())
                .outstandingPrincipal(calculation.getOutstandingPrincipal())
                .accruedInterest(calculation.getAccruedInterest())
                .penaltyCharges(calculation.getPenaltyCharges())
                .totalPayable(calculation.getTotalPayable())
                .discountPercentage(calculation.getDiscountPercentage())
                .discountAmount(calculation.getDiscountAmount())
                .earlyRepaymentAmount(calculation.getEarlyRepaymentAmount())
                .totalInterestIfNormal(calculation.getTotalInterestIfNormal())
                .totalInterestIfEarly(calculation.getTotalInterestIfEarly())
                .interestSavings(calculation.getInterestSavings())
                .interestSavingsPercentage(calculation.getInterestSavingsPercentage())
                .originalTenure(loan.getTenureMonths())
                .remainingTenure(calculateRemainingTenure(loan))
                .requestedDate(LocalDate.now())
                .requestedBy(currentUser)
                .reason(requestDto.getReason())
                .preferredPaymentMethod(GeneralConfig.PaymentMethod.valueOf(requestDto.getPreferredPaymentMethod()))
                .targetSettlementDate(requestDto.getTargetSettlementDate())
                .status(GeneralConfig.EarlyRepaymentStatus.PENDING)
                .build();

    } catch (Exception e) {
        log.error("ERROR BUILDING EARLY_REPAYMENT_REQUEST: {}", e.getMessage(), e);
        throw e;
    }

        // Validate borrower name is not null
        if (request.getBorrower() == null) {
            log.error("Borrower is null for loan: {}", loan.getId());
            throw new BusinessException("Loan has no associated borrower");
        }

        EarlyRepaymentRequest savedRequest = earlyRepaymentRepository.save(request);

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedRequest.getId())) {
            auditService.masterAuditLogs(
                    savedRequest.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.EARLY_REPAYMENT_REQUEST_ACTIVITY,
                    "EARLY_REPAYMENT_REQUEST",
                    "Early Repayment Request of ID: "+savedRequest.getId()+" Loan No:"+savedRequest.getLoan().getLoanAccountNumber()+ " has been Created by:"+createdByName+"-"+createdById

            );
        }
        //End Audit Section


        log.info("Saved early repayment request with ID: {}, number: {}", savedRequest.getId(), savedRequest.getRequestNumber());
        // Create history entry
        createHistoryEntry(savedRequest, "CREATED", currentUser, "Early repayment request created");

        return mapToDto(savedRequest);
    }

    @Transactional(readOnly = true)
    public EarlyRepaymentCalculationDto calculateEarlyRepayment(Long loanId, BigDecimal customDiscountPercentage) {
        log.info("Calculating early repayment for loan: {} with discount: {}", loanId, customDiscountPercentage);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        // Get all installments
        List<RepaymentSchedule> installments = repaymentScheduleRepository
                .findByLoanIdOrderByInstallmentNumberAsc(loanId);

        // Calculate outstanding principal (sum of remaining principal)
        BigDecimal outstandingPrincipal = installments.stream()
                .filter(i -> i.getStatus() != GeneralConfig.InstallmentStatus.PAID)
                .map(RepaymentSchedule::getPrincipalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate accrued interest (interest on current installment only)
        BigDecimal accruedInterest = installments.stream()
                .filter(i -> i.getStatus() == GeneralConfig.InstallmentStatus.PENDING ||
                        i.getStatus() == GeneralConfig.InstallmentStatus.PARTIAL)
                .findFirst()
                .map(RepaymentSchedule::getInterestDue)
                .orElse(BigDecimal.ZERO);

        // Calculate penalty charges (if any)
        BigDecimal penaltyCharges = calculatePenaltyCharges(loan, installments);

        // Calculate total payable before discount
        BigDecimal totalPayable = outstandingPrincipal.add(accruedInterest).add(penaltyCharges);

        // Calculate discount
        BigDecimal discountPercentage = customDiscountPercentage != null ?
                customDiscountPercentage : getDefaultDiscountPercentage(loan, installments);

        BigDecimal discountAmount = totalPayable
                .multiply(discountPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal earlyRepaymentAmount = totalPayable.subtract(discountAmount);

        // Calculate total interest if paid normally (ALL pending interest)
        BigDecimal totalInterestIfNormal = installments.stream()
                .filter(i -> i.getStatus() != GeneralConfig.InstallmentStatus.PAID)
                .map(RepaymentSchedule::getInterestDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total interest if paid early (only current month's interest)
        BigDecimal totalInterestIfEarly = installments.stream()
                .filter(i -> i.getStatus() == GeneralConfig.InstallmentStatus.PENDING ||
                        i.getStatus() == GeneralConfig.InstallmentStatus.PARTIAL)
                .findFirst()
                .map(RepaymentSchedule::getInterestDue)
                .orElse(BigDecimal.ZERO);

        // Calculate interest savings
        BigDecimal interestSavings = totalInterestIfNormal.subtract(totalInterestIfEarly).max(BigDecimal.ZERO);

        BigDecimal interestSavingsPercentage = totalInterestIfNormal.compareTo(BigDecimal.ZERO) > 0 ?
                interestSavings.multiply(BigDecimal.valueOf(100))
                        .divide(totalInterestIfNormal, 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Generate options
        List<EarlyRepaymentOptionDto> options = generateEarlyRepaymentOptions(
                loan, installments, totalPayable, totalInterestIfNormal, totalInterestIfEarly);

        return EarlyRepaymentCalculationDto.builder()
                .loanId(loanId)
                .outstandingPrincipal(outstandingPrincipal)
                .accruedInterest(accruedInterest)
                .penaltyCharges(penaltyCharges)
                .totalPayable(totalPayable)
                .discountPercentage(discountPercentage)
                .discountAmount(discountAmount)
                .earlyRepaymentAmount(earlyRepaymentAmount)
                .totalInterestIfNormal(totalInterestIfNormal)  // Added
                .totalInterestIfEarly(totalInterestIfEarly)    // Added
                .interestSavings(interestSavings)
                .interestSavingsPercentage(interestSavingsPercentage)
                .options(options)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<EarlyRepaymentRequestDto> getEarlyRepaymentRequests(
            GeneralConfig.EarlyRepaymentStatus status, Long branchId, Long loanProductId,
            String search, Pageable pageable) {

        Page<EarlyRepaymentRequest> requests = earlyRepaymentRepository
                .findEarlyRepaymentRequests(status, branchId, loanProductId, search, pageable);

        return requests.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public EarlyRepaymentRequestDto getEarlyRepaymentRequestById(Long id) {
        EarlyRepaymentRequest request = earlyRepaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Early repayment request not found with id: " + id));
        return mapToDto(request);
    }

    @Transactional
    public EarlyRepaymentRequestDto approveEarlyRepayment(Long id, ApproveEarlyRepaymentDto approveDto, User approver) {
        log.info("Approving early repayment request: {}", id);

        EarlyRepaymentRequest request = earlyRepaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Early repayment request not found with id: " + id));

        if (request.getStatus() != GeneralConfig.EarlyRepaymentStatus.PENDING &&
            request.getStatus() != GeneralConfig.EarlyRepaymentStatus.UNDER_REVIEW) {
            throw new BusinessException("Request cannot be approved in current status: " + request.getStatus());
        }

        // Recalculate if custom discount provided
        if (approveDto.getCustomDiscountPercentage() != null) {
            EarlyRepaymentCalculationDto recalc = calculateEarlyRepayment(
                    request.getLoan().getId(), approveDto.getCustomDiscountPercentage());
            request.setDiscountPercentage(recalc.getDiscountPercentage());
            request.setDiscountAmount(recalc.getDiscountAmount());
            request.setEarlyRepaymentAmount(recalc.getEarlyRepaymentAmount());
            request.setInterestSavings(recalc.getInterestSavings());
            request.setInterestSavingsPercentage(recalc.getInterestSavingsPercentage());
        }

        request.setStatus(GeneralConfig.EarlyRepaymentStatus.APPROVED);
        request.setApprovedBy(approver);
        request.setApprovalDate(LocalDate.now());
        request.setApprovalComments(approveDto.getComments());

        EarlyRepaymentRequest savedRequest = earlyRepaymentRepository.save(request);

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedRequest.getId())) {
            auditService.masterAuditLogs(
                    savedRequest.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.EARLY_REPAYMENT_REQUEST_ACTIVITY,
                    "EARLY_REPAYMENT_REQUEST",
                    "Early Repayment Request of ID:"+savedRequest.getId()+" Loan No:"+savedRequest.getLoan().getLoanAccountNumber()+  " has been APPROVED by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        // Create history entry
        createHistoryEntry(savedRequest, "APPROVED", approver, approveDto.getComments());

        return mapToDto(savedRequest);
    }

    @Transactional
    public EarlyRepaymentRequestDto rejectEarlyRepayment(Long id, RejectEarlyRepaymentDto rejectDto, User rejector) {
        log.info("Rejecting early repayment request: {}", id);

        EarlyRepaymentRequest request = earlyRepaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Early repayment request not found with id: " + id));

        if (request.getStatus() != GeneralConfig.EarlyRepaymentStatus.PENDING &&
            request.getStatus() != GeneralConfig.EarlyRepaymentStatus.UNDER_REVIEW) {
            throw new BusinessException("Request cannot be rejected in current status: " + request.getStatus());
        }

        request.setStatus(GeneralConfig.EarlyRepaymentStatus.REJECTED);
        request.setRejectedBy(rejector);
        request.setRejectionDate(LocalDate.now());
        request.setRejectionReason(rejectDto.getReason());

        EarlyRepaymentRequest savedRequest = earlyRepaymentRepository.save(request);



        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedRequest.getId())) {
            auditService.masterAuditLogs(
                    savedRequest.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.EARLY_REPAYMENT_REQUEST_ACTIVITY,
                    "EARLY_REPAYMENT_REQUEST",
                    "Early Repayment Request of ID:"+savedRequest.getId()+" Loan No:"+savedRequest.getLoan().getLoanAccountNumber()+  " has been REJECTED by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        // Create history entry
        createHistoryEntry(savedRequest, "REJECTED", rejector, rejectDto.getReason());

        return mapToDto(savedRequest);
    }

    @Transactional
    public EarlyRepaymentRequestDto processEarlyRepaymentPayment(Long id, EarlyRepaymentPaymentDto paymentDto, User processor) {
        log.info("Processing early repayment payment for request: {}", id);

        EarlyRepaymentRequest request = earlyRepaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Early repayment request not found with id: " + id));

        if (request.getStatus() != GeneralConfig.EarlyRepaymentStatus.APPROVED) {
            throw new BusinessException("Only approved requests can be processed for payment");
        }

        // Create loan repayment record
        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoan(request.getLoan());
        repayment.setBorrower(request.getBorrower());
        repayment.setAmountPaid(request.getEarlyRepaymentAmount());
        repayment.setPaymentDate(paymentDto.getPaymentDate());
        repayment.setPaymentMethod(GeneralConfig.PaymentMethod.valueOf(paymentDto.getPaymentMethod()));
        repayment.setReceivedBy(processor);
        repayment.setStatus(GeneralConfig.RepaymentStatus.COMPLETED);
        repayment.setNotes("Early repayment - " + request.getRequestNumber());
        repayment.generateReceiptNumber();

        loanRepaymentRepository.save(repayment);

        // Mark all remaining installments as paid
        markRemainingInstallmentsAsPaid(request.getLoan(), repayment, processor);

        // Update loan status to CLOSED
        Loan loan = request.getLoan();
        loan.setStatus(GeneralConfig.LoanStatus.CLOSED);
        loan.setClosedDate(LocalDate.now());
        loan.setClosedBy(processor);
        loanRepository.save(loan);

        // Update request
        request.setStatus(GeneralConfig.EarlyRepaymentStatus.PAID);
        request.setSettlementDate(paymentDto.getPaymentDate());
        request.setSettlementReference(paymentDto.getReference());

        // Generate settlement letter
        generateSettlementLetter(request);

        EarlyRepaymentRequest savedRequest = earlyRepaymentRepository.save(request);


        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedRequest.getId())) {
            auditService.masterAuditLogs(
                    savedRequest.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.EARLY_REPAYMENT_REQUEST_ACTIVITY,
                    "EARLY_REPAYMENT_REQUEST",
                    "Early Repayment Request of ID:"+savedRequest.getId()+ " Loan No:"+savedRequest.getLoan().getLoanAccountNumber()+  " has been PROCESSED by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        // Create history entry
        createHistoryEntry(savedRequest, "PAID", processor, "Early repayment payment processed. Ref: " + paymentDto.getReference());

        return mapToDto(savedRequest);
    }

    @Transactional(readOnly = true)
    public EarlyRepaymentStatisticsDto getEarlyRepaymentStatistics() {
        log.info("Fetching early repayment statistics");

        return EarlyRepaymentStatisticsDto.builder()
                .totalEarlyRepayments(earlyRepaymentRepository.getTotalEarlyRepayments())
                .totalInterestSaved(earlyRepaymentRepository.getTotalInterestSaved())
                .activeRequests(earlyRepaymentRepository.getActiveRequests())
                .averageDiscount(earlyRepaymentRepository.getAverageDiscount())
                .approvedCount(earlyRepaymentRepository.countByStatus(GeneralConfig.EarlyRepaymentStatus.APPROVED))
                .rejectedCount(earlyRepaymentRepository.countByStatus(GeneralConfig.EarlyRepaymentStatus.REJECTED))
                .pendingCount(earlyRepaymentRepository.countByStatus(GeneralConfig.EarlyRepaymentStatus.PENDING))
                .paidCount(earlyRepaymentRepository.countByStatus(GeneralConfig.EarlyRepaymentStatus.PAID))
                .averageProcessingTime(calculateAverageProcessingTime())
                .trends(getMonthlyTrends())
                .build();
    }

    @Transactional
    public byte[] generateSettlementLetter(Long requestId) {
        log.info("Generating settlement letter for request: {}", requestId);

        EarlyRepaymentRequest request = earlyRepaymentRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Early repayment request not found with id: " + requestId));

        return generateSettlementLetter(request);
    }

    // Private helper methods

    private void validateLoanForEarlyRepayment(Loan loan) {
        if (loan.getStatus() != GeneralConfig.LoanStatus.ACTIVE &&
            loan.getStatus() != GeneralConfig.LoanStatus.DELINQUENT) {
            throw new BusinessException("Loan is not eligible for early repayment. Current status: " + loan.getStatus());
        }

        // Check minimum period requirement (e.g., 3 months)
        if (loan.getDisbursementDate() != null) {
            long monthsActive = ChronoUnit.MONTHS.between(loan.getDisbursementDate(), LocalDate.now());
            if (monthsActive < 3) {
                throw new BusinessException("Loan must be active for at least 3 months before early repayment");
            }
        }
    }

    private Integer calculateRemainingTenure(Loan loan) {
        List<RepaymentSchedule> pendingInstallments = repaymentScheduleRepository
                .findByLoanIdAndStatus(loan.getId(), GeneralConfig.InstallmentStatus.PENDING);
        return pendingInstallments.size();
    }

    private BigDecimal calculatePenaltyCharges(Loan loan, List<RepaymentSchedule> installments) {
        // Check if loan has early repayment penalty clause
        if (loan.getLoanProduct() != null && loan.getLoanProduct().getEarlyRepaymentFeeRate() != null) {
            BigDecimal outstandingPrincipal = installments.stream()
                    .filter(i -> i.getStatus() != GeneralConfig.InstallmentStatus.PAID)
                    .map(RepaymentSchedule::getPrincipalDue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return outstandingPrincipal
                    .multiply(loan.getLoanProduct().getEarlyRepaymentFeeRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .max(BigDecimal.valueOf(100)); // Minimum fee
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getDefaultDiscountPercentage(Loan loan, List<RepaymentSchedule> installments) {
        // Calculate discount based on remaining tenure
        long paidInstallments = installments.stream()
                .filter(i -> i.getStatus() == GeneralConfig.InstallmentStatus.PAID)
                .count();

        long totalInstallments = installments.size();
        long remainingInstallments = totalInstallments - paidInstallments;

        if (remainingInstallments >= 12) return BigDecimal.valueOf(5.0);
        if (remainingInstallments >= 6) return BigDecimal.valueOf(3.0);
        if (remainingInstallments >= 3) return BigDecimal.valueOf(1.5);
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateTotalInterestIfNormal(Loan loan, List<RepaymentSchedule> installments) {
        return installments.stream()
                .filter(i -> i.getStatus() != GeneralConfig.InstallmentStatus.PAID)
                .map(RepaymentSchedule::getInterestDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalInterestIfEarly(Loan loan, List<RepaymentSchedule> installments,
                                                     BigDecimal earlyRepaymentAmount) {
        // Simplified calculation - actual interest is just the accrued interest
        return installments.stream()
                .filter(i -> i.getStatus() == GeneralConfig.InstallmentStatus.PENDING ||
                           i.getStatus() == GeneralConfig.InstallmentStatus.PARTIAL)
                .findFirst()
                .map(RepaymentSchedule::getInterestDue)
                .orElse(BigDecimal.ZERO);
    }


    private BigDecimal calculateInterestSavings(Loan loan, List<RepaymentSchedule> installments,
                                                BigDecimal discountPercentage) {
        BigDecimal totalInterest = calculateTotalInterestIfNormal(loan, installments);
        BigDecimal interestAfterDiscount = totalInterest
                .multiply(BigDecimal.valueOf(100).subtract(discountPercentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return totalInterest.subtract(interestAfterDiscount);
    }

    private void markRemainingInstallmentsAsPaid(Loan loan, LoanRepayment repayment, User processor) {
        List<RepaymentSchedule> pendingInstallments = repaymentScheduleRepository
                .findByLoanIdAndStatus(loan.getId(), GeneralConfig.InstallmentStatus.PENDING);

        for (RepaymentSchedule installment : pendingInstallments) {
            installment.setStatus(GeneralConfig.InstallmentStatus.PAID);
            installment.setPaidDate(repayment.getPaymentDate());
            installment.setPaidAmount(installment.getTotalDue());
            repaymentScheduleRepository.save(installment);
        }
    }

    private byte[] generateSettlementLetter(EarlyRepaymentRequest request) {
        // Implementation depends on your PDF generation logic
        // Use the existing PdfGenerationService
        return pdfGenerationService.generateSettlementLetter(request);
    }

    private void createHistoryEntry(EarlyRepaymentRequest request, String action,
                                    User performedBy, String comments) {
        // You can implement a history entity if needed
        // For now, just log it
        log.info("Early repayment request {}: {} by {} - {}",
                request.getRequestNumber(), action, performedBy.getUsername(), comments);
    }

    private BigDecimal calculateAverageProcessingTime() {
        // Implement based on your data
        return BigDecimal.valueOf(2.5); // Placeholder
    }

    private List<EarlyRepaymentTrendDto> getMonthlyTrends() {
        List<Object[]> trends = earlyRepaymentRepository.getMonthlyTrends();
        return trends.stream()
                .map(t -> EarlyRepaymentTrendDto.builder()
                        .period((String) t[0])
                        .count(((Number) t[1]).intValue())
                        .amount((BigDecimal) t[2])
                        .interestSaved((BigDecimal) t[3])
                        .build())
                .collect(Collectors.toList());
    }

    private EarlyRepaymentRequestDto mapToDto(EarlyRepaymentRequest request) {
        if (request == null) return null;

        log.debug("Requested by: {}", request.getRequestedBy() != null ?
                request.getRequestedBy().getUsername() : "null");

        EarlyRepaymentRequestDto.EarlyRepaymentRequestDtoBuilder builder = EarlyRepaymentRequestDto.builder()
                .id(request.getId())
                .requestNumber(request.getRequestNumber())
                .loanId(request.getLoan().getId())
                .loanNumber(request.getLoan().getLoanAccountNumber())
                .borrowerId(request.getBorrower().getId())
                .borrowerName(request.getBorrower().getFirstName() + " " +
                              (request.getBorrower().getLastName() != null ? request.getBorrower().getLastName() : ""))
                .borrowerIdNumber(request.getBorrower().getBorrowerNumber())
                .originalLoanAmount(request.getLoan().getPrincipalAmount())
                .disbursementDate(request.getLoan().getDisbursementDate())
                .originalTenure(request.getOriginalTenure())
                .remainingTenure(request.getRemainingTenure())
                .outstandingPrincipal(request.getOutstandingPrincipal())
                .accruedInterest(request.getAccruedInterest())
                .penaltyCharges(request.getPenaltyCharges())
                .totalPayable(request.getTotalPayable())
                .discountPercentage(request.getDiscountPercentage())
                .discountAmount(request.getDiscountAmount())
                .earlyRepaymentAmount(request.getEarlyRepaymentAmount())
                .totalInterestIfNormal(request.getTotalInterestIfNormal())
                .totalInterestIfEarly(request.getTotalInterestIfEarly())
                .interestSavings(request.getInterestSavings())
                .interestSavingsPercentage(request.getInterestSavingsPercentage())
                .requestedDate(request.getRequestedDate())
                .reason(request.getReason())
                .preferredPaymentMethod(request.getPreferredPaymentMethod() != null ?
                        request.getPreferredPaymentMethod().name() : null)
                .targetSettlementDate(request.getTargetSettlementDate())
                .status(request.getStatus() != null ? request.getStatus().name() : null);

        if (request.getLoan() != null && request.getLoan().getLoanProduct() != null) {
            builder.loanProductId(request.getLoan().getLoanProduct().getId())
                   .loanProductName(request.getLoan().getLoanProduct().getName());
        }

        if (request.getLoan() != null && request.getLoan().getBranch() != null) {
            builder.branchId(request.getLoan().getBranch().getId())
                   .branchName(request.getLoan().getBranch().getName());
        }

        if (request.getRequestedBy() != null) {
            builder.requestedBy(request.getRequestedBy().getUsername());
        }

        if (request.getApprovedBy() != null) {
            builder.approvedBy(request.getApprovedBy().getUsername())
                   .approvalDate(request.getApprovalDate())
                   .approvalComments(request.getApprovalComments());
        }

        if (request.getRejectedBy() != null) {
            builder.rejectedBy(request.getRejectedBy().getUsername())
                   .rejectionDate(request.getRejectionDate())
                   .rejectionReason(request.getRejectionReason());
        }

        return builder.build();
    }

    /**
     * Get eligible loans for early repayment
     * Returns loans that are active and eligible for early repayment
     */
    @Transactional(readOnly = true)
    public List<EligibleLoanDto> getEligibleLoans(User currentUser) {
        log.info("Fetching eligible loans for early repayment for user: {}", currentUser.getUsername());

        // Determine branch filter based on user permissions
        Long branchId = null;
       /* if (@permissionCheckService.hasPermission(currentUser, "LOAN_VIEW_ALL")) {
            if (@permissionCheckService.hasPermission(currentUser, "LOAN_VIEW_BRANCH")) {
                branchId = currentUser.getBranchId() != null ? currentUser.getBranchId() : null;
            } else if (@permissionCheckService.hasPermission(currentUser, "LOAN_VIEW_OWN")) {
                // For own loans, we'll filter by loan officer ID
                // This would need a different query
            }
        }*/

        branchId = currentUser.getBranchId() != null ? currentUser.getBranchId() : null;

        // Get eligible loans from repository
        List<Loan> eligibleLoans = loanRepository.findEligibleForEarlyRepayment(branchId);

        // Convert to DTOs
        return eligibleLoans.stream()
                .map(this::mapToEligibleLoanDto)
                .collect(Collectors.toList());
    }

    /**
     * Map Loan entity to EligibleLoanDto
     */
    private EligibleLoanDto mapToEligibleLoanDto(Loan loan) {
        if (loan == null) return null;

        // Calculate remaining tenure
        Integer remainingTenure = calculateRemainingTenure(loan);

        // Calculate monthly payment (simplified)
        BigDecimal monthlyPayment = calculateMonthlyPayment(loan);

        // Calculate early repayment fee based on loan product
        BigDecimal earlyRepaymentFee = calculateEarlyRepaymentFee(loan);

        return EligibleLoanDto.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanAccountNumber())
                .borrowerName(loan.getBorrower() != null ?
                        loan.getBorrower().getFirstName() + " " +
                                (loan.getBorrower().getLastName() != null ? loan.getBorrower().getLastName() : "") : null)
                .borrowerId(loan.getBorrower() != null ? loan.getBorrower().getId() : null)
                .borrowerIdNumber(loan.getBorrower() != null ? loan.getBorrower().getBorrowerNumber() : null)
                .loanProductId(loan.getLoanProduct() != null ? loan.getLoanProduct().getId() : null)
                .loanProductName(loan.getLoanProduct() != null ? loan.getLoanProduct().getName() : null)
                .outstandingBalance(loan.getOutstandingBalance())
                .remainingTenure(remainingTenure)
                .monthlyPayment(monthlyPayment)
                .totalInterestDue(loan.getTotalDue().subtract(loan.getPrincipalAmount()))
                .earlyRepaymentFee(earlyRepaymentFee)
                .disbursementDate(loan.getDisbursementDate())
                .principalAmount(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .displayName(loan.getLoanAccountNumber() + " - " +
                        (loan.getBorrower() != null ? loan.getBorrower().getFullName() : "Unknown") +
                        " (" + formatCurrency(loan.getOutstandingBalance()) + ")")
                .build();
    }

    /**
     * Calculate monthly payment (simplified - based on first pending installment)
     */
    private BigDecimal calculateMonthlyPayment(Loan loan) {
        List<RepaymentSchedule> installments = repaymentScheduleRepository
                .findByLoanIdOrderByInstallmentNumberAsc(loan.getId());

        if (!installments.isEmpty()) {
            // Get the first unpaid installment
            RepaymentSchedule firstUnpaid = installments.stream()
                    .filter(i -> i.getStatus() != GeneralConfig.InstallmentStatus.PAID)
                    .findFirst()
                    .orElse(null);

            if (firstUnpaid != null) {
                return firstUnpaid.getTotalDue();
            }
        }

        // Fallback: calculate based on loan parameters
        if (loan.getTenureMonths() > 0) {
            return loan.getTotalDue().divide(BigDecimal.valueOf(loan.getTenureMonths()), 2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Calculate early repayment fee based on loan product
     */
    private BigDecimal calculateEarlyRepaymentFee(Loan loan) {
        if (loan.getLoanProduct() != null && loan.getLoanProduct().getEarlyRepaymentFeeRate() != null) {
            return loan.getOutstandingBalance()
                    .multiply(loan.getLoanProduct().getEarlyRepaymentFeeRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .max(BigDecimal.valueOf(100)); // Minimum fee
        }
        return BigDecimal.ZERO;
    }

    /**
     * Helper method to format currency for display name
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toString();
    }




    // ==================== MISSING SERVICE METHODS ====================

    /**
     * Validate if a loan is eligible for early repayment
     */
    @Transactional(readOnly = true)
    public EligibilityResponseDto validateLoanEligibility(Long loanId) {
        log.info("Validating loan eligibility for early repayment: {}", loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        // Check if loan exists and is active
        if (loan == null) {
            return EligibilityResponseDto.builder()
                    .eligible(false)
                    .message("Loan not found")
                    .reason("LOAN_NOT_FOUND")
                    .build();
        }

        // Check if loan is active or delinquent
        if (loan.getStatus() != GeneralConfig.LoanStatus.ACTIVE &&
                loan.getStatus() != GeneralConfig.LoanStatus.DELINQUENT) {
            return EligibilityResponseDto.builder()
                    .eligible(false)
                    .message("Loan is not active. Current status: " + loan.getStatus())
                    .reason("INACTIVE_LOAN")
                    .build();
        }

        // Check minimum period requirement (e.g., 3 months)
        if (loan.getDisbursementDate() != null) {
            long monthsActive = ChronoUnit.MONTHS.between(loan.getDisbursementDate(), LocalDate.now());
            if (monthsActive < 3) {
                return EligibilityResponseDto.builder()
                        .eligible(false)
                        .message("Loan must be active for at least 3 months before early repayment")
                        .reason("MINIMUM_PERIOD_NOT_MET")
                        .build();
            }
        }

        // Check if there are any pending installments
        List<RepaymentSchedule> pendingInstallments = repaymentScheduleRepository
                .findByLoanIdAndStatus(loan.getId(), GeneralConfig.InstallmentStatus.PENDING);

        if (pendingInstallments.isEmpty()) {
            return EligibilityResponseDto.builder()
                    .eligible(false)
                    .message("No pending installments found. Loan may already be fully paid.")
                    .reason("NO_PENDING_INSTALLMENTS")
                    .build();
        }

        // Check for existing pending early repayment requests
        List<EarlyRepaymentRequest> existingRequests = earlyRepaymentRepository.findByLoanId(loan.getId());
        boolean hasPending = existingRequests.stream()
                .anyMatch(r -> r.getStatus() == GeneralConfig.EarlyRepaymentStatus.PENDING ||
                        r.getStatus() == GeneralConfig.EarlyRepaymentStatus.UNDER_REVIEW ||
                        r.getStatus() == GeneralConfig.EarlyRepaymentStatus.APPROVED);

        if (hasPending) {
            return EligibilityResponseDto.builder()
                    .eligible(false)
                    .message("Loan already has a pending or approved early repayment request")
                    .reason("PENDING_REQUEST_EXISTS")
                    .build();
        }

        return EligibilityResponseDto.builder()
                .eligible(true)
                .message("Loan is eligible for early repayment")
                .reason("ELIGIBLE")
                .build();
    }

    /**
     * Calculate early repayment with optional amount and custom discount
     */
    @Transactional(readOnly = true)
    public EarlyRepaymentCalculationDto calculateEarlyRepayment(Long loanId, BigDecimal amount, BigDecimal customDiscount) {
        log.info("Calculating early repayment for loan: {} with amount: {}, discount: {}", loanId, amount, customDiscount);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        // Get all installments
        List<RepaymentSchedule> installments = repaymentScheduleRepository
                .findByLoanIdOrderByInstallmentNumberAsc(loanId);

        // Calculate outstanding principal (sum of remaining principal)
        BigDecimal outstandingPrincipal = installments.stream()
                .filter(i -> i.getStatus() != GeneralConfig.InstallmentStatus.PAID)
                .map(RepaymentSchedule::getPrincipalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate accrued interest (interest on current installment only)
        BigDecimal accruedInterest = installments.stream()
                .filter(i -> i.getStatus() == GeneralConfig.InstallmentStatus.PENDING ||
                        i.getStatus() == GeneralConfig.InstallmentStatus.PARTIAL)
                .findFirst()
                .map(RepaymentSchedule::getInterestDue)
                .orElse(BigDecimal.ZERO);

        // Calculate penalty charges (if any)
        BigDecimal penaltyCharges = calculatePenaltyCharges(loan, installments);

        // Calculate total payable before discount
        BigDecimal totalPayable = outstandingPrincipal.add(accruedInterest).add(penaltyCharges);

        // Calculate discount
        BigDecimal discountPercentage = customDiscount != null ?
                customDiscount : getDefaultDiscountPercentage(loan, installments);

        BigDecimal discountAmount = totalPayable
                .multiply(discountPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal earlyRepaymentAmount = totalPayable.subtract(discountAmount);

        // If a specific amount is provided, use it (but validate it's within limits)
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            if (amount.compareTo(totalPayable) > 0) {
                throw new BusinessException("Repayment amount cannot exceed total payable: " + totalPayable);
            }
            earlyRepaymentAmount = amount;
            // Recalculate discount based on the amount
            discountAmount = totalPayable.subtract(amount);
            discountPercentage = totalPayable.compareTo(BigDecimal.ZERO) > 0 ?
                    discountAmount.multiply(BigDecimal.valueOf(100))
                            .divide(totalPayable, 2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;
        }

        // Calculate total interest if paid normally (ALL pending interest)
        BigDecimal totalInterestIfNormal = installments.stream()
                .filter(i -> i.getStatus() != GeneralConfig.InstallmentStatus.PAID)
                .map(RepaymentSchedule::getInterestDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total interest if paid early (only current month's interest)
        BigDecimal totalInterestIfEarly = installments.stream()
                .filter(i -> i.getStatus() == GeneralConfig.InstallmentStatus.PENDING ||
                        i.getStatus() == GeneralConfig.InstallmentStatus.PARTIAL)
                .findFirst()
                .map(RepaymentSchedule::getInterestDue)
                .orElse(BigDecimal.ZERO);

        // Calculate interest savings
        BigDecimal interestSavings = totalInterestIfNormal.subtract(totalInterestIfEarly).max(BigDecimal.ZERO);

        BigDecimal interestSavingsPercentage = totalInterestIfNormal.compareTo(BigDecimal.ZERO) > 0 ?
                interestSavings.multiply(BigDecimal.valueOf(100))
                        .divide(totalInterestIfNormal, 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Generate options
        List<EarlyRepaymentOptionDto> options = generateEarlyRepaymentOptions(
                loan, installments, totalPayable, totalInterestIfNormal, totalInterestIfEarly);

        return EarlyRepaymentCalculationDto.builder()
                .loanId(loanId)
                .outstandingPrincipal(outstandingPrincipal)
                .accruedInterest(accruedInterest)
                .penaltyCharges(penaltyCharges)
                .totalPayable(totalPayable)
                .discountPercentage(discountPercentage)
                .discountAmount(discountAmount)
                .earlyRepaymentAmount(earlyRepaymentAmount)
                .totalInterestIfNormal(totalInterestIfNormal)  // Now guaranteed not null
                .totalInterestIfEarly(totalInterestIfEarly)    // Now guaranteed not null
                .interestSavings(interestSavings)
                .interestSavingsPercentage(interestSavingsPercentage)
                .options(options)
                .build();
    }

    /**
     * Get early repayment options for a loan
     */
    @Transactional(readOnly = true)
    public List<EarlyRepaymentOptionDto> getEarlyRepaymentOptions(Long loanId) {
        log.info("Fetching early repayment options for loan: {}", loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        List<RepaymentSchedule> installments = repaymentScheduleRepository
                .findByLoanIdOrderByInstallmentNumberAsc(loanId);

        // Calculate base values
        BigDecimal outstandingPrincipal = installments.stream()
                .filter(i -> i.getStatus() != GeneralConfig.InstallmentStatus.PAID)
                .map(RepaymentSchedule::getPrincipalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal accruedInterest = installments.stream()
                .filter(i -> i.getStatus() == GeneralConfig.InstallmentStatus.PENDING ||
                        i.getStatus() == GeneralConfig.InstallmentStatus.PARTIAL)
                .findFirst()
                .map(RepaymentSchedule::getInterestDue)
                .orElse(BigDecimal.ZERO);

        BigDecimal penaltyCharges = calculatePenaltyCharges(loan, installments);
        BigDecimal totalPayable = outstandingPrincipal.add(accruedInterest).add(penaltyCharges);

        BigDecimal totalInterestIfNormal = calculateTotalInterestIfNormal(loan, installments);
        BigDecimal totalInterestIfEarly = calculateTotalInterestIfEarly(loan, installments);

        return generateEarlyRepaymentOptions(loan, installments, totalPayable, totalInterestIfNormal, totalInterestIfEarly);
    }

    /**
     * Get early repayment fee structure for a loan product
     */
    @Transactional(readOnly = true)
    public EarlyRepaymentFeeStructureDto getEarlyRepaymentFeeStructure(Long loanProductId) {
        log.info("Fetching early repayment fee structure for loan product: {}", loanProductId);

        LoanProduct loanProduct = loanProductRepository.findById(loanProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found with id: " + loanProductId));

        BigDecimal feeRate = loanProduct.getEarlyRepaymentFeeRate() != null ?
                loanProduct.getEarlyRepaymentFeeRate() : BigDecimal.ZERO;

        BigDecimal minimumFee = BigDecimal.valueOf(100); // Default minimum fee

        return EarlyRepaymentFeeStructureDto.builder()
                .loanProductId(loanProductId)
                .productName(loanProduct.getName())
                .feeRate(feeRate)
                .feeAmount(BigDecimal.ZERO) // Will be calculated based on outstanding balance
                .minimumFee(minimumFee)
                .feeType("PERCENTAGE")
                .calculationBasis("OUTSTANDING_PRINCIPAL")
                .description("Early repayment fee is " + feeRate + "% of outstanding principal (min " + minimumFee + ")")
                .build();
    }

    /**
     * Generate early repayment options
     */
    private List<EarlyRepaymentOptionDto> generateEarlyRepaymentOptions(Loan loan,
                                                                        List<RepaymentSchedule> installments,
                                                                        BigDecimal totalPayable,
                                                                        BigDecimal totalInterestIfNormal,
                                                                        BigDecimal totalInterestIfEarly) {
        List<EarlyRepaymentOptionDto> options = new ArrayList<>();

        // Standard option (default discount)
        BigDecimal standardDiscount = getDefaultDiscountPercentage(loan, installments);
        BigDecimal standardDiscountAmount = totalPayable
                .multiply(standardDiscount)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal standardInterestSavings = calculateInterestSavings(totalInterestIfNormal, standardDiscount);
        BigDecimal standardInterestSavingsPercentage = totalInterestIfNormal.compareTo(BigDecimal.ZERO) > 0 ?
                standardInterestSavings.multiply(BigDecimal.valueOf(100))
                        .divide(totalInterestIfNormal, 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        options.add(EarlyRepaymentOptionDto.builder()
                .optionType("STANDARD")
                .discountPercentage(standardDiscount)
                .earlyRepaymentAmount(totalPayable.subtract(standardDiscountAmount))
                .interestSavings(standardInterestSavings)
                .totalInterestIfNormal(totalInterestIfNormal)
                .totalInterestIfEarly(totalInterestIfEarly)
                .interestSavingsPercentage(standardInterestSavingsPercentage)
                .description("Standard early repayment with " + standardDiscount + "% discount")
                .build());

        // Premium option (higher discount)
        BigDecimal premiumDiscount = standardDiscount.add(BigDecimal.valueOf(2.0)).min(BigDecimal.valueOf(15.0));
        BigDecimal premiumDiscountAmount = totalPayable
                .multiply(premiumDiscount)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal premiumInterestSavings = calculateInterestSavings(totalInterestIfNormal, premiumDiscount);
        BigDecimal premiumInterestSavingsPercentage = totalInterestIfNormal.compareTo(BigDecimal.ZERO) > 0 ?
                premiumInterestSavings.multiply(BigDecimal.valueOf(100))
                        .divide(totalInterestIfNormal, 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        options.add(EarlyRepaymentOptionDto.builder()
                .optionType("PREMIUM")
                .discountPercentage(premiumDiscount)
                .earlyRepaymentAmount(totalPayable.subtract(premiumDiscountAmount))
                .interestSavings(premiumInterestSavings)
                .totalInterestIfNormal(totalInterestIfNormal)
                .totalInterestIfEarly(totalInterestIfEarly)
                .interestSavingsPercentage(premiumInterestSavingsPercentage)
                .description("Premium option with higher " + premiumDiscount + "% discount")
                .build());

        // Quick settlement option (higher discount for immediate payment)
        BigDecimal quickDiscount = standardDiscount.add(BigDecimal.valueOf(3.0)).min(BigDecimal.valueOf(20.0));
        BigDecimal quickDiscountAmount = totalPayable
                .multiply(quickDiscount)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal quickInterestSavings = calculateInterestSavings(totalInterestIfNormal, quickDiscount);
        BigDecimal quickInterestSavingsPercentage = totalInterestIfNormal.compareTo(BigDecimal.ZERO) > 0 ?
                quickInterestSavings.multiply(BigDecimal.valueOf(100))
                        .divide(totalInterestIfNormal, 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        options.add(EarlyRepaymentOptionDto.builder()
                .optionType("QUICK_SETTLEMENT")
                .discountPercentage(quickDiscount)
                .earlyRepaymentAmount(totalPayable.subtract(quickDiscountAmount))
                .interestSavings(quickInterestSavings)
                .totalInterestIfNormal(totalInterestIfNormal)
                .totalInterestIfEarly(totalInterestIfEarly)
                .interestSavingsPercentage(quickInterestSavingsPercentage)
                .description("Quick settlement with " + quickDiscount + "% discount (valid for 7 days)")
                .build());

        return options;
    }

    /**
     * Calculate interest savings based on discount percentage
     */
    private BigDecimal calculateInterestSavings(BigDecimal totalInterestIfNormal, BigDecimal discountPercentage) {
        return totalInterestIfNormal
                .multiply(discountPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }


    /**
     * Calculate total interest if paid early
     */
    private BigDecimal calculateTotalInterestIfEarly(Loan loan, List<RepaymentSchedule> installments) {
        // For early repayment, only the accrued interest on the current installment is charged
        return installments.stream()
                .filter(i -> i.getStatus() == GeneralConfig.InstallmentStatus.PENDING ||
                        i.getStatus() == GeneralConfig.InstallmentStatus.PARTIAL)
                .findFirst()
                .map(RepaymentSchedule::getInterestDue)
                .orElse(BigDecimal.ZERO);
    }



    // ==================== HISTORY METHODS ====================

    @Transactional
    public Page<EarlyRepaymentHistoryDto> getEarlyRepaymentHistory(
            GeneralConfig.EarlyRepaymentStatus status,
            Long branchId,
            Long loanProductId,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable) {

        log.info("Fetching early repayment history with filters");

        Page<EarlyRepaymentRequest> requests = earlyRepaymentRepository.findHistoryWithFilters(
                status, branchId, loanProductId, startDate, endDate, search, pageable);

        return requests.map(this::mapToHistoryDto);
    }


    // ======================================================= REPORT GENERATION METHODS ====================

  @Transactional
    public byte[] generateHistoryReport(
            GeneralConfig.EarlyRepaymentStatus status,
            Long branchId,
            Long loanProductId,
            LocalDate startDate,
            LocalDate endDate,
            String format) {

        log.info("Generating early repayment history report in format: {}", format);

        // Set default date range if not provided
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(12);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Fetch all matching records (unpaginated)
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<EarlyRepaymentHistoryDto> historyPage = getEarlyRepaymentHistory(
                status, branchId, loanProductId, startDate, endDate, null, pageable);

        try {
            if ("PDF".equalsIgnoreCase(format)) {
                return generateHistoryPdf(historyPage.getContent(), startDate, endDate);
            } else if ("EXCEL".equalsIgnoreCase(format)) {
                return generateHistoryExcel(historyPage.getContent(), startDate, endDate);
            } else if ("CSV".equalsIgnoreCase(format)) {
                return generateHistoryCsv(historyPage.getContent(), startDate, endDate);
            } else {
                throw new BusinessException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            log.error("Error generating history report", e);
            throw new BusinessException("Failed to generate history report: " + e.getMessage());
        }
    }

    @Transactional
    public byte[] generateSummaryReport(
            LocalDate startDate,
            LocalDate endDate,
            Long branchId,
            String groupBy,
            String format) {

        log.info("Generating early repayment summary report in format: {}", format);

        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(12);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Get summary statistics
        EarlyRepaymentStatisticsDto stats = getEarlyRepaymentStatistics();

        // Get filtered requests for the period
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<EarlyRepaymentHistoryDto> historyPage = getEarlyRepaymentHistory(
                null, branchId, null, startDate, endDate, null, pageable);

        try {
            if ("PDF".equalsIgnoreCase(format)) {
                return generateSummaryPdf(historyPage.getContent(), stats, startDate, endDate, groupBy);
            } else if ("EXCEL".equalsIgnoreCase(format)) {
                return generateSummaryExcel(historyPage.getContent(), stats, startDate, endDate, groupBy);
            } else {
                throw new BusinessException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            log.error("Error generating summary report", e);
            throw new BusinessException("Failed to generate summary report: " + e.getMessage());
        }
    }

    @Transactional
    public byte[] generateDetailedReport(
            LocalDate startDate,
            LocalDate endDate,
            Long branchId,
            GeneralConfig.EarlyRepaymentStatus status,
            String format) {

        log.info("Generating early repayment detailed report in format: {}", format);

        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(12);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<EarlyRepaymentHistoryDto> historyPage = getEarlyRepaymentHistory(
                status, branchId, null, startDate, endDate, null, pageable);

        try {
            if ("PDF".equalsIgnoreCase(format)) {
                return generateDetailedPdf(historyPage.getContent(), startDate, endDate);
            } else if ("EXCEL".equalsIgnoreCase(format)) {
                return generateDetailedExcel(historyPage.getContent(), startDate, endDate);
            } else {
                throw new BusinessException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            log.error("Error generating detailed report", e);
            throw new BusinessException("Failed to generate detailed report: " + e.getMessage());
        }
    }

    @Transactional
    public byte[] generateDiscountAnalysisReport(
            LocalDate startDate,
            LocalDate endDate,
            Long branchId,
            BigDecimal minDiscount,
            BigDecimal maxDiscount,
            String format) {

        log.info("Generating early repayment discount analysis report in format: {}", format);

        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(12);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<EarlyRepaymentHistoryDto> historyPage = getEarlyRepaymentHistory(
                null, branchId, null, startDate, endDate, null, pageable);

        // Filter by discount range
        List<EarlyRepaymentHistoryDto> filteredList = historyPage.getContent().stream()
                .filter(item -> {
                    boolean matches = true;
                    if (minDiscount != null) {
                        matches = item.getDiscountPercentage().compareTo(minDiscount) >= 0;
                    }
                    if (maxDiscount != null && matches) {
                        matches = item.getDiscountPercentage().compareTo(maxDiscount) <= 0;
                    }
                    return matches;
                })
                .collect(Collectors.toList());

        try {
            if ("PDF".equalsIgnoreCase(format)) {
                return generateDiscountAnalysisPdf(filteredList, startDate, endDate, minDiscount, maxDiscount);
            } else if ("EXCEL".equalsIgnoreCase(format)) {
                return generateDiscountAnalysisExcel(filteredList, startDate, endDate, minDiscount, maxDiscount);
            } else {
                throw new BusinessException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            log.error("Error generating discount analysis report", e);
            throw new BusinessException("Failed to generate discount analysis report: " + e.getMessage());
        }
    }

    @Transactional
    public List<RecentReportDto> getRecentReports() {
        log.info("Fetching recent early repayment reports");

        // In a real implementation, you would fetch this from a reports table
        // For now, return mock data
        List<RecentReportDto> recentReports = new ArrayList<>();

        recentReports.add(RecentReportDto.builder()
                .reportName("Early Repayment Summary - March 2024")
                .generatedDate(LocalDateTime.now().minusHours(2))
                .format("PDF")
                .generatedBy("System Admin")
                .reportType("SUMMARY")
                .downloadUrl("/api/early-repayments/reports/summary?format=PDF")
                .build());

        recentReports.add(RecentReportDto.builder()
                .reportName("Early Repayment History - Q1 2024")
                .generatedDate(LocalDateTime.now().minusDays(1))
                .format("EXCEL")
                .generatedBy("System Admin")
                .reportType("HISTORY")
                .downloadUrl("/api/early-repayments/reports/history?format=EXCEL")
                .build());

        recentReports.add(RecentReportDto.builder()
                .reportName("Discount Analysis - March 2024")
                .generatedDate(LocalDateTime.now().minusDays(2))
                .format("PDF")
                .generatedBy("System Admin")
                .reportType("DISCOUNT")
                .downloadUrl("/api/early-repayments/reports/discount-analysis?format=PDF")
                .build());

        return recentReports;
    }

    // ==================== MAPPING METHODS ====================

    private EarlyRepaymentHistoryDto mapToHistoryDto(EarlyRepaymentRequest request) {
        if (request == null) return null;

        Loan loan = request.getLoan();

        return EarlyRepaymentHistoryDto.builder()
                .id(request.getId())
                .requestNumber(request.getRequestNumber())
                .loanNumber(loan != null ? loan.getLoanAccountNumber() : null)
                .borrowerName(request.getBorrower() != null ? request.getBorrower().getFullName() : null)
                .borrowerIdNumber(request.getBorrower() != null ? request.getBorrower().getBorrowerNumber() : null)
                .outstandingPrincipal(request.getOutstandingPrincipal())
                .accruedInterest(request.getAccruedInterest())
                .earlyRepaymentAmount(request.getEarlyRepaymentAmount())
                .discountPercentage(request.getDiscountPercentage())
                .discountAmount(request.getDiscountAmount())
                .interestSavings(request.getInterestSavings())
                .interestSavingsPercentage(request.getInterestSavingsPercentage())
                .status(request.getStatus() != null ? request.getStatus().name() : null)
                .requestedDate(request.getRequestedDate())
                .approvedDate(request.getApprovalDate())
                .paymentDate(request.getSettlementDate())
                .requestedBy(request.getRequestedBy() != null ? request.getRequestedBy().getFullName() : null)
                .approvedBy(request.getApprovedBy() != null ? request.getApprovedBy().getFullName() : null)
                .rejectedBy(request.getRejectedBy() != null ? request.getRejectedBy().getFullName() : null)
                .rejectionReason(request.getRejectionReason())
                .approvalComments(request.getApprovalComments())
                .branchId(loan != null && loan.getBranch() != null ? loan.getBranch().getId() : null)
                .branchName(loan != null && loan.getBranch() != null ? loan.getBranch().getName() : null)
                .loanProductId(loan != null && loan.getLoanProduct() != null ? loan.getLoanProduct().getId() : null)
                .loanProductName(loan != null && loan.getLoanProduct() != null ? loan.getLoanProduct().getName() : null)
                .build();
    }




    // ==================== PDF GENERATION METHODS ====================

    private byte[] generateHistoryPdf(List<EarlyRepaymentHistoryDto> history, LocalDate startDate, LocalDate endDate) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, baos);
        document.open();

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("EARLY REPAYMENT HISTORY REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));

        // Date range
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        document.add(new Paragraph("Period: " + startDate + " to " + endDate, normalFont));
        document.add(new Paragraph("Generated On: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")), normalFont));
        document.add(new Paragraph(" "));

        // Table
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 1.5f, 2f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f});

        // Headers
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        addTableCell(table, "Request #", headerFont);
        addTableCell(table, "Loan #", headerFont);
        addTableCell(table, "Borrower", headerFont);
        addTableCell(table, "Outstanding", headerFont);
        addTableCell(table, "Early Amount", headerFont);
        addTableCell(table, "Discount", headerFont);
        addTableCell(table, "Interest Saved", headerFont);
        addTableCell(table, "Status", headerFont);
        addTableCell(table, "Requested Date", headerFont);

        // Data
        Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        for (EarlyRepaymentHistoryDto item : history) {
            addTableCell(table, item.getRequestNumber(), dataFont);
            addTableCell(table, item.getLoanNumber(), dataFont);
            addTableCell(table, item.getBorrowerName(), dataFont);
            addTableCell(table, formatCurrency(item.getOutstandingPrincipal()), dataFont);
            addTableCell(table, formatCurrency(item.getEarlyRepaymentAmount()), dataFont);
            addTableCell(table, item.getDiscountPercentage() + "%", dataFont);
            addTableCell(table, formatCurrency(item.getInterestSavings()), dataFont);
            addTableCell(table, item.getStatus(), dataFont);
            addTableCell(table, item.getRequestedDate() != null ? item.getRequestedDate().toString() : "", dataFont);
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }

    private byte[] generateHistoryExcel(List<EarlyRepaymentHistoryDto> history, LocalDate startDate, LocalDate endDate) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Early Repayment History");

        // Title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("EARLY REPAYMENT HISTORY REPORT");
        CellStyle titleStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        // Date range
        Row dateRow = sheet.createRow(2);
        dateRow.createCell(0).setCellValue("Period:");
        dateRow.createCell(1).setCellValue(startDate + " to " + endDate);

        Row generatedRow = sheet.createRow(3);
        generatedRow.createCell(0).setCellValue("Generated On:");
        generatedRow.createCell(1).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));

        // Headers
        String[] headers = {"Request #", "Loan #", "Borrower", "Outstanding", "Early Amount",
                "Discount %", "Interest Saved", "Status", "Requested Date"};

        Row headerRow = sheet.createRow(5);
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        int rowNum = 6;
        for (EarlyRepaymentHistoryDto item : history) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getRequestNumber());
            row.createCell(1).setCellValue(item.getLoanNumber());
            row.createCell(2).setCellValue(item.getBorrowerName());
            row.createCell(3).setCellValue(item.getOutstandingPrincipal().doubleValue());
            row.createCell(4).setCellValue(item.getEarlyRepaymentAmount().doubleValue());
            row.createCell(5).setCellValue(item.getDiscountPercentage().doubleValue());
            row.createCell(6).setCellValue(item.getInterestSavings().doubleValue());
            row.createCell(7).setCellValue(item.getStatus());
            row.createCell(8).setCellValue(item.getRequestedDate() != null ? item.getRequestedDate().toString() : "");
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        return baos.toByteArray();
    }

    private byte[] generateHistoryCsv(List<EarlyRepaymentHistoryDto> history, LocalDate startDate, LocalDate endDate) throws Exception {
        StringBuilder csv = new StringBuilder();

        csv.append("EARLY REPAYMENT HISTORY REPORT\n");
        csv.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n");
        csv.append("Generated On: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))).append("\n\n");

        csv.append("Request #,Loan #,Borrower,Outstanding,Early Amount,Discount %,Interest Saved,Status,Requested Date\n");

        for (EarlyRepaymentHistoryDto item : history) {
            csv.append(escapeCsv(item.getRequestNumber())).append(",");
            csv.append(escapeCsv(item.getLoanNumber())).append(",");
            csv.append(escapeCsv(item.getBorrowerName())).append(",");
            csv.append(item.getOutstandingPrincipal()).append(",");
            csv.append(item.getEarlyRepaymentAmount()).append(",");
            csv.append(item.getDiscountPercentage()).append(",");
            csv.append(item.getInterestSavings()).append(",");
            csv.append(escapeCsv(item.getStatus())).append(",");
            csv.append(item.getRequestedDate() != null ? item.getRequestedDate().toString() : "").append("\n");
        }

        return csv.toString().getBytes();
    }

    private byte[] generateSummaryPdf(List<EarlyRepaymentHistoryDto> history, EarlyRepaymentStatisticsDto stats,
                                      LocalDate startDate, LocalDate endDate, String groupBy) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("EARLY REPAYMENT SUMMARY REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));

        // Date range
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        document.add(new Paragraph("Period: " + startDate + " to " + endDate, normalFont));
        document.add(new Paragraph("Grouped By: " + groupBy, normalFont));
        document.add(new Paragraph("Generated On: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")), normalFont));
        document.add(new Paragraph(" "));

        // Summary statistics
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        document.add(new Paragraph("SUMMARY STATISTICS", headerFont));
        document.add(new Paragraph(" "));

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(70);
        summaryTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addSummaryRow(summaryTable, "Total Early Repayments:", formatCurrency(stats.getTotalEarlyRepayments()), normalFont);
        addSummaryRow(summaryTable, "Total Interest Saved:", formatCurrency(stats.getTotalInterestSaved()), normalFont);
        addSummaryRow(summaryTable, "Active Requests:", String.valueOf(stats.getActiveRequests()), normalFont);
        addSummaryRow(summaryTable, "Average Discount:", stats.getAverageDiscount() + "%", normalFont);
        addSummaryRow(summaryTable, "Approved:", String.valueOf(stats.getApprovedRequests()), normalFont);
        addSummaryRow(summaryTable, "Rejected:", String.valueOf(stats.getRejectedRequests()), normalFont);
        addSummaryRow(summaryTable, "Pending:", String.valueOf(stats.getPendingRequests()), normalFont);

        document.add(summaryTable);
        document.add(new Paragraph(" "));

        // Grouped data (simplified - in real implementation, you'd group by the specified field)
        document.add(new Paragraph("PERIOD BREAKDOWN", headerFont));
        document.add(new Paragraph(" "));

        PdfPTable breakdownTable = new PdfPTable(4);
        breakdownTable.setWidthPercentage(100);
        breakdownTable.setWidths(new float[]{2f, 2f, 2f, 2f});

        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        addTableCell(breakdownTable, "Period", tableHeaderFont);
        addTableCell(breakdownTable, "Count", tableHeaderFont);
        addTableCell(breakdownTable, "Total Amount", tableHeaderFont);
        addTableCell(breakdownTable, "Interest Saved", tableHeaderFont);

        // Group by month (simplified example)
        Map<String, List<EarlyRepaymentHistoryDto>> grouped = history.stream()
                .collect(Collectors.groupingBy(item ->
                        item.getRequestedDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))));

        Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        for (Map.Entry<String, List<EarlyRepaymentHistoryDto>> entry : grouped.entrySet()) {
            BigDecimal totalAmount = entry.getValue().stream()
                    .map(EarlyRepaymentHistoryDto::getEarlyRepaymentAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalSaved = entry.getValue().stream()
                    .map(EarlyRepaymentHistoryDto::getInterestSavings)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            addTableCell(breakdownTable, entry.getKey(), dataFont);
            addTableCell(breakdownTable, String.valueOf(entry.getValue().size()), dataFont);
            addTableCell(breakdownTable, formatCurrency(totalAmount), dataFont);
            addTableCell(breakdownTable, formatCurrency(totalSaved), dataFont);
        }

        document.add(breakdownTable);
        document.close();

        return baos.toByteArray();
    }

    private byte[] generateSummaryExcel(List<EarlyRepaymentHistoryDto> history, EarlyRepaymentStatisticsDto stats,
                                        LocalDate startDate, LocalDate endDate, String groupBy) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Early Repayment Summary");

        // Title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("EARLY REPAYMENT SUMMARY REPORT");
        CellStyle titleStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        // Period
        Row periodRow = sheet.createRow(2);
        periodRow.createCell(0).setCellValue("Period:");
        periodRow.createCell(1).setCellValue(startDate + " to " + endDate);

        Row groupRow = sheet.createRow(3);
        groupRow.createCell(0).setCellValue("Grouped By:");
        groupRow.createCell(1).setCellValue(groupBy);

        Row generatedRow = sheet.createRow(4);
        generatedRow.createCell(0).setCellValue("Generated On:");
        generatedRow.createCell(1).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));

        // Statistics
        Row statsHeaderRow = sheet.createRow(6);
        Cell statsHeaderCell = statsHeaderRow.createCell(0);
        statsHeaderCell.setCellValue("SUMMARY STATISTICS");
        CellStyle statsHeaderStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font statsHeaderFont = workbook.createFont();
        statsHeaderFont.setBold(true);
        statsHeaderFont.setFontHeightInPoints((short) 14);
        statsHeaderStyle.setFont(statsHeaderFont);
        statsHeaderCell.setCellStyle(statsHeaderStyle);

        Row statsRow1 = sheet.createRow(7);
        statsRow1.createCell(0).setCellValue("Total Early Repayments:");
        statsRow1.createCell(1).setCellValue(stats.getTotalEarlyRepayments().doubleValue());

        Row statsRow2 = sheet.createRow(8);
        statsRow2.createCell(0).setCellValue("Total Interest Saved:");
        statsRow2.createCell(1).setCellValue(stats.getTotalInterestSaved().doubleValue());

        Row statsRow3 = sheet.createRow(9);
        statsRow3.createCell(0).setCellValue("Active Requests:");
        statsRow3.createCell(1).setCellValue(stats.getActiveRequests());

        Row statsRow4 = sheet.createRow(10);
        statsRow4.createCell(0).setCellValue("Average Discount:");
        statsRow4.createCell(1).setCellValue(String.valueOf(stats.getAverageDiscount()));

        // Breakdown headers
        Row breakdownHeaderRow = sheet.createRow(12);
        String[] headers = {"Period", "Count", "Total Amount", "Interest Saved"};
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = breakdownHeaderRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Group by month
        Map<String, List<EarlyRepaymentHistoryDto>> grouped = history.stream()
                .collect(Collectors.groupingBy(item ->
                        item.getRequestedDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))));

        int rowNum = 13;
        for (Map.Entry<String, List<EarlyRepaymentHistoryDto>> entry : grouped.entrySet()) {
            BigDecimal totalAmount = entry.getValue().stream()
                    .map(EarlyRepaymentHistoryDto::getEarlyRepaymentAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalSaved = entry.getValue().stream()
                    .map(EarlyRepaymentHistoryDto::getInterestSavings)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue().size());
            row.createCell(2).setCellValue(totalAmount.doubleValue());
            row.createCell(3).setCellValue(totalSaved.doubleValue());
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        return baos.toByteArray();
    }

    private byte[] generateDetailedPdf(List<EarlyRepaymentHistoryDto> history, LocalDate startDate, LocalDate endDate) throws Exception {
        // Similar to history PDF but with more columns
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("EARLY REPAYMENT DETAILED REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Period: " + startDate + " to " + endDate, new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL)));
        document.add(new Paragraph(" "));

        // Create table with more columns
        PdfPTable table = new PdfPTable(12);
        table.setWidthPercentage(100);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        addTableCell(table, "Request #", headerFont);
        addTableCell(table, "Loan #", headerFont);
        addTableCell(table, "Borrower", headerFont);
        addTableCell(table, "Outstanding", headerFont);
        addTableCell(table, "Accrued Int", headerFont);
        addTableCell(table, "Early Amount", headerFont);
        addTableCell(table, "Discount %", headerFont);
        addTableCell(table, "Discount Amt", headerFont);
        addTableCell(table, "Int Saved", headerFont);
        addTableCell(table, "Status", headerFont);
        addTableCell(table, "Requested", headerFont);
        addTableCell(table, "Approved", headerFont);

        Font dataFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
        for (EarlyRepaymentHistoryDto item : history) {
            addTableCell(table, item.getRequestNumber(), dataFont);
            addTableCell(table, item.getLoanNumber(), dataFont);
            addTableCell(table, item.getBorrowerName(), dataFont);
            addTableCell(table, formatCurrency(item.getOutstandingPrincipal()), dataFont);
            addTableCell(table, formatCurrency(item.getAccruedInterest()), dataFont);
            addTableCell(table, formatCurrency(item.getEarlyRepaymentAmount()), dataFont);
            addTableCell(table, item.getDiscountPercentage() + "%", dataFont);
            addTableCell(table, formatCurrency(item.getDiscountAmount()), dataFont);
            addTableCell(table, formatCurrency(item.getInterestSavings()), dataFont);
            addTableCell(table, item.getStatus(), dataFont);
            addTableCell(table, item.getRequestedDate() != null ? item.getRequestedDate().toString() : "", dataFont);
            addTableCell(table, item.getApprovedDate() != null ? item.getApprovedDate().toString() : "", dataFont);
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }

    private byte[] generateDetailedExcel(List<EarlyRepaymentHistoryDto> history, LocalDate startDate, LocalDate endDate) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Detailed Transactions");

        // Title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("EARLY REPAYMENT DETAILED REPORT");
        CellStyle titleStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        // Period
        Row periodRow = sheet.createRow(2);
        periodRow.createCell(0).setCellValue("Period:");
        periodRow.createCell(1).setCellValue(startDate + " to " + endDate);

        // Headers
        String[] headers = {"Request #", "Loan #", "Borrower", "Outstanding", "Accrued Int",
                "Early Amount", "Discount %", "Discount Amt", "Int Saved",
                "Status", "Requested", "Approved"};

        Row headerRow = sheet.createRow(4);
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 5;
        for (EarlyRepaymentHistoryDto item : history) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getRequestNumber());
            row.createCell(1).setCellValue(item.getLoanNumber());
            row.createCell(2).setCellValue(item.getBorrowerName());
            row.createCell(3).setCellValue(item.getOutstandingPrincipal().doubleValue());
            row.createCell(4).setCellValue(item.getAccruedInterest().doubleValue());
            row.createCell(5).setCellValue(item.getEarlyRepaymentAmount().doubleValue());
            row.createCell(6).setCellValue(item.getDiscountPercentage().doubleValue());
            row.createCell(7).setCellValue(item.getDiscountAmount().doubleValue());
            row.createCell(8).setCellValue(item.getInterestSavings().doubleValue());
            row.createCell(9).setCellValue(item.getStatus());
            row.createCell(10).setCellValue(item.getRequestedDate() != null ? item.getRequestedDate().toString() : "");
            row.createCell(11).setCellValue(item.getApprovedDate() != null ? item.getApprovedDate().toString() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        return baos.toByteArray();
    }

    private byte[] generateDiscountAnalysisPdf(List<EarlyRepaymentHistoryDto> history, LocalDate startDate, LocalDate endDate,
                                               BigDecimal minDiscount, BigDecimal maxDiscount) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("EARLY REPAYMENT DISCOUNT ANALYSIS", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Period: " + startDate + " to " + endDate, new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL)));

        String discountRange = (minDiscount != null ? minDiscount : "0") + "% - " +
                (maxDiscount != null ? maxDiscount : "100") + "%";
        document.add(new Paragraph("Discount Range: " + discountRange, new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL)));
        document.add(new Paragraph(" "));

        // Summary statistics
        BigDecimal avgDiscount = history.stream()
                .map(EarlyRepaymentHistoryDto::getDiscountPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size() > 0 ? history.size() : 1), 2, RoundingMode.HALF_UP);

        BigDecimal totalDiscountAmount = history.stream()
                .map(EarlyRepaymentHistoryDto::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        document.add(new Paragraph("Average Discount: " + avgDiscount + "%", new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL)));
        document.add(new Paragraph("Total Discount Amount: " + formatCurrency(totalDiscountAmount), new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL)));
        document.add(new Paragraph("Number of Transactions: " + history.size(), new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL)));
        document.add(new Paragraph(" "));

        // Table
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        addTableCell(table, "Request #", headerFont);
        addTableCell(table, "Loan #", headerFont);
        addTableCell(table, "Borrower", headerFont);
        addTableCell(table, "Outstanding", headerFont);
        addTableCell(table, "Early Amount", headerFont);
        addTableCell(table, "Discount %", headerFont);
        addTableCell(table, "Discount Amt", headerFont);

        Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        for (EarlyRepaymentHistoryDto item : history) {
            addTableCell(table, item.getRequestNumber(), dataFont);
            addTableCell(table, item.getLoanNumber(), dataFont);
            addTableCell(table, item.getBorrowerName(), dataFont);
            addTableCell(table, formatCurrency(item.getOutstandingPrincipal()), dataFont);
            addTableCell(table, formatCurrency(item.getEarlyRepaymentAmount()), dataFont);
            addTableCell(table, item.getDiscountPercentage() + "%", dataFont);
            addTableCell(table, formatCurrency(item.getDiscountAmount()), dataFont);
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }

    private byte[] generateDiscountAnalysisExcel(List<EarlyRepaymentHistoryDto> history, LocalDate startDate, LocalDate endDate,
                                                 BigDecimal minDiscount, BigDecimal maxDiscount) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Discount Analysis");

        // Title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("EARLY REPAYMENT DISCOUNT ANALYSIS");
        CellStyle titleStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        // Period
        Row periodRow = sheet.createRow(2);
        periodRow.createCell(0).setCellValue("Period:");
        periodRow.createCell(1).setCellValue(startDate + " to " + endDate);

        String discountRange = (minDiscount != null ? minDiscount : "0") + "% - " +
                (maxDiscount != null ? maxDiscount : "100") + "%";
        Row rangeRow = sheet.createRow(3);
        rangeRow.createCell(0).setCellValue("Discount Range:");
        rangeRow.createCell(1).setCellValue(discountRange);

        // Summary
        BigDecimal avgDiscount = history.stream()
                .map(EarlyRepaymentHistoryDto::getDiscountPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size() > 0 ? history.size() : 1), 2, RoundingMode.HALF_UP);

        BigDecimal totalDiscountAmount = history.stream()
                .map(EarlyRepaymentHistoryDto::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Row avgRow = sheet.createRow(5);
        avgRow.createCell(0).setCellValue("Average Discount:");
        avgRow.createCell(1).setCellValue(avgDiscount.doubleValue());

        Row totalRow = sheet.createRow(6);
        totalRow.createCell(0).setCellValue("Total Discount Amount:");
        totalRow.createCell(1).setCellValue(totalDiscountAmount.doubleValue());

        Row countRow = sheet.createRow(7);
        countRow.createCell(0).setCellValue("Number of Transactions:");
        countRow.createCell(1).setCellValue(history.size());

        // Headers
        String[] headers = {"Request #", "Loan #", "Borrower", "Outstanding", "Early Amount", "Discount %", "Discount Amt"};
        Row headerRow = sheet.createRow(9);
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 10;
        for (EarlyRepaymentHistoryDto item : history) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getRequestNumber());
            row.createCell(1).setCellValue(item.getLoanNumber());
            row.createCell(2).setCellValue(item.getBorrowerName());
            row.createCell(3).setCellValue(item.getOutstandingPrincipal().doubleValue());
            row.createCell(4).setCellValue(item.getEarlyRepaymentAmount().doubleValue());
            row.createCell(5).setCellValue(item.getDiscountPercentage().doubleValue());
            row.createCell(6).setCellValue(item.getDiscountAmount().doubleValue());
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        return baos.toByteArray();
    }

    // ==================== HELPER METHODS ====================

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }




}