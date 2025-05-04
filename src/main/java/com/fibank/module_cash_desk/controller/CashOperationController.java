package com.fibank.module_cash_desk.controller;

import com.fibank.module_cash_desk.model.CashOperationRequest;
import com.fibank.module_cash_desk.service.Storage;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cash-operation")
public class CashOperationController {
    private static final Logger logger = LoggerFactory.getLogger(CashOperationController.class);
    public static final String API_KEY_HEADER = "FIB-X-AUTH";
    private final Storage storage;

    @Value("${API_KEY}")
    private String API_KEY;

    public CashOperationController(Storage storage) {
        this.storage = storage;
    }

    @PostMapping
    public ResponseEntity<String> performOperation(
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @Valid @RequestBody CashOperationRequest request) {
        if (!API_KEY.equals(apiKey)) {
            logger.warn("Invalid API key for cash operation");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid API key");
        }
        try {
            storage.performOperation(request);
            logger.info("Operation successful: {} {} {} by {}",
                    request.getOperationType(), request.getDenominations(),
                    request.getCurrency(), request.getCashierName());
            return ResponseEntity.ok("Operation successful");
        } catch (IllegalArgumentException e) {
            logger.error("Operation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during operation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }
}