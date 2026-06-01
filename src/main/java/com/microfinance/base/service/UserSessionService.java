package com.microfinance.base.service;

import com.microfinance.base.dto.UserSessionDto;
import com.microfinance.base.entity.User;
import com.microfinance.base.entity.UserSession;
import com.microfinance.base.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSessionService {
    
    private final UserSessionRepository userSessionRepository;

    @Transactional
    public UserSession createSession(User user, String ipAddress, String userAgent) {
        UserSession session = new UserSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setLoginTime(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());
        session.setActive(true);
        
        return userSessionRepository.save(session);
    }

    @Transactional
    public void updateSessionActivity(String sessionId) {
        userSessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setLastActivity(LocalDateTime.now());
            userSessionRepository.save(session);
        });
    }

    @Transactional
    public void logoutSession(String sessionId) {
        userSessionRepository.logoutSession(sessionId, LocalDateTime.now());
    }

    @Transactional
    public void logoutAllUserSessions(User user) {
        userSessionRepository.logoutAllUserSessions(user, LocalDateTime.now());
    }
/*
    public List<UserSession> getActiveUserSessions(User user) {
        return userSessionRepository.findByUserAndActiveTrue(user);
    }
    */


    public List<UserSessionDto> getActiveUserSessions(User user) {
        List<UserSession> sessions = userSessionRepository.findByUserAndActiveTrue(user);
        // Convert to DTOs while still in transactional context
        return sessions.stream()
                .map(session -> UserSessionDto.builder()
                        .id(session.getId())
                        .userId(session.getUser().getId())
                        .username(session.getUser().getUsername())
                        .sessionId(session.getSessionId())
                        .ipAddress(session.getIpAddress())
                        .userAgent(session.getUserAgent())
                        .loginTime(session.getLoginTime())
                        .lastActivity(session.getLastActivity())
                        .active(session.getActive())
                        .build())
                .collect(Collectors.toList());
    }



    @Transactional
    public void cleanupInactiveSessions(int inactivityMinutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(inactivityMinutes);
        List<UserSession> inactiveSessions = userSessionRepository.findInactiveSessions(cutoffTime);
        
        inactiveSessions.forEach(session -> {
            session.setActive(false);
            session.setLastActivity(LocalDateTime.now());
            userSessionRepository.save(session);
        });
    }
}