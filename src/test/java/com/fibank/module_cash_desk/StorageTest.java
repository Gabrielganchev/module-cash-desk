package com.fibank.module_cash_desk;

import com.fibank.module_cash_desk.model.CashBalanceResponse;
import com.fibank.module_cash_desk.model.CashOperationRequest;
import com.fibank.module_cash_desk.service.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StorageTest {
    private Storage storage;
    private Path tempBalancesFile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempBalancesFile = tempDir.resolve("balances.txt");
        storage = new Storage(tempBalancesFile.toString(), tempDir.resolve("transactions.txt").toString());
    }

    @Test
    void testInitialization() throws IOException {
        assertTrue(Files.exists(tempBalancesFile), "Файлът balances.txt трябва да съществува");
        List<String> lines = Files.readAllLines(tempBalancesFile);
        assertTrue(lines.contains("MARTINA|BGN|10|50"), "Трябва да има 50x10 BGN за MARTINA");
    }

    @Test
    void testDepositAndBalance() {
        CashOperationRequest request = new CashOperationRequest();
        request.setCashierName("MARTINA");
        request.setOperationType("DEPOSIT");
        request.setCurrency("BGN");
        request.setDenominations(Map.of(10, 5)); // 5x10 = 50 BGN

        assertDoesNotThrow(() -> storage.performOperation(request));

        List<CashBalanceResponse> balances = storage.getBalances("MARTINA", null, null);
        assertEquals(1, balances.size());
        CashBalanceResponse response = balances.get(0);
        assertEquals("MARTINA", response.getCashierName());
        assertEquals(Map.of(10, 55, 50, 10), response.getBalances().get("BGN").getDenominations());
        assertEquals(1050, response.getBalances().get("BGN").getTotal());
    }

    @Test
    void testWithdraw() {
        CashOperationRequest request = new CashOperationRequest();
        request.setCashierName("MARTINA");
        request.setOperationType("WITHDRAWAL");
        request.setCurrency("BGN");
        request.setDenominations(Map.of(10, 10)); // 10x10 = 100 BGN

        assertDoesNotThrow(() -> storage.performOperation(request));

        List<CashBalanceResponse> balances = storage.getBalances("MARTINA", null, null);
        assertEquals(Map.of(10, 40, 50, 10), balances.get(0).getBalances().get("BGN").getDenominations());
        assertEquals(900, balances.get(0).getBalances().get("BGN").getTotal());
    }
}