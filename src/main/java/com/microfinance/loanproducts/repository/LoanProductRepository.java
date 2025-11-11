package com.microfinance.loanproducts.repository;

import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducttype.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
    
    boolean existsByProductCode(String productCode);
    boolean existsByName(String name);
    
    List<LoanProduct> findByActiveTrue();
    List<LoanProduct> findByActiveTrueAndStatus(LoanProduct.ProductStatus status);
    List<LoanProduct> findByStatus(LoanProduct.ProductStatus status);
    List<LoanProduct> findByIsTemplateTrueAndActiveTrue();
    
    List<LoanProduct> findByNameContainingAndActiveTrue(String name);
    List<LoanProduct> findByProductTypeAndActiveTrue(ProductType productType);
    List<LoanProduct> findByInterestMethodAndActiveTrue(LoanProduct.InterestMethod interestMethod);
    
    List<LoanProduct> findByNameContainingAndProductTypeAndActiveTrue(String name, ProductType productType);
    List<LoanProduct> findByNameContainingAndInterestMethodAndActiveTrue(String name, LoanProduct.InterestMethod interestMethod);
    List<LoanProduct> findByNameContainingAndProductTypeAndInterestMethodAndActiveTrue(
            String name, ProductType productType, LoanProduct.InterestMethod interestMethod);
    
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
            @Param("interestMethod") LoanProduct.InterestMethod interestMethod);

    List<LoanProduct> findByProductTypeIdAndActiveTrue(Long productTypeId);

}