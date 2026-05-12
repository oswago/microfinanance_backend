package com.microfinance.loanapplications.service;

import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.loanapplications.dto.repayment.ReceiptDto;
import com.microfinance.loanapplications.dto.repayment.ReceiptRequestDto;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.LoanRepayment;
import com.microfinance.loanapplications.entity.RepaymentSchedule;
import com.microfinance.loanapplications.repository.LoanRepaymentRepository;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final LoanRepaymentRepository loanRepaymentRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final LoanRepository loanRepository;


    @Transactional
    public ReceiptDto generateReceipt(ReceiptRequestDto request) {
        log.info("Generating receipt for request: {}", request);

        ReceiptDto receipt;

        if ("INSTALLMENT".equalsIgnoreCase(request.getReceiptType()) && request.getInstallmentId() != null) {
            receipt = generateInstallmentReceipt(request.getInstallmentId());
        } else if (request.getRepaymentId() != null) {
            receipt = generateRepaymentReceipt(request.getRepaymentId());
        } else if (request.getLoanId() != null) {
            receipt = generateLoanSummaryReceipt(request.getLoanId());
        } else {
            throw new IllegalArgumentException("Invalid receipt request parameters");
        }

        // ✅ Handle null safely
        if (request.getIncludeQrCode() != null && request.getIncludeQrCode()) {
            receipt.setQrCode(generateQrCode(receipt));
            receipt.setVerificationUrl(generateVerificationUrl(receipt.getReceiptNumber()));
        }

        return receipt;
    }



    @Transactional(readOnly = true)
    public ReceiptDto generateInstallmentReceipt(Long installmentId) {
        log.info("Generating installment receipt for installment: {}", installmentId);

        RepaymentSchedule installment = repaymentScheduleRepository.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found with id: " + installmentId));

        if (installment.getPaidAmount() == null || installment.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Cannot generate receipt for unpaid installment");
        }

        // Get all repayments for this installment
        List<LoanRepayment> repayments = loanRepaymentRepository.findByInstallmentId(installmentId);

        // Calculate totals from repayments
        BigDecimal totalPrincipalPaid = repayments.stream()
                .map(LoanRepayment::getPrincipalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInterestPaid = repayments.stream()
                .map(LoanRepayment::getInterestAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPenaltyPaid = repayments.stream()
                .map(LoanRepayment::getPenaltyAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get the latest repayment for this installment
        LoanRepayment latestRepayment = repayments.stream()
                .max((r1, r2) -> r1.getPaymentDate().compareTo(r2.getPaymentDate()))
                .orElse(null);

        Loan loan = installment.getLoan();

        return ReceiptDto.builder()
                .receiptNumber(latestRepayment != null ?
                        latestRepayment.getReceiptNumber() :
                        generateReceiptNumber("INST", installmentId))
                .receiptDate(LocalDate.now())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .borrowerName(loan.getBorrower().getFullName())
                .borrowerIdNumber(loan.getBorrower().getBorrowerNumber())
                .borrowerPhone(loan.getBorrower().getPhoneNumber())
                .borrowerEmail(loan.getBorrower().getEmail())

                // Payment details from repayments
                .amountPaid(installment.getPaidAmount())
                .principalAmount(totalPrincipalPaid)
                .interestAmount(totalInterestPaid)
                .penaltyAmount(totalPenaltyPaid)
                .paymentMethod(latestRepayment != null && latestRepayment.getPaymentMethod() != null ?
                        latestRepayment.getPaymentMethod().name() : installment.getPaymentMethod())
                .transactionReference(latestRepayment != null ?
                        latestRepayment.getTransactionReference() : installment.getTransactionReference())
                .paymentDate(installment.getPaidDate())
                .installmentNumber(installment.getInstallmentNumber())
                .dueDate(installment.getDueDate())
                .receivedBy(latestRepayment != null && latestRepayment.getReceivedBy() != null ?
                        latestRepayment.getReceivedBy().getUsername() :
                        (loan.getDisbursedBy() != null ? loan.getDisbursedBy().getUsername() : "System"))
                .branchName(loan.getBranch() != null ? loan.getBranch().getName() : "N/A")
                .branchCode(loan.getBranch() != null ? loan.getBranch().getCode() : "N/A")
                .receiptType("INSTALLMENT")
                .status("COMPLETED")
                .notes(installment.getNotes())
                .build();
    }

    @Transactional(readOnly = true)
    public ReceiptDto generateRepaymentReceipt(Long repaymentId) {
        log.info("Generating repayment receipt for repayment: {}", repaymentId);

        LoanRepayment repayment = loanRepaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Repayment not found with id: " + repaymentId));

        Loan loan = repayment.getLoan();

        return ReceiptDto.builder()
                .receiptNumber(repayment.getReceiptNumber())
                .receiptDate(LocalDate.now())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .borrowerName(loan.getBorrower().getFullName())
                .borrowerIdNumber(loan.getBorrower().getBorrowerNumber())
                .borrowerPhone(loan.getBorrower().getPhoneNumber())
                .borrowerEmail(loan.getBorrower().getEmail())
                .amountPaid(repayment.getAmountPaid())
                .principalAmount(repayment.getPrincipalAmount())
                .interestAmount(repayment.getInterestAmount())
                .penaltyAmount(repayment.getPenaltyAmount())
                .feesAmount(repayment.getFeesAmount())
                .paymentMethod(repayment.getPaymentMethod() != null ?
                        repayment.getPaymentMethod().name() : null)
                .transactionReference(repayment.getTransactionReference())
                .paymentDate(repayment.getPaymentDate())
                .installmentNumber(repayment.getInstallment() != null ?
                        repayment.getInstallment().getInstallmentNumber() : null)
                .receivedBy(repayment.getReceivedBy() != null ?
                        repayment.getReceivedBy().getUsername() : "System")
                .branchName(loan.getBranch() != null ? loan.getBranch().getName() : "N/A")
                .branchCode(loan.getBranch() != null ? loan.getBranch().getCode() : "N/A")
                .receiptType("REPAYMENT")
                .status(repayment.getStatus() != null ? repayment.getStatus().name() : "COMPLETED")
                .notes(repayment.getNotes())
                .build();
    }

    @Transactional(readOnly = true)
    public ReceiptDto generateLoanSummaryReceipt(Long loanId) {
        log.info("Generating loan summary receipt for loan: {}", loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        // Get all repayments for this loan
        List<LoanRepayment> repayments = loanRepaymentRepository.findByLoanId(loanId);

        // Calculate totals from repayments
        BigDecimal totalPrincipalPaid = repayments.stream()
                .map(LoanRepayment::getPrincipalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInterestPaid = repayments.stream()
                .map(LoanRepayment::getInterestAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPenaltyPaid = repayments.stream()
                .map(LoanRepayment::getPenaltyAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFeesPaid = repayments.stream()
                .map(LoanRepayment::getFeesAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get the most recent repayment for payment method/details
        LoanRepayment latestRepayment = repayments.stream()
                .max((r1, r2) -> r1.getPaymentDate().compareTo(r2.getPaymentDate()))
                .orElse(null);

        return ReceiptDto.builder()
                .receiptNumber(generateReceiptNumber("LOAN", loanId))
                .receiptDate(LocalDate.now())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .borrowerName(loan.getBorrower().getFullName())
                .borrowerIdNumber(loan.getBorrower().getBorrowerNumber())
                .borrowerPhone(loan.getBorrower().getPhoneNumber())
                .borrowerEmail(loan.getBorrower().getEmail())

                // Payment totals from repayments
                .amountPaid(loan.getTotalPaid()) // This should already be the total
                .principalAmount(totalPrincipalPaid)
                .interestAmount(totalInterestPaid)
                .penaltyAmount(totalPenaltyPaid)
                .feesAmount(totalFeesPaid)

                // Use latest payment details for method/reference
                .paymentMethod(latestRepayment != null && latestRepayment.getPaymentMethod() != null ?
                        latestRepayment.getPaymentMethod().name() : null)
                .transactionReference(latestRepayment != null ?
                        latestRepayment.getTransactionReference() : null)
                .paymentDate(latestRepayment != null ?
                        latestRepayment.getPaymentDate() : loan.getDisbursementDate())

                .receivedBy(latestRepayment != null && latestRepayment.getReceivedBy() != null ?
                        latestRepayment.getReceivedBy().getUsername() :
                        (loan.getDisbursedBy() != null ? loan.getDisbursedBy().getUsername() : "System"))

                .branchName(loan.getBranch() != null ? loan.getBranch().getName() : "N/A")
                .branchCode(loan.getBranch() != null ? loan.getBranch().getCode() : "N/A")
                .receiptType("LOAN_SUMMARY")
                .status(loan.getStatus() != null ? loan.getStatus().name() : "ACTIVE")

                // Add summary info
                .notes(String.format("Total of %d repayment(s) processed", repayments.size()))
                .build();
    }

    private String generateReceiptNumber(String prefix, Long id) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = String.valueOf(System.currentTimeMillis()).substring(8);
        return String.format("%s-%s-%d-%s", prefix, date, id, time);
    }

    private byte[] generateQrCode(ReceiptDto receipt) {
        // Implement QR code generation logic
        // You can use libraries like ZXing
        String qrContent = String.format(
                "Receipt: %s\nAmount: %s\nDate: %s\nLoan: %s",
                receipt.getReceiptNumber(),
                receipt.getAmountPaid(),
                receipt.getPaymentDate(),
                receipt.getLoanAccountNumber()
        );
        return Base64.getEncoder().encode(qrContent.getBytes());
    }

    private String generateVerificationUrl(String receiptNumber) {
        return "/api/receipts/verify/" + receiptNumber;
    }
}