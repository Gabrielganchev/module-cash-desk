package com.fibank.module_cash_desk.service;

import com.fibank.module_cash_desk.model.CashBalanceResponse;
import com.fibank.module_cash_desk.model.CashOperationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// клас за управление на касови операции и баланси
@Component
public class Storage {
    private static final Logger logger = LoggerFactory.getLogger(Storage.class);
    private static final String TRANSACTIONS_FILE = "transactions.txt";
    private static final String BALANCES_FILE = "balances.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> SUPPORTED_CURRENCIES = List.of("BGN", "EUR");
    private static final List<String> SUPPORTED_OPERATIONS = List.of("DEPOSIT", "WITHDRAW");

    private Map<String, Map<String, Double>> balances;
    private Map<String, Map<String, String>> denominations;

    public Storage() {
        balances = new HashMap<>();
        denominations = new HashMap<>();
        initializeCashiers();
        loadBalances();
    }

    private void initializeCashiers() {
        List<String> cashiers = List.of("MARTINA", "PETER", "LINDA");
        for (String cashier : cashiers) {
            Map<String, Double> cashierBalances = new HashMap<>();
            cashierBalances.put("BGN", 1000.0);
            cashierBalances.put("EUR", 2000.0);
            balances.put(cashier, cashierBalances);

            Map<String, String> cashierDenominations = new HashMap<>();
            cashierDenominations.put("BGN", "10x50;50x10");
            cashierDenominations.put("EUR", "10x100;50x20");
            denominations.put(cashier, cashierDenominations);
        }
        saveBalances();
    }

    // обработка на депозит или теглене с валидации
    public void performOperation(CashOperationRequest request) {
        String cashier = request.getCashierName();
        String currency = request.getCurrency();
        double amount = request.getAmount();
        String operationType = request.getOperationType();
        String denominationStr = request.getDenominations();

        // проверка за поддържана валута
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new IllegalArgumentException("Unsupported currency: " + currency + ". Supported currencies are: " + SUPPORTED_CURRENCIES);
        }

        // проверка за поддържана операция
        if (!SUPPORTED_OPERATIONS.contains(operationType)) {
            throw new IllegalArgumentException("Unsupported operation: " + operationType + ". Supported operations are: " + SUPPORTED_OPERATIONS);
        }

        Map<String, Double> cashierBalances = balances.get(cashier);
        double currentBalance = cashierBalances.get(currency);

        // при депозит проверяваме дали сумата съвпада с деноминациите
        if (operationType.equals("DEPOSIT")) {
            validateDenominations(amount, denominationStr);
            cashierBalances.put(currency, currentBalance + amount);
        }
        // при теглене проверяваме дали има достатъчно пари
        else if (operationType.equals("WITHDRAW")) {
            if (currentBalance < amount) {
                throw new IllegalArgumentException("Insufficient funds for " + cashier + ". Available: " + currentBalance + " " + currency + ", requested: " + amount);
            }
            cashierBalances.put(currency, currentBalance - amount);
        }

        denominations.get(cashier).put(currency, denominationStr);

