package com.microfinance.borrower.repository;

import com.microfinance.borrower.entity.Borrower;
import com.microfinance.common.config.GeneralConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowerRepository extends JpaRepository<Borrower, Long> {
    
    Optional<Borrower> findByBorrowerNumber(String borrowerNumber);
    
    boolean existsByPhoneNumber(String phoneNumber);
    
    boolean existsByIdentificationNumber(String identificationNumber);
    
    Page<Borrower> findByBranchId(Long branchId, Pageable pageable);
    
    Page<Borrower> findByGroupId(Long groupId, Pageable pageable);
    
    List<Borrower> findByStatus(GeneralConfig.BorrowerStatus status);
    
    List<Borrower> findByKycStatus(GeneralConfig.KycStatus kycStatus);
    
    @Query("SELECT b FROM Borrower b WHERE " +
           "LOWER(b.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "b.phoneNumber LIKE CONCAT('%', :search, '%') OR " +
           "b.borrowerNumber LIKE CONCAT('%', :search, '%')")
    Page<Borrower> searchBorrowers(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.branch.id = :branchId AND b.status = 'ACTIVE'")
    Long countActiveBorrowersByBranch(@Param("branchId") Long branchId);
    
    @Query("SELECT b FROM Borrower b WHERE b.group.id = :groupId AND b.status = 'ACTIVE'")
    List<Borrower> findActiveMembersByGroup(@Param("groupId") Long groupId);


    // Credit assessment queries
    @Query("SELECT b FROM Borrower b WHERE b.creditScore >= :minScore AND b.kycStatus = 'VERIFIED'")
    List<Borrower> findCreditworthyBorrowers(@Param("minScore") Integer minScore);

    @Query("SELECT b FROM Borrower b WHERE b.riskRating = :riskRating")
    List<Borrower> findByRiskRating(@Param("riskRating") GeneralConfig.RiskRating riskRating);

    // Portfolio queries
    /*
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.borrower.id = :borrowerId AND l.status = 'ACTIVE'")
    Long countActiveLoans(@Param("borrowerId") Long borrowerId);

    @Query("SELECT COALESCE(SUM(l.loanAmount), 0) FROM Loan l WHERE l.borrower.id = :borrowerId AND l.status = 'ACTIVE'")
    BigDecimal getTotalActiveLoanAmount(@Param("borrowerId") Long borrowerId);
   */


    @Query("SELECT b FROM Borrower b WHERE b.kycStatus = 'PENDING' AND b.status = 'ACTIVE'")
    List<Borrower> findPendingKycBorrowers();

    @Query("SELECT b FROM Borrower b WHERE b.createdAt >= :startDate AND b.createdAt <= :endDate")
    List<Borrower> findBorrowersCreatedBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                               @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.status = 'ACTIVE'")
    Long countAllActiveBorrowers();

    // Find by multiple statuses
    List<Borrower> findByStatusIn(List<GeneralConfig.BorrowerStatus> statuses);

    // Find by city/region
    List<Borrower> findByCity(String city);
    List<Borrower> findByState(String state);

    @Query("SELECT b FROM Borrower b WHERE b.id = :id")
    Optional<Borrower> findByIdWithMinimalData(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Borrower b WHERE b.id = :borrowerId AND b.group.id = :groupId")
    boolean isBorrowerInGroup(@Param("borrowerId") Long borrowerId, @Param("groupId") Long groupId);

    @Query("SELECT b.group.id FROM Borrower b WHERE b.id = :borrowerId")
    Long findGroupIdByBorrowerId(@Param("borrowerId") Long borrowerId);

    // In BorrowerRepository
    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.group.id = :groupId")
    int countByGroupId(@Param("groupId") Long groupId);

    boolean existsByGroupIdAndId(@Param("groupId") Long groupId, @Param("borrowerId") Long borrowerId);



    // Add these methods to your BorrowerRepository.java

// ==================== BORROWER COUNT METHODS ====================

    /**
     * Count active borrowers
     * Borrowers with status = 'ACTIVE'
     *
     * @return Count of active borrowers
     */
    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.status = 'ACTIVE'")
    Integer countActiveBorrowers();

    /**
     * Count borrowers with KYC verified
     *
     * @return Count of KYC verified borrowers
     */
    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.kycStatus = 'VERIFIED' AND b.status = 'ACTIVE'")
    Integer countKycVerified();

    /**
     * Count borrowers with KYC pending
     *
     * @return Count of KYC pending borrowers
     */
    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.kycStatus = 'PENDING' AND b.status = 'ACTIVE'")
    Integer countKycPending();

    /**
     * Count borrowers with KYC expired
     *
     * @return Count of KYC expired borrowers
     */
    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.kycStatus = 'EXPIRED' AND b.status = 'ACTIVE'")
    Integer countKycExpired();

    /**
     * Count borrowers with KYC rejected
     *
     * @return Count of KYC rejected borrowers
     */
    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.kycStatus = 'REJECTED' AND b.status = 'ACTIVE'")
    Integer countKycRejected();

    /**
     * Count total borrowers (all statuses)
     *
     * @return Total count of all borrowers
     */
    @Query("SELECT COUNT(b) FROM Borrower b")
    Integer countTotalBorrowers();

    /**
     * Count inactive borrowers
     *
     * @return Count of inactive borrowers
     */
    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.status = 'INACTIVE'")
    Integer countInactiveBorrowers();

    /**
     * Count borrowers created in a date range
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of borrowers created in period
     */
    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.createdAt BETWEEN :startDate AND :endDate")
    Integer countBorrowersCreatedInPeriod(@Param("startDate") java.time.LocalDateTime startDate,
                                          @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Count active borrowers by branch
     *
     * @param branchId Branch ID
     * @return Count of active borrowers in branch
     */
  //  @Query("SELECT COUNT(b) FROM Borrower b WHERE b.branch.id = :branchId AND b.status = 'ACTIVE'")
   // Integer countActiveBorrowersByBranch(@Param("branchId") Long branchId);

    /**
     * Count KYC verified borrowers by branch
     *
     * @param branchId Branch ID
     * @return Count of KYC verified borrowers in branch
     */
    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.branch.id = :branchId AND b.kycStatus = 'VERIFIED' AND b.status = 'ACTIVE'")
    Integer countKycVerifiedByBranch(@Param("branchId") Long branchId);

    /**
     * Count borrowers by risk rating
     *
     * @param riskRating Risk rating (LOW, MEDIUM, HIGH, CRITICAL)
     * @return Count of borrowers with given risk rating
     */
    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.riskRating = :riskRating AND b.status = 'ACTIVE'")
    Integer countByRiskRating(@Param("riskRating") GeneralConfig.RiskRating riskRating);

    /**
     * Get KYC statistics for reports
     *
     * @return Object array [totalBorrowers, verifiedCount, pendingCount, expiredCount, rejectedCount]
     */
    @Query("SELECT " +
            "COUNT(b), " +
            "SUM(CASE WHEN b.kycStatus = 'VERIFIED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.kycStatus = 'PENDING' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.kycStatus = 'EXPIRED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.kycStatus = 'REJECTED' THEN 1 ELSE 0 END) " +
            "FROM Borrower b WHERE b.status = 'ACTIVE'")
    Object[] getKYCStatistics();

    /**
     * Get KYC statistics by branch
     *
     * @param branchId Branch ID
     * @return Object array [totalBorrowers, verifiedCount, pendingCount, expiredCount]
     */
    @Query("SELECT " +
            "COUNT(b), " +
            "SUM(CASE WHEN b.kycStatus = 'VERIFIED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.kycStatus = 'PENDING' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.kycStatus = 'EXPIRED' THEN 1 ELSE 0 END) " +
            "FROM Borrower b WHERE b.branch.id = :branchId AND b.status = 'ACTIVE'")
    Object[] getKYCStatisticsByBranch(@Param("branchId") Long branchId);

    /**
     * Count borrowers with expiring KYC in next X days
     *
     * @param days Number of days to look ahead
     * @return Count of borrowers with KYC expiring soon
     */
    @Query("SELECT COUNT(b) FROM Borrower b " +
            "WHERE b.kycExpiryDate BETWEEN CURRENT_DATE AND CURRENT_DATE + :days " +
            "AND b.status = 'ACTIVE'")
    Integer countBorrowersWithKycExpiringInDays(@Param("days") Integer days);

    /**
     * Count borrowers with expired KYC
     *
     * @return Count of borrowers with expired KYC
     */
    @Query("SELECT COUNT(b) FROM Borrower b " +
            "WHERE b.kycExpiryDate < CURRENT_DATE " +
            "AND b.status = 'ACTIVE'")
    Integer countBorrowersWithExpiredKyc();


    /**
     * Count borrowers by gender
     *
     * @param gender Gender (MALE, FEMALE, OTHER)
     * @return Count of borrowers by gender
     */
    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.gender = :gender AND b.status = 'ACTIVE'")
    Integer countByGender(@Param("gender") String gender);

    /**
     * Count borrowers by age group
     *
     * @param minAge Minimum age
     * @param maxAge Maximum age
     * @return Count of borrowers in age range
     */
    @Query("SELECT COUNT(b) FROM Borrower b " +
            "WHERE b.dateOfBirth IS NOT NULL " +
            "AND TIMESTAMPDIFF(YEAR, b.dateOfBirth, CURRENT_DATE) BETWEEN :minAge AND :maxAge " +
            "AND b.status = 'ACTIVE'")
    Integer countByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);


    /**
     * Get borrower registration trend by month
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of objects [year, month, count]
     */
    @Query("SELECT YEAR(b.createdAt), MONTH(b.createdAt), COUNT(b) " +
            "FROM Borrower b " +
            "WHERE b.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY YEAR(b.createdAt), MONTH(b.createdAt) " +
            "ORDER BY YEAR(b.createdAt), MONTH(b.createdAt)")
    List<Object[]> getBorrowerRegistrationTrend(@Param("startDate") java.time.LocalDateTime startDate,
                                                @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Count borrowers with active loans
     *
     * @return Count of borrowers who have active loans
     */
    @Query("SELECT COUNT(DISTINCT l.borrower.id) FROM Loan l " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT')")
    Integer countBorrowersWithActiveLoans();

    /**
     * Count borrowers with delinquent loans
     *
     * @return Count of borrowers with delinquent loans
     */
    @Query("SELECT COUNT(DISTINCT l.borrower.id) FROM Loan l " +
            "WHERE l.status = 'DELINQUENT' OR l.daysDelinquent > 0")
    Integer countBorrowersWithDelinquentLoans();

    /**
     * Count borrowers with multiple active loans
     *
     * @return Count of borrowers with more than one active loan
     */
    @Query("SELECT COUNT(DISTINCT l.borrower.id) FROM Loan l " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT') " +
            "GROUP BY l.borrower.id " +
            "HAVING COUNT(l.id) > 1")
    Integer countBorrowersWithMultipleLoans();

    /**
     * Get top borrowers by total loan amount
     *
     * @param limit Maximum number of results
     * @return List of objects [borrowerId, borrowerName, totalLoanAmount]
     */
    @Query(value = "SELECT b.id, CONCAT(b.first_name, ' ', b.last_name), SUM(l.principal_amount) " +
            "FROM borrowers b " +
            "JOIN loans l ON l.borrower_id = b.id " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT') " +
            "GROUP BY b.id, b.first_name, b.last_name " +
            "ORDER BY SUM(l.principal_amount) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopBorrowersByLoanAmount(@Param("limit") Integer limit);

    /**
     * Get borrower KYC status distribution
     *
     * @return Map-like list of [kycStatus, count]
     */
    @Query("SELECT b.kycStatus, COUNT(b) FROM Borrower b " +
            "WHERE b.status = 'ACTIVE' " +
            "GROUP BY b.kycStatus")
    List<Object[]> getKYCStatusDistribution();

    /**
     * Get borrower status distribution
     *
     * @return List of objects [status, count]
     */
    @Query("SELECT b.status, COUNT(b) FROM Borrower b GROUP BY b.status")
    List<Object[]> getBorrowerStatusDistribution();





}