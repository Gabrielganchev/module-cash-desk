package com.fibank.module_cash_desk;


import com.fibank.module_cash_desk.model.CashOperationRequest;
import com.fibank.module_cash_desk.service.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StorageTest {
    private Storage storage;

    @BeforeEach
    void setUp() {
        storage = new Storage();
    }

    @Test
    void testDepositDenominationsValidation() {
        CashOperationRequest request = new CashOperationRequest();
        request.setCashierName("MARTINA");
        request.setOperationType("DEPOSIT");
        request.setCurrency("BGN");
        request.setAmount(600);
        request.setDenominations("10x10;50x10"); // 10x10 + 50x10 = 100 + 500 = 600

        // трябва да мине успешно, защото сумата съвпада с деноминациите
        assertDoesNotThrow(() -> storage.performOperation(request));

        // променяме деноминациите, за да не съвпадат със сумата
        request.setDenominations("10x5;50x5"); // 10x5 + 50x5 = 50 + 250 = 300
        Exception exception = assertThrows(IllegalArgumentException.class, () -> storage.performOperation(request));
        assertEquals("Amount 600.0 does not match the sum of denominations 300.0", exception.getMessage());
    }

    @Test
    void testWithdrawInsufficientFunds() {
        CashOperationRequest request = new CashOperationRequest();
        request.setCashierName("MARTINA");
        request.setOperationType("WITHDRAW");
        request.setCurrency("BGN");
        request.setAmount(1500); // MARTINA има само 1000 BGN
        request.setDenominations("10x50;50x20");

        // опитваме да изтеглим повече, отколкото има
        Exception exception = assertThrows(IllegalArgumentException.class, () -> storage.performOperation(request));
        assertEquals("Insufficient funds for MARTINA. Available: 1000.0 BGN, requested: 1500.0", exception.getMessage());

        // теглим сума, която е по-малка от наличната
        request.setAmount(500);
        assertDoesNotThrow(() -> storage.performOperation(request));
    }
}