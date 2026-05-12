package com.microfinance.loanapplications.service;

import com.microfinance.base.entity.User;
import com.microfinance.base.repository.RolePermissionRepository;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.BusinessException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.loanapplications.dto.*;
import com.microfinance.loanapplications.dto.LoanRescheduleRequestDto;
import com.microfinance.loanapplications.dto.LoanRescheduleResponseDto;
import com.microfinance.loanapplications.dto.application.PortfolioStats;
import com.microfinance.loanapplications.dto.collection.AssignmentResultDto;
import com.microfinance.loanapplications.dto.collection.BulkAssignResultDto;
import com.microfinance.loanapplications.dto.collection.LoanEligibleForRecoveryDto;
import com.microfinance.loanapplications.dto.disbursement.PortfolioSummaryDto;
import com.microfinance.loanapplications.dto.repayment.*;
import com.microfinance.loanapplications.entity.*;
import com.microfinance.loanapplications.mapper.LoanMapper;
import com.microfinance.loanapplications.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final LoanRescheduleRepository loanRescheduleRepository;
    private final LoanRestructureRepository loanRestructureRepository;
    private final LoanDocumentRepository loanDocumentRepository;
    private final LoanAuditRepository loanAuditRepository;
    private final UserService userService;
    private final SecurityUtils securityUtils;
    private final RolePermissionRepository rolePermissionRepository;
    private final RecoveryCaseRepository recoveryCaseRepository;
    private final CollectionActionRepository collectionActionRepository;

    // Note: Repayment operations are handled by LoanRepaymentService
    // Disbursement operations are handled by LoanDisbursementService
    // Reschedule operations are handled by LoanRescheduleService

    @Transactional(readOnly = true)
    @Override
    public Page<LoanDto> getLoans(String status, Long branchId, Long borrowerId, Long loanProductId,
                                  LocalDate fromDate, LocalDate toDate, Pageable pageable) {

        log.debug("Fetching loans with filters - status: {}, branch: {}, borrower: {}",
                status, branchId, borrowerId);

        User currentUser = getCurrentUser();

        // Apply permission-based filtering
        Long userBranchId = null;
        Long userId = null;

        if (!hasCurrentUserPermission("LOAN_VIEW_ALL")) {
            if (hasCurrentUserPermission("LOAN_VIEW_BRANCH")) {
                userBranchId = getCurrentUserBranchId();
            } else if (hasCurrentUserPermission("LOAN_VIEW_OWN")) {
                userId = getCurrentUserId();
            } else {
                log.warn("User {} has no permission to view loans", currentUser.getUsername());
                return Page.empty(pageable);
            }
        }

        // Use branchId from parameter or user's branch
        Long effectiveBranchId = branchId != null ? branchId : userBranchId;

        // Get loans from repository
        Page<Loan> loansPage = findLoansByFilters(
                status, effectiveBranchId, borrowerId, loanProductId, fromDate, toDate, userId, pageable);

        // Convert to DTOs and enrich
        List<LoanDto> loanDtos = loansPage.getContent().stream()
                .map(loan -> {
                    LoanDto dto = loanMapper.toDto(loan);
                    enrichLoanDto(dto, loan);
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(loanDtos, pageable, loansPage.getTotalElements());
    }


    @Override
    public LoanRestructureResponseDto requestRestructure(Long loanId, LoanRestructureRequestDto request, User currentUser) {
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public LoanDto getLoanById(Long id) {
        log.debug("Fetching loan by ID: {}", id);

        User currentUser = getCurrentUser();

        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with ID: " + id));

        // Check permission
        checkLoanAccess(loan,currentUser);

        LoanDto dto = loanMapper.toDto(loan);
        enrichLoanDto(dto, loan);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public LoanDto getLoanByAccountNumber(String accountNumber, User currentUser) {
        log.debug("Fetching loan by account number: {}", accountNumber);

        Loan loan = loanRepository.findByLoanAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with account number: " + accountNumber));

        // Check permission
        checkLoanAccess(loan, currentUser);

        LoanDto dto = loanMapper.toDto(loan);
        enrichLoanDto(dto, loan);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanSummaryDto> getLoansByBorrower(Long borrowerId) {
        log.debug("Fetching loans for borrower: {}", borrowerId);

        // Check permission
        if (!hasCurrentUserPermission("LOAN_VIEW_ALL") &&
                !hasCurrentUserPermission("LOAN_VIEW_BRANCH") &&
                !hasCurrentUserPermission("LOAN_VIEW_OWN")) {
            throw new BusinessException("You don't have permission to view loans");
        }

        List<Loan> loans = loanRepository.findByBorrowerId(borrowerId);

        return loans.stream()
                .map(loanMapper::toSummaryDto)
                .collect(Collectors.toList());
    }



    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getLoanSummary(Long branchId) {
        log.debug("Fetching loan summary for branch: {}", branchId);

        Map<String, Object> summary = new HashMap<>();

        // Get counts by status
        Map<String, Long> countsByStatus = new HashMap<>();
        for (GeneralConfig.LoanStatus status : GeneralConfig.LoanStatus.values()) {
            Long count = countLoansByStatusAndBranch(status.name(), branchId);
            countsByStatus.put(status.name(), count != null ? count : 0L);
        }
        summary.put("countsByStatus", countsByStatus);

        // Get amounts by status
        Map<String, BigDecimal> amountsByStatus = new HashMap<>();
        for (GeneralConfig.LoanStatus status : GeneralConfig.LoanStatus.values()) {
            BigDecimal amount = calculateOutstandingByStatus(status.name(), branchId);
            amountsByStatus.put(status.name(), amount != null ? amount : BigDecimal.ZERO);
        }
        summary.put("amountsByStatus", amountsByStatus);

        // Total portfolio
        BigDecimal totalPortfolio = branchId != null ?
                loanRepository.sumOutstandingBalanceByBranch(branchId) :
                loanRepository.getTotalOutstandingPortfolio();
        summary.put("totalPortfolio", totalPortfolio != null ? totalPortfolio : BigDecimal.ZERO);

        // Branch-specific stats if branchId provided
        if (branchId != null) {
            Long activeCount = loanRepository.countActiveLoansByBranch(branchId);
            summary.put("activeLoansInBranch", activeCount != null ? activeCount : 0L);

            BigDecimal branchOutstanding = loanRepository.sumOutstandingBalanceByBranch(branchId);
            summary.put("branchOutstanding", branchOutstanding != null ? branchOutstanding : BigDecimal.ZERO);

            Long delinquentCount = loanRepository.countDelinquentLoansByBranch(branchId);
            summary.put("branchDelinquentCount", delinquentCount != null ? delinquentCount : 0L);
        }

        // Delinquency stats
        long delinquentCount = loanRepository.countDelinquentLoans();
        summary.put("totalDelinquentCount", delinquentCount);

        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepaymentScheduleDto> getRepaymentSchedule(Long loanId) {
        log.debug("Fetching repayment schedule for loan: {}", loanId);

        // Check if user can view this loan's details
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with ID: " + loanId));

        checkLoanAccess(loan, getCurrentUser());

        List<RepaymentSchedule> schedules = repaymentScheduleRepository
                .findByLoanIdOrderByInstallmentNumberAsc(loanId);

        if (schedules.isEmpty()) {
            throw new ResourceNotFoundException("Repayment schedule not found for loan ID: " + loanId);
        }

        return schedules.stream()
                .map(this::convertToScheduleDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RepaymentReceiptDto makeRepayment(Long loanId, RepaymentRequestDto request, User currentUser) {
        // DELEGATE to LoanRepaymentService to avoid duplication
        // This method should be removed or delegated as repayment operations belong to LoanRepaymentService
        throw new UnsupportedOperationException("Please use LoanRepaymentController for repayment operations");
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepaymentReceiptDto> getRepaymentHistory(Long loanId) {
        // DELEGATE to LoanRepaymentService
        throw new UnsupportedOperationException("Please use LoanRepaymentController for repayment history");
    }

    @Override
    public LoanRescheduleResponseDto requestReschedule(Long loanId, LoanRescheduleRequestDto request, User currentUser) {
        return null;
    }


    @Override
    @Transactional(readOnly = true)
    public List<LoanDocumentDto> getLoanDocuments(Long loanId) {
        log.debug("Fetching documents for loan: {}", loanId);

        List<LoanDocument> documents = loanDocumentRepository.findByLoanId(loanId);

        return documents.stream()
                .map(this::convertToDocumentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanAuditDto> getLoanAuditTrail(Long loanId) {
        log.debug("Fetching audit trail for loan: {}", loanId);

        List<LoanAudit> audits = loanAuditRepository.findByLoanIdOrderByPerformedAtDesc(loanId);

        return audits.stream()
                .map(this::convertToAuditDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioSummaryDto getPortfolioSummary(Long branchId, LocalDate asOfDate) {
        log.debug("Generating portfolio summary for branch: {}, as of: {}", branchId, asOfDate);

        LocalDate summaryDate = asOfDate != null ? asOfDate : LocalDate.now();

        // Convert to date range for the query
        LocalDateTime startOfDay = summaryDate.atStartOfDay();
        LocalDateTime endOfDay = summaryDate.atTime(LocalTime.MAX);

        log.debug("Querying with date range: {} to {}", startOfDay, endOfDay);

        // Get portfolio statistics using your repository method with date range
        PortfolioStats stats = loanRepository.getPortfolioStatistics(branchId, startOfDay, endOfDay);

        PortfolioSummaryDto summary = new PortfolioSummaryDto();
        summary.setAsOfDate(summaryDate);
        summary.setTotalActiveLoans(stats != null ? stats.getActiveLoans().intValue() : 0);
        summary.setTotalOutstanding(stats != null ? stats.getOutstandingPrincipal() : BigDecimal.ZERO);
        summary.setTotalPortfolioValue(stats != null ? stats.getTotalPortfolioValue() : BigDecimal.ZERO);
        summary.setDelinquentLoans(stats != null ? stats.getDelinquentLoans().intValue() : 0);
        summary.setLoansDisbursedThisMonth(stats != null ? stats.getLoansDisbursedThisMonth().intValue() : 0);
        summary.setAmountDisbursedThisMonth(stats != null ? stats.getAmountDisbursedThisMonth() : BigDecimal.ZERO);

        // Calculate PAR (Portfolio at Risk)
        if (summary.getTotalOutstanding() != null && summary.getTotalOutstanding().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal parAmount = calculatePortfolioAtRisk(branchId);
            summary.setPortfolioAtRisk(parAmount);

            BigDecimal parPercentage = parAmount.multiply(BigDecimal.valueOf(100))
                    .divide(summary.getTotalOutstanding(), 2, RoundingMode.HALF_UP);
            summary.setParPercentage(parPercentage);
        }

        return summary;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<LoanDto> getDelinquentLoans(Integer daysOverdue, Long branchId, Pageable pageable) {
        log.debug("Fetching delinquent loans with days overdue: {}, branch: {}", daysOverdue, branchId);

        Integer minDays = daysOverdue != null ? daysOverdue : 1;

        // You'll need to add this query method to your repository
        Page<Loan> loansPage = findDelinquentLoans(minDays, branchId, pageable);

        List<LoanDto> dtos = loansPage.getContent().stream()
                .map(loan -> {
                    LoanDto dto = loanMapper.toDto(loan);
                    enrichLoanDto(dto, loan);
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, loansPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalOutstanding(Long branchId) {
        if (branchId != null) {
            return loanRepository.sumOutstandingBalanceByBranch(branchId);
        } else {
            return loanRepository.getTotalOutstandingPortfolio();
        }
    }



    @Transactional
    @Override
    public LoanDto assignCollectionOfficer(Long loanId, Long officerId, User currentUser) {
        log.debug("Assigning collection officer {} to loan {}", officerId, loanId);

        // Find the loan
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with ID: " + loanId));

        // Find the officer
        User officer = userService.getUserById(officerId);
        if (officer == null) {
            throw new ResourceNotFoundException("Officer not found with ID: " + officerId);
        }

        // Verify officer has COLLECTION_OFFICER role
        if (officer.getRole() != User.UserRole.COLLECTION_OFFICER &&
                officer.getRole() != User.UserRole.SUPER_ADMIN &&
                officer.getRole() != User.UserRole.BRANCH_MANAGER) {
            throw new BusinessException("User is not a collection officer");
        }

        // Assign the officer
        loan.setLoanOfficer(officer);

        // Update audit fields
        loan.setUpdatedBy(currentUser.getId());
        loan.setUpdatedAt(LocalDateTime.now());

        Loan savedLoan = loanRepository.save(loan);

        // Log the assignment
        log.info("Collection officer {} assigned to loan {} by user {}",
                officer.getUsername(), loan.getLoanAccountNumber(), currentUser.getUsername());

        return loanMapper.toDto(savedLoan);
    }

    @Transactional
    @Override
    public LoanDto unassignCollectionOfficer(Long loanId, User currentUser) {
        log.debug("Unassigning collection officer from loan {}", loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with ID: " + loanId));

        loan.setLoanOfficer(null);
        loan.setUpdatedBy(currentUser.getId());
        loan.setUpdatedAt(LocalDateTime.now());

        Loan savedLoan = loanRepository.save(loan);

        log.info("Collection officer unassigned from loan {} by user {}",
                loan.getLoanAccountNumber(), currentUser.getUsername());

        return loanMapper.toDto(savedLoan);
    }

    @Transactional
    @Override
    public BulkAssignResultDto bulkAssignCollectionOfficers(List<Long> loanIds, Long officerId, String notes, User currentUser) {
        log.debug("Bulk assigning collection officer {} to {} loans", officerId, loanIds.size());

        List<AssignmentResultDto> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;

        // Find the officer once
        User officer = userService.getUserById(officerId);
        if (officer == null) {
            throw new ResourceNotFoundException("Officer not found with ID: " + officerId);
        }

        // Verify officer has appropriate role
        if (officer.getRole() != User.UserRole.COLLECTION_OFFICER &&
                officer.getRole() != User.UserRole.SUPER_ADMIN &&
                officer.getRole() != User.UserRole.BRANCH_MANAGER) {
            throw new BusinessException("User is not a collection officer");
        }

        for (Long loanId : loanIds) {
            try {
                Optional<Loan> loanOpt = loanRepository.findById(loanId);
                if (loanOpt.isPresent()) {
                    Loan loan = loanOpt.get();
                    loan.setLoanOfficer(officer);
                    loan.setUpdatedBy(currentUser.getId());
                    loan.setUpdatedAt(LocalDateTime.now());
                    loanRepository.save(loan);

                    results.add(AssignmentResultDto.builder()
                            .loanId(loanId)
                            .loanAccountNumber(loan.getLoanAccountNumber())
                            .borrowerName(loan.getBorrower() != null ? loan.getBorrower().getFullName() : "Unknown")
                            .success(true)
                            .build());
                    successful++;
                } else {
                    results.add(AssignmentResultDto.builder()
                            .loanId(loanId)
                            .success(false)
                            .errorMessage("Loan not found")
                            .build());
                    failed++;
                }
            } catch (Exception e) {
                log.error("Error assigning officer to loan {}: {}", loanId, e.getMessage());
                results.add(AssignmentResultDto.builder()
                        .loanId(loanId)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
                failed++;
            }
        }

        log.info("Bulk assignment completed. Successful: {}, Failed: {}", successful, failed);

        return BulkAssignResultDto.builder()
                .totalProcessed(loanIds.size())
                .successful(successful)
                .failed(failed)
                .results(results)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<LoanDto> getLoansByCollectionOfficer(Long officerId, String status, Pageable pageable) {
        log.debug("Fetching loans assigned to collection officer: {}", officerId);

        Page<Loan> loansPage;

        if (status != null && !status.isEmpty()) {
            loansPage = loanRepository.findByLoanOfficerIdAndStatus(officerId, GeneralConfig.LoanStatus.valueOf(status), pageable);
        } else {
            loansPage = loanRepository.findByLoanOfficerId(officerId, pageable);
        }

        return loansPage.map(loan -> {
            LoanDto dto = loanMapper.toDto(loan);
            enrichLoanDto(dto, loan);
            return dto;
        });
    }


    // ==================== PRIVATE HELPER METHODS ====================

    private Page<Loan> findLoansByFilters(String status, Long branchId, Long borrowerId,
                                          Long loanProductId, LocalDate fromDate, LocalDate toDate,
                                          Long userId, Pageable pageable) {
        // Implement filtering logic - you may need to add a custom query in your repository
        // For now, return all and filter in memory (not efficient for production)
        List<Loan> allLoans = loanRepository.findAll();

        List<Loan> filteredLoans = allLoans.stream()
                .filter(loan -> status == null || loan.getStatus().name().equals(status))
                .filter(loan -> branchId == null || (loan.getBranch() != null && loan.getBranch().getId().equals(branchId)))
                .filter(loan -> borrowerId == null || (loan.getBorrower() != null && loan.getBorrower().getId().equals(borrowerId)))
                .filter(loan -> loanProductId == null || (loan.getLoanProduct() != null && loan.getLoanProduct().getId().equals(loanProductId)))
                .filter(loan -> fromDate == null || (loan.getDisbursementDate() != null && !loan.getDisbursementDate().isBefore(fromDate)))
                .filter(loan -> toDate == null || (loan.getDisbursementDate() != null && !loan.getDisbursementDate().isAfter(toDate)))
                .filter(loan -> userId == null || loan.getCreatedBy().equals(userId))
                .collect(Collectors.toList());

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredLoans.size());

        List<Loan> pageContent = start < filteredLoans.size() ?
                filteredLoans.subList(start, end) :
                new ArrayList<>();

        return new PageImpl<>(pageContent, pageable, filteredLoans.size());
    }

    private Page<Loan> findDelinquentLoans(Integer minDays, Long branchId, Pageable pageable) {
        List<Loan> allLoans = branchId != null ?
                loanRepository.findByBranchId(branchId) :
                loanRepository.findAll();

        List<Loan> delinquentLoans = allLoans.stream()
                .filter(loan -> loan.getDaysDelinquent() != null && loan.getDaysDelinquent() >= minDays)
                .sorted((l1, l2) -> l2.getDaysDelinquent().compareTo(l1.getDaysDelinquent()))
                .collect(Collectors.toList());

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), delinquentLoans.size());

        List<Loan> pageContent = start < delinquentLoans.size() ?
                delinquentLoans.subList(start, end) :
                new ArrayList<>();

        return new PageImpl<>(pageContent, pageable, delinquentLoans.size());
    }



    private void enrichLoanDto(LoanDto dto, Loan loan) {
        // Calculate total interest
        dto.setTotalInterestDue(loan.getTotalDue().subtract(loan.getPrincipalAmount()));

        // FIXED: Get next due installment - now works correctly
        Optional<RepaymentSchedule> nextDue = repaymentScheduleRepository
                .findNextDueByLoanId(loan.getId(), LocalDate.now());

        if (nextDue.isPresent()) {
            dto.setNextDueDate(nextDue.get().getDueDate());
            dto.setNextDueAmount(nextDue.get().getTotalDue());
        } else {
            log.debug("No next due installment found for loan: {}", loan.getId());
        }

        // Calculate progress
        int totalInstallments = loan.getTenureMonths();
        long paidInstallments = repaymentScheduleRepository.countPaidInstallments(loan.getId());
        dto.setTotalInstallments(totalInstallments);
        dto.setInstallmentsPaid((int) paidInstallments);

        if (totalInstallments > 0) {
            double progress = (paidInstallments * 100.0) / totalInstallments;
            dto.setProgressPercentage(Math.min(progress, 100.0));
        }

        // Calculate total arrears
        BigDecimal totalArrears = repaymentScheduleRepository.calculateTotalArrears(loan.getId(), LocalDate.now());
        dto.setTotalArrears(totalArrears != null ? totalArrears : BigDecimal.ZERO);

        // Get upcoming installments (next 5) - using Pageable
        List<RepaymentSchedule> upcoming = repaymentScheduleRepository
                .findUpcomingByLoanId(
                        loan.getId(),
                        LocalDate.now(),
                        PageRequest.of(0, 5)
                );

        dto.setUpcomingInstallments(upcoming.stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList()));

        // Get last payment
        Optional<LoanRepayment> lastPayment = repaymentRepository.findTopByLoanIdOrderByPaymentDateDesc(loan.getId());
        lastPayment.ifPresent(repayment -> dto.setLastPayment(convertToSummaryDto(repayment)));
    }




    @Override
    @Transactional(readOnly = true)
    public List<LoanEligibleForRepaymentDto> getLoansEligibleForRepayment(Long branchId, Long loanProductId, String status, User currentUser) {
        log.debug("Fetching loans eligible for repayment - branch: {}, product: {}, status: {}",
                branchId, loanProductId, status);

        // Determine which loans the user can access based on permissions
        List<Loan> eligibleLoans;

        if (isSuperAdmin(currentUser) || hasCurrentUserPermission("LOAN_VIEW_ALL")) {
            // User can see all loans
            if (branchId != null) {
                eligibleLoans = loanRepository.findByBranchId(branchId);
            } else {
                eligibleLoans = loanRepository.findAll();
            }
        } else if (isSuperAdmin(currentUser) || hasCurrentUserPermission("LOAN_VIEW_BRANCH")) {
            // User can only see loans in their branch
            Long userBranchId = currentUser.getBranchId();
            if (userBranchId == null) {
                log.warn("User {} has no branch assigned but has LOAN_VIEW_BRANCH permission", currentUser.getUsername());
                return Collections.emptyList();
            }

            // If branch filter is provided, ensure it matches user's branch
            if (branchId != null && !branchId.equals(userBranchId)) {
                log.warn("User {} attempted to access branch {} but only has access to branch {}",
                        currentUser.getUsername(), branchId, userBranchId);
                return Collections.emptyList();
            }

            eligibleLoans = loanRepository.findByBranchId(userBranchId);
        } else if (isSuperAdmin(currentUser) || hasCurrentUserPermission("LOAN_VIEW_OWN")) {
            // User can only see loans they created
            eligibleLoans = loanRepository.findByCreatedBy(currentUser.getId());
        } else {
            log.warn("User {} has no permission to view loans", currentUser.getUsername());
            return Collections.emptyList();
        }

        // Filter for loans that are eligible for repayment (active/overdue/disbursed)
        List<String> eligibleStatuses = Arrays.asList(
                GeneralConfig.LoanStatus.ACTIVE.name(),
                GeneralConfig.LoanStatus.OVERDUE.name(),
                GeneralConfig.LoanStatus.DISBURSED.name()
        );

        // Apply filters and transform to DTO
        return eligibleLoans.stream()
                .filter(loan -> eligibleStatuses.contains(loan.getStatus().name()))
                .filter(loan -> loanProductId == null ||
                        (loan.getLoanProduct() != null && loan.getLoanProduct().getId().equals(loanProductId)))
                .filter(loan -> status == null ||
                        (status.equals("OVERDUE") && loan.getDaysDelinquent() > 0) ||
                        (status.equals("ACTIVE") && loan.getDaysDelinquent() == 0))
                .map(this::convertToEligibleForRepaymentDto)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public Page<OverdueLoanDto> getOverdueLoans(LocalDate date, Long branchId, Long loanOfficerId,
                                                Integer minDaysOverdue, Integer maxDaysOverdue,
                                                User currentUser, Pageable pageable) {
        log.debug("Fetching overdue loans - date: {}, branch: {}, officer: {}, minDays: {}, maxDays: {}",
                date, branchId, loanOfficerId, minDaysOverdue, maxDaysOverdue);

        // Apply permission-based filtering
        Long effectiveBranchId = branchId;
        Long effectiveOfficerId = loanOfficerId;

        // Check if user is SUPER_ADMIN first (always has access)
        boolean isSuperAdmin = currentUser.getRole() == User.UserRole.SUPER_ADMIN;

        if (isSuperAdmin) {
            // Super admin can see all loans with any filters
            log.debug("Super admin accessing overdue loans");
        } else if (hasCurrentUserPermission("LOAN_VIEW_ALL")) {
            // User has VIEW_ALL permission
            log.debug("User has LOAN_VIEW_ALL permission");
        } else if (hasCurrentUserPermission("LOAN_VIEW_BRANCH")) {
            // User can only see loans in their branch
            Long userBranchId = currentUser.getBranchId();
            if (userBranchId == null) {
                log.warn("User has LOAN_VIEW_BRANCH but no branch assigned");
                return Page.empty(pageable);
            }

            // If branch filter is provided, ensure it matches user's branch
            if (branchId != null && !branchId.equals(userBranchId)) {
                log.warn("User attempted to access branch {} but only has access to branch {}",
                        branchId, userBranchId);
                return Page.empty(pageable);
            }

            effectiveBranchId = userBranchId;
            log.debug("Filtering by user's branch: {}", userBranchId);
        } else if (hasCurrentUserPermission("LOAN_VIEW_OWN")) {
            // User can only see loans they created or are assigned to
            effectiveOfficerId = currentUser.getId();
            log.debug("Filtering by user's own loans: {}", effectiveOfficerId);
        } else {
            log.warn("User {} has no permission to view loans", currentUser.getUsername());
            return Page.empty(pageable);
        }

        // Use default min days if not specified
        Integer minDays = minDaysOverdue != null ? minDaysOverdue : 1;

        // Fetch overdue loans from repository
        Page<Loan> loansPage = loanRepository.findOverdueLoansTest(
                effectiveBranchId, effectiveOfficerId, minDays, maxDaysOverdue, pageable);

        log.debug("Found {} overdue loans", loansPage.getNumberOfElements());

        // Convert to DTOs
        List<OverdueLoanDto> dtos = loansPage.getContent().stream()
                .map(this::convertToOverdueLoanDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, loansPage.getTotalElements());
    }


    private OverdueLoanDto convertToOverdueLoanDto(Loan loan) {
        OverdueLoanDto.OverdueLoanDtoBuilder builder = OverdueLoanDto.builder()
                .id(loan.getId())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .daysOverdue(loan.getDaysDelinquent() != null ? loan.getDaysDelinquent() : 0)
                .outstandingBalance(loan.getOutstandingBalance()) // Add this
                .totalLoanAmount(loan.getPrincipalAmount()); // Add this

        // Borrower information
        if (loan.getBorrower() != null) {
            builder.borrowerId(loan.getBorrower().getId())
                    .borrowerName(loan.getBorrower().getFullName())
                    .borrowerIdNumber(loan.getBorrower().getIdNumber())
                    .borrowerPhone(loan.getBorrower().getPhoneNumber())
                    .alternatePhone(loan.getBorrower().getAlternatePhone())
                    .email(loan.getBorrower().getEmail())
                    .address(loan.getBorrower().getPhysicalAddress());
        }

        // Branch information
        if (loan.getBranch() != null) {
            builder.branchId(loan.getBranch().getId())
                    .branchName(loan.getBranch().getName());
        }

        // Loan officer information
        if (loan.getLoanOfficer() != null) {
            builder.loanOfficerId(loan.getLoanOfficer().getId())
                    .loanOfficerName(loan.getLoanOfficer().getFirstName() + " " +
                            (loan.getLoanOfficer().getLastName() != null ? loan.getLoanOfficer().getLastName() : ""));
        }

        // Loan product information
        if (loan.getLoanProduct() != null) {
            builder.loanProductId(loan.getLoanProduct().getId())
                    .loanProductName(loan.getLoanProduct().getName());
        }

        // Calculate overdue amount from repayment schedules
        BigDecimal totalOverdue = BigDecimal.ZERO;
        BigDecimal principalOverdue = BigDecimal.ZERO;
        BigDecimal interestOverdue = BigDecimal.ZERO;
        BigDecimal penaltyOverdue = BigDecimal.ZERO;
        LocalDate earliestDueDate = null;

        List<RepaymentSchedule> overdueSchedules = repaymentScheduleRepository
                .findOverdueByLoanId(loan.getId(), LocalDate.now());

        for (RepaymentSchedule schedule : overdueSchedules) {
            BigDecimal scheduleOverdue = schedule.getOutstandingAmount() != null ?
                    schedule.getOutstandingAmount() : BigDecimal.ZERO;
            totalOverdue = totalOverdue.add(scheduleOverdue);

            // Estimate principal/interest split (you may need to adjust based on your data model)
            if (schedule.getPrincipalDue() != null) {
                principalOverdue = principalOverdue.add(schedule.getPrincipalDue());
            }
            if (schedule.getInterestDue() != null) {
                interestOverdue = interestOverdue.add(schedule.getInterestDue());
            }

            if (earliestDueDate == null || (schedule.getDueDate() != null &&
                    schedule.getDueDate().isBefore(earliestDueDate))) {
                earliestDueDate = schedule.getDueDate();
            }
        }

        builder.overdueAmount(totalOverdue)
                .principalOverdue(principalOverdue)
                .interestOverdue(interestOverdue)
                .penaltyOverdue(penaltyOverdue)
                .dueDate(earliestDueDate);

        // Get last payment date
        Optional<LoanRepayment> lastPayment = repaymentRepository
                .findTopByLoanIdOrderByPaymentDateDesc(loan.getId());
        lastPayment.ifPresent(repayment -> builder.lastPaymentDate(repayment.getPaymentDate()));

        // Determine risk level based on days overdue
        if (loan.getDaysDelinquent() != null) {
            if (loan.getDaysDelinquent() <= 7) {
                builder.riskLevel("LOW");
                builder.collectionStage("NEW");
            } else if (loan.getDaysDelinquent() <= 30) {
                builder.riskLevel("MEDIUM");
                builder.collectionStage("FOLLOW_UP");
            } else if (loan.getDaysDelinquent() <= 90) {
                builder.riskLevel("HIGH");
                builder.collectionStage("ESCALATED");
            } else {
                builder.riskLevel("CRITICAL");
                builder.collectionStage("LEGAL");
            }
        }

        return builder.build();
    }




    @Transactional(readOnly = true)
    @Override
    public List<LoanEligibleForRecoveryDto> getLoansEligibleForRecovery(User currentUser) {
        log.debug("Fetching loans eligible for recovery");

        // Get overdue loans that are not already in recovery
        // First, get all recovery case loan IDs
        List<Long> recoveryLoanIds = recoveryCaseRepository.findAllLoanIdsWithActiveCases();

        // Get overdue loans (daysDelinquent > 0) that are not in recovery
        List<Loan> eligibleLoans = loanRepository.findOverdueLoansNotInRecoveryTest(recoveryLoanIds);

        return eligibleLoans.stream()
                .map(this::convertToEligibleForRecoveryDto)
                .collect(Collectors.toList());
    }

    private LoanEligibleForRecoveryDto convertToEligibleForRecoveryDto(Loan loan) {
        // Calculate recovery attempts (count of previous recovery cases)
        int recoveryAttempts = recoveryCaseRepository.countByLoanId(loan.getId());

        // Get last contact date from collection actions
        LocalDate lastContactDate = collectionActionRepository.findLastContactDateByLoanId(loan.getId())
                .orElse(null);

        return LoanEligibleForRecoveryDto.builder()
                .id(loan.getId())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .borrowerName(loan.getBorrower() != null ? loan.getBorrower().getFullName() : "Unknown")
                .borrowerPhone(loan.getBorrower() != null ? loan.getBorrower().getPhoneNumber() : null)
                .outstandingBalance(loan.getOutstandingBalance())
                .daysOverdue(loan.getDaysDelinquent() != null ? loan.getDaysDelinquent() : 0)
                .lastPaymentDate(getLastPaymentDate(loan))
                .lastContactDate(lastContactDate)
                .recoveryAttempts(recoveryAttempts)
                .build();
    }

    private LocalDate getLastPaymentDate(Loan loan) {
        return loan.getRepayments().stream()
                .map(LoanRepayment::getPaymentDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }



    private void checkLoanAccess(Loan loan, User currentUser) {
        if (!canAccessLoan(loan, currentUser)) {
            throw new BusinessException("You don't have permission to view this loan");
        }
    }

    private boolean canAccessLoan(Loan loan, User user) {

        // Check if user is SUPER_ADMIN first (always has access)
        if (user.getRole() == User.UserRole.SUPER_ADMIN) {
            return true;
        }

        if (hasCurrentUserPermission("LOAN_VIEW_ALL")) {
            return true;
        }

        if (hasCurrentUserPermission("LOAN_VIEW_BRANCH")) {
            return loan.getBranch() != null &&
                    loan.getBranch().getId().equals(user.getBranchId());
        }

        if (hasCurrentUserPermission("LOAN_VIEW_OWN")) {
            return loan.getCreatedBy().equals(user.getId());
        }

        return false;
    }


    private Long countLoansByStatusAndBranch(String status, Long branchId) {
        // This would need a custom query in your repository
        // For now, return 0
        return 0L;
    }

    private BigDecimal calculateOutstandingByStatus(String status, Long branchId) {
        // This would need a custom query in your repository
        // For now, return BigDecimal.ZERO
        return BigDecimal.ZERO;
    }

    private BigDecimal calculatePortfolioAtRisk(Long branchId) {
        // This would need a custom query in your repository
        // For now, return BigDecimal.ZERO
        return BigDecimal.ZERO;
    }

    // ==================== CURRENT USER HELPER METHODS ====================

    /**
     * Get the current authenticated user
     */
    private User getCurrentUser() {
        Long currentUserId = securityUtils.getCurrentUserId();
        return userService.getUserById(currentUserId);
    }

    /**
     * Check if current user has a specific permission
     */
    private boolean hasCurrentUserPermission(String permission) {
        if (permission == null) {
            return false;
        }

        try {
            User currentUser = getCurrentUser();

            if (currentUser == null) {
                log.warn("No current user found");
                return false;
            }
            User.UserRole userRole = currentUser.getRole();

            if (userRole == null) {
                log.warn("User {} has no role assigned", currentUser.getUsername());
                return false;
            }
            if(userRole == User.UserRole.SUPER_ADMIN){
                return true;
            }

            boolean hasPermission = rolePermissionRepository.existsByRoleAndPermission(userRole, permission);

            log.debug("Current user {} with role {} has permission {}: {}",
                    currentUser.getUsername(), userRole, permission, hasPermission);

            return hasPermission;

        } catch (Exception e) {
            log.error("Error checking permission {}: {}", permission, e.getMessage());
            return false;
        }
    }

    /**
     * Get current user's branch ID
     */
    private Long getCurrentUserBranchId() {
        try {
            User currentUser = getCurrentUser();
            return currentUser.getBranchId() != null ? currentUser.getBranchId() : null;
        } catch (Exception e) {
            log.error("Error getting current user branch", e);
            return null;
        }
    }

    /**
     * Get current user's role
     */
    private User.UserRole getCurrentUserRole() {
        try {
            User currentUser = getCurrentUser();
            return currentUser.getRole();
        } catch (Exception e) {
            log.error("Error getting current user role", e);
            return null;
        }
    }

    /**
     * Get current user's ID
     */
    private Long getCurrentUserId() {
        try {
            User currentUser = getCurrentUser();
            return currentUser.getId();
        } catch (Exception e) {
            log.error("Error getting current user ID", e);
            return null;
        }
    }


    private boolean isSuperAdmin(User user) {
        return user != null && user.getRole() == User.UserRole.SUPER_ADMIN;
    }

    private boolean isBranchManager(User user) {
        return user != null && user.getRole() == User.UserRole.BRANCH_MANAGER;
    }

    private boolean isLoanOfficer(User user) {
        return user != null && user.getRole() == User.UserRole.LOAN_OFFICER;
    }


    // ==================== CONVERSION METHODS ====================

    private LoanSummaryDto convertToSummaryDto(Loan loan) {
        Optional<RepaymentSchedule> nextDue = repaymentScheduleRepository
                .findNextDueByLoanId(loan.getId(), LocalDate.now());

        return LoanSummaryDto.builder()
                .id(loan.getId())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .principalAmount(loan.getPrincipalAmount())
                .outstandingBalance(loan.getOutstandingBalance())
                .status(loan.getStatus() != null ? loan.getStatus().name() : null)
                .disbursementDate(loan.getDisbursementDate())
                .nextDueDate(nextDue.map(RepaymentSchedule::getDueDate).orElse(null))
                .nextDueAmount(nextDue.map(RepaymentSchedule::getTotalDue).orElse(BigDecimal.ZERO))
                .daysDelinquent(loan.getDaysDelinquent())
                .isDelinquent(loan.getDaysDelinquent() != null && loan.getDaysDelinquent() > 0)
                .progressPercentage(calculateProgress(loan))
                .build();
    }

    private Integer calculateProgress(Loan loan) {
        int totalInstallments = loan.getTenureMonths();
        long paidInstallments = repaymentScheduleRepository.countPaidInstallments(loan.getId());
        return totalInstallments > 0 ? (int) ((paidInstallments * 100) / totalInstallments) : 0;
    }

    private RepaymentScheduleDto convertToScheduleDto(RepaymentSchedule schedule) {
        return RepaymentScheduleDto.builder()
                .id(schedule.getId())
                .installmentNumber(schedule.getInstallmentNumber())
                .dueDate(schedule.getDueDate())
                .principalAmount(schedule.getPrincipalAmount())
                .interestAmount(schedule.getInterestAmount())
                .totalDue(schedule.getTotalDue())
                .paidAmount(schedule.getPaidAmount())
                .outstandingAmount(schedule.getOutstandingAmount())
                .status(schedule.getStatus() != null ? schedule.getStatus().name() : null)
                .isOverdue(schedule.isOverdue())
                .paidDate(schedule.getPaidDate())
                .build();
    }

    private RepaymentScheduleSummaryDto convertToSummaryDto(RepaymentSchedule schedule) {
        return RepaymentScheduleSummaryDto.builder()
                .id(schedule.getId())
                .installmentNumber(schedule.getInstallmentNumber())
                .dueDate(schedule.getDueDate())
                .dueAmount(schedule.getTotalDue())
                .paidAmount(schedule.getPaidAmount())
                .outstandingAmount(schedule.getOutstandingAmount())
                .status(schedule.getStatus() != null ? schedule.getStatus().name() : null)
                .isOverdue(schedule.isOverdue())
                .paidDate(schedule.getPaidDate())
                .build();
    }

    private RepaymentScheduleSummaryDto convertToSummaryDto(LoanRepayment repayment) {
        return RepaymentScheduleSummaryDto.builder()
                .id(repayment.getId())
                .dueDate(repayment.getPaymentDate())
                .dueAmount(repayment.getAmountPaid())
                .paidAmount(repayment.getAmountPaid())
                .status("PAID")
                .paidDate(repayment.getPaymentDate())
                .build();
    }

    private LoanDocumentDto convertToDocumentDto(LoanDocument document) {
        return LoanDocumentDto.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .documentName(document.getDocumentName())
                .filePath(document.getFilePath())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .uploadedAt(document.getUploadedAt())
                .uploadedBy(document.getUploadedBy() != null ? document.getUploadedBy().getUsername() : null)
                .isVerified(document.getIsVerified())
                .verifiedAt(document.getVerifiedAt())
                .verifiedBy(document.getVerifiedBy() != null ? document.getVerifiedBy().getUsername() : null)
                .build();
    }

    private LoanAuditDto convertToAuditDto(LoanAudit audit) {
        return LoanAuditDto.builder()
                .id(audit.getId())
                .action(audit.getAction())
                .entityType(audit.getEntityType())
                .entityId(audit.getEntityId())
                .fieldName(audit.getFieldName())
                .oldValue(audit.getOldValue())
                .newValue(audit.getNewValue())
                .performedBy(audit.getPerformedBy() != null ? audit.getPerformedBy().getUsername() : null)
                .performedAt(audit.getPerformedAt())
                .ipAddress(audit.getIpAddress())
                .details(audit.getDetails())
                .build();
    }


    private LoanEligibleForRepaymentDto convertToEligibleForRepaymentDto(Loan loan) {
        LoanEligibleForRepaymentDto dto = new LoanEligibleForRepaymentDto();

        dto.setId(loan.getId());
        dto.setLoanNumber(loan.getLoanAccountNumber());

        // Borrower information
        if (loan.getBorrower() != null) {
            dto.setBorrowerId(loan.getBorrower().getId());
            dto.setBorrowerName(loan.getBorrower().getFullName());
            dto.setBorrowerIdNumber(loan.getBorrower().getIdNumber());
        }

        // Branch information
        if (loan.getBranch() != null) {
            dto.setBranchId(loan.getBranch().getId());
            dto.setBranchName(loan.getBranch().getName());
        }

        // Loan product information
        if (loan.getLoanProduct() != null) {
            dto.setLoanProductId(loan.getLoanProduct().getId());
            dto.setLoanProductName(loan.getLoanProduct().getName());
        }

        dto.setOutstandingBalance(loan.getOutstandingBalance() != null ?
                loan.getOutstandingBalance() : BigDecimal.ZERO);
        dto.setDaysOverdue(loan.getDaysDelinquent() != null ? loan.getDaysDelinquent() : 0);
        dto.setStatus(loan.getStatus() != null ? loan.getStatus().name() : null);

        // Get next payment information from repayment schedule
        Optional<RepaymentSchedule> nextDue = repaymentScheduleRepository
                .findNextDueByLoanId(loan.getId(), LocalDate.now());

        if (nextDue.isPresent()) {
            dto.setNextPaymentDate(nextDue.get().getDueDate().toString());
            dto.setNextPaymentAmount(nextDue.get().getTotalDue());
        } else {
            dto.setNextPaymentAmount(BigDecimal.ZERO);
        }

        return dto;
    }

}