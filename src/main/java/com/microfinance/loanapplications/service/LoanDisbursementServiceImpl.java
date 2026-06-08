package com.microfinance.loanapplications.service;

import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;

import com.microfinance.audit.service.AuditService;
import com.microfinance.audit.service.AuditServiceImpl;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.common.service.NotificationService;
import com.microfinance.integrations.service.FinancialIntegrationService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.microfinance.loanapplications.dto.LoanDto;
import com.microfinance.loanapplications.dto.RescheduleRequestDto;
import com.microfinance.loanapplications.dto.application.PortfolioStats;
import com.microfinance.loanapplications.dto.disbursement.*;
import com.microfinance.loanapplications.entity.*;
import com.microfinance.loanapplications.mapper.EfficientLoanMapper;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanapplications.repository.LoanApplicationRepository;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import com.microfinance.base.entity.User;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.BusinessException;
import com.microfinance.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoanDisbursementServiceImpl implements LoanDisbursementService {
    
    private final LoanRepository loanRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final EfficientLoanMapper loanMapper;

    private final SecurityUtils securityUtils;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final FinancialIntegrationService financialIntegrationService;
    @Autowired
    private final AuditService auditService;
    private final NotificationService notificationService;

    private final RepaymentScheduleGenerationService scheduleGenerationService;


    @Override
    public LoanDto disburseLoan(Long loanId, DisburseLoanDto dto, User currentUser) {
        log.info("Disbursing loan {} by user: {}", loanId, currentUser.getUsername());

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        LoanApplication loanApp = loanApplicationRepository.findById(loan.getLoanApplication().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan Application not found"));

        // Validate loan can be disbursed
        if (!canDisburseLoan(loanId)) {
            throw new BusinessException("Loan cannot be disbursed. Current status: " + loan.getStatus());
        }

        // Validate disbursement amount
        validateDisbursementAmount(loan, dto);

        // Update loan status and disbursement details
        loan.setStatus(GeneralConfig.LoanStatus.ACTIVE);
        loan.setDisbursementDate(dto.getDisbursementDate());
        loan.setDisbursedBy(currentUser);
        loan.setBranch(loanApp.getBranch());
        loan.setLoanProduct(loanApp.getLoanProduct());

        // Calculate net disbursement amount after deductions
        BigDecimal netAmount = calculateNetDisbursementAmount(loan.getPrincipalAmount(), dto);
        loan.setNetDisbursementAmount(netAmount);
        loan.setDisbursementMethod(dto.getDisbursementMethod());
        loan.setTransactionReference(dto.getTransactionReference());
        loan.setDisbursementNotes(dto.getDisbursementNotes());

        // Set maturity date
        loan.setMaturityDate(calculateMaturityDate(dto.getDisbursementDate(), loan.getTenureMonths()));
        // Generate/update repayment schedule using centralized service
        // This will update the existing schedules with actual disbursement date
        scheduleGenerationService.generateOrUpdateRepaymentSchedule(loan, currentUser);

        Loan savedLoan = loanRepository.save(loan);

        // Update associated loan application
        if (loan.getLoanApplication() != null) {
            LoanApplication application = loan.getLoanApplication();
            application.setStatus(GeneralConfig.LoanApplicationStatus.DISBURSED);
            application.setDisbursedDate(LocalDateTime.now());
            loanApplicationRepository.save(application);
        }

        log.info("Loan {} successfully disbursed. Net amount: {}", loanId, netAmount);

        // Integrate Financials and Log
        if (savedLoan != null && savedLoan.getId() != null) {
            auditService.logDisbursementAction(loan.getId(), currentUser.getId(), loan.getNetDisbursementAmount());

            //Audit Section
            Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
            String createdByName ="";
            Long createdById=null;
            if(currentUser1.isPresent()){
                createdByName=currentUser1.get().getFullName();
                createdById=currentUser1.get().getId();
            }
            if (Objects.nonNull(savedLoan.getId())) {
                auditService.masterAuditLogs(
                        savedLoan.getBorrower().getId(),
                        GeneralConfig.BorrowerActivityType.LOAN_DISBURSED,
                        "LOAN_DISBURSEMENT",
                        "Loan of ID:"+savedLoan.getId()+" Loan No:"+savedLoan.getLoanAccountNumber()+  " has been DISBURSED by:"+createdByName+"-"+createdById
                );
            }
            //End Audit Section
            financialIntegrationService.recordLoanDisbursement(loan, loan.getNetDisbursementAmount(), currentUser);

            //Send Disrbursement Notification Email
            notificationService.sendDisbursementNotification(
                    savedLoan.getBorrower().getEmail(),
                    savedLoan.getLoanAccountNumber(),
                    savedLoan.getNetDisbursementAmount()
                    );
            //InApp Notification
            notificationService.createLoanDisbursedNotification(
              savedLoan.getId(),
              savedLoan.getLoanAccountNumber(),
              savedLoan.getBorrower().getId()
            );
        }


        return loanMapper.toDto(savedLoan);
    }


    public LoanDto disburseLoanORG(Long loanId, DisburseLoanDto dto, User currentUser) {
        log.info("Disbursing loan {} by user: {}", loanId, currentUser.getUsername());
        
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        LoanApplication loanApp = loanApplicationRepository.findById(loan.getLoanApplication().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan Application not found with id: " + loan.getLoanApplication().getId()));
        
        // Validate loan can be disbursed
        if (!canDisburseLoan(loanId)) {
            throw new BusinessException("Loan cannot be disbursed. Current status: " + loan.getStatus());
        }
        
        // Validate disbursement amount
        validateDisbursementAmount(loan, dto);
        
        // Update loan status and disbursement details
        loan.setStatus(GeneralConfig.LoanStatus.ACTIVE);
        loan.setDisbursementDate(dto.getDisbursementDate());
        loan.setDisbursedBy(currentUser);
        loan.setBranch(loanApp.getBranch());
        loan.setLoanProduct(loanApp.getLoanProduct());

        
        // Calculate net disbursement amount after deductions
        BigDecimal netAmount = calculateNetDisbursementAmount(loan.getPrincipalAmount(), dto);
        loan.setNetDisbursementAmount(netAmount);
        loan.setDisbursementMethod(dto.getDisbursementMethod());
        loan.setTransactionReference(dto.getTransactionReference());
        loan.setDisbursementNotes(dto.getDisbursementNotes());
        
        // Set maturity date
        loan.setMaturityDate(calculateMaturityDate(dto.getDisbursementDate(), loan.getTenureMonths()));
        
        // Generate repayment schedule
        generateRepaymentSchedule(loan,currentUser);
        
        Loan savedLoan = loanRepository.save(loan);
        
        // Update associated loan application
        if (loan.getLoanApplication() != null) {
            LoanApplication application = loan.getLoanApplication();
            application.setStatus(GeneralConfig.LoanApplicationStatus.DISBURSED);
            application.setDisbursedDate(LocalDateTime.now());
            loanApplicationRepository.save(application);
        }
        log.info("Loan {} successfully disbursed. Net amount: {}", loanId, netAmount);

        //Integrate Financials and Log as well
        if (savedLoan != null && savedLoan.getId() != null) {
            auditService.logDisbursementAction(loan.getId(),currentUser.getId(),loan.getNetDisbursementAmount());
            financialIntegrationService.recordLoanDisbursement(loan, loan.getNetDisbursementAmount(), currentUser);
        }

        return loanMapper.toDto(savedLoan);
    }




    
    @Override
    public DisbursementReceiptDto generateDisbursementReceipt(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));
        
        if (loan.getDisbursementDate() == null) {
            throw new BusinessException("Loan has not been disbursed yet");
        }
        
        return DisbursementReceiptDto.builder()
                .receiptNumber(generateReceiptNumber())
                .receiptDate(LocalDateTime.now())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .borrowerName(loan.getBorrower().getFullName())
                .borrowerNumber(loan.getBorrower().getBorrowerNumber())
                .principalAmount(loan.getPrincipalAmount())
                .processingFee(BigDecimal.ZERO) // You can calculate actual fees
                .insuranceFee(BigDecimal.ZERO)  // You can calculate actual fees
                .netDisbursementAmount(loan.getNetDisbursementAmount())
                .interestRate(loan.getInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .maturityDate(loan.getMaturityDate())
                .disbursementMethod(loan.getDisbursementMethod())
                .transactionReference(loan.getTransactionReference())
                .disbursedByName(loan.getDisbursedBy() != null ? loan.getDisbursedBy().getFirstName() + loan.getDisbursedBy().getLastName()  : "System")
                .branchName(loan.getBranch() != null ? loan.getBranch().getName() : "Main Branch")
                .termsAndConditions(generateTermsAndConditions())
                .build();
    }
    
    @Override
    public List<LoanDto> getLoansPendingDisbursement() {
        List<Loan> loans = loanRepository.findByStatus(GeneralConfig.LoanStatus.PENDING_DISBURSEMENT);
        return loans.stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public LoanDto getLoanByAccountNumber(String accountNumber) {
        Loan loan = loanRepository.findByLoanAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with account number: " + accountNumber));
        return loanMapper.toDto(loan);
    }
    
    @Override
    public LoanDto closeLoan(Long loanId, User currentUser) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));
        
        if (loan.getStatus() != GeneralConfig.LoanStatus.ACTIVE) {
            throw new BusinessException("Only active loans can be closed");
        }
        
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Cannot close loan with outstanding balance");
        }
        
        loan.setStatus(GeneralConfig.LoanStatus.CLOSED);
        loan.setClosedDate(LocalDate.now());
        loan.setClosedBy(currentUser);
        
        Loan savedLoan = loanRepository.save(loan);


        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedLoan.getId())) {
            auditService.masterAuditLogs(
                    savedLoan.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LOAN_CLOSED,
                    "LOAN_CLOSED",
                    "Loan of ID:"+savedLoan.getId()+" Loan No:"+savedLoan.getLoanAccountNumber()+  " has been CLOSED by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        log.info("Loan {} closed by user: {}", loanId, currentUser.getUsername());

        
        return loanMapper.toDto(savedLoan);
    }

    /*
    Write off methods
     */

    @Override
    public LoanDto writeOffLoan(Long loanId, WriteOffRequestDto dto, User currentUser) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));
        
        // Validate write-off conditions
        if (loan.getStatus() != GeneralConfig.LoanStatus.ACTIVE && 
            loan.getStatus() != GeneralConfig.LoanStatus.DELINQUENT) {
            throw new BusinessException("Only active or delinquent loans can be written off");
        }
        
        loan.setStatus(GeneralConfig.LoanStatus.WRITTEN_OFF);
        loan.setWriteOffAmount(dto.getWriteOffAmount());
        loan.setWriteOffReason(dto.getWriteOffReason());
        loan.setWriteOffDate(LocalDate.now());
        loan.setWriteOffBy(currentUser);
        loan.setRecoveryPlan(dto.getRecoveryPlan());
        loan.setWriteOffApprovalReference(dto.getApprovalReference());
        
        Loan savedLoan = loanRepository.save(loan);
        log.info("Loan {} written off by user: {}. Amount: {}", 
                loanId, currentUser.getUsername(), dto.getWriteOffAmount());

        //Integrate Financials and Log as well
        //Get Provisionamonut
        if (loan != null && loan.getId() != null) {
            auditService.logWriteOffAction(loan.getId(),currentUser.getId(),loan.getWriteOffAmount());
           // financialIntegrationService.recordLoanWriteOff(loan, dto.getWriteOffAmount(),dto.getWriteOffAmount(), currentUser);

            //Audit Section
            Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
            String createdByName ="";
            Long createdById=null;
            if(currentUser1.isPresent()){
                createdByName=currentUser1.get().getFullName();
                createdById=currentUser1.get().getId();
            }
            if (Objects.nonNull(savedLoan.getId())) {
                auditService.masterAuditLogs(
                        savedLoan.getBorrower().getId(),
                        GeneralConfig.BorrowerActivityType.LOAN_WRITE_OFF,
                        "LOAN_WRITE_OFF",
                        "Loan of ID:"+savedLoan.getId()+" Loan No:"+savedLoan.getLoanAccountNumber()+  " has been WRITTEN OFF by:"+createdByName+"-"+createdById
                );
            }
            //End Audit Section

        }

        return loanMapper.toDto(savedLoan);
    }


    @Override
    @Transactional
    public WriteOffResponseDto processWriteOff(Long loanId, WriteOffRequestDto dto, User currentUser) {
        log.info("Processing write-off for loan {} by user: {}", loanId, currentUser.getUsername());

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        // Validate write-off conditions
        validateWriteOffEligibility(loan);

        // Check if amount is reasonable
        if (dto.getWriteOffAmount().compareTo(loan.getOutstandingBalance()) > 0) {
            throw new BusinessException("Write-off amount cannot exceed outstanding balance");
        }

        // Check for required approval based on amount
        boolean requiresApproval = dto.getWriteOffAmount().compareTo(new BigDecimal("10000")) > 0;

        if (requiresApproval) {
            loan.setWriteOffStatus(GeneralConfig.WriteOffStatus.PENDING);
            loan.setWriteOffComments("Pending approval for amount: " + dto.getWriteOffAmount());
        } else {
            loan.setStatus(GeneralConfig.LoanStatus.WRITTEN_OFF);
            loan.setWriteOffStatus(GeneralConfig.WriteOffStatus.APPROVED);
        }

        loan.setWriteOffAmount(dto.getWriteOffAmount());
        loan.setWriteOffReason(dto.getWriteOffReason());
        loan.setWriteOffDate(dto.getWriteOffDate() != null ? dto.getWriteOffDate() : LocalDate.now());
        loan.setWriteOffBy(currentUser);
        loan.setWriteOffApprovalReference(dto.getApprovalReference());
        loan.setRecoveryPlan(dto.getRecoveryPlan());
        loan.setWriteOffComments(dto.getComments());

        Loan savedLoan = loanRepository.save(loan);


        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedLoan.getId())) {
            auditService.masterAuditLogs(
                    savedLoan.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LOAN_WRITE_OFF,
                    "LOAN_WRITE_OFF_PROCESSED",
                    "Loan WRITE OFF of ID:"+savedLoan.getId()+" Loan No:"+savedLoan.getLoanAccountNumber()+  " has been PROCESSED by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section


        log.info("Write-off processed for loan {}. Requires approval: {}", loanId, requiresApproval);

        return mapToWriteOffResponse(savedLoan);
    }

    @Override
    public Page<LoanDto> getLoansByStatus(String status, Pageable pageable) {
        log.info("Fetching loans by status: {}", status);

        Page<Loan> loans;

        if (status != null && !status.trim().isEmpty()) {
            try {
                GeneralConfig.LoanStatus loanStatus = GeneralConfig.LoanStatus.valueOf(status.toUpperCase());
                loans = loanRepository.findByStatus(loanStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid loan status: " + status);
            }
        } else {
            loans = loanRepository.findAll(pageable);
        }

        return loans.map(loanMapper::toDto);
    }

    @Override
    public List<LoanDto> getEligibleLoansForWriteOff() {
        log.info("Fetching loans eligible for write-off");

        List<Loan> eligibleLoans = loanRepository.findEligibleForWriteOff();

        return eligibleLoans.stream()
                .map(loan -> {
                    LoanDto dto = loanMapper.toDto(loan);
                    dto.setOutstandingBalance(loan.getOutstandingBalance());
                    dto.setDaysDelinquent(loan.getDaysDelinquent());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<LoanDto> getWrittenOffLoans(WriteOffSearchCriteria criteria, Pageable pageable) {
        log.info("Fetching written-off loans with criteria: {}", criteria);

        Page<Loan> loans = loanRepository.findWrittenOffLoans(
                criteria.getBranchId(),
                criteria.getStartDate(),
                criteria.getEndDate(),
                criteria.getRecoveryPlan(),
                criteria.getSearchTerm(),
                pageable
        );

        return loans.map(loan -> {
            LoanDto dto = loanMapper.toDto(loan);
            dto.setWriteOffAmount(loan.getWriteOffAmount());
            dto.setWriteOffReason(loan.getWriteOffReason());
            dto.setWriteOffDate(loan.getWriteOffDate());
            dto.setWrittenOffBy(loan.getWriteOffBy() != null ?
                    loan.getWriteOffBy().getFullName() : "System");
            dto.setRecoveryPlan(loan.getRecoveryPlan());
            return dto;
        });
    }



    @Override
    public WriteOffSummaryDto getWriteOffSummary(LocalDate startDate, LocalDate endDate, Long branchId) {
        log.info("Getting write-off summary from {} to {} for branch: {}", startDate, endDate, branchId);

        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(12); // Default to last 12 months
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Get main stats - FIXED CASTING
        List<Object[]> mainStatsList = loanRepository.getWriteOffStats(startDate, endDate);
        Object[] mainStats = mainStatsList.isEmpty() ? new Object[]{0L, BigDecimal.ZERO} : mainStatsList.get(0);

        // Safe casting
        long totalWriteOffs = 0L;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (mainStats != null && mainStats.length >= 2) {
            // First element is COUNT - could be Long or Integer
            if (mainStats[0] instanceof Long) {
                totalWriteOffs = (Long) mainStats[0];
            } else if (mainStats[0] instanceof Integer) {
                totalWriteOffs = ((Integer) mainStats[0]).longValue();
            }

            // Second element is SUM - should be BigDecimal
            if (mainStats[1] instanceof BigDecimal) {
                totalAmount = (BigDecimal) mainStats[1];
            } else if (mainStats[1] instanceof Double) {
                totalAmount = BigDecimal.valueOf((Double) mainStats[1]);
            }
        }

        // Get stats by reason
        List<Object[]> byReason = loanRepository.getWriteOffsByReason();

        // Get pending approvals
        List<Loan> pendingApprovals = loanRepository.findPendingWriteOffApprovals();

        // Build stats
        return WriteOffSummaryDto.builder()
                .totalWriteOffs(totalWriteOffs)
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .pendingApprovals(pendingApprovals.size())
                .approvedWriteOffs(totalWriteOffs)
                .averageWriteOffAmount(totalWriteOffs > 0 ?
                        totalAmount.divide(BigDecimal.valueOf(totalWriteOffs), 2, RoundingMode.HALF_UP) :
                        BigDecimal.ZERO)
                .build();
    }

    @Override
    @Transactional
    public WriteOffResponseDto approveWriteOff(Long loanId, User currentUser, String comments) {
        log.info("Approving write-off for loan {} by user: {}", loanId, currentUser.getUsername());

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (loan.getWriteOffStatus() != GeneralConfig.WriteOffStatus.PENDING) {
            throw new BusinessException("Loan is not pending write-off approval");
        }

        loan.setStatus(GeneralConfig.LoanStatus.WRITTEN_OFF);
        loan.setWriteOffStatus(GeneralConfig.WriteOffStatus.APPROVED);
        loan.setWriteOffComments(comments);

        Loan savedLoan = loanRepository.save(loan);

        //Integrate Financials and Log as well
        //Get Provisionamonut
        if (savedLoan != null && savedLoan.getId() != null) {
            auditService.logWriteOffAction(loan.getId(),currentUser.getId(),loan.getWriteOffAmount());
            financialIntegrationService.recordLoanWriteOff(loan, loan.getWriteOffAmount(),loan.getWriteOffAmount(), currentUser);

            //Audit Section
            Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
            String createdByName ="";
            Long createdById=null;
            if(currentUser1.isPresent()){
                createdByName=currentUser1.get().getFullName();
                createdById=currentUser1.get().getId();
            }
            if (Objects.nonNull(savedLoan.getId())) {
                auditService.masterAuditLogs(
                        savedLoan.getBorrower().getId(),
                        GeneralConfig.BorrowerActivityType.LOAN_WRITE_OFF_APPROVED,
                        "LOAN_WRITE_OFF",
                        "Loan Write Off of ID:"+savedLoan.getId()+" Loan No:"+savedLoan.getLoanAccountNumber()+  " has been APPROVED by:"+createdByName+"-"+createdById
                );
            }
            //End Audit Section

        }
        return mapToWriteOffResponse(savedLoan);
    }

    @Override
    @Transactional
    public WriteOffResponseDto rejectWriteOff(Long loanId, User currentUser, String reason) {
        log.info("Rejecting write-off for loan {} by user: {}", loanId, currentUser.getUsername());

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        loan.setWriteOffStatus(GeneralConfig.WriteOffStatus.REJECTED);
        loan.setWriteOffComments("Rejected: " + reason);

        Loan savedLoan = loanRepository.save(loan);

        //log Reject
        if (savedLoan != null && savedLoan.getId() != null) {
            auditService.logRejectWriteOffAction(loan.getId(), currentUser.getId(), loan.getWriteOffAmount());
            //Audit Section
            Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
            String createdByName ="";
            Long createdById=null;
            if(currentUser1.isPresent()){
                createdByName=currentUser1.get().getFullName();
                createdById=currentUser1.get().getId();
            }
            if (Objects.nonNull(savedLoan.getId())) {
                auditService.masterAuditLogs(
                        savedLoan.getBorrower().getId(),
                        GeneralConfig.BorrowerActivityType.LOAN_WRITE_OFF_REJECTED,
                        "LOAN_WRITE_OFF",
                        "Loan Write Off of ID:"+savedLoan.getId()+" Loan No:"+savedLoan.getLoanAccountNumber()+  " has been REJECTED by:"+createdByName+"-"+createdById
                );
            }
            //End Audit Section
        }

        return mapToWriteOffResponse(savedLoan);
    }

    @Override
    public byte[] generateWriteOffReport(WriteOffSearchCriteria criteria) {
        log.info("Generating write-off report with criteria: {}", criteria);

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<LoanDto> writtenOffLoans = getWrittenOffLoans(criteria, pageable);

        try {
            return generateWriteOffPdfReport(writtenOffLoans.getContent(), criteria);
        } catch (Exception e) {
            log.error("Error generating write-off report", e);
            throw new BusinessException("Failed to generate write-off report");
        }
    }

    // Helper methods
    private void validateWriteOffEligibility(Loan loan) {
        if (loan.getStatus() != GeneralConfig.LoanStatus.ACTIVE &&
                loan.getStatus() != GeneralConfig.LoanStatus.DELINQUENT) {
            throw new BusinessException("Only active or delinquent loans can be written off. Current status: " + loan.getStatus());
        }

        if (loan.getWriteOffStatus() == GeneralConfig.WriteOffStatus.APPROVED) {
            throw new BusinessException("Loan has already been written off");
        }

        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Loan has no outstanding balance to write off");
        }
    }

    private WriteOffResponseDto mapToWriteOffResponse(Loan loan) {
        return WriteOffResponseDto.builder()
                .loanId(loan.getId())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .borrowerName(loan.getBorrower() != null ? loan.getBorrower().getFullName() : "N/A")
                .borrowerNumber(loan.getBorrower() != null ? loan.getBorrower().getBorrowerNumber() : "N/A")
                .writeOffAmount(loan.getWriteOffAmount())
                .originalPrincipal(loan.getPrincipalAmount())
                .outstandingBalance(loan.getOutstandingBalance())
                .writeOffReason(loan.getWriteOffReason())
                .writeOffStatus(loan.getWriteOffStatus() != null ? loan.getWriteOffStatus().toString() : "APPROVED")
                .writeOffDate(loan.getWriteOffDate())
                .writtenOffBy(loan.getWriteOffBy() != null ? loan.getWriteOffBy().getFullName() : "System")
                .approvalReference(loan.getWriteOffApprovalReference())
                .recoveryPlan(loan.getRecoveryPlan())
                .processedAt(LocalDateTime.now())
                .build();
    }

    private byte[] generateWriteOffPdfReport(List<LoanDto> loans, WriteOffSearchCriteria criteria) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, baos);
        document.open();

        // Add header
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("WRITE-OFF REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));

        // Add criteria info
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        if (criteria.getStartDate() != null && criteria.getEndDate() != null) {
            document.add(new Paragraph("Period: " + criteria.getStartDate() + " to " + criteria.getEndDate(), normalFont));
        }
        if (criteria.getBranchId() != null) {
            document.add(new Paragraph("Branch: " + getBranchName(criteria.getBranchId()), normalFont));
        }
        document.add(new Paragraph("Generated On: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")), normalFont));
        document.add(new Paragraph(" "));

        // Add summary
        BigDecimal totalAmount = loans.stream()
                .map(LoanDto::getWriteOffAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        document.add(new Paragraph("Total Write-Offs: " + loans.size(), normalFont));
        document.add(new Paragraph("Total Amount: " + formatCurrency(totalAmount), normalFont));
        document.add(new Paragraph(" "));

        // Add table
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);

        // Table headers
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        addTableCell(table, "Loan Account", headerFont);
        addTableCell(table, "Borrower", headerFont);
        addTableCell(table, "Amount", headerFont);
        addTableCell(table, "Write-Off Date", headerFont);
        addTableCell(table, "Reason", headerFont);
        addTableCell(table, "Recovery Plan", headerFont);
        addTableCell(table, "Status", headerFont);
        addTableCell(table, "Written Off By", headerFont);

        // Table data
        Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        for (LoanDto loan : loans) {
            addTableCell(table, loan.getLoanAccountNumber(), dataFont);
            addTableCell(table, loan.getBorrowerName(), dataFont);
            addTableCell(table, formatCurrency(loan.getWriteOffAmount()), dataFont);
            addTableCell(table, loan.getWriteOffDate() != null ? loan.getWriteOffDate().toString() : "N/A", dataFont);
            addTableCell(table, loan.getWriteOffReason() != null ? loan.getWriteOffReason() : "N/A", dataFont);
            addTableCell(table, loan.getRecoveryPlan() != null ? loan.getRecoveryPlan() : "N/A", dataFont);
            addTableCell(table, loan.getWriteOffStatus() != null ? loan.getWriteOffStatus() : "N/A", dataFont);
            addTableCell(table, loan.getWrittenOffBy() != null ? loan.getWrittenOffBy() : "N/A", dataFont);
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }

    /*
    End of Write off Section
     */

    @Override
    public LoanDto rescheduleLoan(Long loanId, RescheduleRequestDto dto, User currentUser) {
        return null;
    }


    @Override
    public Page<LoanDto> getLoans(String status, Long branchId, Long borrowerId, Pageable pageable) {
        Page<Loan> loans;
        
        if (status != null && !status.trim().isEmpty()) {
            try {
                GeneralConfig.LoanStatus loanStatus = GeneralConfig.LoanStatus.valueOf(status.toUpperCase());
                loans = loanRepository.findByStatus(loanStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid loan status: " + status);
            }
        } else if (borrowerId != null) {
            loans = loanRepository.findByBorrowerId(borrowerId, pageable);
        } else {
            loans = loanRepository.findAll(pageable);
        }
        
        return loans.map(loanMapper::toDto);
    }


    @Override
    public PortfolioSummaryDto getPortfolioSummary(Long branchId, LocalDate asOfDate) {
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        log.info("Getting portfolio summary for branch: {}, as of: {}", branchId, asOfDate);

        // Convert LocalDate to LocalDateTime range
        LocalDateTime startOfDay = asOfDate.atStartOfDay();
        LocalDateTime endOfDay = asOfDate.atTime(LocalTime.MAX);

        log.debug("Querying with date range: {} to {}", startOfDay, endOfDay);

        // Get portfolio statistics from repository with date range
        PortfolioStats stats = loanRepository.getPortfolioStatistics(branchId, startOfDay, endOfDay);

        // Handle null stats
        if (stats == null) {
            stats = new PortfolioStats(0L, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, BigDecimal.ZERO);
        }

        return PortfolioSummaryDto.builder()
                .reportDate(asOfDate)
                .branchId(branchId)
                .totalActiveLoans(stats.getActiveLoans() != null ? stats.getActiveLoans().intValue() : 0)
                .totalPortfolioValue(stats.getTotalPortfolioValue() != null ? stats.getTotalPortfolioValue() : BigDecimal.ZERO)
                .totalOutstandingPrincipal(stats.getOutstandingPrincipal() != null ? stats.getOutstandingPrincipal() : BigDecimal.ZERO)
                .delinquentLoans(stats.getDelinquentLoans() != null ? stats.getDelinquentLoans().intValue() : 0)
                .portfolioAtRisk(calculatePortfolioAtRisk(stats))
                .loansDisbursedThisMonth(stats.getLoansDisbursedThisMonth() != null ? stats.getLoansDisbursedThisMonth().intValue() : 0)
                .amountDisbursedThisMonth(stats.getAmountDisbursedThisMonth() != null ? stats.getAmountDisbursedThisMonth() : BigDecimal.ZERO)
                .build();
    }

    private BigDecimal calculatePortfolioAtRisk(PortfolioStats stats) {
        if (stats == null || stats.getOutstandingPrincipal() == null ||
                stats.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // Simple PAR calculation - you may want to adjust this logic
        BigDecimal delinquentAmount = stats.getOutstandingPrincipal()
                .multiply(BigDecimal.valueOf(0.1)); // Assuming 10% is at risk

        return delinquentAmount;
    }






    
    @Override
    public boolean canDisburseLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        return loan.getStatus() == GeneralConfig.LoanStatus.PENDING_DISBURSEMENT;
    }
    
    @Override
    public BigDecimal calculateNetDisbursementAmount(BigDecimal principal, DisburseLoanDto dto) {
        BigDecimal totalDeductions = BigDecimal.ZERO;
        
        if (dto.getProcessingFee() != null) {
            totalDeductions = totalDeductions.add(dto.getProcessingFee());
        }
        if (dto.getInsuranceFee() != null) {
            totalDeductions = totalDeductions.add(dto.getInsuranceFee());
        }
        if (dto.getOtherDeductions() != null) {
            totalDeductions = totalDeductions.add(dto.getOtherDeductions());
        }
        
        return principal.subtract(totalDeductions);
    }
    
    @Override
    public void generateRepaymentSchedule(Long loanId, User user) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        
        generateRepaymentSchedule(loan,user);
    }


    private void generateRepaymentSchedule(Loan loan,User user) {
        log.info("Generating/updating repayment schedule for loan: {}", loan.getId());

        // Check if schedules already exist (created at approval stage)
        List<RepaymentSchedule> existingSchedules = loan.getRepaymentSchedules();

        if (existingSchedules != null && !existingSchedules.isEmpty()) {
            log.info("Updating {} existing repayment schedules for loan {}",
                    existingSchedules.size(), loan.getId());

            // Update existing schedules with actual disbursement data
            updateExistingSchedules(loan, existingSchedules,user);

            // Save the updated schedules
            repaymentScheduleRepository.saveAll(existingSchedules);

            log.info("Updated {} repayment schedules for loan {}", existingSchedules.size(), loan.getId());
        } else {
            log.info("No existing schedules found. Creating new repayment schedules for loan: {}", loan.getId());

            // Create new schedules from scratch
            List<RepaymentSchedule> newSchedules = createNewSchedules(loan,user);

            // Save new schedules
            repaymentScheduleRepository.saveAll(newSchedules);

            // Update loan's collection
            loan.setRepaymentSchedules(newSchedules);

            log.info("Created {} new repayment schedules for loan {}", newSchedules.size(), loan.getId());
        }
    }

    /**
     * Update existing schedules with actual disbursement data
     */
    private void updateExistingSchedules(Loan loan, List<RepaymentSchedule> existingSchedules,User user) {
        // Calculate new schedule parameters based on actual disbursement
        BigDecimal monthlyPrincipal = loan.getPrincipalAmount()
                .divide(BigDecimal.valueOf(loan.getTenureMonths()), 2, RoundingMode.HALF_UP);

        BigDecimal monthlyInterestRate = loan.getInterestRate()
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        BigDecimal remainingPrincipal = loan.getPrincipalAmount();
        LocalDate disbursementDate = loan.getDisbursementDate();

        // Sort existing schedules by installment number to ensure correct order
        existingSchedules.sort(Comparator.comparing(RepaymentSchedule::getInstallmentNumber));

        for (int i = 0; i < existingSchedules.size(); i++) {
            RepaymentSchedule schedule = existingSchedules.get(i);
            int installmentNumber = i + 1;

            // Calculate due date based on disbursement date
            LocalDate dueDate = disbursementDate.plusMonths(installmentNumber);

            // Calculate interest on remaining principal
            BigDecimal interest = remainingPrincipal.multiply(monthlyInterestRate)
                    .setScale(2, RoundingMode.HALF_UP);

            // For last installment, adjust principal to avoid rounding issues
            BigDecimal principalForThisInstallment = monthlyPrincipal;
            if (installmentNumber == loan.getTenureMonths()) {
                principalForThisInstallment = remainingPrincipal;
            }

            BigDecimal totalDue = principalForThisInstallment.add(interest);

            // Update the existing schedule (not creating new one)
            schedule.setDueDate(dueDate);
            schedule.setPrincipalAmount(principalForThisInstallment);
            schedule.setInterestAmount(interest);
            schedule.setPrincipalDue(principalForThisInstallment);
            schedule.setInterestDue(interest);
            schedule.setTotalDue(totalDue);
            schedule.setTotalDueAmount(totalDue);
            schedule.setOutstandingAmount(totalDue);

            // Ensure status is PENDING (in case it was set to something else)
            schedule.setStatus(GeneralConfig.InstallmentStatus.PENDING);

            // Reset payment fields (no payments made yet)
            schedule.setPrincipalPaid(BigDecimal.ZERO);
            schedule.setInterestPaid(BigDecimal.ZERO);
            schedule.setTotalPaid(BigDecimal.ZERO);
            schedule.setPaidDate(null);
            schedule.setPaidAmount(null);

            // Reset penalty fields
            schedule.setPenaltyAmount(BigDecimal.ZERO);
            schedule.setPenaltyAccrued(BigDecimal.ZERO);
            schedule.setDaysOverdue(0);

            // Update audit fields
            schedule.setUpdatedAt(LocalDateTime.now());
            schedule.setUpdatedBy(user.getId());

            // Update remaining principal for next iteration
            remainingPrincipal = remainingPrincipal.subtract(principalForThisInstallment);
        }
    }

    /**
     * Create new repayment schedules from scratch
     */
    private List<RepaymentSchedule> createNewSchedules(Loan loan,User user) {
        List<RepaymentSchedule> schedules = new ArrayList<>();

        BigDecimal monthlyPrincipal = loan.getPrincipalAmount()
                .divide(BigDecimal.valueOf(loan.getTenureMonths()), 2, RoundingMode.HALF_UP);

        BigDecimal monthlyInterestRate = loan.getInterestRate()
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        BigDecimal remainingPrincipal = loan.getPrincipalAmount();
        LocalDate disbursementDate = loan.getDisbursementDate();

        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            LocalDate dueDate = disbursementDate.plusMonths(i);

            BigDecimal interest = remainingPrincipal.multiply(monthlyInterestRate)
                    .setScale(2, RoundingMode.HALF_UP);

            // For last installment, adjust principal to avoid rounding issues
            BigDecimal principalForThisInstallment = monthlyPrincipal;
            if (i == loan.getTenureMonths()) {
                principalForThisInstallment = remainingPrincipal;
            }

            BigDecimal totalDue = principalForThisInstallment.add(interest);

            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setLoan(loan);
            schedule.setInstallmentNumber(i);
            schedule.setDueDate(dueDate);

            // Set all amount fields
            schedule.setPrincipalAmount(principalForThisInstallment);
            schedule.setInterestAmount(interest);
            schedule.setPrincipalDue(principalForThisInstallment);
            schedule.setInterestDue(interest);
            schedule.setTotalDue(totalDue);
            schedule.setTotalDueAmount(totalDue);
            schedule.setOutstandingAmount(totalDue);

            // Status and audit fields
            schedule.setStatus(GeneralConfig.InstallmentStatus.PENDING);
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setCreatedBy(user.getId());
            schedule.setDeleted(false);

            // Default payment fields
            schedule.setPrincipalPaid(BigDecimal.ZERO);
            schedule.setInterestPaid(BigDecimal.ZERO);
            schedule.setTotalPaid(BigDecimal.ZERO);
            schedule.setPenaltyAmount(BigDecimal.ZERO);
            schedule.setPenaltyAccrued(BigDecimal.ZERO);
            schedule.setDaysOverdue(0);

            schedules.add(schedule);

            // Update remaining principal
            remainingPrincipal = remainingPrincipal.subtract(principalForThisInstallment);
        }

        validateRepaymentSchedules(schedules);
        return schedules;
    }


    
    private void validateDisbursementAmount(Loan loan, DisburseLoanDto dto) {
        if (dto.getDisbursementAmount().compareTo(loan.getPrincipalAmount()) != 0) {
            throw new BusinessException("Disbursement amount must equal approved principal amount");
        }
        
        BigDecimal netAmount = calculateNetDisbursementAmount(loan.getPrincipalAmount(), dto);
        if (netAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Net disbursement amount must be greater than zero");
        }
    }


    private void validateRepaymentSchedules(List<RepaymentSchedule> schedules) {
        for (RepaymentSchedule schedule : schedules) {
            if (schedule.getPrincipalDue() == null) {
                throw new IllegalStateException("principalDue cannot be null for installment " +
                        schedule.getInstallmentNumber());
            }
            if (schedule.getInterestDue() == null) {
                throw new IllegalStateException("interestDue cannot be null for installment " +
                        schedule.getInstallmentNumber());
            }
            if (schedule.getTotalDue() == null) {
                throw new IllegalStateException("totalDue cannot be null for installment " +
                        schedule.getInstallmentNumber());
            }
        }
    }
    
    private LocalDate calculateMaturityDate(LocalDate disbursementDate, Integer tenureMonths) {
        return disbursementDate.plusMonths(tenureMonths);
    }
    
    private String generateReceiptNumber() {
        return "RCP-" + System.currentTimeMillis() + 
               String.format("%04d", (int)(Math.random() * 10000));
    }
    
    private String generateTermsAndConditions() {
        return "1. Loan must be repaid according to the schedule.\n" +
               "2. Late payments will attract penalties.\n" +
               "3. The lender reserves the right to take legal action for non-payment.\n" +
               "4. Terms and conditions are subject to change without notice.";
    }



    @Override
    public List<LoanDto> getRecentDisbursements(int limit) {
        log.info("Fetching {} recent disbursements", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by("disbursementDate").descending());
        List<Loan> recentLoans = loanRepository.findRecentDisbursements(
                GeneralConfig.LoanStatus.ACTIVE, pageable);

        return recentLoans.stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public DisbursementStatsDto getDisbursementStatistics() {
        log.info("Fetching disbursement statistics");

        LocalDate now = LocalDate.now();
        LocalDate startOfDay = now;
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate startOfMonth = now.withDayOfMonth(1);

        // Count pending disbursements (APPROVED loans not yet disbursed)
        long pendingCount = loanRepository.countByStatus(GeneralConfig.LoanStatus.PENDING_DISBURSEMENT);

        // Calculate today's disbursements
        BigDecimal todayAmount = loanRepository.sumDisbursedAmountForToday(
                GeneralConfig.LoanStatus.ACTIVE, startOfDay);

        // Calculate this week's disbursements
        BigDecimal weekAmount = loanRepository.sumDisbursedAmountForWeek(
                GeneralConfig.LoanStatus.ACTIVE, startOfWeek);

        // Calculate this month's disbursements
        BigDecimal monthAmount = loanRepository.sumDisbursedAmountForMonth(
                GeneralConfig.LoanStatus.ACTIVE, startOfMonth);

        return DisbursementStatsDto.builder()
                .pending(pendingCount)
                .todayAmount(todayAmount != null ? todayAmount : BigDecimal.ZERO)
                .weekAmount(weekAmount != null ? weekAmount : BigDecimal.ZERO)
                .monthAmount(monthAmount != null ? monthAmount : BigDecimal.ZERO)
                .build();
    }


    @Override
    @Transactional
    public BulkDisbursementResponseDto processBulkDisbursement(BulkDisbursementRequestDto request, User currentUser) {
        log.info(">>> Processing bulk disbursement for {} loans by user: {}",
                request.getLoanIds().size(), currentUser.getUsername());

        List<BulkDisbursementResult> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;

        for (Long loanId : request.getLoanIds()) {
            try {
                // Fetch loan
                Loan loan = loanRepository.findById(loanId)
                        .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));

                // Validate loan can be disbursed
                if (!canDisburseLoan(loanId)) {
                    throw new BusinessException("Loan cannot be disbursed. Current status: " + loan.getStatus());
                }

                // Create disbursement DTO for individual loan
                DisburseLoanDto dto = new DisburseLoanDto();
                dto.setDisbursementDate(request.getDisbursementDate());
                dto.setDisbursementMethod(request.getDisbursementMethod());
                dto.setDisbursementAmount(loan.getPrincipalAmount());
                dto.setTransactionReference(generateBatchTransactionReference(request.getBatchReference(), loanId));
                dto.setDisbursementNotes(request.getNotes());
                dto.setProcessingFee(BigDecimal.ZERO);
                dto.setInsuranceFee(BigDecimal.ZERO);

                // Disburse the loan
                disburseLoan(loanId, dto, currentUser);

                results.add(BulkDisbursementResult.builder()
                        .loanId(loanId)
                        .loanAccountNumber(loan.getLoanAccountNumber())
                        .status("SUCCESS")
                        .message("Loan disbursed successfully")
                        .build());
                successful++;

            } catch (Exception e) {
                log.error("Failed to disburse loan {}: {}", loanId, e.getMessage());
                results.add(BulkDisbursementResult.builder()
                        .loanId(loanId)
                        .status("FAILED")
                        .message(e.getMessage())
                        .build());
                failed++;
            }
        }

        return BulkDisbursementResponseDto.builder()
                .batchReference(request.getBatchReference() != null ?
                        request.getBatchReference() : generateBatchReference())
                .totalProcessed(request.getLoanIds().size())
                .successful(successful)
                .failed(failed)
                .results(results)
                .processedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public List<LoanDto> getLoansForBulkDisbursement(List<Long> loanIds) {
        if (loanIds == null || loanIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Loan> loans = loanRepository.findAllById(loanIds);
        return loans.stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
    }

    private String generateBatchTransactionReference(String batchRef, Long loanId) {
        String base = batchRef != null ? batchRef : "BATCH-" + System.currentTimeMillis();
        return base + "-LN" + loanId;
    }

    private String generateBatchReference() {
        return "BATCH-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }






    @Override
    public byte[] generateDisbursementReceiptPdf(Long loanId) {
        log.info("Generating disbursement receipt PDF for loan: {}", loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        if (loan.getDisbursementDate() == null) {
            throw new BusinessException("Loan has not been disbursed yet");
        }

        try {
            // Create PDF document
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content
            addReceiptHeader(document, loan);
            addBorrowerInfo(document, loan);
            addLoanDetails(document, loan);
            addDisbursementDetails(document, loan);
            addTermsAndConditions(document);
            addSignatureSection(document);

            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF receipt for loan {}", loanId, e);
            throw new BusinessException("Failed to generate receipt PDF: " + e.getMessage());
        }
    }

    // Helper methods for PDF generation
    private void addReceiptHeader(Document document, Loan loan) throws DocumentException {
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        Paragraph title = new Paragraph("LOAN DISBURSEMENT RECEIPT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" ")); // Empty line

        Paragraph receiptNo = new Paragraph("Receipt No: " + generateReceiptNumber(loan), normalFont);
        receiptNo.setAlignment(Element.ALIGN_RIGHT);
        document.add(receiptNo);

        Paragraph date = new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), normalFont);
        date.setAlignment(Element.ALIGN_RIGHT);
        document.add(date);

        document.add(new Paragraph(" "));
    }

    private void addBorrowerInfo(Document document, Loan loan) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        document.add(new Paragraph("BORROWER INFORMATION", headerFont));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Borrower Name: " + loan.getBorrower().getFullName(), normalFont));
        document.add(new Paragraph("Borrower Number: " + loan.getBorrower().getBorrowerNumber(), normalFont));
        document.add(new Paragraph("Phone: " + loan.getBorrower().getPhoneNumber(), normalFont));
        document.add(new Paragraph("Email: " + loan.getBorrower().getEmail(), normalFont));

        document.add(new Paragraph(" "));
    }

    private void addLoanDetails(Document document, Loan loan) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        document.add(new Paragraph("LOAN DETAILS", headerFont));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Loan Account: " + loan.getLoanAccountNumber(), normalFont));
        document.add(new Paragraph("Principal Amount: " + formatCurrency(loan.getPrincipalAmount()), normalFont));
        document.add(new Paragraph("Interest Rate: " + loan.getInterestRate() + "%", normalFont));
        document.add(new Paragraph("Tenure: " + loan.getTenureMonths() + " months", normalFont));

        document.add(new Paragraph(" "));
    }

    private void addDisbursementDetails(Document document, Loan loan) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        Font amountFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

        document.add(new Paragraph("DISBURSEMENT DETAILS", headerFont));
        document.add(new Paragraph(" "));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        // Safely format disbursement date
        String disbursementDateStr = loan.getDisbursementDate() != null ?
                loan.getDisbursementDate().format(dateFormatter) : "N/A";
        document.add(new Paragraph("Disbursement Date: " + disbursementDateStr, normalFont));

        // Safely get disbursement method
        String disbursementMethod = loan.getDisbursementMethod() != null ?
                loan.getDisbursementMethod() : "N/A";
        document.add(new Paragraph("Disbursement Method: " + disbursementMethod, normalFont));

        // Safely get transaction reference
        String transactionRef = loan.getTransactionReference() != null ?
                loan.getTransactionReference() : "N/A";
        document.add(new Paragraph("Transaction Reference: " + transactionRef, normalFont));

        // FIX: Safely get disbursed by user name
        String disbursedByName = "System";
        if (loan.getDisbursedBy() != null) {
            try {
                // Try to get the full name, but handle proxy exceptions
                disbursedByName = loan.getDisbursedBy().getFullName();
            } catch (Exception e) {
                log.warn("Could not load disbursedBy user details for loan {}: {}",
                        loan.getId(), e.getMessage());
                disbursedByName = "Unknown User (ID: " + loan.getDisbursedBy().getId() + ")";
            }
        }
        document.add(new Paragraph("Disbursed By: " + disbursedByName, normalFont));

        document.add(new Paragraph(" "));

        // Safely format net amount
        String netAmountStr = formatCurrency(loan.getNetDisbursementAmount());
        Paragraph netAmount = new Paragraph("Net Disbursement Amount: " + netAmountStr, amountFont);
        netAmount.setAlignment(Element.ALIGN_RIGHT);
        document.add(netAmount);

        document.add(new Paragraph(" "));
    }


    private void addTermsAndConditions(Document document) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

        document.add(new Paragraph("TERMS AND CONDITIONS", headerFont));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("1. The borrower agrees to repay the loan according to the repayment schedule.", normalFont));
        document.add(new Paragraph("2. Late payments will attract penalties as per the loan agreement.", normalFont));
        document.add(new Paragraph("3. The lender reserves the right to take legal action for non-payment.", normalFont));
        document.add(new Paragraph("4. This receipt is electronically generated and valid without signature.", normalFont));
    }

    private void addSignatureSection(Document document) throws DocumentException {
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("_________________________", normalFont));
        document.add(new Paragraph("Authorized Signature", normalFont));
    }

    private String generateReceiptNumber(Loan loan) {
        return "DR-" + loan.getId() + "-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toString();
    }


    /// ///////////////////
    @Override
    public byte[] generateDisbursementReport(LocalDate startDate, LocalDate endDate, Long branchId, String format) {
        log.info("Generating disbursement report from {} to {} for branch: {} in format: {}",
                startDate, endDate, branchId, format);

        // Fetch disbursement data
        List<Loan> disbursedLoans;
        if (branchId != null) {
            disbursedLoans = loanRepository.findDisbursedLoansByBranchAndDateRange(branchId, startDate, endDate);
        } else {
            disbursedLoans = loanRepository.findDisbursedLoansByDateRange(startDate, endDate);
        }

        // Generate report based on format
        try {
            switch (format.toUpperCase()) {
                case "PDF":
                    return generateDisbursementPdfReport(disbursedLoans, startDate, endDate, branchId);
                case "EXCEL":
                    return generateDisbursementExcelReport(disbursedLoans, startDate, endDate, branchId);
                case "CSV":
                    return generateDisbursementCsvReport(disbursedLoans, startDate, endDate, branchId);
                default:
                    throw new BusinessException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            log.error("Error generating disbursement report", e);
            throw new BusinessException("Failed to generate report: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportPortfolioSummary(Long branchId, LocalDate asOfDate) {
        log.info("Exporting portfolio summary as of {} for branch: {}", asOfDate, branchId);

        // Get portfolio summary data
        PortfolioSummaryDto summary = getPortfolioSummary(branchId, asOfDate);

        // Get recent disbursements for the summary
        List<LoanDto> recentDisbursements = getRecentDisbursements(10);

        try {
            return generatePortfolioSummaryPdf(summary, recentDisbursements, branchId, asOfDate);
        } catch (Exception e) {
            log.error("Error exporting portfolio summary", e);
            throw new BusinessException("Failed to export portfolio summary: " + e.getMessage());
        }
    }

// ============== PDF Report Generation Methods ==============

    private byte[] generateDisbursementPdfReport(List<Loan> loans, LocalDate startDate, LocalDate endDate, Long branchId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, baos);
        document.open();

        // Add report header
        addReportHeader(document, "DISBURSEMENT REPORT", startDate, endDate, branchId);

        // Add summary statistics
        addDisbursementSummary(document, loans);

        // Add table
        PdfPTable table = createDisbursementTable();

        for (Loan loan : loans) {
            addLoanToTable(table, loan);
        }

        document.add(table);

        // Add footer
        addReportFooter(document, loans.size());

        document.close();
        return baos.toByteArray();
    }

    private void addReportHeader(Document document, String title, LocalDate startDate, LocalDate endDate, Long branchId) throws DocumentException {
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        Paragraph header = new Paragraph(title, titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);

        document.add(new Paragraph(" "));

        document.add(new Paragraph("Period: " + startDate + " to " + endDate, normalFont));
        if (branchId != null) {
            String branchName = getBranchName(branchId);
            document.add(new Paragraph("Branch: " + branchName, normalFont));
        } else {
            document.add(new Paragraph("Branch: All Branches", normalFont));
        }
        document.add(new Paragraph("Generated On: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")), normalFont));
        document.add(new Paragraph(" "));
    }

    private void addDisbursementSummary(Document document, List<Loan> loans) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        document.add(new Paragraph("SUMMARY STATISTICS", headerFont));
        document.add(new Paragraph(" "));

        BigDecimal totalAmount = loans.stream()
                .map(Loan::getNetDisbursementAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        document.add(new Paragraph("Total Loans Disbursed: " + loans.size(), normalFont));
        document.add(new Paragraph("Total Amount Disbursed: " + formatCurrency(totalAmount), normalFont));
        document.add(new Paragraph("Average Loan Amount: " + formatCurrency(
                loans.isEmpty() ? BigDecimal.ZERO : totalAmount.divide(BigDecimal.valueOf(loans.size()), 2, RoundingMode.HALF_UP)
        ), normalFont));
        document.add(new Paragraph(" "));
    }

    private PdfPTable createDisbursementTable() {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Set column widths
        try {
            table.setWidths(new float[]{1.5f, 2f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 2f});
        } catch (DocumentException e) {
            log.error("Error setting table widths", e);
        }

        // Add headers
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        addTableCell(table, "S.No", headerFont);
        addTableCell(table, "Loan Account", headerFont);
        addTableCell(table, "Borrower", headerFont);
        addTableCell(table, "Amount", headerFont);
        addTableCell(table, "Disbursement Date", headerFont);
        addTableCell(table, "Method", headerFont);
        addTableCell(table, "Branch", headerFont);
        addTableCell(table, "Disbursed By", headerFont);

        return table;
    }

    private void addLoanToTable(PdfPTable table, Loan loan) {
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

        addTableCell(table, String.valueOf(table.getRows().size()), normalFont);
        addTableCell(table, loan.getLoanAccountNumber(), normalFont);
        addTableCell(table, loan.getBorrower() != null ? loan.getBorrower().getFullName() : "N/A", normalFont);
        addTableCell(table, formatCurrency(loan.getNetDisbursementAmount()), normalFont);
        addTableCell(table, loan.getDisbursementDate() != null ?
                loan.getDisbursementDate().toString() : "N/A", normalFont);
        addTableCell(table, loan.getDisbursementMethod() != null ?
                loan.getDisbursementMethod() : "N/A", normalFont);
        addTableCell(table, loan.getBranch() != null ?
                loan.getBranch().getName() : "N/A", normalFont);

        String disbursedBy = "N/A";
        if (loan.getDisbursedBy() != null) {
            try {
                disbursedBy = loan.getDisbursedBy().getFullName();
            } catch (Exception e) {
                disbursedBy = "User ID: " + loan.getDisbursedBy().getId();
            }
        }
        addTableCell(table, disbursedBy, normalFont);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addReportFooter(Document document, int totalLoans) throws DocumentException {
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("This is a system-generated report. Total records: " + totalLoans, footerFont));
    }

