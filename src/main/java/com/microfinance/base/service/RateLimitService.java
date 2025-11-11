package com.microfinance.base.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class RateLimitService {

    @Value("${app.security.rate-limit.login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.rate-limit.window-minutes:1}")
    private int windowMinutes;

    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    public boolean isRateLimited(String username) {
        LoginAttempt attempt = loginAttempts.get(username);

        if (attempt == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long windowMillis = windowMinutes * 60 * 1000L;

        // Reset if window has passed
        if (currentTime - attempt.getFirstAttemptTime() > windowMillis) {
            loginAttempts.remove(username);
            return false;
        }

        // Check if exceeded max attempts
        if (attempt.getCount() >= maxLoginAttempts) {
            log.warn("Rate limit exceeded for user: {}", username);
            return true;
        }

        return false;
    }

    public void recordLoginAttempt(String username) {
        LoginAttempt attempt = loginAttempts.computeIfAbsent(username,
                k -> new LoginAttempt());
        attempt.increment();
    }

    public void resetRateLimit(String username) {
        loginAttempts.remove(username);
        log.debug("Rate limit reset for user: {}", username);
    }

    public int getRemainingAttempts(String username) {
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt == null) {
            return maxLoginAttempts;
        }
        return Math.max(0, maxLoginAttempts - attempt.getCount());
    }

    private static class LoginAttempt {
        private final AtomicInteger count = new AtomicInteger(0);
        @Getter
        private final long firstAttemptTime = System.currentTimeMillis();

        public void increment() {
            count.incrementAndGet();
        }

        public int getCount() {
            return count.get();
        }

    }
}