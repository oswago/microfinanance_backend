package com.microfinance.borrower.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.system.entity.Branch;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "borrower_groups")
@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowerGroup extends BaseEntity {

    @NotBlank
    @Column(unique = true)
    private String groupCode;

    @NotBlank
    private String groupName;

    private String description;

    @Enumerated(EnumType.STRING)
    @NotNull
    private GroupType groupType;

    @Enumerated(EnumType.STRING)
    @NotNull
    private GroupStatus status = GroupStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    private Integer maxMembers;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<Borrower> members = new ArrayList<>();

    @Column(name = "created_by")
    private Long createdBy;

    @Column(columnDefinition = "TEXT")
    private String meetingSchedule; // e.g., "Every Monday at 2 PM"

    private String meetingLocation;

    // ADD THESE FOR GROUP MANAGEMENT:
    private LocalDate formationDate;
    private String groupLeaderName;
    private String groupLeaderPhone;

    // Group performance metrics
    private Integer totalLoansTaken;
    private BigDecimal totalLoanAmount;
    private BigDecimal totalSavings;
    private Integer successfulLoans;

    // Meeting management
    private String meetingFrequency; // WEEKLY, BI_WEEKLY, MONTHLY
    private DayOfWeek meetingDay;
    private LocalTime meetingTime;

    // Group hierarchy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    private BorrowerGroup parentGroup; // For federation structures

    @OneToMany(mappedBy = "parentGroup", fetch = FetchType.LAZY)
    private List<BorrowerGroup> subGroups = new ArrayList<>();


    @Enumerated(EnumType.STRING)
    private JointLiabilityType jointLiabilityType;

    public enum GroupType {
        JOINT_LIABILITY, SAVINGS, AGRICULTURAL, WOMEN, YOUTH, COMMUNITY
    }

    public enum GroupStatus {
        ACTIVE, INACTIVE, DISSOLVED
    }

    public enum JointLiabilityType {
        FULL, PARTIAL, NONE
    }

    public Integer getCurrentMemberCount() {
        return members != null ? members.size() : 0;
    }
}