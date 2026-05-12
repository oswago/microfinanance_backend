// src/main/java/com/microfinance/system/mapper/ActivityLogServiceMapper.java
package com.microfinance.system.mapper;

import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.system.dto.ActivityLogDto;
import com.microfinance.system.entity.ActivityLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ActivityLogServiceMapper {

    private final BorrowerRepository borrowerRepository;
    // Remove BorrowerService dependency if not needed
    // private final BorrowerService borrowerService;

    public ActivityLogDto toDto(ActivityLog activityLog) {
        if (activityLog == null) {
            return null;
        }

        ActivityLogDto dto = new ActivityLogDto();
        dto.setId(activityLog.getId());
        dto.setBorrowerId(activityLog.getBorrowerId());
        dto.setActivityType(activityLog.getActivityType());
        dto.setDescription(activityLog.getDescription());
        dto.setPerformedBy(activityLog.getPerformedBy());
        dto.setIpAddress(activityLog.getIpAddress());
        dto.setActivityDate(activityLog.getActivityDate());
        dto.setGroupId(activityLog.getGroupId());
        dto.setLoanId(activityLog.getLoanId());
        dto.setDocumentId(activityLog.getDocumentId());
        dto.setOldValue(activityLog.getOldValue());
        dto.setNewValue(activityLog.getNewValue());

        // Populate enhanced fields
        populateEnhancedFields(dto, activityLog);

        return dto;
    }

    public ActivityLog toEntity(ActivityLogDto dto) {
        if (dto == null) {
            return null;
        }

        ActivityLog activityLog = new ActivityLog();
        activityLog.setId(dto.getId());
        activityLog.setBorrowerId(dto.getBorrowerId());
        activityLog.setActivityType(dto.getActivityType());
        activityLog.setDescription(dto.getDescription());
        activityLog.setPerformedBy(dto.getPerformedBy());
        activityLog.setIpAddress(dto.getIpAddress());
        activityLog.setActivityDate(dto.getActivityDate());
        activityLog.setGroupId(dto.getGroupId());
        activityLog.setLoanId(dto.getLoanId());
        activityLog.setDocumentId(dto.getDocumentId());
        activityLog.setOldValue(dto.getOldValue());
        activityLog.setNewValue(dto.getNewValue());

        return activityLog;
    }

    private void populateEnhancedFields(ActivityLogDto dto, ActivityLog activityLog) {
        // Populate borrower information
        if (activityLog.getBorrowerId() != null) {
            try {
                Borrower borrower = borrowerRepository.findById(activityLog.getBorrowerId()).orElse(null);
                if (borrower != null) {
                    dto.setBorrowerName(borrower.getFirstName() + " " + borrower.getLastName());
                    dto.setBorrowerNumber(borrower.getBorrowerNumber());
                } else {
                    dto.setBorrowerName("Unknown Borrower");
                    dto.setBorrowerNumber("N/A");
                }
            } catch (Exception e) {
                dto.setBorrowerName("Error Loading Borrower");
                dto.setBorrowerNumber("N/A");
            }
        }

        // Populate performed by user name
        if (activityLog.getPerformedBy() != null) {
            // TODO: Integrate with UserService when available
            dto.setPerformedByName("User " + activityLog.getPerformedBy());
        }

        // Populate group name
        if (activityLog.getGroupId() != null) {
            // TODO: Integrate with BorrowerGroupService when available
            dto.setGroupName("Group " + activityLog.getGroupId());
        }

        // Populate loan number
        if (activityLog.getLoanId() != null) {
            // TODO: Integrate with LoanService when available
            dto.setLoanNumber("Loan " + activityLog.getLoanId());
        }
    }

    public List<ActivityLogDto> toDtoList(List<ActivityLog> activityLogs) {
        return activityLogs.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}