package com.microfinance.borrower.repository;

import com.microfinance.borrower.entity.BorrowerGuarantor;
import com.microfinance.common.config.GeneralConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GuarantorRepository extends JpaRepository<BorrowerGuarantor, Long> {

    Page<BorrowerGuarantor> findByBorrowerId(Long borrowerId, Pageable pageable);

    @Query("SELECT g FROM BorrowerGuarantor g WHERE g.borrower.id = :borrowerId AND " +
           "(LOWER(g.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(g.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(g.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(g.occupation) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<BorrowerGuarantor> searchByBorrowerId(@Param("borrowerId") Long borrowerId, 
                                              @Param("query") String query, 
                                              Pageable pageable);

    long countByBorrowerId(Long borrowerId);

    long countByBorrowerIdAndStatus(Long borrowerId, GeneralConfig.GuarantorStatus status);

    boolean existsByBorrowerIdAndFullNameAndPhoneNumber(Long borrowerId, String fullName, String phoneNumber);
}