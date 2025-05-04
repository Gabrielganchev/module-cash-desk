package com.fibank.module_cash_desk.model;

import java.util.HashMap;
import java.util.Map;

public abstract class CashBase {
    private String currency;
    private Map<Integer, Integer> denominations;

    public CashBase() {
        this.currency = "";
        this.denominations = new HashMap<>();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Map<Integer, Integer> getDenominations() {
        return denominations;
    }

    public void setDenominations(Map<Integer, Integer> denominations) {
        validateDenominations(denominations);
        this.denominations = denominations != null ? denominations : new HashMap<>();
    }

    public int getTotal() {
        return denominations.entrySet().stream()
                .mapToInt(entry -> entry.getKey() * entry.getValue())
                .sum();
    }

    protected void validateDenominations(Map<Integer, Integer> denominations) {
        if (denominations == null || denominations.isEmpty()) {
            throw new IllegalArgumentException("Denominations must not be empty");
        }
        for (Map.Entry<Integer, Integer> entry : denominations.entrySet()) {
            Integer denom = entry.getKey();
            Integer count = entry.getValue();
            if (count == null || count <= 0) {
                throw new IllegalArgumentException("Denomination count must be positive");
            }
            if ("BGN".equals(currency) && !(denom == 10 || denom == 50)) {
                throw new IllegalArgumentException("Invalid BGN denomination: " + denom);
            } else if ("EUR".equals(currency) && !(denom == 20 || denom == 100)) {
                throw new IllegalArgumentException("Invalid EUR denomination: " + denom);
            }
        }
    }
}