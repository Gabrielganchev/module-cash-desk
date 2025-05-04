package com.fibank.module_cash_desk.controller;

import com.fibank.module_cash_desk.model.CashBalanceResponse;
import com.fibank.module_cash_desk.service.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cash-balance")
public class CashBalanceController {
    private static final Logger logger = LoggerFactory.getLogger(CashBalanceController.class);
    public static final String API_KEY_HEADER = "FIB-X-AUTH";
    private final Storage storage;

    @Value("${API_KEY}")
    private String API_KEY;

    public CashBalanceController(Storage storage) {
        this.storage = storage;
    }

    @GetMapping
    public ResponseEntity<List<CashBalanceResponse>> getBalance(
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @RequestParam(required = false) String cashier,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        if (!API_KEY.equals(apiKey)) {
            logger.warn("Invalid API key for balance query");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            List<CashBalanceResponse> balances = storage.getBalances(cashier, dateFrom, dateTo);
            logger.info("Balance retrieved for cashier: {}, from: {}, to: {}", cashier, dateFrom, dateTo);
            return ResponseEntity.ok(balances);
        } catch (IllegalArgumentException e) {
            logger.error("Balance query failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of());
        } catch (Exception e) {
            logger.error("Unexpected error during balance query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
}