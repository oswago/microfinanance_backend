package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.ApprovalComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApprovalCommentRepository extends JpaRepository<ApprovalComment, Long> {
    
    // Basic queries
    List<ApprovalComment> findByLoanApplicationIdOrderByCreatedAtDesc(Long applicationId);
    
    List<ApprovalComment> findByLoanApplicationIdAndIsDeletedFalseOrderByCreatedAtDesc(Long applicationId);
    
    @Query("SELECT c FROM ApprovalComment c WHERE c.loanApplication.id = :applicationId AND c.parentCommentId IS NULL ORDER BY c.createdAt DESC")
    List<ApprovalComment> findRootCommentsByApplicationId(@Param("applicationId") Long applicationId);
    
    @Query("SELECT c FROM ApprovalComment c WHERE c.parentCommentId = :parentId ORDER BY c.createdAt ASC")
    List<ApprovalComment> findRepliesByParentId(@Param("parentId") Long parentId);
    
    // User-specific queries
    List<ApprovalComment> findByCommenterIdOrderByCreatedAtDesc(Long commenterId);
    
    @Query("SELECT c FROM ApprovalComment c WHERE c.commenter.id = :commenterId AND c.createdAt >= :since ORDER BY c.createdAt DESC")
    List<ApprovalComment> findByCommenterAndDateRange(@Param("commenterId") Long commenterId,
                                                       @Param("since") LocalDateTime since);
    
    // Internal/external filtering
    List<ApprovalComment> findByLoanApplicationIdAndIsInternalFalseOrderByCreatedAtDesc(Long applicationId);
    
    List<ApprovalComment> findByLoanApplicationIdAndIsInternalTrueOrderByCreatedAtDesc(Long applicationId);
    
    // FIXED: Mention queries - Use native query for text search
    @Query(value = "SELECT * FROM approval_comments c " +
           "WHERE c.mention_user_ids LIKE CONCAT('%', :userId, '%') " +
           "AND c.is_deleted = false " +
           "ORDER BY c.created_at DESC", 
           nativeQuery = true)
    List<ApprovalComment> findCommentsMentioningUser(@Param("userId") Long userId);
    
    @Query(value = "SELECT * FROM approval_comments c " +
           "WHERE c.mention_user_ids LIKE CONCAT('%', :userId, '%') " +
           "AND c.created_at >= :since " +
           "AND c.is_deleted = false " +
           "ORDER BY c.created_at DESC", 
           nativeQuery = true)
    List<ApprovalComment> findCommentsMentioningUserSince(@Param("userId") Long userId,
                                                           @Param("since") LocalDateTime since);
    
    // Recent queries
    @Query("SELECT c FROM ApprovalComment c WHERE c.loanApplication.id = :applicationId AND c.createdAt >= :since ORDER BY c.createdAt DESC")
    List<ApprovalComment> findRecentCommentsByApplication(@Param("applicationId") Long applicationId,
                                                           @Param("since") LocalDateTime since);
    
    // Pagination
    @Query("SELECT c FROM ApprovalComment c WHERE c.loanApplication.id = :applicationId ORDER BY c.createdAt DESC")
    List<ApprovalComment> findPaginatedByApplicationId(@Param("applicationId") Long applicationId,
                                                        Pageable pageable);
    
    // Statistics
    @Query("SELECT COUNT(c) FROM ApprovalComment c WHERE c.loanApplication.id = :applicationId")
    Long countCommentsByApplicationId(@Param("applicationId") Long applicationId);
    
    @Query("SELECT COUNT(c) FROM ApprovalComment c WHERE c.commenter.id = :commenterId")
    Long countCommentsByCommenter(@Param("commenterId") Long commenterId);
    
    @Query("SELECT c.commenter.id, COUNT(c) FROM ApprovalComment c WHERE c.loanApplication.id = :applicationId GROUP BY c.commenter.id")
    List<Object[]> countCommentsByCommenterForApplication(@Param("applicationId") Long applicationId);
    
    // Date range queries
    @Query("SELECT c FROM ApprovalComment c WHERE c.createdAt BETWEEN :startDate AND :endDate ORDER BY c.createdAt DESC")
    List<ApprovalComment> findCommentsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);
    
    // Soft delete
    @Modifying
    @Transactional
    @Query("UPDATE ApprovalComment c SET c.isDeleted = true, c.updatedAt = :now, c.updatedBy = :userId WHERE c.id = :commentId")
    void softDeleteComment(@Param("commentId") Long commentId,
                           @Param("userId") Long userId,
                           @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE ApprovalComment c SET c.isDeleted = true WHERE c.loanApplication.id = :applicationId")
    void softDeleteAllCommentsForApplication(@Param("applicationId") Long applicationId);


    
    // Batch operations
    @Modifying
    @Transactional
    @Query("DELETE FROM ApprovalComment c WHERE c.isDeleted = true AND c.createdAt < :cutoffDate")
    int deleteSoftDeletedCommentsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}