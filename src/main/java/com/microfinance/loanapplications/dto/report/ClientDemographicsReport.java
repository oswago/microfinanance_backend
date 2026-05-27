// src/main/java/com/microfinance/loanapplications/dto/report/ClientDemographicsReport.java
package com.microfinance.loanapplications.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDemographicsReport {
    private Integer totalClients;
    private GenderDistribution genderDistribution;
    private AgeGroups ageGroups;
    private MaritalStatusDistribution maritalStatusDistribution;
    private List<OccupationBreakdown> occupationBreakdown;
    private List<LocationBreakdown> locationBreakdown;
    private double clientGrowth;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenderDistribution {
        private Integer male;
        private Integer female;
        private Integer other;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgeGroups {
        private Integer under25;
        private Integer age25_35;
        private Integer age36_50;
        private Integer over50;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaritalStatusDistribution {
        private Integer single;
        private Integer married;
        private Integer divorced;
        private Integer widowed;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OccupationBreakdown {
        private String occupation;
        private Integer count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationBreakdown {
        private String branch;
        private Integer count;
    }
}