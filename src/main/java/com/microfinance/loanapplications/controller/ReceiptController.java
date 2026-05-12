package com.microfinance.loanapplications.controller;

import com.microfinance.loanapplications.dto.repayment.ReceiptDto;
import com.microfinance.loanapplications.dto.repayment.ReceiptRequestDto;
import com.microfinance.loanapplications.service.PdfGenerationService;
import com.microfinance.loanapplications.service.ReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;
    private final PdfGenerationService pdfGenerationService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<ReceiptDto> generateReceipt(@Valid @RequestBody ReceiptRequestDto request) {
        log.info("Generating receipt for request: {}", request);
        ReceiptDto receipt = receiptService.generateReceipt(request);
        return ResponseEntity.ok(receipt);
    }

    @PostMapping("/generate/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> generateReceiptPdf(@Valid @RequestBody ReceiptRequestDto request) {
        log.info("Generating receipt PDF for request: {}", request);

        ReceiptDto receipt = receiptService.generateReceipt(request);
        byte[] pdfBytes = pdfGenerationService.generateReceiptPdf(receipt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename",
                "receipt-" + receipt.getReceiptNumber() + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/installment/{installmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<ReceiptDto> getInstallmentReceipt(@PathVariable Long installmentId) {
        log.info("Fetching receipt for installment: {}", installmentId);

        ReceiptRequestDto request = ReceiptRequestDto.builder()
                .installmentId(installmentId)
                .receiptType("INSTALLMENT")
                .includeQrCode(true)
                .build();

        ReceiptDto receipt = receiptService.generateReceipt(request);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/installment/{installmentId}/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> getInstallmentReceiptPdf(@PathVariable Long installmentId) {
        log.info("Generating PDF receipt for installment: {}", installmentId);

        ReceiptRequestDto request = ReceiptRequestDto.builder()
                .installmentId(installmentId)
                .receiptType("INSTALLMENT")
                .includeQrCode(false)  // ✅ Default to false
                .format("PDF")
                .build();

        ReceiptDto receipt = receiptService.generateReceipt(request);
        byte[] pdfBytes = pdfGenerationService.generateReceiptPdf(receipt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename",
                "installment-receipt-" + installmentId + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/repayment/{repaymentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<ReceiptDto> getRepaymentReceipt(@PathVariable Long repaymentId) {
        log.info("Fetching receipt for repayment: {}", repaymentId);

        ReceiptRequestDto request = ReceiptRequestDto.builder()
                .repaymentId(repaymentId)
                .receiptType("REPAYMENT")
                .includeQrCode(true)
                .build();

        ReceiptDto receipt = receiptService.generateReceipt(request);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/repayment/{repaymentId}/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> getRepaymentReceiptPdf(@PathVariable Long repaymentId) {
        log.info("Generating PDF receipt for repayment: {}", repaymentId);

        ReceiptRequestDto request = ReceiptRequestDto.builder()
                .repaymentId(repaymentId)
                .receiptType("REPAYMENT")
                .includeQrCode(false)
                .format("PDF")
                .build();

        ReceiptDto receipt = receiptService.generateReceipt(request);
        byte[] pdfBytes = pdfGenerationService.generateReceiptPdf(receipt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename",
                "repayment-receipt-" + repaymentId + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/loan/{loanId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<ReceiptDto> getLoanSummaryReceipt(@PathVariable Long loanId) {
        log.info("Fetching loan summary receipt for loan: {}", loanId);

        ReceiptRequestDto request = ReceiptRequestDto.builder()
                .loanId(loanId)
                .receiptType("LOAN_SUMMARY")
                .includeQrCode(true)
                .build();

        ReceiptDto receipt = receiptService.generateReceipt(request);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/verify/{receiptNumber}")
    public ResponseEntity<ReceiptDto> verifyReceipt(@PathVariable String receiptNumber) {
        log.info("Verifying receipt: {}", receiptNumber);

        // Implement receipt verification logic
        // This endpoint should be public for verification

        return ResponseEntity.ok(ReceiptDto.builder()
                .receiptNumber(receiptNumber)
                .status("VERIFIED")
                .build());
    }
}