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
    List<UserSession> findByUserAndActiveTrue(User user);
    Optional<UserSession> findBySessionId(String sessionId);
    
    @Modifying
    @Query("UPDATE UserSession us SET us.active = false, us.lastActivity = :logoutTime WHERE us.user = :user AND us.active = true")
    void logoutAllUserSessions(@Param("user") User user, @Param("logoutTime") LocalDateTime logoutTime);
    
    @Modifying
    @Query("UPDATE UserSession us SET us.active = false, us.lastActivity = :logoutTime WHERE us.sessionId = :sessionId")
    void logoutSession(@Param("sessionId") String sessionId, @Param("logoutTime") LocalDateTime logoutTime);
    
    @Query("SELECT us FROM UserSession us WHERE us.lastActivity < :inactiveTime AND us.active = true")
    List<UserSession> findInactiveSessions(@Param("inactiveTime") LocalDateTime inactiveTime);
}