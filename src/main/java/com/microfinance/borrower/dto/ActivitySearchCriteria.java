package com.microfinance.borrower.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ActivitySearchCriteria {
    private Long borrowerId;
    private List<BorrowerActivityDto.ActivityType> activityTypes;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long performedBy;
    private String referenceType;
    private Long referenceId;
    private Boolean financialActivitiesOnly;
    private Boolean highPriorityOnly;
    private String searchTerm;
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "activityDate";
    private String sortDirection = "DESC";

    private BorrowerActivityDto.ActivityType activityType;
    private String searchText;

}