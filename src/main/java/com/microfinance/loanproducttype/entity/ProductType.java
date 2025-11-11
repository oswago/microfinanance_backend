package com.microfinance.loanproducttype.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microfinance.base.entity.BaseEntity;
import com.microfinance.loanproducts.entity.LoanProduct;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_types")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductType extends BaseEntity {

    @NotBlank
    @Column(unique = true)
    private String code;

    @NotBlank
    @Column(unique = true)
    private String name;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String eligibilityCriteria;

    private String icon; // For UI representation
    private Integer displayOrder = 0;
    private Boolean active = true;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "productType", cascade = CascadeType.ALL)
    @JsonIgnore // Add this annotation to break the circular reference
    private List<LoanProduct> loanProducts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}