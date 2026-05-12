package com.microfinance.borrower.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microfinance.base.entity.BaseEntity;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.system.entity.Branch;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "borrower_groups")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BorrowerGroup extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String groupCode;

    @NotBlank
    private String groupName;

    private String description;

    @Enumerated(EnumType.STRING)
    @NotNull
    private GeneralConfig.GroupType groupType;

    @Enumerated(EnumType.STRING)
    @NotNull
    private GeneralConfig.GroupStatus status = GeneralConfig.GroupStatus.ACTIVE;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    @JsonIgnore
    private Branch branch;

    private Integer maxMembers;


    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    @JsonIgnore // ADD THIS - CRITICAL FIX
    private List<Borrower> members = new ArrayList<>();


    @Column(name = "member_count")
    private Integer memberCount = 0;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(columnDefinition = "TEXT")
    private String meetingSchedule; // e.g., "Every Monday at 2 PM"

    private String meetingLocation;

    // Group management fields
    private LocalDateTime formationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_leader_id")
    @JsonIgnore
    private Borrower groupLeader;

    @Column(name = "group_leader_name")
    private String groupLeaderName;

    @Column(name = "group_leader_phone")
    private String groupLeaderPhone;

    // Group performance metrics
    private Integer totalLoansTaken;
    private BigDecimal totalLoanAmount;
    private BigDecimal totalSavings;
    private Integer successfulLoans;

    // Meeting management
    private String meetingFrequency; // WEEKLY, BI_WEEKLY, MONTHLY
    @Column(name = "meeting_day", columnDefinition = "INTEGER")
    private DayOfWeek meetingDay;
    private LocalDateTime meetingTime;

    // Group hierarchy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    @JsonIgnore
    private BorrowerGroup parentGroup; // For federation structures

    @OneToMany(mappedBy = "parentGroup", fetch = FetchType.LAZY)
    @JsonIgnore // ADD THIS TO PREVENT RECURSION IN SUBGROUPS
    private List<BorrowerGroup> subGroups = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private GeneralConfig.JointLiabilityType jointLiabilityType;

    public Integer getCurrentMemberCount() {
        return memberCount != null ? memberCount : 0;
    }


}