package com.microfinance.loanapplications.dto.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCommentDto {
    
    @NotBlank(message = "Comment cannot be empty")
    @Size(min = 1, max = 2000, message = "Comment must be between 1 and 2000 characters")
    private String comment;
    
    private boolean isInternal;
    
    private String parentCommentId;
    
    @Builder.Default
    private boolean sendNotification = true;
}