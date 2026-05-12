package com.microfinance.common.dto;

import com.microfinance.common.entity.InAppNotification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InAppNotificationDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userRole;
    private String type;
    private String title;
    private String message;
    private String referenceType;
    private Long referenceId;
    private String referenceNumber;
    private Boolean isRead;
    private LocalDateTime readAt;
    private String actionUrl;
    private String actionLabel;
    private String priority;
    private String icon;
    private String color;
    private LocalDateTime createdAt;
    
    public static InAppNotificationDto fromEntity(InAppNotification notification) {
        if (notification == null) return null;
        
        return InAppNotificationDto.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .userName(notification.getUserName())
                .userRole(notification.getUserRole())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .referenceNumber(notification.getReferenceNumber())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .actionUrl(notification.getActionUrl())
                .actionLabel(notification.getActionLabel())
                .priority(notification.getPriority())
                .icon(notification.getIcon())
                .color(notification.getColor())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}