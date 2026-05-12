package com.microfinance.loanapplications.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "recovery_case_stage_dates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_case_id")
    private RecoveryCase recoveryCase;

    @Column(name = "stage", nullable = false, length = 50)
    private String stage;

    @Column(name = "date", nullable = false)
    private LocalDate date;
}