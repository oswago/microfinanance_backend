
// entity/Forecast.java
package com.microfinance.financials.budget.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fin_forecasts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Forecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "forecast_code", nullable = false, unique = true, length = 50)
    private String forecastCode;

    @Column(name = "forecast_name", nullable = false, length = 100)
    private String forecastName;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "forecast_period_start", nullable = false)
    private LocalDate forecastPeriodStart;

    @Column(name = "forecast_period_end", nullable = false)
    private LocalDate forecastPeriodEnd;

    @Column(name = "category", nullable = false, length = 50)
    private String category; // LOAN_DISBURSEMENTS, LOAN_REPAYMENTS, INTEREST_INCOME, OPERATING_EXPENSES

    @Column(name = "forecast_method", length = 50)
    private String forecastMethod; // LINEAR_REGRESSION, MOVING_AVERAGE, EXPONENTIAL_SMOOTHING, MANUAL

    @Column(name = "predicted_amount", precision = 15, scale = 2)
    private BigDecimal predictedAmount;

    @Column(name = "confidence_lower", precision = 15, scale = 2)
    private BigDecimal confidenceLower;

    @Column(name = "confidence_upper", precision = 15, scale = 2)
    private BigDecimal confidenceUpper;

    @Column(name = "confidence_level", precision = 5, scale = 2)
    private BigDecimal confidenceLevel = BigDecimal.valueOf(95);

    @Column(name = "actual_amount", precision = 15, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "forecast_accuracy", precision = 5, scale = 2)
    private BigDecimal forecastAccuracy;

    @Column(columnDefinition = "TEXT")
    private String assumptions;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;
}