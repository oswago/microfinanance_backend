package com.microfinance.loanproducttype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductTypeUpdateRequest {

    @NotBlank(message = "Product type name is required")
    @Size(min = 2, max = 100, message = "Product type name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 1000, message = "Eligibility criteria cannot exceed 1000 characters")
    private String eligibilityCriteria;

    @Size(max = 50, message = "Icon reference cannot exceed 50 characters")
    private String icon;

    private Integer displayOrder;

    private Boolean active;

    // Additional fields for better product type management
    @Size(max = 1000, message = "Target audience cannot exceed 1000 characters")
    private String targetAudience;

    @Size(max = 500, message = "Common use cases cannot exceed 500 characters")
    private String commonUseCases;

    private Boolean requiresCollateral;
    private Boolean requiresGuarantor;
    private Integer minAgeRequirement;
    private Integer maxAgeRequirement;
    private Boolean requiresBusinessPlan;
    private Boolean requiresCreditCheck;

    // Risk assessment fields
    private String riskLevel; // LOW, MEDIUM, HIGH
    private String approvalWorkflow; // AUTO, MANUAL, HYBRID

    // Compliance fields
    private String regulatoryCategory;
    private Boolean kycRequired;
}