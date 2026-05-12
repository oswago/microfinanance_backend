// repository/ReconciliationItemRepository.java
package com.microfinance.financials.reconciliation.repository;

import com.microfinance.financials.reconciliation.entity.ReconciliationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconciliationItemRepository extends JpaRepository<ReconciliationItem, Long> {
    
    List<ReconciliationItem> findByReconciliationId(Long reconciliationId);
    
    List<ReconciliationItem> findByReconciliationIdAndCategory(Long reconciliationId, String category);
    
    List<ReconciliationItem> findByReconciliationIdAndStatus(Long reconciliationId, String status);
    
    @Query("SELECT ri FROM ReconciliationItem ri WHERE ri.reconciliationId = :reconciliationId AND ri.isMatched = false")
    List<ReconciliationItem> findUnmatchedItems(@Param("reconciliationId") Long reconciliationId);
    
    @Query("SELECT COUNT(ri) FROM ReconciliationItem ri WHERE ri.reconciliationId = :reconciliationId AND ri.isMatched = true")
    long countMatchedItems(@Param("reconciliationId") Long reconciliationId);

    @Query("SELECT COUNT(ri) FROM ReconciliationItem ri WHERE ri.reconciliationId = :reconciliationId AND ri.isMatched = false")
    long countUnmatchedItems(@Param("reconciliationId") Long reconciliationId);

    @Query("SELECT SUM(ri.amount) FROM ReconciliationItem ri WHERE ri.reconciliationId = :reconciliationId AND ri.category = :category")
    java.math.BigDecimal sumAmountByReconciliationIdAndCategory(@Param("reconciliationId") Long reconciliationId,
                                                                @Param("category") String category);

    @Query("SELECT ri FROM ReconciliationItem ri WHERE ri.reconciliationId = :reconciliationId AND ri.itemType = :itemType")
    List<ReconciliationItem> findByReconciliationIdAndItemType(@Param("reconciliationId") Long reconciliationId,
                                                               @Param("itemType") String itemType);

    @Query("SELECT ri FROM ReconciliationItem ri WHERE ri.reconciliationId = :reconciliationId AND ri.status = :status ORDER BY ri.transactionDate")
    List<ReconciliationItem> findByReconciliationIdAndStatusOrderByTransactionDate(@Param("reconciliationId") Long reconciliationId,
                                                                                   @Param("status") String status);

    @Query("SELECT COUNT(ri) FROM ReconciliationItem ri WHERE ri.reconciliationId = :reconciliationId AND ri.category = :category")
    long countByReconciliationIdAndCategory(@Param("reconciliationId") Long reconciliationId,
                                            @Param("category") String category);

    @Query("SELECT ri FROM ReconciliationItem ri WHERE ri.reconciliationId = :reconciliationId AND ri.isMatched = :isMatched")
    List<ReconciliationItem> findByReconciliationIdAndIsMatched(@Param("reconciliationId") Long reconciliationId,
                                                                @Param("isMatched") Boolean isMatched);

    @Query("UPDATE ReconciliationItem ri SET ri.isMatched = true, ri.status = 'MATCHED', ri.matchedWith = :matchedWith WHERE ri.id IN :itemIds")
    void markItemsAsMatched(@Param("itemIds") List<Long> itemIds, @Param("matchedWith") String matchedWith);
}