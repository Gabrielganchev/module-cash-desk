package com.fibank.module_cash_desk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class CashBalanceResponse {
    @JsonProperty("cashierName")
    private String cashierName;

    @JsonProperty("balances")
    private Map<String, CashBase> balances;

    public CashBalanceResponse() {
        this.cashierName = "";
        this.balances = new HashMap<>();
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public Map<String, CashBase> getBalances() {
        return balances;
    }

    public void setBalances(Map<String, CashBase> balances) {
        this.balances = balances != null ? balances : new HashMap<>();
    }
}