package com.fibank.module_cash_desk.controller;


import com.fibank.module_cash_desk.model.CashBalanceResponse;
import com.fibank.module_cash_desk.model.CashOperationRequest;
import com.fibank.module_cash_desk.service.Storage;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


// API контролер за касови операции и баланси
@RestController
@RequestMapping("/api/v1")
public class CashController {
    private static final Logger logger = LoggerFactory.getLogger(CashController.class);
    public static final String API_KEY_HEADER = "FIB-X-AUTH";
    private final Storage storage;


    @Value("${API_KEY}")
    private String API_KEY;

    public CashController(Storage storage) {
        this.storage = storage;
    }

    @PostMapping("/cash-operation")
    public ResponseEntity<String> performOperation(
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @Valid @RequestBody CashOperationRequest request) {
        if (!API_KEY.equals(apiKey)) {
            logger.warn("Invalid API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid API key");
        }
        storage.performOperation(request);
        return ResponseEntity.ok("Operation successful");
    }

    @GetMapping("/cash-balance")
    public ResponseEntity<List<CashBalanceResponse>> getBalance(
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @RequestParam(required = false) String cashier,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        if (!API_KEY.equals(apiKey)) {
            logger.warn("Invalid API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(storage.getBalances(cashier, dateFrom, dateTo));
    }
}