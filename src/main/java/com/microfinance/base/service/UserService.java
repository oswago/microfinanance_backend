package com.microfinance.base.service;

import com.microfinance.audit.service.AuditService;
import com.microfinance.base.dto.PasswordChangeRequest;
import com.microfinance.base.dto.UserCreateRequest;
import com.microfinance.base.dto.UserUpdateRequest;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.borrower.service.BorrowerService;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.system.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.mapstruct.control.MappingControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.microfinance.base.utils.SecurityUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils; // Inject SecurityUtils
    private final AuditService auditService;
    private final ActivityLogService activityLogService;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Transactional
    public User createUser(UserCreateRequest createRequest) {
        log.info("Creating new user with username: {}", createRequest.getUsername());

        // Validate required fields
        if (createRequest.getUsername() == null || createRequest.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (createRequest.getEmail() == null || createRequest.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (createRequest.getPassword() == null || createRequest.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (createRequest.getFirstName() == null || createRequest.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (createRequest.getLastName() == null || createRequest.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(createRequest.getUsername())) {
            log.warn("Username already exists: {}", createRequest.getUsername());
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(createRequest.getEmail())) {
            log.warn("Email already exists: {}", createRequest.getEmail());
            throw new RuntimeException("Email already exists");
        }

        // Validate email format
        if (!isValidEmail(createRequest.getEmail())) {
            throw new IllegalArgumentException("Invalid email format: " + createRequest.getEmail());
        }

        User user = new User();

        // Set User fields
        user.setUsername(createRequest.getUsername());
        user.setEmail(createRequest.getEmail());
        user.setPassword(passwordEncoder.encode(createRequest.getPassword()));
        user.setFirstName(createRequest.getFirstName());
        user.setLastName(createRequest.getLastName());
        user.setPhoneNumber(createRequest.getPhoneNumber());
        user.setRole(createRequest.getRole() != null ? createRequest.getRole() : User.UserRole.LOAN_OFFICER);
        user.setBranchId(createRequest.getBranchId());
        user.setActive(true);
        user.setFailedLoginAttempts(0);
        user.setMfaEnabled(false);

        // IMPORTANT: Set BaseEntity fields explicitly
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);  // Add this - required by BaseEntity
        user.setDeleted(false);   // Add this - required by BaseEntity
        user.setCreatedBy(securityUtils.getCurrentUserId());
        user.setUpdatedBy(securityUtils.getCurrentUserId()); // Add this

        log.debug("Saving user with username: {}, email: {}, role: {}, createdAt: {}, updatedAt: {}",
                user.getUsername(), user.getEmail(), user.getRole(), user.getCreatedAt(), user.getUpdatedAt());

        try {
            User savedUser = userRepository.save(user);


            //Audit Section
            Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
            String createdByName ="";
            Long createdById=null;
            if(currentUser.isPresent()){
                createdByName=currentUser.get().getFullName();
                createdById=currentUser.get().getId();
            }
            if (Objects.nonNull(savedUser.getId())) {
                auditLogs(
                        savedUser.getId(),
                        GeneralConfig.BorrowerActivityType.OTHER_ACTIVITY,
                        "USER",
                        "User with Id"+savedUser.getId()+ "has been created by:"+createdByName+"-"+createdById
                );
            }
            //End Audit Section

            log.info("Successfully created user with ID: {}, username: {}", savedUser.getId(), savedUser.getUsername());
            return savedUser;
        } catch (DataIntegrityViolationException e) {
            log.error("Database constraint violation while creating user: {}", e.getMessage(), e);

            if (e.getMessage().contains("CONSTRAINT_6A")) {
                throw new RuntimeException("Failed to create user: Required fields (createdAt, updatedAt, deleted) cannot be null. Please check your data.", e);
            }

            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    public List<User> getAllUsers() {
        return userRepository.findByActiveTrue();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional
    public User updateUser(Long id, UserUpdateRequest updateRequest) {
        User user = getUserById(id);
        
        if (!user.getEmail().equals(updateRequest.getEmail()) && 
            userRepository.existsByEmail(updateRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        user.setEmail(updateRequest.getEmail());
        user.setFirstName(updateRequest.getFirstName());
        user.setLastName(updateRequest.getLastName());
        user.setPhoneNumber(updateRequest.getPhoneNumber());
        user.setRole(updateRequest.getRole());
        user.setBranchId(updateRequest.getBranchId());
        user.setUpdatedBy(securityUtils.getCurrentUserId());
        user.setUpdatedAt(LocalDateTime.now());

        
        if (updateRequest.getActive() != null) {
            user.setActive(updateRequest.getActive());
        }

        if (updateRequest.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }
           User savedUser=userRepository.save(user);

        //Audit Section
        Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser.isPresent()){
            createdByName=currentUser.get().getFullName();
            createdById=currentUser.get().getId();
        }
        if (Objects.nonNull(savedUser.getId())) {
            auditLogs(
                    savedUser.getId(),
                    GeneralConfig.BorrowerActivityType.OTHER_ACTIVITY,
                    "USER",
                    "User with Id"+savedUser.getId()+ "has been updated by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        return savedUser;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        user.setDeleted(true);
        user.setUpdatedBy(securityUtils.getCurrentUserId());
        user.setUpdatedAt(LocalDateTime.now());

        User deletedUser= userRepository.save(user);

        //Audit Section
        Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser.isPresent()){
            createdByName=currentUser.get().getFullName();
            createdById=currentUser.get().getId();
        }
        if (Objects.nonNull(deletedUser.getId())) {
           auditLogs(
                    deletedUser.getId(),
                    GeneralConfig.BorrowerActivityType.OTHER_ACTIVITY,
                    "USER",
                    "User with Id"+deletedUser.getId()+ "has been updated by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

    }

    @Transactional
    public User activateUser(Long id) {
        User user = getUserById(id);
        user.setUpdatedBy(securityUtils.getCurrentUserId());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        User savedUser = userRepository.save(user);

        //Audit Section
        Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser.isPresent()){
            createdByName=currentUser.get().getFullName();
            createdById=currentUser.get().getId();
        }
        if (Objects.nonNull(savedUser.getId())) {
            auditLogs(
                    savedUser.getId(),
                    GeneralConfig.BorrowerActivityType.OTHER_ACTIVITY,
                    "USER",
                    "User with Id"+savedUser.getId()+ "has been activated by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section


        return savedUser;
    }

    @Transactional
    public User deactivateUser(Long id) {
        User user = getUserById(id);
        user.setUpdatedBy(securityUtils.getCurrentUserId());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(false);
        User savedUser=userRepository.save(user);

        //Audit Section
        Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser.isPresent()){
            createdByName=currentUser.get().getFullName();
            createdById=currentUser.get().getId();
        }
        if (Objects.nonNull(savedUser.getId())) {
            auditLogs(
                    savedUser.getId(),
                    GeneralConfig.BorrowerActivityType.OTHER_ACTIVITY,
                    "USER",
                    "User with Id"+savedUser.getId()+ "has been Deactivated by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        return savedUser;
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = getUserById(userId);
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedBy(securityUtils.getCurrentUserId());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }


    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = getUserById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setUpdatedBy(securityUtils.getCurrentUserId());
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    public List<User> getUsersByRole(User.UserRole role) {
        return userRepository.findByRole(role);
    }

    public List<User> getUsersByBranch(Long branchId) {
        return userRepository.findByBranchId(branchId);
    }

    public String getUserNameById(Long userId) {
          User user= (User) userRepository.findUserNamesByIds(Collections.singleton(userId));
          return user.getUsername();

    }

    public String findFullNameById(Long assignedOfficerId) {
        return "Test Verifier Name";
    }

    public List<User> getCollectionOfficers() {
        List<User> user= userRepository.findCollectionOfficers();
        return user;
    }

    public User getSystemUser() {
        User user=new User();
        user.setSystemUser(true);
        user.setFirstName("SYSTEM");
        user.setLastName("SYSTEM");
        return user;
    }




    public void auditLogs(
            Long entityId,
            GeneralConfig.BorrowerActivityType borrowerActivityType,
            String entityType,
            String details
    ){
        Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName;
        Long createdById=null;

        if(currentUser.isPresent()){
            createdByName=currentUser.get().getFullName();
            createdById=currentUser.get().getId();
        }

        activityLogService.logBorrowerActivity(
                entityId,// updatedBorrower.getId()
                borrowerActivityType,//GeneralConfig.BorrowerActivityType.BORROWER_UPDATED,
                details ,//"Borrower Created by name: " + updatedBorrower.getFullName(),
                createdById
        );
        //audit log as well
        auditService.logEntityAction(
                entityId,//updatedBorrower.getId(),
                createdById,
                entityType,//"BORROWER",
                String.valueOf(borrowerActivityType),//"BORROWER UPDATED",
                details//"Borrower with ID: "+updatedBorrower.getFullName()+" Created"
        );
    }

}