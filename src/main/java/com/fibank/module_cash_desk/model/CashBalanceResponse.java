package com.fibank.module_cash_desk.model;

import lombok.Data;


public class CashBalanceResponse extends CashBase{
    private double balance;

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
