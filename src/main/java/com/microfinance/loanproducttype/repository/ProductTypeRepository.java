package com.microfinance.loanproducttype.repository;

import com.microfinance.loanproducttype.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {
    
    boolean existsByCode(String code);
    boolean existsByName(String name);
    
    Optional<ProductType> findByCode(String code);
    Optional<ProductType> findByName(String name);
    
    List<ProductType> findByActiveTrue();
    List<ProductType> findByActiveTrueOrderByDisplayOrderAsc();
    
    @Query("SELECT pt FROM ProductType pt WHERE pt.active = true AND " +
           "(LOWER(pt.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(pt.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<ProductType> searchActiveProductTypes(String searchTerm);
}