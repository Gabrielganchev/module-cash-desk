package com.fibank.module_cash_desk.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public class CashOperationRequest extends CashBase {
    @NotBlank(message = "Cashier name must not be blank")
    private String cashierName;

    @NotBlank(message = "Operation type must not be blank")
    @Pattern(regexp = "DEPOSIT|WITHDRAWAL", message = "Operation type must be DEPOSIT or WITHDRAWAL")
    private String operationType;

    public CashOperationRequest() {
        super();
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    @Override
    @NotEmpty(message = "Denominations must not be empty")
    public Map<Integer, Integer> getDenominations() {
        return super.getDenominations();
    }

    @Override
    public void setDenominations(Map<Integer, Integer> denominations) {
        validateDenominations(denominations);
        super.setDenominations(denominations);
    }

    @Override
    protected void validateDenominations(Map<Integer, Integer> denominations) {
        if (denominations == null || denominations.isEmpty()) {
            throw new IllegalArgumentException("Denominations must not be empty");
        }
        String currency = getCurrency();
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