package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.LoanDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanDocumentRepository extends JpaRepository<LoanDocument, Long> {

    List<LoanDocument> findByLoanId(Long loanId);

    Page<LoanDocument> findByLoanId(Long loanId, Pageable pageable);

    Optional<LoanDocument> findByDocumentReference(String documentReference);

    List<LoanDocument> findByLoanIdAndDocumentType(Long loanId, String documentType);

    @Query("SELECT ld FROM LoanDocument ld WHERE ld.loan.id = :loanId AND ld.isVerified = false")
    List<LoanDocument> findUnverifiedDocumentsByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT ld FROM LoanDocument ld WHERE ld.uploadedBy.id = :userId ORDER BY ld.uploadedAt DESC")
    List<LoanDocument> findByUploadedBy(@Param("userId") Long userId);

    @Query("SELECT COUNT(ld) FROM LoanDocument ld WHERE ld.loan.id = :loanId")
    long countByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT COUNT(ld) FROM LoanDocument ld WHERE ld.loan.id = :loanId AND ld.isVerified = true")
    long countVerifiedDocumentsByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT ld FROM LoanDocument ld WHERE " +
           "(:loanId IS NULL OR ld.loan.id = :loanId) AND " +
           "(:documentType IS NULL OR ld.documentType = :documentType) AND " +
           "(:isVerified IS NULL OR ld.isVerified = :isVerified)")
    List<LoanDocument> findDocumentsByFilters(
            @Param("loanId") Long loanId,
            @Param("documentType") String documentType,
            @Param("isVerified") Boolean isVerified);
}