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

}
