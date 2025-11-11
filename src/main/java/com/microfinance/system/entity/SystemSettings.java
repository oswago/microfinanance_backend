package com.microfinance.system.entity;

import com.microfinance.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_settings")
@Data
public class SystemSettings extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private InterestCalculationMethod defaultInterestCalculationMethod;
    
    private BigDecimal defaultInterestRate;
    private BigDecimal defaultPenaltyRate;
    private Integer defaultPenaltyGracePeriodDays;
    
    private String companyName;
    private String companyAddress;
    private String companyPhone;
    private String companyEmail;
    
    private String defaultCurrency;
    private Integer sessionTimeoutMinutes;
    private Integer passwordExpiryDays;
    
    private boolean mfaEnabled;
    private boolean autoBackupEnabled;
    private String backupSchedule;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum InterestCalculationMethod {
        FLAT_RATE, REDUCING_BALANCE, COMPOUND
    }
}