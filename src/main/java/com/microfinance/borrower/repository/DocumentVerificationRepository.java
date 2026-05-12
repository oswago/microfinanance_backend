package com.microfinance.borrower.repository;

import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.borrower.entity.DocumentVerification;
import com.microfinance.common.config.DocumentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface DocumentVerificationRepository extends JpaRepository<DocumentVerification, Long> {
    
    List<DocumentVerification> findByBorrowerId(Long borrowerId);
    
    List<DocumentVerification> findByBorrowerIdAndVerificationStatus(Long borrowerId, DocumentVerification.VerificationStatus status);
    
    List<DocumentVerification> findByBorrowerDocumentId(Long borrowerDocumentId);

    Optional<DocumentVerification> findByBorrowerIdAndDocumentType(Long borrowerId, DocumentConfig.DocumentType documentType);
    @Query("SELECT dv FROM DocumentVerification dv WHERE dv.borrower.id = :borrowerId AND dv.isActive = true")
    List<DocumentVerification> findActiveByBorrowerId(@Param("borrowerId") Long borrowerId);
    
    @Query("SELECT dv FROM DocumentVerification dv WHERE dv.borrower.id = :borrowerId AND dv.verificationStatus = 'PENDING'")
    List<DocumentVerification> findPendingByBorrowerId(@Param("borrowerId") Long borrowerId);
    
    @Query("SELECT dv FROM DocumentVerification dv WHERE dv.borrower.id = :borrowerId AND dv.verificationStatus = 'VERIFIED'")
    List<DocumentVerification> findVerifiedByBorrowerId(@Param("borrowerId") Long borrowerId);
    
    @Query("SELECT COUNT(dv) FROM DocumentVerification dv WHERE dv.borrower.id = :borrowerId AND dv.verificationStatus = 'VERIFIED' AND dv.isActive = true")
    Long countActiveVerifiedDocuments(@Param("borrowerId") Long borrowerId);
    
    List<DocumentVerification> findByVerificationStatus(DocumentVerification.VerificationStatus status);
    
    @Query("SELECT dv FROM DocumentVerification dv WHERE dv.expiryDate < CURRENT_DATE AND dv.verificationStatus = 'VERIFIED' AND dv.isActive = true")
    List<DocumentVerification> findExpiredVerifications();

    List<DocumentVerification> findByDocumentType(DocumentConfig.DocumentType documentType);

    List<DocumentVerification> findByBorrowerIdAndIsActive(Long borrowerId, Boolean isActive);

    @Query("SELECT dv FROM DocumentVerification dv WHERE dv.borrower.id = :borrowerId AND dv.documentType = :documentType AND dv.isActive = true")
    Optional<DocumentVerification> findActiveByBorrowerIdAndDocumentType(
            @Param("borrowerId") Long borrowerId,
            @Param("documentType") DocumentConfig.DocumentType documentType);

    boolean existsByBorrowerIdAndDocumentTypeInAndVerificationStatus(
            Long borrowerId,
            List<DocumentConfig.DocumentType> documentTypes,
            DocumentVerification.VerificationStatus verificationStatus);

    @Query("SELECT MAX(dv.verificationDate) FROM DocumentVerification dv WHERE dv.borrower.id = :borrowerId AND dv.documentType IN :documentTypes AND dv.verificationStatus = 'VERIFIED'")
    LocalDateTime findLatestVerificationDateByBorrowerAndTypes(@Param("borrowerId") Long borrowerId,
                                                               @Param("documentTypes") List<String> documentTypes);


    List<DocumentVerification> findByBorrowerIdAndIsActiveTrue(Long borrowerId);
}