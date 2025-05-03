
# Cash Desk Module

## Features
- REST API for depositing and withdrawing cash.
- API key validation for secure requests.
- Storage of balances and transactions in `balances.txt` and `transactions.txt`.
- Unit tests to validate business logic.
- Postman collection for API testing.

## Technologies
- **Spring Boot**: For building the REST API.
- **Maven**: For dependency management.
- **Java 17**: Programming language.
- **Postman**: For API testing.
- **SLF4J**: For logging.

## Prerequisites
Before running the project, ensure you have the following installed:
- **Java 17**: Required to run the application.
- **Maven**: For building and running the project.
- **Git**: To clone the repository.
- **Postman** (optional): For testing the API.


A simple Spring Boot application for cash operations.

## Setup
1. Clone the repo: `git clone https://github.com/Gabrielganchev/module-cash-desk.git`
2. Build: `mvn clean install`
3. Configure the API key:
    - Create a `.env` file in the project root.
    - Add the following line: `API_KEY=your-api-key`
    - You will find the `Api-Key` in the file that was sent.
    - See `.env.example` for reference.
    - Load the `.env` file into your environment:
        - On Linux/Mac: `export $(cat .env | xargs)`
        - On Windows (Command Prompt): `set API_KEY=your-api-key`
        - On Windows (PowerShell): `$env:API_KEY="your-api-key"`
      
4. Run: `mvn spring-boot:run`

## API
- **POST /api/v1/cash-operation**: Deposit or withdraw
- **GET /api/v1/cash-balance**: Check balance 

## Postman
Import `postman/CashDeskModule.postman_collection.json` and `postman/CashDeskModule.postman_environment.json` to test the API.
