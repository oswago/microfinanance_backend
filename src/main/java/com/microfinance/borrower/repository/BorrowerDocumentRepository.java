package com.microfinance.borrower.repository;

import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.common.config.GeneralConfig;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Query("SELECT d FROM BorrowerDocument d WHERE d.id IN :documentIds")
    List<BorrowerDocument> findDocumentsByIdsWithoutRelations(@Param("documentIds") List<Long> documentIds);


    boolean existsByBorrowerIdAndDocumentTypeIn(Long borrowerId, List<String> documentTypes);

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM borrower_documents bd " +
            "WHERE bd.borrower_id = :borrowerId AND bd.document_type IN (:documentTypes)",
            nativeQuery = true)
    boolean existsByBorrowerIdAndDocumentTypeInImp(@Param("borrowerId") Long borrowerId,
                                                @Param("documentTypes") List<String> documentTypes);


    @Query("SELECT MAX(bd.createdAt) FROM BorrowerDocument bd WHERE bd.borrower.id = :borrowerId AND bd.documentType IN :documentTypes")
    LocalDateTime findLatestUploadDateByBorrowerAndTypes(@Param("borrowerId") Long borrowerId,
                                                         @Param("documentTypes") List<String> documentTypes);



    List<BorrowerDocument> findByStatus(DocumentConfig.DocumentStatus status);

    List<BorrowerDocument> findByBorrowerIdAndStatus(Long borrowerId, String status);

    @Query("SELECT d FROM BorrowerDocument d WHERE d.status = :status AND (:branchId IS NULL OR d.borrower.branch.id = :branchId)")
    List<BorrowerDocument> findByStatusAndBranch(@Param("status") String status, @Param("branchId") Long branchId);



    // ==================== KYC DOCUMENT DISTRIBUTION ====================

    /**
     * Get KYC document distribution by document type
     * Returns count of verified documents by type
     */
    @Query("SELECT bd.documentType, COUNT(bd) FROM BorrowerDocument bd " +
            "WHERE bd.status = 'VERIFIED' " +
            "GROUP BY bd.documentType")
    List<Object[]> getKYCDistributionByDocumentType();

    /**
     * Get document distribution by status
     */
    @Query("SELECT bd.status, COUNT(bd) FROM BorrowerDocument bd " +
            "GROUP BY bd.status")
    List<Object[]> getDocumentDistributionByStatus();

    // ==================== EXPIRY QUERIES ====================

    /**
     * Count expired documents
     */
    @Query("SELECT COUNT(bd) FROM BorrowerDocument bd " +
            "WHERE bd.expiryDate < CURRENT_DATE")
    Long countExpiredDocuments();

    /**
     * Count documents expiring in next X days
     */
    @Query("SELECT COUNT(bd) FROM BorrowerDocument bd " +
            "WHERE bd.expiryDate BETWEEN CURRENT_DATE AND CURRENT_DATE + :days")
    Long countDocumentsExpiringInDays(@Param("days") Integer days);

    /**
     * Find documents expiring in next X days
     */
    @Query("SELECT bd FROM BorrowerDocument bd " +
            "WHERE bd.expiryDate BETWEEN CURRENT_DATE AND CURRENT_DATE + :days")
    List<BorrowerDocument> findDocumentsExpiringInDays(@Param("days") Integer days);

    /**
     * Find documents by borrower with expiry date before given date
     */
    @Query("SELECT bd FROM BorrowerDocument bd " +
            "WHERE bd.borrower.id = :borrowerId " +
            "AND bd.expiryDate < :date")
    List<BorrowerDocument> findExpiredDocumentsByBorrower(@Param("borrowerId") Long borrowerId,
                                                          @Param("date") LocalDate date);

    // ==================== VERIFICATION QUERIES ====================

    /**
     * Count pending verifications
     */
    @Query("SELECT COUNT(bd) FROM BorrowerDocument bd " +
            "WHERE bd.status = 'PENDING'")
    Long countPendingVerifications();

    /**
     * Find pending verifications
     */
    @Query("SELECT bd FROM BorrowerDocument bd " +
            "WHERE bd.status = 'PENDING' " +
            "ORDER BY bd.createdAt ASC")
    List<BorrowerDocument> findPendingVerifications(Pageable pageable);

    /**
     * Count verified documents in date range
     */
    @Query("SELECT COUNT(bd) FROM BorrowerDocument bd " +
            "WHERE bd.status = 'VERIFIED' " +
            "AND bd.verifiedAt BETWEEN :startDate AND :endDate")
    Long countVerifiedDocumentsInPeriod(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // ==================== BORROWER DOCUMENT STATUS ====================

    /**
     * Check if borrower has verified KYC document
     */
    @Query("SELECT CASE WHEN COUNT(bd) > 0 THEN true ELSE false END FROM BorrowerDocument bd " +
            "WHERE bd.borrower.id = :borrowerId " +
            "AND bd.documentType IN :kycTypes " +
            "AND bd.status = 'VERIFIED'")
    boolean hasVerifiedKYCDocument(@Param("borrowerId") Long borrowerId,
                                   @Param("kycTypes") List<DocumentConfig.DocumentType> kycTypes);

    /**
     * Get latest document by type for borrower
     */
    @Query("SELECT bd FROM BorrowerDocument bd " +
            "WHERE bd.borrower.id = :borrowerId " +
            "AND bd.documentType = :documentType " +
            "ORDER BY bd.createdAt DESC")
    List<BorrowerDocument> findLatestDocumentByType(@Param("borrowerId") Long borrowerId,
                                                    @Param("documentType") DocumentConfig.DocumentType documentType,
                                                    Pageable pageable);



    // Add this method
    @Query("SELECT bd FROM BorrowerDocument bd WHERE bd.borrower.id = :borrowerId ORDER BY bd.createdAt DESC")
    List<BorrowerDocument> findByBorrowerIdOrderByCreatedAtDesc(@Param("borrowerId") Long borrowerId, Pageable pageable);




}