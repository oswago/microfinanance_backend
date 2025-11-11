package com.microfinance.borrower.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class GroupMeetingDto {
    private Long id;
    private Long groupId;
    private String groupName;
    private String groupCode;
    
    // Meeting Basic Information
    private String meetingTitle;
    private String meetingType; // REGULAR, SPECIAL, EMERGENCY, TRAINING
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate meetingDate;
    
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
    
    private String meetingLocation;
    private String meetingAddress;
    private String meetingChairperson;
    private String meetingSecretary;
    
    // Meeting Status
    private String meetingStatus; // SCHEDULED, ONGOING, COMPLETED, CANCELLED, POSTPONED
    private Boolean isRegularMeeting;
    private Integer meetingSequence; // e.g., 1st meeting, 2nd meeting, etc.
    
    // Attendance Information
    private Integer expectedAttendees;
    private Integer actualAttendees;
    private Integer absentees;
    private BigDecimal attendanceRate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime attendanceTakenAt;
    
    private String attendanceTakenBy;
    
    // Financial Transactions
    private BigDecimal totalSavingsCollected;
    private BigDecimal totalLoanRepayments;
    private BigDecimal totalFinesCollected;
    private BigDecimal totalSharesCollected;
    private BigDecimal totalOtherCollections;
    
    // Meeting Agenda and Minutes
    private String meetingAgenda;
    private String meetingMinutes;
    private List<AgendaItem> agendaItems = new ArrayList<>();
    private List<Decision> decisionsMade = new ArrayList<>();
    private List<ActionItem> actionItems = new ArrayList<>();
    
    // Member Attendance Details
    private List<MemberAttendance> memberAttendances = new ArrayList<>();
    
    // Collection Details
    private List<CollectionDetail> collectionDetails = new ArrayList<>();
    
    // Meeting Logistics
    private Boolean minutesApproved;
    private String minutesApprovedBy;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime minutesApprovedAt;
    
    private String notes;
    private String specialResolutions;
    
    // Audit Information
    private Long createdBy;
    private String createdByName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    private Long updatedBy;
    private String updatedByName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Inner classes for detailed information
    @Data
    public static class AgendaItem {
        private Integer itemNumber;
        private String title;
        private String description;
        private String presenter;
        private Integer durationMinutes;
        private String discussionSummary;
        private String outcome;
    }

    @Data
    public static class Decision {
        private String decisionNumber;
        private String description;
        private String proposedBy;
        private String secondedBy;
        private String votingResult; // UNANIMOUS, MAJORITY, MINORITY
        private String implementationDeadline;
        private String responsiblePerson;
    }

    @Data
    public static class ActionItem {
        private String description;
        private String assignedTo;
        private LocalDate dueDate;
        private String status; // PENDING, IN_PROGRESS, COMPLETED
        private String completionNotes;
    }

    @Data
    public static class MemberAttendance {
        private Long memberId;
        private String memberName;
        private String borrowerNumber;
        private String attendanceStatus; // PRESENT, ABSENT, LATE, EXCUSED
        private String arrivalTime;
        private String departureTime;
        private BigDecimal savingsDeposited;
        private BigDecimal loanRepayment;
        private BigDecimal finesPaid;
        private String notes;
        
        // Helper method
        public boolean isPresent() {
            return "PRESENT".equals(attendanceStatus) || "LATE".equals(attendanceStatus);
        }
    }

    @Data
    public static class CollectionDetail {
        private String collectionType; // SAVINGS, LOAN_REPAYMENT, FINE, SHARE, OTHER
        private BigDecimal amount;
        private Integer transactionCount;
        private String collectedBy;
        private String verificationStatus; // VERIFIED, PENDING, DISPUTED
    }

    // Helper methods
    public BigDecimal getAttendanceRatePercentage() {
        return attendanceRate != null ? 
            attendanceRate.multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalCollections() {
        BigDecimal total = BigDecimal.ZERO;
        if (totalSavingsCollected != null) total = total.add(totalSavingsCollected);
        if (totalLoanRepayments != null) total = total.add(totalLoanRepayments);
        if (totalFinesCollected != null) total = total.add(totalFinesCollected);
        if (totalSharesCollected != null) total = total.add(totalSharesCollected);
        if (totalOtherCollections != null) total = total.add(totalOtherCollections);
        return total;
    }
    
    public boolean isCompleted() {
        return "COMPLETED".equals(meetingStatus);
    }
    
    public boolean isScheduled() {
        return "SCHEDULED".equals(meetingStatus);
    }
    
    public boolean isCancelled() {
        return "CANCELLED".equals(meetingStatus);
    }
    
    public String getMeetingDateTime() {
        return meetingDate != null && startTime != null ? 
            meetingDate.toString() + " " + startTime.toString() : "";
    }
    
    public Integer getActionItemsPending() {
        return actionItems != null ? 
            (int) actionItems.stream().filter(item -> "PENDING".equals(item.getStatus())).count() : 0;
    }
    
    public Integer getActionItemsCompleted() {
        return actionItems != null ? 
            (int) actionItems.stream().filter(item -> "COMPLETED".equals(item.getStatus())).count() : 0;
    }
}