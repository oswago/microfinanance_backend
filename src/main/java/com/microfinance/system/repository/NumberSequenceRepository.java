package com.microfinance.system.repository;

import com.microfinance.system.entity.NumberSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NumberSequenceRepository extends JpaRepository<NumberSequence, Long> {
    
    /**
     * Find number sequence by unique code
     */
    Optional<NumberSequence> findBySequenceCode(String sequenceCode);
    
    /**
     * Find all active number sequences
     */
    List<NumberSequence> findByActiveTrue();
    
    /**
     * Check if sequence code exists
     */
    boolean existsBySequenceCode(String sequenceCode);
    
    /**
     * Check if sequence code exists excluding a specific sequence (for updates)
     */
    boolean existsBySequenceCodeAndIdNot(String sequenceCode, Long id);
    
    /**
     * Increment the next value for a sequence
     */
    @Modifying
    @Query("UPDATE NumberSequence n SET n.nextValue = n.nextValue + 1 WHERE n.sequenceCode = :sequenceCode")
    void incrementNextValue(@Param("sequenceCode") String sequenceCode);
    
    /**
     * Reset daily sequences (to be called by scheduler)
     */
    @Modifying
    @Query("UPDATE NumberSequence n SET n.nextValue = 1 WHERE n.resetDaily = true")
    void resetDailySequences();
    
    /**
     * Reset monthly sequences (to be called by scheduler)
     */
    @Modifying
    @Query("UPDATE NumberSequence n SET n.nextValue = 1 WHERE n.resetMonthly = true")
    void resetMonthlySequences();
    
    /**
     * Reset yearly sequences (to be called by scheduler)
     */
    @Modifying
    @Query("UPDATE NumberSequence n SET n.nextValue = 1 WHERE n.resetYearly = true")
    void resetYearlySequences();
    
    /**
     * Get current next value without incrementing
     */
    @Query("SELECT n.nextValue FROM NumberSequence n WHERE n.sequenceCode = :sequenceCode")
    Optional<Long> getCurrentValue(@Param("sequenceCode") String sequenceCode);
    
    /**
     * Set specific next value for a sequence
     */
    @Modifying
    @Query("UPDATE NumberSequence n SET n.nextValue = :nextValue WHERE n.sequenceCode = :sequenceCode")
    void setNextValue(@Param("sequenceCode") String sequenceCode, @Param("nextValue") Long nextValue);
    
    /**
     * Find sequences that need daily reset
     */
    List<NumberSequence> findByResetDailyTrue();
    
    /**
     * Find sequences that need monthly reset
     */
    List<NumberSequence> findByResetMonthlyTrue();
    
    /**
     * Find sequences that need yearly reset
     */
    List<NumberSequence> findByResetYearlyTrue();
}