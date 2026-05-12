package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.ReminderSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReminderScheduleRepository extends JpaRepository<ReminderSchedule, Long> {

    List<ReminderSchedule> findByStatusOrderByScheduledDateAsc(String status);

    List<ReminderSchedule> findByStatusAndScheduledDateLessThanEqual(String status, LocalDate date);

    List<ReminderSchedule> findByLoanIdAndStatus(Long loanId, String status);

    @Query("SELECT rs FROM ReminderSchedule rs WHERE rs.status = 'PENDING' AND rs.scheduledDate <= :date")
    List<ReminderSchedule> findPendingReminders(@Param("date") LocalDate date);
}