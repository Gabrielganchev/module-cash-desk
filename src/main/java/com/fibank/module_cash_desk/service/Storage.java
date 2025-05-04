package com.fibank.module_cash_desk.service;

import com.fibank.module_cash_desk.model.CashBalanceResponse;
import com.fibank.module_cash_desk.model.CashBase;
import com.fibank.module_cash_desk.model.CashOperationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Storage {
    private static final Logger logger = LoggerFactory.getLogger(Storage.class);
    private static final String DEFAULT_BALANCES_FILE = "balances.txt";
    private static final String DEFAULT_TRANSACTIONS_FILE = "transactions.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> SUPPORTED_CURRENCIES = List.of("BGN", "EUR");
    private static final List<String> SUPPORTED_CASHIERS = List.of("MARTINA", "PETER", "LINDA");
    private static final List<String> SUPPORTED_OPERATIONS = List.of("DEPOSIT", "WITHDRAWAL");
    private static final Map<String, Map<String, Map<Integer, Integer>>> INITIAL_DENOMINATIONS = Map.of(
            "MARTINA", Map.of("BGN", Map.of(10, 50, 50, 10), "EUR", Map.of(20, 50, 100, 10)),
            "PETER", Map.of("BGN", Map.of(10, 50, 50, 10), "EUR", Map.of(20, 50, 100, 10)),
            "LINDA", Map.of("BGN", Map.of(10, 50, 50, 10), "EUR", Map.of(20, 50, 100, 10))
    );

    private Map<String, Map<String, Map<Integer, Integer>>> denominations;

    private final String balancesFile;
    private final String transactionsFile;

    public Storage() {
        this(DEFAULT_BALANCES_FILE, DEFAULT_TRANSACTIONS_FILE);
    }

    public Storage(String balancesFile, String transactionsFile) {
        this.balancesFile = balancesFile;
        this.transactionsFile = transactionsFile;
        this.denominations = new HashMap<>();
        initializeCashiers();
        loadBalances();
    }

    private void initializeCashiers() {
        for (String cashier : SUPPORTED_CASHIERS) {
            Map<String, Map<Integer, Integer>> cashierDenoms = new HashMap<>();
            Map<String, Map<Integer, Integer>> initial = INITIAL_DENOMINATIONS.get(cashier);
            for (Map.Entry<String, Map<Integer, Integer>> currencyEntry : initial.entrySet()) {
                cashierDenoms.put(currencyEntry.getKey(), new HashMap<>(currencyEntry.getValue()));
            }
            denominations.put(cashier, cashierDenoms);
        }
        saveBalances();
    }

    public void performOperation(CashOperationRequest request) {
        String cashier = request.getCashierName();
        String currency = request.getCurrency();
        String operationType = request.getOperationType();
        Map<Integer, Integer> operationDenoms = request.getDenominations();

        if (!SUPPORTED_CASHIERS.contains(cashier)) {
            throw new IllegalArgumentException("Invalid cashier: " + cashier);
        }
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
        if (!SUPPORTED_OPERATIONS.contains(operationType)) {
            throw new IllegalArgumentException("Unsupported operation: " + operationType);
        }

        Map<String, Map<Integer, Integer>> cashierDenoms = denominations.get(cashier);
        Map<Integer, Integer> currentDenoms = cashierDenoms.computeIfAbsent(currency, k -> new HashMap<>());

        if (operationType.equals("DEPOSIT")) {
            operationDenoms.forEach((denom, count) ->
                    currentDenoms.merge(denom, count, Integer::sum));
        } else if (operationType.equals("WITHDRAWAL")) {
            for (Map.Entry<Integer, Integer> entry : operationDenoms.entrySet()) {
                int denom = entry.getKey();
                int requestedCount = entry.getValue();
                int availableCount = currentDenoms.getOrDefault(denom, 0);
                if (availableCount < requestedCount) {
                    throw new IllegalArgumentException("Insufficient " + denom + " " + currency + " notes for withdrawal");
                }
                currentDenoms.put(denom, availableCount - requestedCount);
            }
        }

        cashierDenoms.put(currency, currentDenoms);
        denominations.put(cashier, cashierDenoms);

        saveBalances();
        saveTransaction(request);
        logger.info("{} performed: {} {} by {}", operationType, operationDenoms, currency, cashier);
    }

    public List<CashBalanceResponse> getBalances(String cashier, String dateFrom, String dateTo) {
        logger.debug("Getting balances for cashier: {}, dateFrom: {}, dateTo: {}", cashier, dateFrom, dateTo);

        // Използвай текущите деноминации за баланс без дати
        Map<String, Map<String, Map<Integer, Integer>>> tempDenoms;
        if (dateFrom == null && dateTo == null) {
            tempDenoms = new HashMap<>();
            for (String c : SUPPORTED_CASHIERS) {
                Map<String, Map<Integer, Integer>> cashierDenoms = denominations.getOrDefault(c, new HashMap<>());
                Map<String, Map<Integer, Integer>> copiedDenoms = new HashMap<>();
                for (Map.Entry<String, Map<Integer, Integer>> currencyEntry : cashierDenoms.entrySet()) {
                    copiedDenoms.put(currencyEntry.getKey(), new HashMap<>(currencyEntry.getValue()));
                }
                tempDenoms.put(c, copiedDenoms);
            }
        } else {
            // Исторически баланс с дати
            LocalDateTime from = dateFrom != null ? LocalDateTime.parse(dateFrom.replace("T", " ").replace("Z", ""), formatter) : null;
            LocalDateTime to = dateTo != null ? LocalDateTime.parse(dateTo.replace("T", " ").replace("Z", ""), formatter) : null;

            tempDenoms = new HashMap<>();
            for (String c : SUPPORTED_CASHIERS) {
                Map<String, Map<Integer, Integer>> cashierDenoms = new HashMap<>();
                Map<String, Map<Integer, Integer>> initial = INITIAL_DENOMINATIONS.get(c);
                for (Map.Entry<String, Map<Integer, Integer>> currencyEntry : initial.entrySet()) {
                    cashierDenoms.put(currencyEntry.getKey(), new HashMap<>(currencyEntry.getValue()));
                }
                tempDenoms.put(c, cashierDenoms);
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(transactionsFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    LocalDateTime timestamp = LocalDateTime.parse(parts[0], formatter);
                    String c = parts[1];
                    String operation = parts[2];
                    String currency = parts[3];
                    String denomStr = parts[4];

                    if ((from == null || !timestamp.isBefore(from)) && (to == null || !timestamp.isAfter(to))) {
                        Map<Integer, Integer> operationDenoms = parseDenominations(denomStr);
                        Map<Integer, Integer> currentDenoms = tempDenoms.get(c).get(currency);
                        if (operation.equals("DEPOSIT")) {
                            operationDenoms.forEach((denom, count) ->
                                    currentDenoms.merge(denom, count, Integer::sum));
                        } else if (operation.equals("WITHDRAWAL")) {
                            operationDenoms.forEach((denom, count) ->
                                    currentDenoms.merge(denom, -count, Integer::sum));
                        }
                        tempDenoms.get(c).put(currency, currentDenoms);
                    }
                }
            } catch (IOException e) {
                logger.error("Error reading transactions", e);
            }
        }

        List<CashBalanceResponse> responses = new ArrayList<>();
        for (String c : tempDenoms.keySet()) {
            if (cashier != null && !c.equals(cashier)) continue;
            CashBalanceResponse response = new CashBalanceResponse();
            response.setCashierName(c);
            Map<String, CashBase> balancesMap = new HashMap<>();
            for (String currency : tempDenoms.get(c).keySet()) {
                CashBase cashBase = new CashBase() {};
                cashBase.setCurrency(currency);
                cashBase.setDenominations(new HashMap<>(tempDenoms.get(c).get(currency)));
                balancesMap.put(currency, cashBase);
            }
            response.setBalances(balancesMap);
            responses.add(response);
        }
        logger.debug("Returning balances: {}", responses);
        return responses;
    }

    private void saveBalances() {
        logger.debug("Saving balances to {}", balancesFile);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(balancesFile))) {
            for (String cashier : denominations.keySet()) {
                for (String currency : denominations.get(cashier).keySet()) {
                    for (Map.Entry<Integer, Integer> entry : denominations.get(cashier).get(currency).entrySet()) {
                        writer.write(String.format("%s|%s|%d|%d%n", cashier, currency, entry.getKey(), entry.getValue()));
                    }
                }
            }
            logger.debug("Balances saved successfully");
        } catch (IOException e) {
            logger.error("Error saving balances", e);
        }
    }

    private void loadBalances() {
        logger.debug("Loading balances from {}", balancesFile);
        File file = new File(balancesFile);
        if (!file.exists()) {
            logger.info("Balances file does not exist, initializing new file");
            saveBalances();
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(balancesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length != 4) {
                    logger.warn("Invalid line in balances file: " + line);
                    continue;
                }
                String cashier = parts[0];
                String currency = parts[1];
                try {
                    int denom = Integer.parseInt(parts[2]);
                    int count = Integer.parseInt(parts[3]);
                    denominations.computeIfAbsent(cashier, k -> new HashMap<>())
                            .computeIfAbsent(currency, k -> new HashMap<>())
                            .put(denom, count);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid number format in balances file: " + line, e);
                }
            }
            logger.debug("Balances loaded successfully");
        } catch (IOException e) {
            logger.error("Error loading balances", e);
        }
    }

    private void saveTransaction(CashOperationRequest request) {
        logger.debug("Saving transaction for {} to {}", request.getCashierName(), transactionsFile);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(transactionsFile, true))) {
            String denomStr = request.getDenominations().entrySet().stream()
                    .map(entry -> entry.getKey() + "x" + entry.getValue())
                    .collect(Collectors.joining(";"));
            String line = String.format("%s,%s,%s,%s,%s",
                    LocalDateTime.now().format(formatter),
                    request.getCashierName(),
                    request.getOperationType(),
                    request.getCurrency(),
                    denomStr);
            writer.write(line);
            writer.newLine();
            logger.debug("Transaction saved: {}", line);
        } catch (IOException e) {
            logger.error("Error saving transaction", e);
        }
    }

    private Map<Integer, Integer> parseDenominations(String denomStr) {
        Map<Integer, Integer> result = new HashMap<>();
        String[] pairs = denomStr.split(";");
        for (String pair : pairs) {
            String[] parts = pair.split("x");
            if (parts.length == 2) {
                try {
                    int denom = Integer.parseInt(parts[0]);
                    int count = Integer.parseInt(parts[1]);
                    result.put(denom, count);
                } catch (NumberFormatException e) {
                    logger.error("Invalid denomination format: " + pair, e);
                }
            }
        }
        return result;
    }
}