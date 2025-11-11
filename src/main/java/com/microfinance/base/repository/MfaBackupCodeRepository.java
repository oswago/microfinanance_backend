package com.microfinance.base.repository;

import com.microfinance.base.entity.MfaBackupCode;
import com.microfinance.base.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MfaBackupCodeRepository extends JpaRepository<MfaBackupCode, Long> {
    
    List<MfaBackupCode> findByUserAndUsed(User user, boolean used);
    
    @Modifying
    @Query("DELETE FROM MfaBackupCode b WHERE b.user = :user")
    void deleteByUser(@Param("user") User user);
    
    @Query("SELECT b FROM MfaBackupCode b WHERE b.user = :user AND b.code = :code AND b.used = false")
    List<MfaBackupCode> findByUserAndCodeAndNotUsed(@Param("user") User user, @Param("code") String code);
}