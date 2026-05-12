// service/TaxService.java
package com.microfinance.financials.taxmanagement.service;

import com.microfinance.base.entity.User;
import com.microfinance.financials.taxmanagement.dto.*;
import com.microfinance.financials.taxmanagement.entity.*;
import com.microfinance.financials.taxmanagement.repository.*;
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
public class TaxService {

    private final TaxConfigRepository taxConfigRepository;
    private final TaxTransactionRepository taxTransactionRepository;
    private final WithholdingTaxCertificateRepository certificateRepository;

    private static final String COMPANY_NAME = "Finite Solutions Ltd";
    private static final String CURRENCY = "KES";

    // Tax Configuration Methods
    @Transactional
    public TaxConfigDTO createTaxConfig(TaxConfigDTO dto, User currentUser) {
        log.info("User {} creating tax config: {}", currentUser.getUsername(), dto.getTaxCode());

        TaxConfig taxConfig = TaxConfig.builder()
                .taxCode(dto.getTaxCode())
                .taxName(dto.getTaxName())
                .taxType(dto.getTaxType())
                .rate(dto.getRate())
                .calculationMethod(dto.getCalculationMethod())
                .isActive(dto.getIsActive())
                .isCompound(dto.getIsCompound())
                .effectiveFrom(dto.getEffectiveFrom())
                .effectiveTo(dto.getEffectiveTo())
                .minimumAmount(dto.getMinimumAmount())
                .maximumAmount(dto.getMaximumAmount())
                .exemptionThreshold(dto.getExemptionThreshold())
                .glAccountId(dto.getGlAccountId())
                .glAccountCode(dto.getGlAccountCode())
                .glAccountName(dto.getGlAccountName())
                .description(dto.getDescription())
                .createdBy(currentUser.getId())
                .build();

        taxConfig = taxConfigRepository.save(taxConfig);
        return convertToDTO(taxConfig);
    }

    @Transactional
    public TaxConfigDTO updateTaxConfig(Long id, TaxConfigDTO dto, User currentUser) {
        log.info("User {} updating tax config: {}", currentUser.getUsername(), id);

        TaxConfig taxConfig = taxConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax config not found"));

        taxConfig.setTaxName(dto.getTaxName());
        taxConfig.setRate(dto.getRate());
        taxConfig.setCalculationMethod(dto.getCalculationMethod());
        taxConfig.setIsActive(dto.getIsActive());
        taxConfig.setIsCompound(dto.getIsCompound());
        taxConfig.setEffectiveFrom(dto.getEffectiveFrom());
        taxConfig.setEffectiveTo(dto.getEffectiveTo());
        taxConfig.setMinimumAmount(dto.getMinimumAmount());
        taxConfig.setMaximumAmount(dto.getMaximumAmount());
        taxConfig.setExemptionThreshold(dto.getExemptionThreshold());
        taxConfig.setGlAccountId(dto.getGlAccountId());
        taxConfig.setGlAccountCode(dto.getGlAccountCode());
        taxConfig.setGlAccountName(dto.getGlAccountName());
        taxConfig.setDescription(dto.getDescription());
        taxConfig.setUpdatedBy(currentUser.getId());

        taxConfig = taxConfigRepository.save(taxConfig);
        return convertToDTO(taxConfig);
    }

