package com.microfinance.borrower.repository;

import com.microfinance.borrower.entity.Borrower;
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
    
    List<Borrower> findByStatus(Borrower.BorrowerStatus status);
    
    List<Borrower> findByKycStatus(Borrower.KycStatus kycStatus);
    
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
    List<Borrower> findByRiskRating(@Param("riskRating") Borrower.RiskRating riskRating);

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
    List<Borrower> findByStatusIn(List<Borrower.BorrowerStatus> statuses);

    // Find by city/region
    List<Borrower> findByCity(String city);
    List<Borrower> findByState(String state);




}