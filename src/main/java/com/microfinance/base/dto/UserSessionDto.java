package com.microfinance.base.dto;

import com.microfinance.base.entity.UserSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionDto {
    private Long id;
    private Long userId;
    private String username;
    private String sessionId;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivity;
    private Boolean active;
    
    public static UserSessionDto fromEntity(UserSession session) {
        if (session == null) return null;
        
        return UserSessionDto.builder()
                .id(session.getId())
                .userId(session.getUser() != null ? session.getUser().getId() : null)
                .username(session.getUser() != null ? session.getUser().getUsername() : null)
                .sessionId(session.getSessionId())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .loginTime(session.getLoginTime())
                .lastActivity(session.getLastActivity())
                .active(session.getActive())
                .build();
    }
}