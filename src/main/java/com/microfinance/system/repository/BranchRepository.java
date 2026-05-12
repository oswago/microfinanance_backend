package com.microfinance.system.repository;

import com.microfinance.system.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    
    /**
     * Find branch by unique code
     */
    Optional<Branch> findByCode(String code);
    
    /**
     * Find all active branches
     */
    List<Branch> findByActiveTrue();
    
    /**
     * Find branches by type
     */
    List<Branch> findByType(Branch.BranchType type);
    
    /**
     * Find active branches by type
     */
    List<Branch> findByTypeAndActiveTrue(Branch.BranchType type);
    
    /**
     * Find child branches by parent branch
     */
    List<Branch> findByParentBranchId(Long parentBranchId);
    
    /**
     * Find active child branches by parent branch
     */
    List<Branch> findByParentBranchIdAndActiveTrue(Long parentBranchId);
    
    /**
     * Check if branch code exists (for validation)
     */
    boolean existsByCode(String code);
    
    /**
     * Check if branch code exists excluding a specific branch (for updates)
     */
    boolean existsByCodeAndIdNot(String code, Long id);
    
    /**
     * Find branches by name containing (for search)
     */
    List<Branch> findByNameContainingIgnoreCase(String name);
    
    /**
     * Find head office (there should be only one)
     */
    @Query("SELECT b FROM Branch b WHERE b.type = 'HEAD_OFFICE' AND b.active = true")
    Optional<Branch> findHeadOffice();
    
    /**
     * Count active branches by type
     */
    long countByTypeAndActiveTrue(Branch.BranchType type);
    
    /**
     * Find branches with no parent (top-level branches)
     */
    @Query("SELECT b FROM Branch b WHERE b.parentBranch IS NULL AND b.active = true")
    List<Branch> findTopLevelBranches();



    @Query(value = """
    SELECT 
        b.id,
        b.name,
        b.code,
        COUNT(la.id) as totalApplications,
        COUNT(CASE WHEN la.status = 'APPROVED' THEN 1 END) as approvedCount,
        COUNT(CASE WHEN la.status = 'REJECTED' THEN 1 END) as rejectedCount,
        AVG(CASE 
            WHEN la.status = 'APPROVED' 
                 AND la.approved_date IS NOT NULL 
                 AND la.submitted_date IS NOT NULL 
            THEN TIMESTAMPDIFF(HOUR, la.submitted_date, la.approved_date) 
            END) as avgProcessingTime,
        COALESCE(SUM(CASE 
            WHEN la.status = 'APPROVED' 
            THEN la.applied_amount 
            END), 0) as totalApprovedAmount,
        COUNT(CASE 
            WHEN la.status = 'APPROVED' 
                 AND la.approved_date IS NOT NULL 
                 AND la.submitted_date IS NOT NULL 
                 AND TIMESTAMPDIFF(HOUR, la.submitted_date, la.approved_date) > 24 
            THEN 1 
            END) as slaBreaches
    FROM branches b
    LEFT JOIN loan_applications la ON b.id = la.branch_id
    WHERE (:startDate IS NULL OR la.submitted_date >= :startDate)
      AND (:endDate IS NULL OR la.submitted_date <= :endDate)
    GROUP BY b.id, b.name, b.code
    ORDER BY approvedCount DESC
""", nativeQuery = true)
    List<Object[]> findBranchApprovalStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}