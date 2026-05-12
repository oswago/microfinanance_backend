package com.microfinance.borrower.repository;

import com.microfinance.borrower.entity.BorrowerGroup;
import com.microfinance.common.config.GeneralConfig;
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
    
    List<BorrowerGroup> findByStatus(GeneralConfig.GroupStatus status);
    
    @Query("SELECT g FROM BorrowerGroup g WHERE " +
           "LOWER(g.groupName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(g.groupCode) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<BorrowerGroup> searchGroups(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT COUNT(g) FROM BorrowerGroup g WHERE g.branch.id = :branchId AND g.status = 'ACTIVE'")
    Long countActiveGroupsByBranch(@Param("branchId") Long branchId);


    @Query("SELECT g FROM BorrowerGroup g LEFT JOIN FETCH g.groupLeader WHERE g.id = :id")
    Optional<BorrowerGroup> findByIdWithLeader(@Param("id") Long id);

    @Query("SELECT g FROM BorrowerGroup g WHERE g.id = :id")
    Optional<BorrowerGroup> findByIdWithMinimalData(@Param("id") Long id);

    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.group.id = :groupId")
    Long countMembersByGroupId(@Param("groupId") Long groupId);

}