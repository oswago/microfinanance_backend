package com.microfinance.base.controller;

import com.microfinance.base.dto.UserSessionDto;
import com.microfinance.base.entity.User;
import com.microfinance.base.entity.UserSession;
import com.microfinance.base.service.UserSessionService;
import com.microfinance.base.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class UserSessionController {
    
    private final UserSessionService userSessionService;
    private final UserService userService;

    /*
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @userService.getUserById(#userId).username == authentication.name")
    public ResponseEntity<List<UserSession>> getUserSessions(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        List<UserSession> sessions = userSessionService.getActiveUserSessions(user);
        return ResponseEntity.ok(sessions);
    }
*/
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @userService.getUserById(#userId).username == authentication.name")
    public ResponseEntity<List<UserSessionDto>> getUserSessions(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        List<UserSessionDto> sessions = userSessionService.getActiveUserSessions(user);
        return ResponseEntity.ok(sessions);
    }


    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @userService.getUserById(#userId).username == authentication.name")
    public ResponseEntity<Void> logoutAllUserSessions(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        userSessionService.logoutAllUserSessions(user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @userService.getUserById(#userId).username == authentication.name")
    public ResponseEntity<Void> logoutSession(@PathVariable String sessionId) {
        userSessionService.logoutSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> cleanupInactiveSessions(@RequestParam(defaultValue = "30") int inactivityMinutes) {
        userSessionService.cleanupInactiveSessions(inactivityMinutes);
        return ResponseEntity.ok().build();
    }
}