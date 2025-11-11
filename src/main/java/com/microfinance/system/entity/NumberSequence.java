package com.microfinance.system.entity;

import com.microfinance.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "number_sequences")
@Data
public class NumberSequence extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String sequenceCode;
    
    @Column(nullable = false)
    private String description;
    
    private String prefix;
    private String suffix;
    private Long nextValue = 1L;
    private Integer padding = 0;
    private boolean resetDaily;
    private boolean resetMonthly;
    private boolean resetYearly;
    private boolean active = true;
    
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
}