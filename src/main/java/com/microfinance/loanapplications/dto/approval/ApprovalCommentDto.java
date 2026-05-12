package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCommentDto {
    private Long id;
    private Long applicationId;
    private String comment;
    private String commenterName;
    private String commenterRole;
    private LocalDateTime createdAt;
    private boolean isInternal;
    private String applicationNumber;
    private String commenterUsername;
    private Long commenterId;
    private String parentCommentId;
}