package com.microfinance.base.repository;

import com.microfinance.base.entity.RolePermission;
import com.microfinance.base.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRole(User.UserRole role);

    boolean existsByRoleAndPermission(User.UserRole role, String permission);

    Optional<RolePermission> findByRoleAndPermission(User.UserRole role, String permission);

    void deleteByRoleAndPermission(User.UserRole role, String permission);

    List<RolePermission> findByModule(String module);

    @Query("SELECT DISTINCT rp.module FROM RolePermission rp WHERE rp.role = :role")
    List<String> findDistinctModulesByRole(@Param("role") User.UserRole role);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.role = :role AND rp.module = :module")
    List<RolePermission> findByRoleAndModule(@Param("role") User.UserRole role, @Param("module") String module);

    long countByRole(User.UserRole role);
}