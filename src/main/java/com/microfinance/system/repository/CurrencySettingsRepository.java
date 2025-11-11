package com.microfinance.system.repository;

import com.microfinance.system.entity.CurrencySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencySettingsRepository extends JpaRepository<CurrencySettings, String> {
    
    /**
     * Find all active currencies
     */
    List<CurrencySettings> findByActiveTrue();
    
    /**
     * Find the default currency
     */
    Optional<CurrencySettings> findByDefaultValueTrue();
    
    /**
     * Find active default currency
     */
    Optional<CurrencySettings> findByDefaultValueTrueAndActiveTrue();
    
    /**
     * Check if currency code exists
     */
    boolean existsByCurrencyCode(String currencyCode);
    
    /**
     * Find currencies by active status
     */
    List<CurrencySettings> findByActive(boolean active);
    
    /**
     * Get all currency codes
     */
    @Query("SELECT c.currencyCode FROM CurrencySettings c WHERE c.active = true")
    List<String> findAllActiveCurrencyCodes();
    
    /**
     * Update exchange rate for a currency
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE CurrencySettings c SET c.exchangeRate = :exchangeRate WHERE c.currencyCode = :currencyCode")
    void updateExchangeRate(@org.springframework.data.repository.query.Param("currencyCode") String currencyCode, 
                           @org.springframework.data.repository.query.Param("exchangeRate") java.math.BigDecimal exchangeRate);
    
    /**
     * Set all currencies as non-default and set one as default
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE CurrencySettings c SET c.defaultValue = false")
    void clearAllDefaultFlags();
    
    /**
     * Set specific currency as default
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE CurrencySettings c SET c.defaultValue = true WHERE c.currencyCode = :currencyCode")
    void setAsDefault(@org.springframework.data.repository.query.Param("currencyCode") String currencyCode);
}