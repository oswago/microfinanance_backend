package com.microfinance.loanapplications.dto.approval;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFilterDto {

    private Long branchId;

    @Min(value = 0, message = "Minimum amount must be greater than or equal to 0")
    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private String productType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String status;

    private String borrowerName;

    private String applicationNumber;

    @Min(value = 0, message = "Risk score must be between 0 and 100")
    private Integer minRiskScore;

    @Min(value = 0, message = "Risk score must be between 0 and 100")
    private Integer maxRiskScore;

    private String stage;

    private Long createdBy;

    private Long approverId;

    @Pattern(regexp = "applicationNumber|appliedAmount|submittedDate|borrowerName|createdDate",
            message = "Invalid sort field")
    private String sortBy;

    @Pattern(regexp = "ASC|DESC", message = "Sort direction must be ASC or DESC")
    @Builder.Default
    private String sortDirection = "DESC";

    private String slaStatus;

    private Integer approvalLevel;

    private String searchTerm;

    private String purposeCategory;

    @Min(value = 1, message = "Minimum tenure must be at least 1 month")
    private Integer minTenureMonths;

    @Min(value = 1, message = "Maximum tenure must be at least 1 month")
    private Integer maxTenureMonths;

    @Min(value = 0, message = "Processing fee must be greater than or equal to 0")
    private BigDecimal minProcessingFee;

    @Min(value = 0, message = "Processing fee must be greater than or equal to 0")
    private BigDecimal maxProcessingFee;

    // Page and size for pagination
    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    @Min(value = 1, message = "Page size must be at least 1")
    private Integer size = 20;

    /**
     * Validation method to ensure date range is valid
     */
    @AssertTrue(message = "End date must be after or equal to start date")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }

    /**
     * Validation method for amount range
     */
    @AssertTrue(message = "Max amount must be greater than or equal to min amount")
    public boolean isAmountRangeValid() {
        if (minAmount == null || maxAmount == null) {
            return true;
        }
        return maxAmount.compareTo(minAmount) >= 0;
    }

    /**
     * Validation method for risk score range
     */
    @AssertTrue(message = "Max risk score must be between 0 and 100 and greater than or equal to min risk score")
    public boolean isRiskScoreRangeValid() {
        if (minRiskScore == null && maxRiskScore == null) {
            return true;
        }

        if (minRiskScore != null && (minRiskScore < 0 || minRiskScore > 100)) {
            return false;
        }

        if (maxRiskScore != null && (maxRiskScore < 0 || maxRiskScore > 100)) {
            return false;
        }

        if (minRiskScore != null && maxRiskScore != null) {
            return maxRiskScore >= minRiskScore;
        }

        return true;
    }

    /**
     * Helper method to check if any filter is applied
     */
    public boolean hasFilters() {
        return branchId != null ||
                minAmount != null ||
                maxAmount != null ||
                productType != null ||
                startDate != null ||
                endDate != null ||
                status != null ||
                borrowerName != null ||
                applicationNumber != null ||
                minRiskScore != null ||
                maxRiskScore != null ||
                stage != null ||
                createdBy != null ||
                slaStatus != null ||
                approvalLevel != null ||
                searchTerm != null ||
                purposeCategory != null ||
                minTenureMonths != null ||
                maxTenureMonths != null ||
                minProcessingFee != null ||
                maxProcessingFee != null;
    }

    /**
     * Helper method to get pagination offset
     */
    public int getOffset() {
        return page * size;
    }
}