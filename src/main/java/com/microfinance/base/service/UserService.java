package com.microfinance.base.service;

import com.microfinance.base.dto.PasswordChangeRequest;
import com.microfinance.base.dto.UserCreateRequest;
import com.microfinance.base.dto.UserUpdateRequest;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.microfinance.base.utils.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils; // Inject SecurityUtils

    @Transactional
    public User createUser(UserCreateRequest createRequest) {
        // Check if username already exists
        if (userRepository.existsByUsername(createRequest.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(createRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(createRequest.getUsername());
        user.setEmail(createRequest.getEmail());
        user.setPassword(passwordEncoder.encode(createRequest.getPassword()));
        user.setFirstName(createRequest.getFirstName());
        user.setLastName(createRequest.getLastName());
        user.setPhoneNumber(createRequest.getPhoneNumber());
        user.setRole(createRequest.getRole() != null ? createRequest.getRole() : User.UserRole.LOAN_OFFICER);
        user.setBranchId(createRequest.getBranchId());
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy(securityUtils.getCurrentUserId());

        return userRepository.save(user);
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

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        user.setDeleted(true);
        user.setUpdatedBy(securityUtils.getCurrentUserId());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    @Transactional
    public User activateUser(Long id) {
        User user = getUserById(id);
        user.setUpdatedBy(securityUtils.getCurrentUserId());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);

        return userRepository.save(user);
    }

    @Transactional
    public User deactivateUser(Long id) {
        User user = getUserById(id);
        user.setUpdatedBy(securityUtils.getCurrentUserId());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(false);

        return userRepository.save(user);
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
}