    @Transactional(readOnly = true)
    public List<TaxConfigDTO> getAllTaxConfigs() {
        return taxConfigRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaxConfigDTO> getActiveTaxConfigs() {
        return taxConfigRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaxConfigDTO getTaxConfigById(Long id) {
        TaxConfig taxConfig = taxConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax config not found"));
        return convertToDTO(taxConfig);
    }

    // Tax Calculation Methods
    @Transactional
    public TaxCalculationResultDTO calculateTax(TaxCalculationRequestDTO request, User currentUser) {
        log.info("User {} calculating tax for {} on amount: {}", 
                currentUser.getUsername(), request.getTaxCode(), request.getAmount());

        TaxConfig taxConfig = taxConfigRepository.findActiveTaxByCodeAsOfDate(
                request.getTaxCode(), 
                LocalDateTime.now()
        ).orElseThrow(() -> new RuntimeException("Active tax configuration not found for: " + request.getTaxCode()));

        BigDecimal taxableAmount = calculateTaxableAmount(taxConfig, request.getAmount());
        BigDecimal taxAmount = calculateTaxAmount(taxConfig, taxableAmount);
        
        // Create tax transaction
        TaxTransaction transaction = TaxTransaction.builder()
                .taxConfigId(taxConfig.getId())
                .taxCode(taxConfig.getTaxCode())
                .taxName(taxConfig.getTaxName())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .transactionDate(request.getTransactionDate())
                .taxableAmount(taxableAmount)
                .taxRate(taxConfig.getRate())
                .taxAmount(taxAmount)
                .status("CALCULATED")
                .createdBy(currentUser.getId())
                .build();

        transaction = taxTransactionRepository.save(transaction);

        return TaxCalculationResultDTO.builder()
                .taxableAmount(taxableAmount)
                .taxRate(taxConfig.getRate())
                .taxAmount(taxAmount)
                .taxCode(taxConfig.getTaxCode())
                .taxName(taxConfig.getTaxName())
                .transactionId(transaction.getId())
                .build();
    }

    private BigDecimal calculateTaxableAmount(TaxConfig config, BigDecimal amount) {
        BigDecimal taxableAmount = amount;
        
        if (config.getExemptionThreshold() != null && amount.compareTo(config.getExemptionThreshold()) <= 0) {
            return BigDecimal.ZERO;
        }
        
        if (config.getMinimumAmount() != null && amount.compareTo(config.getMinimumAmount()) < 0) {
            taxableAmount = config.getMinimumAmount();
        }
        
        if (config.getMaximumAmount() != null && amount.compareTo(config.getMaximumAmount()) > 0) {
            taxableAmount = config.getMaximumAmount();
        }
        
        return taxableAmount;
    }

    private BigDecimal calculateTaxAmount(TaxConfig config, BigDecimal taxableAmount) {
        if ("PERCENTAGE".equals(config.getCalculationMethod())) {
            return taxableAmount.multiply(config.getRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if ("FIXED_AMOUNT".equals(config.getCalculationMethod())) {
            return config.getRate();
        }
        
        return BigDecimal.ZERO;
    }

    // Tax Transaction Methods
    @Transactional
    public TaxTransactionDTO withholdTax(Long transactionId, User currentUser) {
        log.info("User {} withholding tax for transaction: {}", currentUser.getUsername(), transactionId);

        TaxTransaction transaction = taxTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Tax transaction not found"));

        transaction.setStatus("WITHHELD");
        transaction.setWithheldAmount(transaction.getTaxAmount());
        transaction = taxTransactionRepository.save(transaction);

        // Generate withholding tax certificate
        generateWithholdingCertificate(transaction, currentUser);

        return convertToDTO(transaction);
    }

    @Transactional
    public TaxTransactionDTO remitTax(Long transactionId, String remittanceReference, User currentUser) {
        log.info("User {} remitting tax for transaction: {}", currentUser.getUsername(), transactionId);

        TaxTransaction transaction = taxTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Tax transaction not found"));

        transaction.setStatus("REMITTED");
        transaction.setRemittedAmount(transaction.getTaxAmount());
        transaction.setRemittanceDate(LocalDate.now());
        transaction.setRemittanceReference(remittanceReference);
        transaction = taxTransactionRepository.save(transaction);

        return convertToDTO(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TaxTransactionDTO> getTaxTransactions(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return taxTransactionRepository.findByTransactionDateBetween(startDate, endDate, pageable)
                .map(this::convertToDTO);
    }

    // Withholding Tax Certificate Methods
    @Transactional
    public WithholdingTaxCertificateDTO generateWithholdingCertificate(TaxTransaction transaction, User currentUser) {
        String certificateNumber = generateCertificateNumber();
        
        WithholdingTaxCertificate certificate = WithholdingTaxCertificate.builder()
                .certificateNumber(certificateNumber)
                .taxTransactionId(transaction.getId())
                .borrowerId(transaction.getReferenceId())
                .interestAmount(transaction.getTaxableAmount())
                .withholdingTaxRate(transaction.getTaxRate())
                .withholdingTaxAmount(transaction.getTaxAmount())
                .certificateDate(LocalDate.now())
                .status("GENERATED")
                .createdBy(currentUser.getId())
                .build();

        certificate = certificateRepository.save(certificate);
        return convertToDTO(certificate);
    }

    @Transactional(readOnly = true)
    public List<WithholdingTaxCertificateDTO> getCertificatesByBorrower(Long borrowerId) {
        return certificateRepository.findByBorrowerIdOrderByCertificateDateDesc(borrowerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public byte[] printCertificate(Long certificateId, User currentUser) {
        log.info("User {} printing certificate: {}", currentUser.getUsername(), certificateId);

        WithholdingTaxCertificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));

        certificate.setStatus("PRINTED");
        certificate.setPrintedAt(LocalDateTime.now());
        certificate.setPrintedBy(currentUser.getId());
        certificateRepository.save(certificate);

        // TODO: Generate PDF certificate
        return new byte[0];
    }

    // Tax Report Methods
    @Transactional(readOnly = true)
    public TaxReportDTO generateTaxReport(LocalDate startDate, LocalDate endDate, User currentUser) {
        log.info("User {} generating tax report from {} to {}", 
                currentUser.getUsername(), startDate, endDate);

        List<TaxTransaction> transactions = taxTransactionRepository
                .findByTransactionDateBetween(startDate, endDate, Pageable.unpaged()).getContent();

        // Calculate summary
        BigDecimal totalTaxableAmount = transactions.stream()
                .map(TaxTransaction::getTaxableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalTaxAmount = transactions.stream()
                .map(TaxTransaction::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalWithheldAmount = transactions.stream()
                .filter(t -> "WITHHELD".equals(t.getStatus()))
                .map(TaxTransaction::getWithheldAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalRemittedAmount = transactions.stream()
                .filter(t -> "REMITTED".equals(t.getStatus()))
                .map(TaxTransaction::getRemittedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TaxSummary summary = TaxSummary.builder()
                .totalTaxableAmount(totalTaxableAmount)
                .totalTaxAmount(totalTaxAmount)
                .totalWithheldAmount(totalWithheldAmount)
                .totalRemittedAmount(totalRemittedAmount)
                .outstandingAmount(totalTaxAmount.subtract(totalRemittedAmount))
                .build();

        // Group by tax type
        Map<String, TaxByTypeDTO> taxesByType = new HashMap<>();
        for (TaxTransaction transaction : transactions) {
            String key = transaction.getTaxCode();
            TaxByTypeDTO dto = taxesByType.getOrDefault(key, TaxByTypeDTO.builder()
                    .taxType(transaction.getTaxCode())
                    .taxCode(transaction.getTaxCode())
                    .taxName(transaction.getTaxName())
                    .taxableAmount(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .withheldAmount(BigDecimal.ZERO)
                    .transactionCount(0)
                    .build());
            
            dto.setTaxableAmount(dto.getTaxableAmount().add(transaction.getTaxableAmount()));
            dto.setTaxAmount(dto.getTaxAmount().add(transaction.getTaxAmount()));
            if ("WITHHELD".equals(transaction.getStatus())) {
                dto.setWithheldAmount(dto.getWithheldAmount().add(transaction.getWithheldAmount()));
            }
            dto.setTransactionCount(dto.getTransactionCount() + 1);
            taxesByType.put(key, dto);
        }

        ReportHeader header = buildReportHeader("Tax Report", startDate, endDate);

        return TaxReportDTO.builder()
                .header(header)
                .summary(summary)
                .transactions(transactions.stream().map(this::convertToDTO).collect(Collectors.toList()))
                .taxesByType(taxesByType)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    // Helper Methods
    private String generateCertificateNumber() {
        return "WHT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private ReportHeader buildReportHeader(String reportName, LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String period = startDate.format(formatter) + " to " + endDate.format(formatter);
        
        return ReportHeader.builder()
                .companyName(COMPANY_NAME)
                .reportName(reportName)
                .reportPeriod(period)
                .generatedDate(LocalDate.now())
                .currency(CURRENCY)
                .build();
    }

    private TaxConfigDTO convertToDTO(TaxConfig entity) {
        return TaxConfigDTO.builder()
                .id(entity.getId())
                .taxCode(entity.getTaxCode())
                .taxName(entity.getTaxName())
                .taxType(entity.getTaxType())
                .rate(entity.getRate())
                .calculationMethod(entity.getCalculationMethod())
                .isActive(entity.getIsActive())
                .isCompound(entity.getIsCompound())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .minimumAmount(entity.getMinimumAmount())
                .maximumAmount(entity.getMaximumAmount())
                .exemptionThreshold(entity.getExemptionThreshold())
                .glAccountId(entity.getGlAccountId())
                .glAccountCode(entity.getGlAccountCode())
                .glAccountName(entity.getGlAccountName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TaxTransactionDTO convertToDTO(TaxTransaction entity) {
        return TaxTransactionDTO.builder()
                .id(entity.getId())
                .taxConfigId(entity.getTaxConfigId())
                .taxCode(entity.getTaxCode())
                .taxName(entity.getTaxName())
                .referenceId(entity.getReferenceId())
                .referenceType(entity.getReferenceType())
                .transactionDate(entity.getTransactionDate())
                .taxableAmount(entity.getTaxableAmount())
                .taxRate(entity.getTaxRate())
                .taxAmount(entity.getTaxAmount())
                .withheldAmount(entity.getWithheldAmount())
                .remittedAmount(entity.getRemittedAmount())
                .status(entity.getStatus())
                .remittanceDate(entity.getRemittanceDate())
                .remittanceReference(entity.getRemittanceReference())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private WithholdingTaxCertificateDTO convertToDTO(WithholdingTaxCertificate entity) {
        return WithholdingTaxCertificateDTO.builder()
                .id(entity.getId())
                .certificateNumber(entity.getCertificateNumber())
                .taxTransactionId(entity.getTaxTransactionId())
                .borrowerId(entity.getBorrowerId())
                .borrowerName(entity.getBorrowerName())
                .borrowerTin(entity.getBorrowerTin())
                .loanId(entity.getLoanId())
                .loanAccountNumber(entity.getLoanAccountNumber())
                .interestAmount(entity.getInterestAmount())
                .withholdingTaxRate(entity.getWithholdingTaxRate())
                .withholdingTaxAmount(entity.getWithholdingTaxAmount())
                .certificateDate(entity.getCertificateDate())
                .periodStartDate(entity.getPeriodStartDate())
                .periodEndDate(entity.getPeriodEndDate())
                .status(entity.getStatus())
                .printedAt(entity.getPrintedAt())
                .notes(entity.getNotes())
                .build();
    }
}

