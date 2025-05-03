package com.fibank.module_cash_desk.model;

public abstract class CashBase {
    private String cashierName;
    private String currency;
    private String denominations;

    public String getCashierName() {
        return cashierName;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDenominations() {
        return denominations;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setDenominations(String denominations) {
        this.denominations = denominations;
    }
}
