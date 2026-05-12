// dto/report/DataChangeDto.java
package com.microfinance.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataChangeDto {
    
    private Long id;
    
    private String entityType; // LOAN, BORROWER, REPAYMENT, etc.
    
    private Long entityId;
    
    private String fieldName;
    
    private String oldValue;
    
    private String newValue;
    
    private String changedBy;
    
    private String changedByUsername;
    
    private LocalDateTime changedAt;
    
    private String changeType; // CREATE, UPDATE, DELETE
    
    private String reason;
    private Long changedByUserId;

        // Constructor matching the JPQL query
        public DataChangeDto(Long id, String entityType, Long entityId, String changeType,
                             Object fieldName, String oldValue, String newValue,
                             String username, Long userId, LocalDateTime timestamp, String details) {
            this.id = id;
            this.entityType = entityType;
            this.entityId = entityId;
            this.changeType = changeType;
            this.fieldName = fieldName != null ? fieldName.toString() : null;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.changedBy = username;
            this.changedByUserId = userId;
            this.changedAt = timestamp;
            this.reason = details;
        }
}