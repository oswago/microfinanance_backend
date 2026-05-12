package com.microfinance.borrower.service;

import com.microfinance.borrower.dto.BorrowerActivityDto;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerActivity;
import com.microfinance.borrower.repository.BorrowerActivityRepository;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.common.config.GeneralConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BorrowerActivityService {

    private final BorrowerActivityRepository borrowerActivityRepository;
    private final BorrowerRepository borrowerRepository;

    @Transactional
    public BorrowerActivityDto logActivity(BorrowerActivityDto activityDto) {
        Borrower borrower = borrowerRepository.findById(activityDto.getBorrowerId())
                .orElseThrow(() -> new RuntimeException("Borrower not found with id: " + activityDto.getBorrowerId()));

        BorrowerActivity activity = activityDto.toEntity();
        activity.setBorrower(borrower);
        activity.setActivityDate(LocalDateTime.now());

        BorrowerActivity savedActivity = borrowerActivityRepository.save(activity);
        log.info("Logged activity: {} for borrower: {}", activityDto.getActivityType(), borrower.getFullName());

        return BorrowerActivityDto.fromEntity(savedActivity);
    }

    @Transactional(readOnly = true)
    public Page<BorrowerActivityDto> getBorrowerActivities(Long borrowerId, Pageable pageable) {
        return borrowerActivityRepository.findByBorrowerId(borrowerId, pageable)
                .map(BorrowerActivityDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<BorrowerActivityDto> getRecentActivities(Long borrowerId, int limit) {
        List<BorrowerActivity> activities = borrowerActivityRepository.findByBorrowerIdOrderByActivityDateDesc(borrowerId);
        return activities.stream()
                .limit(limit)
                .map(BorrowerActivityDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BorrowerActivityDto.TimelineGroup> getActivityTimeline(Long borrowerId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<BorrowerActivity> activities = borrowerActivityRepository
                .findByBorrowerIdAndActivityDateAfterOrderByActivityDateDesc(borrowerId, startDate);

        List<BorrowerActivityDto> activityDtos = activities.stream()
                .map(BorrowerActivityDto::fromEntity)
                .collect(Collectors.toList());

        return groupActivitiesByPeriod(activityDtos);
    }

    private List<BorrowerActivityDto.TimelineGroup> groupActivitiesByPeriod(List<BorrowerActivityDto> activities) {
        Map<String, List<BorrowerActivityDto>> groupedActivities = activities.stream()
                .collect(Collectors.groupingBy(activity -> getPeriodForActivity(activity.getActivityDate())));

        return groupedActivities.entrySet().stream()
                .map(entry -> new BorrowerActivityDto.TimelineGroup(entry.getKey(), entry.getValue()))
                .sorted((g1, g2) -> {
                    LocalDateTime date1 = g1.getLatestActivity() != null ? g1.getLatestActivity() : LocalDateTime.MIN;
                    LocalDateTime date2 = g2.getLatestActivity() != null ? g2.getLatestActivity() : LocalDateTime.MIN;
                    return date2.compareTo(date1);
                })
                .collect(Collectors.toList());
    }

    private String getPeriodForActivity(LocalDateTime activityDate) {
        LocalDate activityLocalDate = activityDate.toLocalDate();
        LocalDate today = LocalDate.now();

        if (activityLocalDate.equals(today)) {
            return "Today";
        } else if (activityLocalDate.equals(today.minusDays(1))) {
            return "Yesterday";
        } else if (activityLocalDate.isAfter(today.minusDays(7))) {
            return "This Week";
        } else if (activityLocalDate.isAfter(today.minusDays(30))) {
            return "This Month";
        } else if (activityLocalDate.isAfter(today.minusDays(90))) {
            return "Last 3 Months";
        } else {
            return "Older";
        }
    }



    // Helper method for quick activity logging
    @Transactional
    public void logQuickActivity(Long borrowerId, GeneralConfig.BorrowerActivityType activityType,
                                 String description, Long performedBy, String referenceType, Long referenceId) {
        BorrowerActivityDto activityDto = new BorrowerActivityDto();
        activityDto.setBorrowerId(borrowerId);
        activityDto.setActivityType(activityType);
        activityDto.setDescription(description);
        activityDto.setPerformedBy(performedBy);
        activityDto.setReferenceType(referenceType);
        activityDto.setReferenceId(referenceId);
        
        logActivity(activityDto);
    }
}