// ============== Excel Report Generation ==============

    private byte[] generateDisbursementExcelReport(List<Loan> loans, LocalDate startDate, LocalDate endDate, Long branchId) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Disbursement Report");

        // Create header styles
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

// Title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DISBURSEMENT REPORT");
        CellStyle titleStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        // Report info
        Row infoRow1 = sheet.createRow(2);
        infoRow1.createCell(0).setCellValue("Period:");
        infoRow1.createCell(1).setCellValue(startDate + " to " + endDate);

        Row infoRow2 = sheet.createRow(3);
        infoRow2.createCell(0).setCellValue("Branch:");
        infoRow2.createCell(1).setCellValue(branchId != null ? getBranchName(branchId) : "All Branches");

        Row infoRow3 = sheet.createRow(4);
        infoRow3.createCell(0).setCellValue("Generated On:");
        infoRow3.createCell(1).setCellValue(LocalDateTime.now().toString());

        // Summary
        BigDecimal totalAmount = loans.stream()
                .map(Loan::getNetDisbursementAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Row summaryRow1 = sheet.createRow(6);
        summaryRow1.createCell(0).setCellValue("Total Loans:");
        summaryRow1.createCell(1).setCellValue(loans.size());

        Row summaryRow2 = sheet.createRow(7);
        summaryRow2.createCell(0).setCellValue("Total Amount:");
        summaryRow2.createCell(1).setCellValue(totalAmount.doubleValue());

        // Table headers
        String[] headers = {"S.No", "Loan Account", "Borrower", "Amount", "Disbursement Date", "Method", "Branch", "Disbursed By"};
        Row headerRow = sheet.createRow(9);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Table data
        int rowNum = 10;
        for (int i = 0; i < loans.size(); i++) {
            Loan loan = loans.get(i);
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(loan.getLoanAccountNumber());
            row.createCell(2).setCellValue(loan.getBorrower() != null ? loan.getBorrower().getFullName() : "N/A");
            row.createCell(3).setCellValue(loan.getNetDisbursementAmount() != null ?
                    loan.getNetDisbursementAmount().doubleValue() : 0);
            row.createCell(4).setCellValue(loan.getDisbursementDate() != null ?
                    loan.getDisbursementDate().toString() : "N/A");
            row.createCell(5).setCellValue(loan.getDisbursementMethod() != null ?
                    loan.getDisbursementMethod() : "N/A");
            row.createCell(6).setCellValue(loan.getBranch() != null ?
                    loan.getBranch().getName() : "N/A");

            String disbursedBy = "N/A";
            if (loan.getDisbursedBy() != null) {
                try {
                    disbursedBy = loan.getDisbursedBy().getFullName();
                } catch (Exception e) {
                    disbursedBy = "User ID: " + loan.getDisbursedBy().getId();
                }
            }
            row.createCell(7).setCellValue(disbursedBy);
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

// ============== CSV Report Generation ==============

    private byte[] generateDisbursementCsvReport(List<Loan> loans, LocalDate startDate, LocalDate endDate, Long branchId) throws Exception {
        StringBuilder csv = new StringBuilder();

        // Title and metadata as comments
        csv.append("# DISBURSEMENT REPORT\n");
        csv.append("# Period: ").append(startDate).append(" to ").append(endDate).append("\n");
        csv.append("# Branch: ").append(branchId != null ? getBranchName(branchId) : "All Branches").append("\n");
        csv.append("# Generated On: ").append(LocalDateTime.now()).append("\n\n");

        // Summary
        BigDecimal totalAmount = loans.stream()
                .map(Loan::getNetDisbursementAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        csv.append("# Total Loans: ").append(loans.size()).append("\n");
        csv.append("# Total Amount: ").append(formatCurrency(totalAmount)).append("\n\n");

        // Headers
        csv.append("S.No,Loan Account,Borrower,Amount,Disbursement Date,Method,Branch,Disbursed By\n");

        // Data rows
        for (int i = 0; i < loans.size(); i++) {
            Loan loan = loans.get(i);

            csv.append(i + 1).append(",");
            csv.append(escapeCsv(loan.getLoanAccountNumber())).append(",");
            csv.append(escapeCsv(loan.getBorrower() != null ? loan.getBorrower().getFullName() : "N/A")).append(",");
            csv.append(loan.getNetDisbursementAmount() != null ? loan.getNetDisbursementAmount() : 0).append(",");
            csv.append(loan.getDisbursementDate() != null ? loan.getDisbursementDate() : "N/A").append(",");
            csv.append(escapeCsv(loan.getDisbursementMethod() != null ? loan.getDisbursementMethod() : "N/A")).append(",");
            csv.append(escapeCsv(loan.getBranch() != null ? loan.getBranch().getName() : "N/A")).append(",");

            String disbursedBy = "N/A";
            if (loan.getDisbursedBy() != null) {
                try {
                    disbursedBy = loan.getDisbursedBy().getFullName();
                } catch (Exception e) {
                    disbursedBy = "User ID: " + loan.getDisbursedBy().getId();
                }
            }
            csv.append(escapeCsv(disbursedBy)).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }

// ============== Portfolio Summary PDF ==============

    private byte[] generatePortfolioSummaryPdf(PortfolioSummaryDto summary, List<LoanDto> recentDisbursements,
                                               Long branchId, LocalDate asOfDate) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Header
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        Paragraph title = new Paragraph("PORTFOLIO SUMMARY", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("As of Date: " + asOfDate, normalFont));
        document.add(new Paragraph("Branch: " + (branchId != null ? getBranchName(branchId) : "All Branches"), normalFont));
        document.add(new Paragraph("Generated On: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")), normalFont));
        document.add(new Paragraph(" "));

        // Summary statistics in a table
        PdfPTable statsTable = new PdfPTable(2);
        statsTable.setWidthPercentage(80);
        statsTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addStatRow(statsTable, "Active Loans:", String.valueOf(summary.getTotalActiveLoans()));
        addStatRow(statsTable, "Total Portfolio Value:", formatCurrency(summary.getTotalPortfolioValue()));
        addStatRow(statsTable, "Outstanding Principal:", formatCurrency(summary.getTotalOutstandingPrincipal()));
        addStatRow(statsTable, "Delinquent Loans:", String.valueOf(summary.getDelinquentLoans()));
        addStatRow(statsTable, "Portfolio at Risk:", summary.getPortfolioAtRisk() + "%");
        addStatRow(statsTable, "Loans Disbursed This Month:", String.valueOf(summary.getLoansDisbursedThisMonth()));
        addStatRow(statsTable, "Amount Disbursed This Month:", formatCurrency(summary.getAmountDisbursedThisMonth()));

        document.add(statsTable);
        document.add(new Paragraph(" "));

        // Recent disbursements
        document.add(new Paragraph("RECENT DISBURSEMENTS", headerFont));
        document.add(new Paragraph(" "));

        PdfPTable recentTable = new PdfPTable(4);
        recentTable.setWidthPercentage(100);

        // Headers
        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        addTableCell(recentTable, "Loan Account", tableHeaderFont);
        addTableCell(recentTable, "Borrower", tableHeaderFont);
        addTableCell(recentTable, "Amount", tableHeaderFont);
        addTableCell(recentTable, "Date", tableHeaderFont);

        // Data
        Font tableFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        for (LoanDto loan : recentDisbursements) {
            addTableCell(recentTable, loan.getLoanAccountNumber(), tableFont);
            addTableCell(recentTable, loan.getBorrowerName(), tableFont);
            addTableCell(recentTable, formatCurrency(loan.getNetDisbursementAmount()), tableFont);
            addTableCell(recentTable, loan.getDisbursementDate() != null ?
                    loan.getDisbursementDate().toString() : "N/A", tableFont);
        }

        document.add(recentTable);

        document.close();
        return baos.toByteArray();
    }

    private void addStatRow(PdfPTable table, String label, String value) {
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, boldFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, normalFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }


// ============== Helper Methods ==============

    private String getBranchName(Long branchId) {
        // You can inject BranchRepository and fetch the branch name
        // For now, return a default
        return "Branch " + branchId;
    }




}