// service/ProvisionService.java (Updated with RepaymentSchedule)
package com.microfinance.financials.provisions.service;

import com.microfinance.base.entity.User;
import com.microfinance.financials.provisions.dto.*;
import com.microfinance.financials.provisions.entity.*;
import com.microfinance.financials.provisions.repository.*;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.RepaymentSchedule;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import com.microfinance.common.config.GeneralConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvisionService {

    private final ProvisionCalculationRepository provisionRepository;
    private final WriteOffRequestRepository writeOffRepository;
    private final LoanRecoveryRepository recoveryRepository;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository; // Add this

    // Provision Calculation Methods
    @Transactional
    public List<ProvisionCalculationDTO> calculateProvisions(ProvisionCalculationRequestDTO request, User currentUser) {
        log.info("User {} calculating provisions as of date: {}", currentUser.getUsername(), request.getAsOfDate());

        List<Loan> loans;
        if (request.getLoanId() != null) {
            loans = loanRepository.findById(request.getLoanId()).stream().collect(Collectors.toList());
        } else {
            loans = loanRepository.findAll();
        }

        List<ProvisionCalculation> calculations = new ArrayList<>();
        String calculationNumber = generateCalculationNumber();
        LocalDate calculationDate = LocalDate.now();

        for (Loan loan : loans) {
            // Get next payment due date from repayment schedule
            LocalDate nextPaymentDueDate = getNextPaymentDueDate(loan.getId());
            
            // Calculate days past due
            int daysPastDue = calculateDaysPastDue(nextPaymentDueDate, request.getAsOfDate());
            if (daysPastDue <= 0 && !request.getIncludeAllLoans()) {
                continue;
            }

            String agingBucket = getAgingBucket(daysPastDue);
            BigDecimal provisionRate = getProvisionRate(agingBucket);
            
            // Calculate total outstanding (principal + interest)
            BigDecimal totalOutstanding = getTotalOutstanding(loan);
            BigDecimal provisionAmount = totalOutstanding.multiply(provisionRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Get existing provision
            BigDecimal existingProvision = getExistingProvision(loan.getId(), request.getAsOfDate());
            BigDecimal provisionAdjustment = provisionAmount.subtract(existingProvision);

            ProvisionCalculation calculation = ProvisionCalculation.builder()
                    .calculationNumber(calculationNumber)
                    .calculationDate(calculationDate)
                    .periodStart(request.getAsOfDate().minusMonths(1))
                    .periodEnd(request.getAsOfDate())
                    .loanId(loan.getId())
                    .loanAccountNumber(loan.getLoanAccountNumber())
                    .borrowerId(loan.getBorrower().getId())
                    .borrowerName(loan.getBorrower().getFullName())
                    .principalOutstanding(getPrincipalOutstanding(loan))
                    .interestOutstanding(getInterestOutstanding(loan))
                    .totalOutstanding(totalOutstanding)
                    .daysPastDue(daysPastDue)
                    .agingBucket(agingBucket)
                    .provisionRate(provisionRate)
                    .provisionAmount(provisionAmount)
                    .existingProvision(existingProvision)
                    .provisionAdjustment(provisionAdjustment)
                    .status("DRAFT")
                    .calculatedBy(currentUser.getId())
                    .build();

            calculations.add(provisionRepository.save(calculation));
        }

        return calculations.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Get the next payment due date from repayment schedule
     * @param loanId The loan ID
     * @return The next due date or null if no pending installments
     */
    private LocalDate getNextPaymentDueDate(Long loanId) {
        // Find the earliest unpaid installment
        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanIdAndStatusOrderByDueDateAsc(
                loanId, GeneralConfig.InstallmentStatus.PENDING);
        
        if (schedules != null && !schedules.isEmpty()) {
            return schedules.get(0).getDueDate();
        }
        
        // Check for partial payments as well
        schedules = repaymentScheduleRepository.findByLoanIdAndStatusOrderByDueDateAsc(
                loanId, GeneralConfig.InstallmentStatus.PARTIAL);
        
        if (schedules != null && !schedules.isEmpty()) {
            return schedules.get(0).getDueDate();
        }
        
        return null;
    }

    /**
     * Calculate days past due based on next payment due date
     * @param nextPaymentDueDate The next payment due date
     * @param asOfDate The date to calculate as of
     * @return Number of days past due (negative if not due yet)
     */
    private int calculateDaysPastDue(LocalDate nextPaymentDueDate, LocalDate asOfDate) {
        if (nextPaymentDueDate == null) {
            return 0;
        }
        
        if (asOfDate.isAfter(nextPaymentDueDate)) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(nextPaymentDueDate, asOfDate);
        }
        
        return 0;
    }

    /**
     * Get principal outstanding from repayment schedule
     */
    private BigDecimal getPrincipalOutstanding(Loan loan) {
        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanId(loan.getId());
        return schedules.stream()
                .map(s -> s.getPrincipalDue().subtract(s.getPrincipalPaid()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get interest outstanding from repayment schedule
     */
    private BigDecimal getInterestOutstanding(Loan loan) {
        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanId(loan.getId());
        return schedules.stream()
                .map(s -> s.getInterestDue().subtract(s.getInterestPaid()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total outstanding (principal + interest)
     */
    private BigDecimal getTotalOutstanding(Loan loan) {
        return getPrincipalOutstanding(loan).add(getInterestOutstanding(loan));
    }

    private String getAgingBucket(int daysPastDue) {
        if (daysPastDue <= 30) return "1-30";
        if (daysPastDue <= 60) return "31-60";
        if (daysPastDue <= 90) return "61-90";
        if (daysPastDue <= 180) return "91-180";
        return "180+";
    }

    private BigDecimal getProvisionRate(String agingBucket) {
        switch (agingBucket) {
            case "1-30": return BigDecimal.valueOf(5);
            case "31-60": return BigDecimal.valueOf(10);
            case "61-90": return BigDecimal.valueOf(25);
            case "91-180": return BigDecimal.valueOf(50);
            case "180+": return BigDecimal.valueOf(100);
            default: return BigDecimal.ZERO;
        }
    }

    private BigDecimal getExistingProvision(Long loanId, LocalDate asOfDate) {
        return provisionRepository.findLatestProvisionByLoanId(loanId)
                .map(ProvisionCalculation::getProvisionAmount)
                .orElse(BigDecimal.ZERO);
    }

    // Write-off Methods
    @Transactional
    public WriteOffRequestDTO createWriteOffRequest(WriteOffRequestDTO dto, User currentUser) {
        log.info("User {} creating write-off request for loan: {}", currentUser.getUsername(), dto.getLoanId());

        String requestNumber = generateRequestNumber();
        LocalDate requestDate = LocalDate.now();

        // Get the loan and its outstanding amounts
        Loan loan = loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        
        BigDecimal principalOutstanding = getPrincipalOutstanding(loan);
        BigDecimal interestOutstanding = getInterestOutstanding(loan);
        BigDecimal totalOutstanding = principalOutstanding.add(interestOutstanding);

        WriteOffRequest request = WriteOffRequest.builder()
                .requestNumber(requestNumber)
                .loanId(dto.getLoanId())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .borrowerId(loan.getBorrower().getId())
                .borrowerName(loan.getBorrower().getFullName())
                .principalAmount(principalOutstanding)
                .interestAmount(interestOutstanding)
                .penaltyAmount(dto.getPenaltyAmount())
                .feesAmount(dto.getFeesAmount())
                .totalAmount(totalOutstanding)
                .provisionAmount(dto.getProvisionAmount())
                .netWriteOff(totalOutstanding.subtract(dto.getProvisionAmount()))
                .requestDate(requestDate)
                .reason(dto.getReason())
                .reasonDescription(dto.getReasonDescription())
                .status("PENDING")
                .notes(dto.getNotes())
                .requestedBy(currentUser.getId())
                .build();

        request = writeOffRepository.save(request);
        return convertToDTO(request);
    }

    @Transactional
    public WriteOffRequestDTO approveWriteOffRequest(Long id, String approvalNotes, User currentUser) {
        log.info("User {} approving write-off request: {}", currentUser.getUsername(), id);

        WriteOffRequest request = writeOffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Write-off request not found"));

        request.setStatus("APPROVED");
        request.setApprovalDate(LocalDate.now());
        request.setApprovedBy(currentUser.getId());
        request.setApprovalNotes(approvalNotes);
        request = writeOffRepository.save(request);

        return convertToDTO(request);
    }

    @Transactional
    public WriteOffRequestDTO rejectWriteOffRequest(Long id, String rejectionReason, User currentUser) {
        log.info("User {} rejecting write-off request: {}", currentUser.getUsername(), id);

        WriteOffRequest request = writeOffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Write-off request not found"));

        request.setStatus("REJECTED");
        request.setApprovalNotes(rejectionReason);
        request = writeOffRepository.save(request);

        return convertToDTO(request);
    }

    @Transactional
    public WriteOffRequestDTO completeWriteOff(Long id, User currentUser) {
        log.info("User {} completing write-off: {}", currentUser.getUsername(), id);

        WriteOffRequest request = writeOffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Write-off request not found"));

        request.setStatus("COMPLETED");
        request.setWriteOffDate(LocalDate.now());
        request = writeOffRepository.save(request);

        // Update loan status
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        loan.setStatus(GeneralConfig.LoanStatus.valueOf("WRITTEN_OFF"));
        loanRepository.save(loan);

        return convertToDTO(request);
    }

    @Transactional(readOnly = true)
    public Page<WriteOffRequestDTO> getWriteOffRequests(String status, Pageable pageable) {
        if (status != null) {
            return writeOffRepository.findByStatus(status, pageable).map(this::convertToDTO);
        }
        return writeOffRepository.findAll(pageable).map(this::convertToDTO);
    }

    // Loan Recovery Methods
    @Transactional
    public LoanRecoveryDTO recordRecovery(LoanRecoveryDTO dto, User currentUser) {
        log.info("User {} recording recovery for loan: {}", currentUser.getUsername(), dto.getLoanId());

        String recoveryNumber = generateRecoveryNumber();

        LoanRecovery recovery = LoanRecovery.builder()
                .recoveryNumber(recoveryNumber)
                .writeOffId(dto.getWriteOffId())
                .loanId(dto.getLoanId())
                .loanAccountNumber(dto.getLoanAccountNumber())
                .borrowerId(dto.getBorrowerId())
                .borrowerName(dto.getBorrowerName())
                .recoveryDate(dto.getRecoveryDate())
                .recoveredAmount(dto.getRecoveredAmount())
                .principalRecovered(dto.getPrincipalRecovered())
                .interestRecovered(dto.getInterestRecovered())
                .penaltyRecovered(dto.getPenaltyRecovered())
                .feesRecovered(dto.getFeesRecovered())
                .recoveryType(dto.getRecoveryType())
                .referenceNumber(dto.getReferenceNumber())
                .notes(dto.getNotes())
                .createdBy(currentUser.getId())
                .build();

        recovery = recoveryRepository.save(recovery);
        return convertToDTO(recovery);
    }

    @Transactional(readOnly = true)
    public Page<LoanRecoveryDTO> getRecoveries(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return recoveryRepository.findByRecoveryDateBetween(startDate, endDate, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRecoveries(LocalDate startDate, LocalDate endDate) {
        BigDecimal total = recoveryRepository.getTotalRecoveriesBetween(startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    // Helper Methods
    private String generateCalculationNumber() {
        return "PROV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private String generateRequestNumber() {
        return "WO-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private String generateRecoveryNumber() {
        return "REC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private ProvisionCalculationDTO convertToDTO(ProvisionCalculation entity) {
        return ProvisionCalculationDTO.builder()
                .id(entity.getId())
                .calculationNumber(entity.getCalculationNumber())
                .calculationDate(entity.getCalculationDate())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .loanId(entity.getLoanId())
                .loanAccountNumber(entity.getLoanAccountNumber())
                .borrowerId(entity.getBorrowerId())
                .borrowerName(entity.getBorrowerName())
                .principalOutstanding(entity.getPrincipalOutstanding())
                .interestOutstanding(entity.getInterestOutstanding())
                .totalOutstanding(entity.getTotalOutstanding())
                .daysPastDue(entity.getDaysPastDue())
                .agingBucket(entity.getAgingBucket())
                .provisionRate(entity.getProvisionRate())
                .provisionAmount(entity.getProvisionAmount())
                .existingProvision(entity.getExistingProvision())
                .provisionAdjustment(entity.getProvisionAdjustment())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private WriteOffRequestDTO convertToDTO(WriteOffRequest entity) {
        return WriteOffRequestDTO.builder()
                .id(entity.getId())
                .requestNumber(entity.getRequestNumber())
                .loanId(entity.getLoanId())
                .loanAccountNumber(entity.getLoanAccountNumber())
                .borrowerId(entity.getBorrowerId())
                .borrowerName(entity.getBorrowerName())
                .principalAmount(entity.getPrincipalAmount())
                .interestAmount(entity.getInterestAmount())
                .penaltyAmount(entity.getPenaltyAmount())
                .feesAmount(entity.getFeesAmount())
                .totalAmount(entity.getTotalAmount())
                .provisionAmount(entity.getProvisionAmount())
                .netWriteOff(entity.getNetWriteOff())
                .requestDate(entity.getRequestDate())
                .reason(entity.getReason())
                .reasonDescription(entity.getReasonDescription())
                .status(entity.getStatus())
                .approvalDate(entity.getApprovalDate())
                .approvalNotes(entity.getApprovalNotes())
                .writeOffDate(entity.getWriteOffDate())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private LoanRecoveryDTO convertToDTO(LoanRecovery entity) {
        return LoanRecoveryDTO.builder()
                .id(entity.getId())
                .recoveryNumber(entity.getRecoveryNumber())
                .writeOffId(entity.getWriteOffId())
                .loanId(entity.getLoanId())
                .loanAccountNumber(entity.getLoanAccountNumber())
                .borrowerId(entity.getBorrowerId())
                .borrowerName(entity.getBorrowerName())
                .recoveryDate(entity.getRecoveryDate())
                .recoveredAmount(entity.getRecoveredAmount())
                .principalRecovered(entity.getPrincipalRecovered())
                .interestRecovered(entity.getInterestRecovered())
                .penaltyRecovered(entity.getPenaltyRecovered())
                .feesRecovered(entity.getFeesRecovered())
                .recoveryType(entity.getRecoveryType())
                .referenceNumber(entity.getReferenceNumber())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ProvisionCalculationDTO> getProvisionCalculations(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Fetching provision calculations from {} to {}", startDate, endDate);

        Page<ProvisionCalculation> calculations = provisionRepository.findByCalculationDateBetween(startDate, endDate, pageable);
        return calculations.map(this::convertToDTO);
    }

    /**
     * Get provision summary grouped by aging bucket
     */

    @Transactional(readOnly = true)
    public List<ProvisionSummaryDTO> getProvisionSummary(LocalDate calculationDate) {
        log.info("Fetching provision summary for date: {}", calculationDate);

        List<Object[]> results = provisionRepository.getProvisionSummaryByBucket(calculationDate);

        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        List<ProvisionSummaryDTO> summaries = new ArrayList<>();
        for (Object[] row : results) {
            ProvisionSummaryDTO dto = new ProvisionSummaryDTO();
            dto.setAgingBucket((String) row[0]);
            dto.setLoanCount(((Long) row[1]).intValue());
            dto.setTotalOutstanding((BigDecimal) row[2]);
            dto.setProvisionRate((BigDecimal) row[3]);
            dto.setProvisionAmount((BigDecimal) row[4]);
            dto.setExistingProvision((BigDecimal) row[5]);
            dto.setAdjustment((BigDecimal) row[6]);
            summaries.add(dto);
        }

        return summaries;
    }


    /**
     * Get provision calculations for a specific loan
     */
    @Transactional(readOnly = true)
    public List<ProvisionCalculationDTO> getProvisionCalculationsByLoan(Long loanId) {
        log.info("Fetching provision calculations for loan: {}", loanId);

        List<ProvisionCalculation> calculations = provisionRepository.findByLoanIdOrderByCalculationDateDesc(loanId);
        return calculations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get the latest provision calculation for a loan
     */
    @Transactional(readOnly = true)
    public ProvisionCalculationDTO getLatestProvisionByLoan(Long loanId) {
        log.info("Fetching latest provision for loan: {}", loanId);

        return provisionRepository.findLatestProvisionByLoanId(loanId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    /**
     * Approve a provision calculation (post to GL)
     */
    @Transactional
    public ProvisionCalculationDTO approveProvisionCalculation(Long calculationId, User currentUser) {
        log.info("User {} approving provision calculation: {}", currentUser.getUsername(), calculationId);

        ProvisionCalculation calculation = provisionRepository.findById(calculationId)
                .orElseThrow(() -> new RuntimeException("Provision calculation not found"));

        calculation.setStatus("APPROVED");
        calculation.setApprovedBy(currentUser.getId());
        calculation.setApprovedAt(LocalDateTime.now());
        calculation = provisionRepository.save(calculation);

        // TODO: Create journal entry for provision adjustment

        return convertToDTO(calculation);
    }

    /**
     * Get all written-off loans for recovery selection
     */
    @Transactional(readOnly = true)
    public List<WriteOffRequestDTO> getCompletedWriteOffs() {
        log.info("Fetching completed write-offs for recovery");

        List<WriteOffRequest> writeOffs = writeOffRepository.findCompletedWriteOffs();
        return writeOffs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get total recoveries summary by type
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getRecoverySummaryByType(LocalDate startDate, LocalDate endDate) {
        log.info("Getting recovery summary by type from {} to {}", startDate, endDate);

        List<Object[]> results = recoveryRepository.getRecoverySummaryByType(startDate, endDate);

        Map<String, BigDecimal> summary = new HashMap<>();
        for (Object[] row : results) {
            String type = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            summary.put(type, amount);
        }

        return summary;
    }






}