# Cash Desk Module

 fixes-may-3
## Описание
Spring Boot приложение за управление на касови операции (депозити и тегления) и проверка на баланси чрез REST API. Поддържа деноминации за BGN (10, 50) и EUR (20, 100) за касиери (MARTINA, PETER, LINDA).

## Функционалности
- **REST API**:
   - POST `/api/v1/cash-operation`: Депозит или теглене на пари.
   - GET `/api/v1/cash-balance`: Проверка на баланс (с опционални филтри: `cashier`, `dateFrom`, `dateTo`).
- **Сигурност**: Задължителен `FIB-X-AUTH` заглавка с API ключ.
- **Съхранение**: Баланси в `balances.txt` (формат: `cashier_name|currency|denomination|count`) и транзакции в `transactions.txt`.
- **Тестове**: Unit тестове за бизнес логиката.
- **Postman**: Колекция за тестване на API.

## Технологии
- **Spring Boot**: За REST API.
- **Maven**: За управление на зависимости.
- **Java 17**: Език за програмиране.
- **Postman**: За тестване на API.
- **SLF4J**: За логиране.

## Изисквания
- **Java 17**
- **Maven**
- **Git**
- **Postman** (по желание)

## Инсталация
1. Клонирай репозиториума: `git clone https://github.com/Gabrielganchev/module-cash-desk.git`
2. Инсталирай: `mvn clean install`
3. Конфигурирай API ключа:
   - Създай `src/main/resources/application.properties`.
   - Добави: `API_KEY=put-the-api-key`
   - Файлът е включен в `.gitignore`, за да не се качва в Git.
4. Стартирай: `mvn spring-boot:run`

## API Ендпойнти
- **POST /api/v1/cash-operation** (`CashOperationController`):
   - Тяло: `CashOperationRequest` (напр. `{"cashierName": "MARTINA", "operationType": "DEPOSIT", "currency": "BGN", "denominations": {"10": 5}}`)
   - Заглавка: `FIB-X-AUTH:put-the-api-key`
- **GET /api/v1/cash-balance** (`CashBalanceController`):
   - Параметри: `cashier` (по желание), `dateFrom` (по желание), `dateTo` (по желание)
   - Заглавка: `FIB-X-AUTH:put-the-api-key`

## Postman
Импортирай `postman/CashDeskModule.postman_collection.json` и `postman/CashDeskModule.postman_environment.json` за тестване на API.

## Тестове
Unit тестове в `src/test/java/com/fibank/module_cash_desk/StorageTest.java` покриват:
- Инициализация на баланси.
- Депозити и тегления с деноминации.
- Проверка на баланс.

Изпълни с: `mvn test`

## Файлове
- **balances.txt**: Съхранява баланси (напр. `MARTINA|BGN|10|50`).
- **transactions.txt**: Логира транзакции (напр. `2025-05-03 12:00:00,MARTINA,DEPOSIT,BGN,10x5`).