package com.microfinance.base.service;

import com.microfinance.base.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionCleanupService {
    
    private final UserSessionRepository userSessionRepository;
    
    // Run daily at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredSessions() {
        log.info("Starting session cleanup job");
        
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        // Soft delete: Mark sessions as inactive
        int deactivatedCount = userSessionRepository.deactivateSessionsOlderThan(thirtyDaysAgo);
        log.info("Deactivated {} old sessions", deactivatedCount);
        
        // Optional: Hard delete sessions older than 90 days (after audit period)
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        int deletedCount = userSessionRepository.deleteSessionsOlderThan(ninetyDaysAgo);
        log.info("Hard deleted {} sessions older than 90 days", deletedCount);
    }
    
    // Clean up expired sessions every hour
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void expireTimedOutSessions() {
        LocalDateTime expiryTime = LocalDateTime.now().minusHours(8); // 8 hours timeout

        
        int expiredCount = userSessionRepository.expireSessionsWithNoActivity(expiryTime,LocalDateTime.now());
        if (expiredCount > 0) {
            log.info("Expired {} inactive sessions", expiredCount);
        }
    }
}