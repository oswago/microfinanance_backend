package com.microfinance.base.repository;

import com.microfinance.base.entity.User;
import com.microfinance.base.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    /*List<UserSession> findByUserAndActiveTrue(User user);*/

    @Query("SELECT us FROM UserSession us LEFT JOIN FETCH us.user WHERE us.user = :user AND us.active = true")
    List<UserSession> findByUserAndActiveTrue(@Param("user") User user);

    Optional<UserSession> findBySessionId(String sessionId);
    
    @Modifying
    @Query("UPDATE UserSession us SET us.active = false, us.lastActivity = :logoutTime WHERE us.user = :user AND us.active = true")
    void logoutAllUserSessions(@Param("user") User user, @Param("logoutTime") LocalDateTime logoutTime);
    
    @Modifying
    @Query("UPDATE UserSession us SET us.active = false, us.lastActivity = :logoutTime WHERE us.sessionId = :sessionId")
    void logoutSession(@Param("sessionId") String sessionId, @Param("logoutTime") LocalDateTime logoutTime);
    
    @Query("SELECT us FROM UserSession us WHERE us.lastActivity < :inactiveTime AND us.active = true")
    List<UserSession> findInactiveSessions(@Param("inactiveTime") LocalDateTime inactiveTime);

    Optional<UserSession> findBySessionIdAndActiveTrue(String sessionId);

    @Modifying
    @Query("UPDATE UserSession us SET us.active = false, us.logoutTime = :cutoffDate WHERE us.active = true AND us.lastActivity < :cutoffDate")
    int deactivateSessionsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.logoutTime < :cutoffDate OR (us.active = false AND us.loginTime < :cutoffDate)")
    int deleteSessionsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("UPDATE UserSession us SET us.active = false, us.logoutTime = :now WHERE us.active = true AND us.lastActivity < :expiryTime")
    int expireSessionsWithNoActivity(@Param("expiryTime") LocalDateTime expiryTime, @Param("now") LocalDateTime now);

    // Get active sessions count for a user
    long countByUserIdAndActiveTrue(Long userId);

    // Force logout all sessions for a user (e.g., password change)
    @Modifying
    @Query("UPDATE UserSession us SET us.active = false, us.logoutTime = :now, us.forceLogout = true, us.forcedLogoutReason = :reason WHERE us.user.id = :userId AND us.active = true")
    int forceLogoutAllUserSessions(@Param("userId") Long userId, @Param("now") LocalDateTime now, @Param("reason") String reason);
}