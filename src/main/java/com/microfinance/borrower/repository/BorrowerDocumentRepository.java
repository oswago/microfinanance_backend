package com.microfinance.borrower.repository;

import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.common.config.DocumentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowerDocumentRepository extends JpaRepository<BorrowerDocument, Long> {
    
    List<BorrowerDocument> findByBorrowerId(Long borrowerId);
    
    List<BorrowerDocument> findByBorrowerIdAndDocumentType(Long borrowerId, DocumentConfig.DocumentType documentType);
    
    List<BorrowerDocument> findByBorrowerIdAndStatus(Long borrowerId, DocumentConfig.DocumentStatus status);
    
    Optional<BorrowerDocument> findByFileName(String fileName);
    
    @Query("SELECT COUNT(d) FROM BorrowerDocument d WHERE d.borrower.id = :borrowerId AND d.documentType = :documentType AND d.status = 'VERIFIED'")
    Long countVerifiedDocumentsByType(@Param("borrowerId") Long borrowerId, 
                                     @Param("documentType") DocumentConfig.DocumentType documentType);
    
    @Query("SELECT d FROM BorrowerDocument d WHERE d.borrower.id = :borrowerId AND d.status = 'PENDING'")
    List<BorrowerDocument> findPendingDocumentsByBorrower(@Param("borrowerId") Long borrowerId);
    
    @Query("SELECT d FROM BorrowerDocument d WHERE d.expiryDate < CURRENT_DATE AND d.status = 'VERIFIED'")
    List<BorrowerDocument> findExpiredDocuments();
}