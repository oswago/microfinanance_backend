package com.microfinance.loanproducts.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.microfinance.loanproducts.entity.LoanProduct;

import java.io.IOException;

public class TenureUnitDeserializer extends JsonDeserializer<LoanProduct.TenureUnit> {
    
    @Override
    public LoanProduct.TenureUnit deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().toUpperCase();
        
        switch (value) {
            case "DAYS":
            case "DAY":
                return LoanProduct.TenureUnit.DAYS;
            case "WEEKS":
            case "WEEK":
                return LoanProduct.TenureUnit.WEEKS;
            case "MONTHS":
            case "MONTH":
                return LoanProduct.TenureUnit.MONTHS;
            case "YEARS":
            case "YEAR":
                return LoanProduct.TenureUnit.YEARS;
            default:
                throw new IllegalArgumentException("Unknown tenure unit: " + value);
        }
    }
}