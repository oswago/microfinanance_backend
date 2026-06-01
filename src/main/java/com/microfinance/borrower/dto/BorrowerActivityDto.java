package com.microfinance.borrower.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.microfinance.borrower.entity.BorrowerActivity;
import com.microfinance.common.config.GeneralConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowerActivityDto {
    private Long id;
    private Long borrowerId;
    private String borrowerName;
    private GeneralConfig.BorrowerActivityType activityType;
    private String description;
    private String details;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime activityDate;

    private Long performedBy;
    private String performedByName;
    private String referenceType;
    private Long referenceId;
    private String referenceNumber;
    private String branchName;
    private String ipAddress;
    private String userAgent;
    private String sessionId;


    // Copy enum from entity to avoid direct dependency
    public enum ActivityType {
        BORROWER_CREATED,
        BORROWER_UPDATED,
        BORROWER_STATUS_CHANGED,
        BORROWER_KYC_INITIATED,
        BORROWER_KYC_VERIFIED,
        BORROWER_KYC_REJECTED,
        BORROWER_KYC_EXPIRED,
        DOCUMENT_UPLOADED,
        DOCUMENT_VERIFIED,
        DOCUMENT_REJECTED,
        DOCUMENT_DELETED,
        GROUP_ASSIGNED,
        GROUP_REMOVED,
        GROUP_LEADER_ASSIGNED,
        LOAN_APPLICATION_SUBMITTED,
        LOAN_APPLICATION_APPROVED,
        LOAN_APPLICATION_REJECTED,
        LOAN_APPLICATION_WITHDRAWN,
        LOAN_DISBURSED,
        LOAN_DISBURSEMENT_FAILED,
        REPAYMENT_MADE,
        REPAYMENT_SCHEDULED,
        REPAYMENT_OVERDUE,
        REPAYMENT_PARTIAL,
        REPAYMENT_BOUNCE,
        SAVINGS_DEPOSIT,
        SAVINGS_WITHDRAWAL,
        SAVINGS_INTEREST_APPLIED,
        SMS_SENT,
        EMAIL_SENT,
        NOTIFICATION_SENT,
        REMINDER_SENT,
        PROFILE_VIEWED,
        PASSWORD_CHANGED,
        CONTACT_UPDATED,
        EMPLOYMENT_UPDATED,
        RISK_RATING_UPDATED,
        CREDIT_SCORE_UPDATED,
        BLACKLISTED,
        BLACKLIST_REMOVED,
        GUARANTOR_ADDED,
        GUARANTOR_REMOVED,
        GUARANTOR_VERIFIED,
        MEETING_ATTENDED,
        MEETING_MISSED,
        NOTE_ADDED,
        FILE_UPLOADED,
        SYSTEM_AUTO_UPDATE
    }

    // Conversion methods
    public static BorrowerActivityDto fromEntity(BorrowerActivity entity) {
        if (entity == null) return null;

        return BorrowerActivityDto.builder()
                .id(entity.getId())
                .borrowerId(entity.getBorrower().getId())
                .borrowerName(entity.getBorrower().getFullName())
                .activityType(GeneralConfig.BorrowerActivityType.valueOf(entity.getActivityType().name()))
                .description(entity.getDescription())
                .details(entity.getDetails())
                .activityDate(entity.getActivityDate())
                .performedBy(entity.getPerformedBy())
                .performedByName(entity.getPerformedByName())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .referenceNumber(entity.getReferenceNumber())
                .branchName(entity.getBranchName())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .sessionId(entity.getSessionId())
                .build();
    }

    public BorrowerActivity toEntity() {
        BorrowerActivity entity = new BorrowerActivity();
        entity.setActivityType(GeneralConfig.BorrowerActivityType.valueOf(this.activityType.name()));
        entity.setDescription(this.description);
        entity.setDetails(this.details);
        entity.setActivityDate(this.activityDate != null ? this.activityDate : LocalDateTime.now());
        entity.setPerformedBy(this.performedBy);
        entity.setPerformedByName(this.performedByName);
        entity.setReferenceType(this.referenceType);
        entity.setReferenceId(this.referenceId);
        entity.setReferenceNumber(this.referenceNumber);
        entity.setBranchName(this.branchName);
        entity.setIpAddress(this.ipAddress);
        entity.setUserAgent(this.userAgent);
        entity.setSessionId(this.sessionId);

        return entity;
    }

    // TimelineGroup inner class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineGroup {
        private String period;
        private LocalDate startDate;
        private LocalDate endDate;
        private List<BorrowerActivityDto> activities;
        private Integer activityCount;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime earliestActivity;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime latestActivity;

        public TimelineGroup(String period, List<BorrowerActivityDto> activities) {
            this.period = period;
            this.activities = activities;
            this.activityCount = activities != null ? activities.size() : 0;

            if (activities != null && !activities.isEmpty()) {
                this.earliestActivity = activities.stream()
                        .map(BorrowerActivityDto::getActivityDate)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);
                this.latestActivity = activities.stream()
                        .map(BorrowerActivityDto::getActivityDate)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);
            }
        }
    }
}