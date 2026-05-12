package com.microfinance.loanproducts.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanproducts.entity.LoanProduct;

import java.io.IOException;

public class TenureUnitDeserializer extends JsonDeserializer<GeneralConfig.TenureUnit> {
    
    @Override
    public GeneralConfig.TenureUnit deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().toUpperCase();
        
        switch (value) {
            case "DAYS":
            case "DAY":
                return GeneralConfig.TenureUnit.DAYS;
            case "WEEKS":
            case "WEEK":
                return GeneralConfig.TenureUnit.WEEKS;
            case "MONTHS":
            case "MONTH":
                return GeneralConfig.TenureUnit.MONTHS;
            case "YEARS":
            case "YEAR":
                return GeneralConfig.TenureUnit.YEARS;
            default:
                throw new IllegalArgumentException("Unknown tenure unit: " + value);
        }
    }
}