        saveBalances();
        saveTransaction(request);
        logger.info("{} performed: {} {} {} by {}", operationType, amount, currency, cashier);
    }

    // валидация на деноминациите спрямо сумата
    private void validateDenominations(double amount, String denominationStr) {
        if (denominationStr == null || denominationStr.isEmpty()) {
            throw new IllegalArgumentException("Denominations cannot be empty for deposit");
        }

        double calculatedAmount = 0;
        String[] pairs = denominationStr.split(";");
        for (String pair : pairs) {
            String[] parts = pair.split("x");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid denomination format: " + pair + ". Expected format: valueXcount");
            }
            try {
                double value = Double.parseDouble(parts[0]);
                int count = Integer.parseInt(parts[1]);
                calculatedAmount += value * count;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid denomination numbers in: " + pair);
            }
        }

        if (Math.abs(calculatedAmount - amount) > 0.01) {
            throw new IllegalArgumentException("Amount " + amount + " does not match the sum of denominations " + calculatedAmount);
        }
    }

    public List<CashBalanceResponse> getBalances(String cashier, String dateFrom, String dateTo) {
        LocalDateTime from = dateFrom != null ? LocalDateTime.parse(dateFrom.replace("T", " ").replace("Z", ""), formatter) : null;
        LocalDateTime to = dateTo != null ? LocalDateTime.parse(dateTo.replace("T", " ").replace("Z", ""), formatter) : null;

        if (from == null && to == null) {
            List<CashBalanceResponse> responses = new ArrayList<>();
            for (String c : balances.keySet()) {
                if (cashier != null && !c.equals(cashier)) continue;
                for (String currency : balances.get(c).keySet()) {
                    CashBalanceResponse response = new CashBalanceResponse();
                    response.setCashierName(c);
                    response.setCurrency(currency);
                    response.setBalance(balances.get(c).get(currency));
                    response.setDenominations(denominations.get(c).get(currency));
                    responses.add(response);
                }
            }
            return responses;
        }

        Map<String, Map<String, Double>> tempBalances = new HashMap<>();
        Map<String, Map<String, String>> tempDenominations = new HashMap<>();
        for (String c : balances.keySet()) {
            tempBalances.put(c, new HashMap<>(Map.of("BGN", 1000.0, "EUR", 2000.0)));
            tempDenominations.put(c, new HashMap<>(Map.of("BGN", "10x50;50x10", "EUR", "10x100;50x20")));
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(TRANSACTIONS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                LocalDateTime timestamp = LocalDateTime.parse(parts[0], formatter);
                String c = parts[1];
                String operation = parts[2];
                String currency = parts[3];
                double amount = Double.parseDouble(parts[4]);
                String denom = parts[5];

                if ((from == null || !timestamp.isBefore(from)) && (to == null || !timestamp.isAfter(to))) {
                    Map<String, Double> cashierBalances = tempBalances.get(c);
                    double currentBalance = cashierBalances.get(currency);
                    if (operation.equals("DEPOSIT")) {
                        cashierBalances.put(currency, currentBalance + amount);
                    } else if (operation.equals("WITHDRAW")) {
                        cashierBalances.put(currency, currentBalance - amount);
                    }
                    tempDenominations.get(c).put(currency, denom);
                }
            }
        } catch (IOException e) {
            logger.error("Error reading transactions", e);
        }

        List<CashBalanceResponse> responses = new ArrayList<>();
        for (String c : tempBalances.keySet()) {
            if (cashier != null && !c.equals(cashier)) continue;
            for (String currency : tempBalances.get(c).keySet()) {
                CashBalanceResponse response = new CashBalanceResponse();
                response.setCashierName(c);
                response.setCurrency(currency);
                response.setBalance(tempBalances.get(c).get(currency));
                response.setDenominations(tempDenominations.get(c).get(currency));
                responses.add(response);
            }
        }
        return responses;
    }

    private void saveBalances() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BALANCES_FILE))) {
            for (String cashier : balances.keySet()) {
                for (String currency : balances.get(cashier).keySet()) {
                    writer.write(String.format("%s,%s,%.2f,%s",
                            cashier, currency, balances.get(cashier).get(currency),
                            denominations.get(cashier).get(currency)));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            logger.error("Error saving balances", e);
        }
    }

    private void loadBalances() {
        File file = new File(BALANCES_FILE);
        if (!file.exists()) {
            saveBalances();
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(BALANCES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                String cashier = parts[0];
                String currency = parts[1];
                double balance = Double.parseDouble(parts[2]);
                String denom = parts[3];
                balances.get(cashier).put(currency, balance);
                denominations.get(cashier).put(currency, denom);
            }
        } catch (IOException e) {
            logger.error("Error loading balances", e);
        }
    }

    private void saveTransaction(CashOperationRequest request) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TRANSACTIONS_FILE, true))) {
            String line = String.format("%s,%s,%s,%s,%.2f,%s",
                    LocalDateTime.now().format(formatter),
                    request.getCashierName(),
                    request.getOperationType(),
                    request.getCurrency(),
                    request.getAmount(),
                    request.getDenominations());
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            logger.error("Error saving transaction", e);
        }
    }
}