package com.microfinance.common.service;


import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.common.dto.InAppNotificationDto;
import com.microfinance.common.entity.InAppNotification;
import com.microfinance.common.repository.InAppNotificationRepository;
import com.microfinance.exception.ResourceNotFoundException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final UserRepository userRepository;
    
    @Value("${app.notification.email.enabled:true}")
    private boolean emailEnabled;
    
    @Value("${app.notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.notification.push.enabled:false}")
    private boolean pushEnabled;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.notifications.email.from:noreply@microfinance.com}")
    private String fromEmail;

    @Value("${app.notifications.in-app.enabled:true}")
    private boolean inAppEnabled;


    @Value("${app.support.email:support@microfinance.com}")
    private String supportEmail;


    @Value("${app.company.name:Microfinance System}")
    private String companyName;

    private final InAppNotificationRepository inAppNotificationRepository;

    
    @Override
    @Async
    public void sendApprovalNotification(Long userId, String applicationNumber, String action, 
                                         String comments, String approverName) {
        try {
            log.info("Sending approval notification for application: {}, action: {}, to user: {}", 
                    applicationNumber, action, userId);
            
            // Get user email from user service (you need to implement this)
            String userEmail = getUserEmail(userId);
            
            if (emailEnabled && userEmail != null) {
                sendApprovalEmail(userEmail, applicationNumber, action, comments, approverName);
            }
            
            if (smsEnabled) {
                sendSmsNotification(userId, 
                    String.format("Application %s has been %s by %s", 
                        applicationNumber, action, approverName));
            }
            
            log.info("Approval notification sent successfully for application: {}", applicationNumber);
            
        } catch (Exception e) {
            log.error("Failed to send approval notification: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public void sendBorrowerNotification(String email, String applicationNumber, String action, String comments) {
        try {
            log.info("Sending borrower notification for application: {}, action: {}, to: {}", 
                    applicationNumber, action, email);
            
            if (emailEnabled && email != null) {
                sendBorrowerEmail(email, applicationNumber, action, comments);
            }
            
            log.info("Borrower notification sent successfully for application: {}", applicationNumber);
            
        } catch (Exception e) {
            log.error("Failed to send borrower notification: {}", e.getMessage(), e);
        }
    }


    
    @Override
    @Async
    public void sendEscalationNotification(String applicationNumber, String escalatedBy, int newLevel) {
        try {
            log.info("Sending escalation notification for application: {}, escalated by: {}, new level: {}", 
                    applicationNumber, escalatedBy, newLevel);
            
            // Get users who should receive escalation notifications
            String[] escalationEmails = getEscalationEmails(newLevel);
            
            if (emailEnabled && escalationEmails != null) {
                for (String email : escalationEmails) {
                    sendEscalationEmail(email, applicationNumber, escalatedBy, newLevel);
                }
            }
            
            log.info("Escalation notification sent successfully for application: {}", applicationNumber);
            
        } catch (Exception e) {
            log.error("Failed to send escalation notification: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public void sendRejectionNotification(String email, String applicationNumber, String reason) {
        try {
            log.info("Sending rejection notification for application: {}, to: {}", applicationNumber, email);
            
            if (emailEnabled && email != null) {
                sendRejectionEmail(email, applicationNumber, reason);
            }
            
            log.info("Rejection notification sent successfully for application: {}", applicationNumber);
            
        } catch (Exception e) {
            log.error("Failed to send rejection notification: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public void sendDisbursementNotification(String email, String applicationNumber, BigDecimal amount) {
        try {
            log.info("Sending disbursement notification for application: {}, amount: {}, to: {}", 
                    applicationNumber, amount, email);
            
            if (emailEnabled && email != null) {
                sendDisbursementEmail(email, applicationNumber, amount);
            }
            
            log.info("Disbursement notification sent successfully for application: {}", applicationNumber);
            
        } catch (Exception e) {
            log.error("Failed to send disbursement notification: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public void sendOverdueNotification(String applicationNumber, int daysOverdue, String assignedTo) {
        try {
            log.info("Sending overdue notification for application: {}, days overdue: {}, assigned to: {}", 
                    applicationNumber, daysOverdue, assignedTo);
            
            String userEmail = getUserEmailByUsername(assignedTo);
            
            if (emailEnabled && userEmail != null) {
                sendOverdueEmail(userEmail, applicationNumber, daysOverdue);
            }
            
            log.info("Overdue notification sent successfully for application: {}", applicationNumber);
            
        } catch (Exception e) {
            log.error("Failed to send overdue notification: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public void sendSlaBreachNotification(String applicationNumber, String role, int hoursOverdue) {
        try {
            log.info("Sending SLA breach notification for application: {}, role: {}, hours overdue: {}", 
                    applicationNumber, role, hoursOverdue);
            
            String[] roleEmails = getRoleEmails(role);
            
            if (emailEnabled && roleEmails != null) {
                for (String email : roleEmails) {
                    sendSlaBreachEmail(email, applicationNumber, hoursOverdue);
                }
            }
            
            log.info("SLA breach notification sent successfully for application: {}", applicationNumber);
            
        } catch (Exception e) {
            log.error("Failed to send SLA breach notification: {}", e.getMessage(), e);
        }
    }


    @Async
    @Override
    public void sendApprovalRequestNotification(Long approverId, String applicationNumber,
                                                String subject, String message,
                                                Integer currentLevel, Integer totalLevels,
                                                String previousApprover) {
        try {
            log.info("Sending approval request notification to approver ID: {}, application: {}",
                    approverId, applicationNumber);

            User approver = userRepository.findById(approverId)
                    .orElseThrow(() -> new ResourceNotFoundException("Approver not found: " + approverId));

            // Send email notification
            if (emailEnabled && approver.getEmail() != null) {
                sendApprovalRequestEmail(approver, applicationNumber, subject, message,
                        currentLevel, totalLevels, previousApprover);
                log.info("Approval request email sent to: {}", approver.getEmail());
            }

            // Send SMS notification
            if (smsEnabled && approver.getPhoneNumber() != null) {
                sendApprovalRequestSms(approver, applicationNumber, subject, message,
                        currentLevel, totalLevels);
                log.info("Approval request SMS sent to: {}", approver.getPhoneNumber());
            }

            // Send in-app notification
            if (inAppEnabled) {
                createInAppNotification(approverId, applicationNumber, subject, message,
                        currentLevel, totalLevels);
                log.info("In-app notification created for user: {}", approverId);
            }

            log.info("Approval request notification sent successfully to: {}", approver.getUsername());

        } catch (Exception e) {
            log.error("Failed to send approval request notification to approver {}: {}",
                    approverId, e.getMessage(), e);
        }
    }



    /**
     * Send approval request SMS
     */
    private void sendApprovalRequestSms(User approver, String applicationNumber,
                                        String subject, String message,
                                        Integer currentLevel, Integer totalLevels) {
        try {
            String levelInfo = "";
            if (currentLevel != null && totalLevels != null) {
                levelInfo = String.format(" (Level %d of %d)", currentLevel + 1, totalLevels);
            }

            String smsMessage = String.format("Approval Required: %s%s. App: %s. %s",
                    subject,
                    levelInfo,
                    applicationNumber,
                    message != null && message.length() > 100 ? message.substring(0, 97) + "..." : message);

            // Truncate to 160 characters if needed
            if (smsMessage.length() > 160) {
                smsMessage = smsMessage.substring(0, 157) + "...";
            }

            // TODO: Implement SMS sending based on your SMS provider
            log.debug("SMS to {}: {}", approver.getPhoneNumber(), smsMessage);

        } catch (Exception e) {
            log.error("Failed to send approval request SMS to {}: {}", approver.getPhoneNumber(), e.getMessage(), e);
        }
    }

    /**
     * Create in-app notification
     */
    private void createInAppNotification(Long approverId, String applicationNumber,
                                         String subject, String message,
                                         Integer currentLevel, Integer totalLevels) {
        try {
            String levelInfo = "";
            if (currentLevel != null && totalLevels != null) {
                levelInfo = String.format(" (Level %d of %d)", currentLevel + 1, totalLevels);
            }

            InAppNotification notification = InAppNotification.builder()
                    .userId(approverId)
                    .type("APPROVAL_REQUEST")
                    .title(subject + levelInfo)
                    .message(message != null && message.length() > 500 ?
                            message.substring(0, 497) + "..." : message)
                    .referenceNumber(applicationNumber)
                    .referenceType("LOAN_APPLICATION")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            inAppNotificationRepository.save(notification);

        } catch (Exception e) {
            log.error("Failed to create in-app notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification to the next approver
     */
    @Async
    @Override
    public void sendToNextApprover(Long nextApproverId, String applicationNumber,
                                   int currentLevel, int totalLevels,
                                   String previousApprover, String comments) {
        String subject = String.format("Approval Required: Application %s - Level %d of %d",
                applicationNumber, currentLevel + 1, totalLevels);

        String message = String.format(
                "Application %s has been approved at level %d by %s and now requires your approval at level %d.\n\n" +
                        "Comments: %s",
                applicationNumber, currentLevel, previousApprover, currentLevel + 1,
                comments != null ? comments : "No comments provided"
        );

        sendApprovalRequestNotification(nextApproverId, applicationNumber, subject, message,
                currentLevel, totalLevels, previousApprover);
    }

    /**
     * Get display name for role
     */
    private String getRoleDisplayName(String role) {
        switch (role) {
            case "LOAN_OFFICER": return "Loan Officer";
            case "CREDIT_OFFICER": return "Credit Officer";
            case "BRANCH_MANAGER": return "Branch Manager";
            case "REGIONAL_MANAGER": return "Regional Manager";
            case "CREDIT_APPROVER": return "Credit Approver";
            case "SUPER_ADMIN": return "Super Administrator";
            default: return role;
        }
    }


    // Implementation
    @Override
    public Page<InAppNotificationDto> getUserNotifications(Long userId, Pageable pageable) {
        Page<InAppNotification> notifications = inAppNotificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(InAppNotificationDto::fromEntity);
    }

    @Override
    public long getUnreadNotificationCount(Long userId) {
        return inAppNotificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    @Override
    public void markNotificationAsRead(Long notificationId, Long userId) {
        inAppNotificationRepository.markAsRead(notificationId, LocalDateTime.now(), userId);
    }


    // ========== PRIVATE METHODS ==========

    /**
     * Send approval request email using Thymeleaf template
     */
    private void sendApprovalRequestEmail(User approver, String applicationNumber,
                                          String subject, String message,
                                          Integer currentLevel, Integer totalLevels,
                                          String previousApprover) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            Context context = new Context(Locale.ENGLISH);

            // Common variables
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentYear", LocalDate.now().getYear());
            context.setVariable("companyName", companyName);
            context.setVariable("baseUrl", baseUrl);

            // Approval request specific variables
            context.setVariable("approverName", approver.getFullName());
            context.setVariable("applicationNumber", applicationNumber);
            context.setVariable("subject", subject);
            context.setVariable("message", message);
            context.setVariable("applicationUrl", baseUrl + "/loan-approvals/" + applicationNumber);
            context.setVariable("requestDate", LocalDateTime.now());

            // Level information
            context.setVariable("currentLevel", currentLevel != null ? currentLevel : 1);
            context.setVariable("totalLevels", totalLevels != null ? totalLevels : 1);
            context.setVariable("nextRole", getRoleDisplayName(approver.getRole().name()));
            context.setVariable("previousApprover", previousApprover != null ? previousApprover : "System");
            context.setVariable("comments", message != null && message.length() > 200 ?
                    message.substring(0, 197) + "..." : message);

            String htmlContent = templateEngine.process("email/approval-request", context);

            helper.setFrom(fromEmail);
            helper.setTo(approver.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            log.error("Failed to send approval request email to {}: {}", approver.getEmail(), e.getMessage(), e);
        }
    }




    
    private void sendApprovalEmail(String toEmail, String applicationNumber, String action, 
                                   String comments, String approverName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            
            Context context = new Context(Locale.ENGLISH);
            // Add common variables
            context.setVariable("supportEmail", "support@microfinance.com");
            context.setVariable("currentYear", LocalDate.now().getYear());
            context.setVariable("companyName", "Microfinance System");

            context.setVariable("applicationNumber", applicationNumber);
            context.setVariable("action", action);
            context.setVariable("comments", comments);
            context.setVariable("approverName", approverName);
            context.setVariable("date", LocalDateTime.now());
            context.setVariable("applicationUrl", baseUrl + "/applications/" + applicationNumber);
            
            String htmlContent = templateEngine.process("email/approval-notification", context);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("Application %s - %s", applicationNumber, getActionSubject(action)));
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
        } catch (Exception e) {
            log.error("Failed to send approval email: {}", e.getMessage(), e);
        }
    }
    
    private void sendBorrowerEmail(String toEmail, String applicationNumber, String action, String comments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            
            Context context = new Context(Locale.ENGLISH);
            // Add common variables
            context.setVariable("supportEmail", "support@microfinance.com");
            context.setVariable("currentYear", LocalDate.now().getYear());
            context.setVariable("companyName", "Microfinance System");

            context.setVariable("applicationNumber", applicationNumber);
            context.setVariable("action", action);
            context.setVariable("comments", comments);
            context.setVariable("date", LocalDateTime.now());
            context.setVariable("applicationUrl", baseUrl + "/applications/" + applicationNumber);
            context.setVariable("supportEmail", "support@microfinance.com");
            
            String htmlContent = templateEngine.process("email/borrower-notification", context);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("Your Loan Application %s - Update", applicationNumber));
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
        } catch (Exception e) {
            log.error("Failed to send borrower email: {}", e.getMessage(), e);
        }
    }
    
    private void sendEscalationEmail(String toEmail, String applicationNumber, String escalatedBy, int newLevel) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            
            Context context = new Context(Locale.ENGLISH);
            // Add common variables
            context.setVariable("supportEmail", "support@microfinance.com");
            context.setVariable("currentYear", LocalDate.now().getYear());
            context.setVariable("companyName", "Microfinance System");

            context.setVariable("applicationNumber", applicationNumber);
            context.setVariable("escalatedBy", escalatedBy);
            context.setVariable("newLevel", newLevel);
            context.setVariable("date", LocalDateTime.now());
            context.setVariable("applicationUrl", baseUrl + "/applications/" + applicationNumber);
            
            String htmlContent = templateEngine.process("email/escalation-notification", context);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("Application %s - Escalation Required", applicationNumber));
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
        } catch (Exception e) {
            log.error("Failed to send escalation email: {}", e.getMessage(), e);
        }
    }
    
    private void sendRejectionEmail(String toEmail, String applicationNumber, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            
            Context context = new Context(Locale.ENGLISH);
            // Add common variables
            context.setVariable("supportEmail", "support@microfinance.com");
            context.setVariable("currentYear", LocalDate.now().getYear());
            context.setVariable("companyName", "Microfinance System");

            context.setVariable("applicationNumber", applicationNumber);
            context.setVariable("reason", reason);
            context.setVariable("date", LocalDateTime.now());
            context.setVariable("reapplyUrl", baseUrl + "/applications/new");
            context.setVariable("supportEmail", "support@microfinance.com");
            
            String htmlContent = templateEngine.process("email/rejection-notification", context);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("Update on Your Loan Application %s", applicationNumber));
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
        } catch (Exception e) {
            log.error("Failed to send rejection email: {}", e.getMessage(), e);
        }
    }
    
    private void sendDisbursementEmail(String toEmail, String applicationNumber, BigDecimal amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            
            Context context = new Context(Locale.ENGLISH);
            // Add common variables
            context.setVariable("supportEmail", "support@microfinance.com");
            context.setVariable("currentYear", LocalDate.now().getYear());
            context.setVariable("companyName", "Microfinance System");

            context.setVariable("applicationNumber", applicationNumber);
            context.setVariable("amount", amount);
            context.setVariable("date", LocalDateTime.now());
            context.setVariable("loanDetailsUrl", baseUrl + "/loans/" + applicationNumber);
            
            String htmlContent = templateEngine.process("email/disbursement-notification", context);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("Loan Disbursement - Application %s", applicationNumber));
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
        } catch (Exception e) {
            log.error("Failed to send disbursement email: {}", e.getMessage(), e);
        }
    }
    
    private void sendOverdueEmail(String toEmail, String applicationNumber, int daysOverdue) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            
            Context context = new Context(Locale.ENGLISH);
            context.setVariable("applicationNumber", applicationNumber);
            context.setVariable("daysOverdue", daysOverdue);
            context.setVariable("date", LocalDateTime.now());
            context.setVariable("applicationUrl", baseUrl + "/applications/" + applicationNumber);
            
            String htmlContent = templateEngine.process("email/overdue-notification", context);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("Application %s - Overdue for %d days", applicationNumber, daysOverdue));
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
        } catch (Exception e) {
            log.error("Failed to send overdue email: {}", e.getMessage(), e);
        }
    }
    
    private void sendSlaBreachEmail(String toEmail, String applicationNumber, int hoursOverdue) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            
            Context context = new Context(Locale.ENGLISH);
            context.setVariable("applicationNumber", applicationNumber);
            context.setVariable("hoursOverdue", hoursOverdue);
            context.setVariable("date", LocalDateTime.now());
            context.setVariable("applicationUrl", baseUrl + "/applications/" + applicationNumber);
            
            String htmlContent = templateEngine.process("email/sla-breach-notification", context);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("SLA Breach - Application %s", applicationNumber));
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
        } catch (Exception e) {
            log.error("Failed to send SLA breach email: {}", e.getMessage(), e);
        }
    }


    
    private void sendSmsNotification(Long userId, String message) {
        // Implement SMS notification logic
        // This could integrate with Twilio, AWS SNS, etc.
        log.debug("Sending SMS to user {}: {}", userId, message);
    }
    
    private String getActionSubject(String action) {
        switch (action.toUpperCase()) {
            case "APPROVED": return "Approved";
            case "REJECTED": return "Rejected";
            case "RETURNED": return "Returned for Revision";
            case "ESCALATED": return "Escalated";
            default: return "Updated";
        }
    }
    
    // Helper methods to get user/role emails (you need to implement these based on your user service)
    private String getUserEmail(Long userId) {
        // Call user service to get user email
        return "user@example.com"; // Placeholder
    }
    
    private String getUserEmailByUsername(String username) {
        // Call user service to get user email by username
        return "user@example.com"; // Placeholder
    }
    
    private String[] getEscalationEmails(int level) {
        // Get emails for users who handle escalation at this level
        return new String[]{"manager@example.com", "supervisor@example.com"};
    }
    
    private String[] getRoleEmails(String role) {
        // Get emails for all users with this role
        return new String[]{"admin@example.com"};
    }
}