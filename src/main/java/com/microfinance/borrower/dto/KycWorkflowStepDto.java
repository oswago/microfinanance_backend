package com.microfinance.borrower.dto;

import com.microfinance.borrower.enums.KycWorkflowStep;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycWorkflowStepDto {
    private Long id;
    private String step;
    private String name;
    private String description;
    private String category;
    private Integer order; // Add this field

    // Status fields (same as API response)
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long completedBy;
    private String completedByName;
    private String notes;
    private LocalDateTime dueDate;
    private Boolean isRequired;
    private Boolean isOverdue;
    private Integer retryCount;

    public static KycWorkflowStepDto fromStep(KycWorkflowStep step) {
        KycWorkflowStepDto dto = new KycWorkflowStepDto();
        dto.setStep(step.name());
        dto.setName(getStepDisplayName(step));
        dto.setDescription(getStepDescription(step));
        dto.setCategory(getStepCategory(step));
        dto.setOrder(step.getOrder()); // Set the order from the enum
        
        // Set default status values
        dto.setStatus("PENDING");
        dto.setIsRequired(step.isRequired());
        dto.setIsOverdue(false);
        dto.setRetryCount(0);
        dto.setDueDate(LocalDateTime.now().plusDays(7));
        return dto;
    }
    
    private static String getStepDisplayName(KycWorkflowStep step) {
        // Use the display name from the enum instead of generating it
        return step.getDisplayName();
    }
    
    private static String getStepDescription(KycWorkflowStep step) {
        // Use the description from the enum instead of the map
        return step.getDescription();
    }
    
    private static String getStepCategory(KycWorkflowStep step) {
        if (step.name().startsWith("UPLOAD")) {
            return "UPLOAD";
        } else if (step.name().startsWith("VERIFY")) {
            return "VERIFICATION";
        } else if (step.name().contains("APPROVAL")) {
            return "APPROVAL";
        } else if (step.name().equals("INITIATE_KYC")) {
            return "INITIATION";
        } else if (step.name().equals("KYC_COMPLETION")) {
            return "COMPLETION";
        }
        return "OTHER";
    }


    // Optional: Add builder pattern for cleaner object creation
    public static KycWorkflowStepDtoBuilder builder() {
        return new KycWorkflowStepDtoBuilder();
    }

    public static class KycWorkflowStepDtoBuilder {
        private String step;
        private String name;
        private String description;
        private String category;
        private Integer order;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Long completedBy;
        private String completedByName;
        private String notes;
        private LocalDateTime dueDate;
        private Boolean isRequired;
        private Boolean isOverdue;
        private Integer retryCount;

        public KycWorkflowStepDtoBuilder step(String step) {
            this.step = step;
            return this;
        }

        public KycWorkflowStepDtoBuilder step(KycWorkflowStep step) {
            this.step = step.name();
            this.name = step.getDisplayName();
            this.description = step.getDescription();
            this.order = step.getOrder();
            this.isRequired = step.isRequired();
            return this;
        }

        public KycWorkflowStepDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public KycWorkflowStepDtoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public KycWorkflowStepDtoBuilder category(String category) {
            this.category = category;
            return this;
        }

        public KycWorkflowStepDtoBuilder order(Integer order) {
            this.order = order;
            return this;
        }

        public KycWorkflowStepDtoBuilder status(String status) {
            this.status = status;
            return this;
        }

        public KycWorkflowStepDtoBuilder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public KycWorkflowStepDtoBuilder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public KycWorkflowStepDtoBuilder completedBy(Long completedBy) {
            this.completedBy = completedBy;
            return this;
        }

        public KycWorkflowStepDtoBuilder completedByName(String completedByName) {
            this.completedByName = completedByName;
            return this;
        }

        public KycWorkflowStepDtoBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public KycWorkflowStepDtoBuilder dueDate(LocalDateTime dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public KycWorkflowStepDtoBuilder isRequired(Boolean isRequired) {
            this.isRequired = isRequired;
            return this;
        }

        public KycWorkflowStepDtoBuilder isOverdue(Boolean isOverdue) {
            this.isOverdue = isOverdue;
            return this;
        }

        public KycWorkflowStepDtoBuilder retryCount(Integer retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public KycWorkflowStepDto build() {
            KycWorkflowStepDto dto = new KycWorkflowStepDto();
            dto.setStep(this.step);
            dto.setName(this.name);
            dto.setDescription(this.description);
            dto.setCategory(this.category);
            dto.setOrder(this.order);
            dto.setStatus(this.status);
            dto.setStartedAt(this.startedAt);
            dto.setCompletedAt(this.completedAt);
            dto.setCompletedBy(this.completedBy);
            dto.setCompletedByName(this.completedByName);
            dto.setNotes(this.notes);
            dto.setDueDate(this.dueDate);
            dto.setIsRequired(this.isRequired);
            dto.setIsOverdue(this.isOverdue);
            dto.setRetryCount(this.retryCount);
            return dto;
        }
    }
}