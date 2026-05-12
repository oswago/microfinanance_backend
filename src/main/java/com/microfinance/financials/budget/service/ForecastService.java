// service/ForecastService.java
package com.microfinance.financials.budget.service;

import com.microfinance.base.entity.User;
import com.microfinance.financials.budget.dto.*;
import com.microfinance.financials.budget.entity.Forecast;
import com.microfinance.financials.budget.repository.ForecastRepository;
import com.microfinance.financials.generalledger.entity.GeneralLedger;
import com.microfinance.financials.generalledger.repository.GeneralLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastService {

    private final ForecastRepository forecastRepository;
    private final GeneralLedgerRepository generalLedgerRepository;

    @Transactional
    public ForecastDTO createForecast(ForecastDTO dto, User currentUser) {
        log.info("User {} creating forecast: {}", currentUser.getUsername(), dto.getForecastName());

        String forecastCode = generateForecastCode();
        
        Forecast forecast = Forecast.builder()
                .forecastCode(forecastCode)
                .forecastName(dto.getForecastName())
                .forecastDate(LocalDate.now())
                .forecastPeriodStart(dto.getForecastPeriodStart())
                .forecastPeriodEnd(dto.getForecastPeriodEnd())
                .category(dto.getCategory())
                .forecastMethod(dto.getForecastMethod())
                .predictedAmount(dto.getPredictedAmount())
                .confidenceLower(dto.getConfidenceLower())
                .confidenceUpper(dto.getConfidenceUpper())
                .confidenceLevel(dto.getConfidenceLevel())
                .assumptions(dto.getAssumptions())
                .notes(dto.getNotes())
                .createdBy(currentUser.getId())
                .build();

        forecast = forecastRepository.save(forecast);
        return convertToDTO(forecast);
    }

    @Transactional(readOnly = true)
    public List<ForecastDTO> getForecastsByCategory(String category) {
        return forecastRepository.findByCategoryOrderByForecastDateDesc(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ForecastResultDTO generateForecast(ForecastRequestDTO request) {
        log.info("Generating forecast for category: {} using method: {}", 
                request.getCategory(), request.getForecastMethod());

        List<HistoricalDataPoint> historicalData = getHistoricalData(request);
        
        List<ForecastDataPoint> forecastedValues;
        BigDecimal growthRate;
        
        switch (request.getForecastMethod()) {
            case "LINEAR_REGRESSION":
                forecastedValues = calculateLinearRegression(historicalData, request.getForecastPeriods());
                growthRate = calculateGrowthRate(historicalData);
                break;
            case "MOVING_AVERAGE":
                forecastedValues = calculateMovingAverage(historicalData, request.getForecastPeriods());
                growthRate = BigDecimal.ZERO;
                break;
            case "EXPONENTIAL_SMOOTHING":
                forecastedValues = calculateExponentialSmoothing(historicalData, request.getForecastPeriods());
                growthRate = calculateGrowthRate(historicalData);
                break;
            default:
                forecastedValues = calculateLinearRegression(historicalData, request.getForecastPeriods());
                growthRate = BigDecimal.ZERO;
        }
        
        BigDecimal totalForecast = forecastedValues.stream()
                .map(ForecastDataPoint::getForecast)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageForecast = totalForecast.divide(BigDecimal.valueOf(forecastedValues.size()), 
                2, RoundingMode.HALF_UP);

        return ForecastResultDTO.builder()
                .forecastedValues(forecastedValues)
                .totalForecast(totalForecast)
                .averageForecast(averageForecast)
                .growthRate(growthRate)
                .confidenceLevel(BigDecimal.valueOf(95))
                .method(request.getForecastMethod())
                .notes("Forecast generated using " + request.getForecastMethod() + " method")
                .build();
    }

    private List<HistoricalDataPoint> getHistoricalData(ForecastRequestDTO request) {
        if (request.getHistoricalData() != null && !request.getHistoricalData().isEmpty()) {
            return request.getHistoricalData();
        }
        
        // Fetch from General Ledger
        List<GeneralLedger> ledgerEntries = generalLedgerRepository.findByTransactionDateBetween(
                request.getStartDate(), request.getEndDate(), org.springframework.data.domain.Pageable.unpaged())
                .getContent();
        
        Map<LocalDate, BigDecimal> monthlyTotals = ledgerEntries.stream()
                .collect(Collectors.groupingBy(
                        gl -> gl.getTransactionDate().withDayOfMonth(1),
                        Collectors.mapping(gl -> gl.getAmount(), 
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
        
        return monthlyTotals.entrySet().stream()
                .map(entry -> HistoricalDataPoint.builder()
                        .date(entry.getKey())
                        .amount(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(HistoricalDataPoint::getDate))
                .collect(Collectors.toList());
    }

    private List<ForecastDataPoint> calculateLinearRegression(List<HistoricalDataPoint> data, int periods) {
        List<ForecastDataPoint> forecasts = new ArrayList<>();
        
        if (data.size() < 2) return forecasts;
        
        // Calculate linear regression coefficients
        int n = data.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = data.get(i).getAmount().doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        
        // Generate forecasts
        for (int i = 1; i <= periods; i++) {
            double x = n + i;
            double forecast = slope * x + intercept;
            double standardError = calculateStandardError(data, slope, intercept);
            
            forecasts.add(ForecastDataPoint.builder()
                    .period("Period " + (n + i))
                    .date(LocalDate.now().plusMonths(i))
                    .forecast(BigDecimal.valueOf(Math.max(0, forecast)))
                    .lowerBound(BigDecimal.valueOf(Math.max(0, forecast - 1.96 * standardError)))
                    .upperBound(BigDecimal.valueOf(forecast + 1.96 * standardError))
                    .build());
        }
        
        return forecasts;
    }

    private List<ForecastDataPoint> calculateMovingAverage(List<HistoricalDataPoint> data, int periods) {
        List<ForecastDataPoint> forecasts = new ArrayList<>();
        
        if (data.size() < 3) return forecasts;
        
        int windowSize = Math.min(3, data.size());
        
        for (int i = 1; i <= periods; i++) {
            List<BigDecimal> lastNValues = data.stream()
                    .skip(Math.max(0, data.size() - windowSize))
                    .map(HistoricalDataPoint::getAmount)
                    .collect(Collectors.toList());
            
            BigDecimal average = lastNValues.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(lastNValues.size()), 2, RoundingMode.HALF_UP);
            
            forecasts.add(ForecastDataPoint.builder()
                    .period("Period " + (data.size() + i))
                    .date(LocalDate.now().plusMonths(i))
                    .forecast(average)
                    .lowerBound(average.multiply(BigDecimal.valueOf(0.9)))
                    .upperBound(average.multiply(BigDecimal.valueOf(1.1)))
                    .build());
            
            // Add the forecast as historical for next iteration
            data.add(HistoricalDataPoint.builder()
                    .date(LocalDate.now().plusMonths(i))
                    .amount(average)
                    .build());
        }
        
        return forecasts;
    }

    private List<ForecastDataPoint> calculateExponentialSmoothing(List<HistoricalDataPoint> data, int periods) {
        List<ForecastDataPoint> forecasts = new ArrayList<>();
        
        if (data.isEmpty()) return forecasts;
        
        double alpha = 0.3; // Smoothing factor
        double forecast = data.get(0).getAmount().doubleValue();
        
        for (int i = 1; i <= periods; i++) {
            if (i - 1 < data.size()) {
                double actual = data.get(i - 1).getAmount().doubleValue();
                forecast = alpha * actual + (1 - alpha) * forecast;
            }
            
            forecasts.add(ForecastDataPoint.builder()
                    .period("Period " + (data.size() + i))
                    .date(LocalDate.now().plusMonths(i))
                    .forecast(BigDecimal.valueOf(forecast))
                    .lowerBound(BigDecimal.valueOf(forecast * 0.9))
                    .upperBound(BigDecimal.valueOf(forecast * 1.1))
                    .build());
        }
        
        return forecasts;
    }

    private double calculateStandardError(List<HistoricalDataPoint> data, double slope, double intercept) {
        double sumSquaredErrors = 0;
        for (int i = 0; i < data.size(); i++) {
            double x = i + 1;
            double predicted = slope * x + intercept;
            double actual = data.get(i).getAmount().doubleValue();
            sumSquaredErrors += Math.pow(actual - predicted, 2);
        }
        return Math.sqrt(sumSquaredErrors / (data.size() - 2));
    }

    private BigDecimal calculateGrowthRate(List<HistoricalDataPoint> data) {
        if (data.size() < 2) return BigDecimal.ZERO;
        
        BigDecimal first = data.get(0).getAmount();
        BigDecimal last = data.get(data.size() - 1).getAmount();
        
        if (first.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        
        return last.subtract(first)
                .divide(first, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private String generateForecastCode() {
        return "FCST-" + System.currentTimeMillis();
    }

    private ForecastDTO convertToDTO(Forecast forecast) {
        return ForecastDTO.builder()
                .id(forecast.getId())
                .forecastCode(forecast.getForecastCode())
                .forecastName(forecast.getForecastName())
                .forecastDate(forecast.getForecastDate())
                .forecastPeriodStart(forecast.getForecastPeriodStart())
                .forecastPeriodEnd(forecast.getForecastPeriodEnd())
                .category(forecast.getCategory())
                .forecastMethod(forecast.getForecastMethod())
                .predictedAmount(forecast.getPredictedAmount())
                .confidenceLower(forecast.getConfidenceLower())
                .confidenceUpper(forecast.getConfidenceUpper())
                .confidenceLevel(forecast.getConfidenceLevel())
                .actualAmount(forecast.getActualAmount())
                .forecastAccuracy(forecast.getForecastAccuracy())
                .assumptions(forecast.getAssumptions())
                .notes(forecast.getNotes())
                .createdAt(forecast.getCreatedAt())
                .build();
    }
}