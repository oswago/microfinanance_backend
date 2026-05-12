package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseNoteDto {
    private Long id;
    private String content;
    private String createdBy;
    private LocalDateTime createdDate;
    private Long createdById;
    private String type;
    private List<AttachmentDto> attachments;
}