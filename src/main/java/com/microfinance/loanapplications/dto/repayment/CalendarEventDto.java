// CalendarEventDto.java
package com.microfinance.loanapplications.dto.repayment;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventDto {
    
    private Long id;
    private String title;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean allDay;
    private String status; // paid, overdue, pending
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private Long loanId;
    private String loanAccountNumber;
    private String borrowerName;
    private Integer installmentNumber;
    private String backgroundColor;
    private String borderColor;
    private String textColor;
    private String description;
    private String url;
    
    // Helper method to set colors based on status
    public void setColorsByStatus() {
        if (status == null) {
            this.backgroundColor = "#3b82f6"; // blue for default
            this.borderColor = "#3b82f6";
            this.textColor = "#ffffff";
        } else {
            switch (status.toLowerCase()) {
                case "paid":
                    this.backgroundColor = "#10b981"; // green
                    this.borderColor = "#10b981";
                    this.textColor = "#ffffff";
                    break;
                case "overdue":
                    this.backgroundColor = "#ef4444"; // red
                    this.borderColor = "#ef4444";
                    this.textColor = "#ffffff";
                    break;
                case "pending":
                    this.backgroundColor = "#f59e0b"; // orange
                    this.borderColor = "#f59e0b";
                    this.textColor = "#ffffff";
                    break;
                default:
                    this.backgroundColor = "#3b82f6"; // blue
                    this.borderColor = "#3b82f6";
                    this.textColor = "#ffffff";
                    break;
            }
        }
    }
    
    // Builder with default colors
    public static class CalendarEventDtoBuilder {
        public CalendarEventDto build() {
            CalendarEventDto dto = new CalendarEventDto();
            dto.setId(this.id);
            dto.setTitle(this.title);
            dto.setStartDate(this.startDate);
            dto.setEndDate(this.endDate);
            dto.setAllDay(this.allDay != null ? this.allDay : true);
            dto.setStatus(this.status);
            dto.setAmount(this.amount);
            dto.setPaidAmount(this.paidAmount);
            dto.setLoanId(this.loanId);
            dto.setLoanAccountNumber(this.loanAccountNumber);
            dto.setBorrowerName(this.borrowerName);
            dto.setInstallmentNumber(this.installmentNumber);
            dto.setDescription(this.description);
            dto.setUrl(this.url);
            
            // Set colors based on status
            dto.setColorsByStatus();
            
            return dto;
        }
    }
}