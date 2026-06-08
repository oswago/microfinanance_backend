package com.microfinance.loanproducts.repository;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducttype.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
    
    boolean existsByProductCode(String productCode);
    boolean existsByName(String name);
    
    List<LoanProduct> findByActiveTrue();
    List<LoanProduct> findByActiveTrueAndStatus(GeneralConfig.ProductStatus status);

    @Query("SELECT DISTINCT lp FROM LoanProduct lp " +
            "LEFT JOIN FETCH lp.productType " +
            "WHERE lp.active = true AND lp.status = :status")
    List<LoanProduct> findActiveProductsWithType(@Param("status") GeneralConfig.ProductStatus status);


    List<LoanProduct> findByStatus(GeneralConfig.ProductStatus status);
    List<LoanProduct> findByIsTemplateTrueAndActiveTrue();
    
    List<LoanProduct> findByNameContainingAndActiveTrue(String name);
    List<LoanProduct> findByProductTypeAndActiveTrue(ProductType productType);
    List<LoanProduct> findByInterestMethodAndActiveTrue(GeneralConfig.InterestMethod interestMethod);
    
    List<LoanProduct> findByNameContainingAndProductTypeAndActiveTrue(String name, ProductType productType);
    List<LoanProduct> findByNameContainingAndInterestMethodAndActiveTrue(String name, GeneralConfig.InterestMethod interestMethod);
    List<LoanProduct> findByNameContainingAndProductTypeAndInterestMethodAndActiveTrue(
            String name, ProductType productType, GeneralConfig.InterestMethod interestMethod);
    
    Optional<LoanProduct> findByProductCodeAndActiveTrue(String productCode);
    
    @Query("SELECT lp FROM LoanProduct lp WHERE lp.active = true AND lp.status = 'ACTIVE' AND " +
           "(:minAmount IS NULL OR lp.maxLoanAmount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR lp.minLoanAmount <= :maxAmount)")
    List<LoanProduct> findEligibleProductsByAmountRange(
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.loanProduct.id = :productId AND l.status IN ('ACTIVE', 'PENDING')")
    Long countActiveLoansByProductId(@Param("productId") Long productId);


    @Query("SELECT lp FROM LoanProduct lp WHERE lp.active = true AND " +
            "(:name IS NULL OR LOWER(lp.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:productTypeId IS NULL OR lp.productType.id = :productTypeId) AND " +
            "(:interestMethod IS NULL OR lp.interestMethod = :interestMethod)")
    List<LoanProduct> searchLoanProducts(
            @Param("name") String name,
            @Param("productTypeId") Long productTypeId,
            @Param("interestMethod") GeneralConfig.InterestMethod interestMethod);

    List<LoanProduct> findByProductTypeIdAndActiveTrue(Long productTypeId);


    @Query("""
        SELECT 
            lp.productType,
            lp.name,
            lp.productCode,
            COUNT(la.id) as totalApplications,
            SUM(CASE WHEN la.status = 'APPROVED' THEN 1 ELSE 0 END) as approvedCount,
            SUM(CASE WHEN la.status = 'REJECTED' THEN 1 ELSE 0 END) as rejectedCount,
            AVG(CASE WHEN aa.decisionDate IS NOT NULL AND aa.createdAt IS NOT NULL 
                 THEN TIMESTAMPDIFF(HOUR, aa.createdAt, aa.decisionDate) END) as avgProcessingTime,
            AVG(la.appliedAmount) as avgApprovedAmount,
            SUM(CASE WHEN la.status = 'APPROVED' THEN la.appliedAmount ELSE 0 END) as totalApprovedAmount,
            AVG(la.riskScore) as avgRiskScore
        FROM LoanProduct lp
        LEFT JOIN LoanApplication la ON la.loanProduct = lp
        LEFT JOIN ApplicationApproval aa ON aa.loanApplication = la
        WHERE la.submittedDate BETWEEN :startDate AND :endDate
        AND (:branchId IS NULL OR la.branch.id = :branchId)
        AND la.submittedDate IS NOT NULL
        GROUP BY lp.productType, lp.name, lp.productCode
        ORDER BY totalApplications DESC
    """)
    List<Object[]> findProductApprovalStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("branchId") Long branchId);





}