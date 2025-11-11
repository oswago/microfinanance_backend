package com.microfinance.base.repository;

import com.microfinance.base.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    
    List<User> findByRole(User.UserRole role);
    List<User> findByActiveTrue();
    List<User> findByBranchId(Long branchId);
    
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts WHERE u.username = :username")
    void updateFailedLoginAttempts(@Param("username") String username, @Param("attempts") Integer attempts);
    
    @Modifying
    @Query("UPDATE User u SET u.accountLockedUntil = :lockTime WHERE u.username = :username")
    void lockUserAccount(@Param("username") String username, @Param("lockTime") LocalDateTime lockTime);
    
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :loginTime WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime);


    @Query("SELECT u.id, u.firstName || u.lastName as fullName FROM User u WHERE u.id IN :userIds")
    List<Object[]> findUserNamesByIds(@Param("userIds") Set<Long> userIds);
    // Helper method to convert to Map
    default Map<Long, String> findUserNamesMapByIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Object[]> results = findUserNamesByIds(userIds);
        return results.stream()
                .collect(Collectors.toMap(
                        obj -> (Long) obj[0],
                        obj -> (String) obj[1]
                ));
    }



}