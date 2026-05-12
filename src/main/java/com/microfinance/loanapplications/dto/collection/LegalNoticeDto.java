// dto/collection/LegalNoticeDto.java
package com.microfinance.loanapplications.dto.collection;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LegalNoticeDto {
    private Long id;
    private String noticeNumber;
    private Long loanId;
    private String loanNumber;
    private Long recoveryCaseId;
    private String borrowerName;
    private String borrowerPhone;
    private String borrowerEmail;
    private String noticeType;
    private LocalDate noticeDate;
    private LocalDate complianceDate;
    private String status;
    private String reason;
    private String legalGrounds;
    private Long assignedOfficerId;
    private String assignedOfficerName;
    private String additionalNotes;
    private String deliveryMethod;
    private String documentPath;
    private LocalDate sentDate;
    private LocalDate acknowledgedDate;
    private String acknowledgedBy;
    private String acknowledgementNotes;
    private Boolean generateDocument;
    private Boolean notifyLegalTeam;
    private Boolean attachToCase;
    private String createdBy;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}