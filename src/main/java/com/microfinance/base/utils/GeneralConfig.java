package com.microfinance.base.utils;

public class GeneralConfig {

    public enum InterestMethod {
        FLAT,
        REDUCING_BALANCE,
        COMPOUND
    }

    public enum TenureUnit {
        DAYS,
        WEEKS,
        MONTHS,
        YEARS
    }

    public enum ProductStatus {
        DRAFT,
        ACTIVE,
        INACTIVE,
        ARCHIVED
    }

    public enum RepaymentStatus {
        CURRENT,
        ADVANCE,
        DUE,
        OVERDUE,
        DEFAULT,
        PAID,
        WRITTEN_OFF
    }

    public enum DelinquencyBucket {
        CURRENT("0-0 days"),
        DAYS_1_30("1-30 days"),
        DAYS_31_60("31-60 days"),
        DAYS_61_90("61-90 days"),
        DAYS_90_PLUS("90+ days");

        private final String description;

        DelinquencyBucket(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }


}
