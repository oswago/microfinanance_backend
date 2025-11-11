package com.microfinance.loanproducts.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.microfinance.loanproducts.entity.LoanProduct;

import java.io.IOException;

public class InterestMethodDeserializer extends JsonDeserializer<LoanProduct.InterestMethod> {
    
    @Override
    public LoanProduct.InterestMethod deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().toUpperCase().replace(" ", "_");
        
        // Handle different input formats
        switch (value) {
            case "FLAT":
            case "FLAT_RATE":
            case "FLATRATE":
                return LoanProduct.InterestMethod.FLAT;
            case "REDUCING_BALANCE":
            case "REDUCING":
            case "REDUCINGBALANCE":
                return LoanProduct.InterestMethod.REDUCING_BALANCE;
            case "COMPOUND":
            case "COMPOUND_INTEREST":
                return LoanProduct.InterestMethod.COMPOUND;
            default:
                throw new IllegalArgumentException("Unknown interest method: " + value);
        }
    }
}