package com.microfinance.loanproducttype.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.microfinance.loanproducttype.entity.ProductType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductTypeDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String eligibilityCriteria;
    private String icon;
    private Integer displayOrder;
    private Boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static ProductTypeDTO fromEntity(ProductType productType) {
        ProductTypeDTO dto = new ProductTypeDTO();
        dto.setId(productType.getId());
        dto.setCode(productType.getCode());
        dto.setName(productType.getName());
        dto.setDescription(productType.getDescription());
        dto.setEligibilityCriteria(productType.getEligibilityCriteria());
        dto.setIcon(productType.getIcon());
        dto.setDisplayOrder(productType.getDisplayOrder());
        dto.setActive(productType.getActive());
        dto.setCreatedAt(productType.getCreatedAt());
        dto.setUpdatedAt(productType.getUpdatedAt());
        
        return dto;
    }

    public ProductType toEntity() {
        ProductType productType = new ProductType();
        productType.setId(this.id);
        productType.setCode(this.code);
        productType.setName(this.name);
        productType.setDescription(this.description);
        productType.setEligibilityCriteria(this.eligibilityCriteria);
        productType.setIcon(this.icon);
        productType.setDisplayOrder(this.displayOrder);
        productType.setActive(this.active);
        
        return productType;
    }
}