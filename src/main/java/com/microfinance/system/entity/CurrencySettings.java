package com.microfinance.system.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microfinance.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "currency_settings")
@Data
public class CurrencySettings {
    @Id
    private String currencyCode;
    
    @Column(nullable = false)
    private String currencyName;
    
    private String symbol;
    private BigDecimal exchangeRate;
   // @JsonProperty("isDefault") // This ensures it serializes as "isDefault"
    //private boolean isDefault;
    //@Column(name = "is_default") // Map to correct column name if different
    private boolean defaultValue; // Rename to defaultValue or just 'default'

    private boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Audit fields
    @Column(name = "created_by")
    private Long createdBy;
    @Column(name = "updated_by")
    private Long updatedBy;

    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}