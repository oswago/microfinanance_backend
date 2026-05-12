// entity/AccountCategory.java
package com.microfinance.financials.chartofaccounts.entity;

import com.microfinance.financials.chartofaccounts.enums.AccountType;
import com.microfinance.financials.chartofaccounts.enums.NormalBalance;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fin_account_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType; // ASSET, LIABILITY, EQUITY, INCOME, EXPENSE
    
    @Column(name = "normal_balance")
    @Enumerated(EnumType.STRING)
    private NormalBalance normalBalance; // DEBIT, CREDIT
    
    private Integer sortOrder;
    
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts = new ArrayList<>();
    
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


