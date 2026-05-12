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

    @Query("SELECT CONCAT(u.firstName, ' ', u.lastName) FROM User u WHERE u.id = :id")
    String getUserNameById(Long id);

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

    @Query("SELECT u FROM User u WHERE u.role = 'COLLECTION_OFFICER'")
    List<User> findCollectionOfficers();



    // Add these methods to your UserRepository.java

// ==================== USER COUNT METHODS ====================

    /**
     * Count active users (users with active = true)
     *
     * @return Count of active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    Integer countActiveUsers();

    /**
     * Count total users (all users including inactive)
     *
     * @return Total count of all users
     */
    @Query("SELECT COUNT(u) FROM User u")
    Integer countTotalUsers();

    /**
     * Count inactive users
     *
     * @return Count of inactive users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = false")
    Integer countInactiveUsers();

    /**
     * Count users by role
     *
     * @param role User role (SUPER_ADMIN, BRANCH_MANAGER, etc.)
     * @return Count of users with given role
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.active = true")
    Integer countUsersByRole(@Param("role") User.UserRole role);

    /**
     * Count users by branch
     *
     * @param branchId Branch ID
     * @return Count of users in branch
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.branchId = :branchId AND u.active = true")
    Integer countUsersByBranch(@Param("branchId") Long branchId);

    /**
     * Count users created in a date range
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of users created in period
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    Integer countUsersCreatedInPeriod(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Count users by role and active status
     *
     * @param role User role
     * @param active Active status
     * @return Count of users matching criteria
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.active = :active")
    Integer countUsersByRoleAndActive(@Param("role") User.UserRole role,
                                      @Param("active") Boolean active);

    /**
     * Get user count statistics by role
     *
     * @return List of objects [role, activeCount, inactiveCount, totalCount]
     */
    @Query("SELECT u.role, " +
            "SUM(CASE WHEN u.active = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN u.active = false THEN 1 ELSE 0 END), " +
            "COUNT(u) " +
            "FROM User u " +
            "GROUP BY u.role")
    List<Object[]> getUserStatisticsByRole();

    /**
     * Get user count statistics by branch
     *
     * @return List of objects [branchId, activeCount, inactiveCount, totalCount]
     */
    @Query("SELECT u.branchId, " +
            "SUM(CASE WHEN u.active = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN u.active = false THEN 1 ELSE 0 END), " +
            "COUNT(u) " +
            "FROM User u " +
            "WHERE u.branchId IS NOT NULL " +
            "GROUP BY u.branchId")
    List<Object[]> getUserStatisticsByBranch();

    /**
     * Count users who have logged in within the last X days
     *
     * @param days Number of days
     * @return Count of active users with recent login
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true " +
            "AND u.lastLogin >= :sinceDate")
    Integer countRecentlyActiveUsers(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Count users who have never logged in
     *
     * @return Count of users with no login record
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLogin IS NULL AND u.active = true")
    Integer countNeverLoggedInUsers();

    /**
     * Count users by their last login date range
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of users who logged in during period
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u WHERE u.lastLogin BETWEEN :startDate AND :endDate")
    Integer countUsersLoggedInPeriod(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Get user registration trend by month
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of objects [year, month, count]
     */
    @Query("SELECT YEAR(u.createdAt), MONTH(u.createdAt), COUNT(u) " +
            "FROM User u " +
            "WHERE u.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY YEAR(u.createdAt), MONTH(u.createdAt) " +
            "ORDER BY YEAR(u.createdAt), MONTH(u.createdAt)")
    List<Object[]> getUserRegistrationTrend(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Find users with specific role and active status
     *
     * @param role User role
     * @param active Active status
     * @return List of users
     */
    List<User> findByRoleAndActive(User.UserRole role, Boolean active);

    /**
     * Find users by branch and role
     *
     * @param branchId Branch ID
     * @param role User role
     * @return List of users
     */
    List<User> findByBranchIdAndRole(Long branchId, User.UserRole role);

    /**
     * Count users by role for report (includes all roles)
     *
     * @return Map of role to count
     */
    @Query("SELECT u.role, COUNT(u) FROM User u WHERE u.active = true GROUP BY u.role")
    List<Object[]> countActiveUsersByRoleForReport();

    /**
     * Get all users with their permission counts (for audit)
     * Joins through the user's role to RolePermission
     */
    @Query("SELECT u.id, u.username, u.role, COUNT(rp) FROM User u " +
            "LEFT JOIN RolePermission rp ON rp.role = u.role " +
            "WHERE u.active = true " +
            "GROUP BY u.id, u.username, u.role")
    List<Object[]> getUsersWithPermissionCounts();




}