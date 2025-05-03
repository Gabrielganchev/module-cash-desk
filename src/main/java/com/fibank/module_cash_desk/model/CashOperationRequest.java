package com.fibank.module_cash_desk.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;


public class CashOperationRequest extends CashBase {
    @NotBlank
    private String operationType;

    @Positive
    private double amount;


    public String getOperationType() {
        return operationType;
    }

    public double getAmount() {
        return amount;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}


