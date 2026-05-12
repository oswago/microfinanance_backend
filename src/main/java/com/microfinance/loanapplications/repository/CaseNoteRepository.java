package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.CaseNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseNoteRepository extends JpaRepository<CaseNote, Long> {

    List<CaseNote> findByRecoveryCaseIdOrderByCreatedAtDesc(Long recoveryCaseId);
}