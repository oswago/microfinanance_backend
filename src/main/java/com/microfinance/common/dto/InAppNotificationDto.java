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
    private String timeAgo;

    
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
                .timeAgo(getTimeAgo(notification.getCreatedAt()))
                .build();
    }

    private static String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";

        LocalDateTime now = LocalDateTime.now();
        long seconds = java.time.Duration.between(dateTime, now).getSeconds();

        if (seconds < 60) return "Just now";
        if (seconds < 3600) return (seconds / 60) + " minutes ago";
        if (seconds < 86400) return (seconds / 3600) + " hours ago";
        if (seconds < 604800) return (seconds / 86400) + " days ago";
        if (seconds < 2592000) return (seconds / 604800) + " weeks ago";

        return dateTime.toLocalDate().toString();
    }

}