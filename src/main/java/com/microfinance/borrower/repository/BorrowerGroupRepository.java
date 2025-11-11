package com.microfinance.borrower.repository;

import com.microfinance.borrower.entity.BorrowerGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowerGroupRepository extends JpaRepository<BorrowerGroup, Long> {
    
    Optional<BorrowerGroup> findByGroupCode(String groupCode);
    
    Page<BorrowerGroup> findByBranchId(Long branchId, Pageable pageable);
    
    List<BorrowerGroup> findByStatus(BorrowerGroup.GroupStatus status);
    
    @Query("SELECT g FROM BorrowerGroup g WHERE " +
           "LOWER(g.groupName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(g.groupCode) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<BorrowerGroup> searchGroups(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT COUNT(g) FROM BorrowerGroup g WHERE g.branch.id = :branchId AND g.status = 'ACTIVE'")
    Long countActiveGroupsByBranch(@Param("branchId") Long branchId);
}