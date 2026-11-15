# Digital Wallet API

A REST API built with Java and Spring Boot to simulate a digital wallet system.

## Overview

Digital Wallet API allows users to create accounts, manage their wallets, make deposits, transfer funds between wallets, and view transaction history.

The project includes JWT authentication, idempotency, concurrency control, and automated tests to ensure reliable wallet operations.

<img width="1455" height="913" alt="Screenshot 2026-11-14 at 15 37 56" src="https://github.com/user-attachments/assets/8f022557-3a0c-426f-8749-227ec6238105" />

## Tech Stack

- **Java 21** and **Spring Boot 4**
- **Spring Data JPA / Hibernate**
- **Spring Security** with JWT
- **PostgreSQL 17**
- **Flyway** for database migrations
- **MapStruct** and **Lombok**
- **Bean Validation**
- **Springdoc OpenAPI / Swagger UI**
- **JUnit 5**, **Mockito** and **Testcontainers**
- **Maven**


## Features

- User registration with automatic wallet creation
- Authentication and authorization using JWT
- Wallet balance inquiries
- Deposits and transfers between wallets
- Transaction history
- Idempotency to prevent duplicate transactions
- Pessimistic locking to handle concurrent transfers
- Wallet ownership validation
- Global exception handling with appropriate HTTP status codes


## How It Works

### 1. User Registration

Users can create an account through the registration endpoint. Each new user automatically receives a digital wallet with an initial balance of zero.

![Successful user registration](https://github.com/user-attachments/assets/5ccf5b60-9654-49e4-8e5d-e7df8e3e3a6a)

*Successful user registration — HTTP 201 Created.*


### 2. JWT Authentication

Users authenticate using their email and password. After a successful login, the API returns a JWT used to access protected endpoints.

![Successful JWT authentication](https://github.com/user-attachments/assets/6dd3c432-ec5c-48c6-a51c-6efd62cdcb41)

*Successful authentication — HTTP 200 OK.*

### 3. Wallet Deposits

Authenticated users can deposit funds into their own wallets. Each deposit updates the wallet balance and creates a transaction record.

The Idempotency-Key header prevents the same deposit from being processed more than once.

![Wallet balance before deposit](https://github.com/user-attachments/assets/6e398efc-3d1e-4e0a-968d-143a996d0c5b)

*Wallet balance before deposit — HTTP 200 OK.*

![Successful wallet deposit](https://github.com/user-attachments/assets/3b9642e7-7259-46ca-b4ac-c31cda17a5b7)

*Successful deposit of 150.00 — HTTP 201 Created.*

![Wallet balance after deposit](https://github.com/user-attachments/assets/c8c4e956-a841-4e01-88cf-9941084f6123)

*Wallet balance after deposit — HTTP 200 OK.*


### 4. Wallet Transfers

Authenticated users can transfer funds from their own wallet to another wallet.

Transfers are processed within a database transaction, with balance validation and pessimistic locking to prevent inconsistencies during concurrent operations.

![Successful wallet transfer](https://github.com/user-attachments/assets/b18aae7e-2e14-4541-afac-d0cdbdef8f3a)

*Successful transfer of 50.00 — HTTP 201 Created.*

![Sender wallet balance](https://github.com/user-attachments/assets/73e94453-b34a-4cd1-ace4-08cbc96476f8)

*Sender wallet balance after transfer — 100.00.*

![Receiver wallet balance](https://github.com/user-attachments/assets/db0e522d-7965-4e72-842e-81a383383259)

*Receiver wallet balance after transfer — 50.00.*


### 5. Idempotency

Deposits and transfers require an `Idempotency-Key` header to prevent duplicate transactions.

When a request is repeated with the same key and data, the API returns the original transaction without processing it again.

In this example, the transfer was repeated using the same key. The API returned the original transaction (ID 7), and the sender's balance remained unchanged.

![Repeated transfer with same idempotency key](https://github.com/user-attachments/assets/3d68c48f-a07a-43c5-993c-0d271c4b6de3)

*Repeated transfer — original transaction returned (HTTP 201 Created).*

![Wallet balance unchanged after repeated transfer](https://github.com/user-attachments/assets/0096d907-e7ba-40bb-aafd-8b1727d9d220)

*Wallet balance remains at 100.00 after the repeated request — HTTP 200 OK.*

Reusing the same `Idempotency-Key` with different request data returns HTTP 409 Conflict.

![Idempotency key conflict](https://github.com/user-attachments/assets/3e67bac4-059d-4739-9d18-98254c9c5f52)

*Idempotency conflict — HTTP 409 Conflict.*



## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users` | Register a new user |
| GET | `/users/{id}` | Get user information |
| POST | `/auth/login` | Authenticate and receive a JWT |
| GET | `/wallets/me` | Get the authenticated user's wallet |
| GET | `/wallets/{id}` | Get wallet by ID |
| POST | `/transactions/create-deposit` | Deposit funds into a wallet |
| POST | `/transactions/create-transfer` | Transfer funds between wallets |
| GET | `/transactions/{id}` | Get transaction by ID |
| GET | `/transactions/wallet/{walletId}` | Get wallet transaction history |


## Getting Started

### Prerequisites

- Java 21
- Git
- Docker or OrbStack (required for integration tests)
- PostgreSQL 17 (installed locally or running in a Docker container)

### Database Setup

The application uses PostgreSQL 17 as its database.

You can start a PostgreSQL container using Docker:

```bash
docker run -d \
  --name wallet-postgres \
  -e POSTGRES_DB=wallet \
  -e POSTGRES_USER=wallet \
  -e POSTGRES_PASSWORD=your_password \
  -p 127.0.0.1:5433:5432 \
  -v wallet_data:/var/lib/postgresql/data \
  postgres:17
```

The database will be available at `localhost:5433`, with the name `wallet`.

Flyway automatically applies the database migrations when the application starts.


### Environment Variables

The application requires the following environment variables:

| Variable | Description |
|----------|-------------|
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Secret key used to sign JWTs |
| `JWT_EXPIRATION` | JWT expiration time in seconds (e.g., 3600 for 1 hour) |

The `JWT_SECRET` must be generated before starting the application. See the **Generating a JWT Secret** section below.

**macOS and Linux**

```bash
export DB_USERNAME=wallet
export DB_PASSWORD=your_password
export JWT_EXPIRATION=3600
```

**Windows (PowerShell)**

```powershell
$env:DB_USERNAME = "wallet"
$env:DB_PASSWORD = "your_password"
$env:JWT_EXPIRATION = "3600"
```

Replace `your_password` with the password configured for PostgreSQL.

Run these commands in the same terminal session used to start the application.

### Generating a JWT Secret

The application uses HMAC256 to sign JWTs. Generate a secure random secret before starting the application.

**macOS and Linux**

```bash
export JWT_SECRET="$(openssl rand -hex 32)"
```

**Windows (PowerShell)**

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [BitConverter]::ToString($bytes).Replace("-", "").ToLowerInvariant()
```

Both commands generate a cryptographically secure 256-bit secret and set the `JWT_SECRET` environment variable.

Never commit passwords or JWT secrets to the repository.


### Running the Application

Clone the repository:

```bash
git clone https://github.com/MouraDeveloper/digital-wallet-api.git
cd digital-wallet-api
```

Make sure PostgreSQL is running and the required environment variables are configured.

Start the application using the Maven Wrapper:

```bash
./mvnw spring-boot:run
```


On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The API will be available at `http://localhost:8080`.

Access the Swagger UI at:

http://localhost:8080/swagger-ui/index.html

Flyway automatically runs the database migrations during application startup.

## Running Tests

The project includes unit and integration tests covering business rules, persistence, and API endpoints.

Integration tests use Testcontainers to run PostgreSQL 17 in an isolated environment, without requiring a manually configured test database.

To run the test suite on macOS or Linux:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

The current test suite contains 111 tests, including unit and integration tests.

<img width="745" height="202" alt="Screenshot 2026-11-15 at 11 33 23" src="https://github.com/user-attachments/assets/8d81aa99-7ac4-44a1-8015-c5fde4b76166" />


*Test execution — 111 tests passed, BUILD SUCCESS.*

## Technical Decisions

The following decisions were made to improve data consistency, security, and reliability.

### Transaction Management

Deposits and transfers use Spring's `@Transactional` to ensure atomic operations. If an operation fails, database changes are rolled back, preventing partial balance updates.

### Monetary Precision

Monetary values use Java's `BigDecimal` and PostgreSQL's `NUMERIC(12,2)` instead of floating-point types such as `double`. This avoids binary floating-point precision issues and provides fixed decimal precision for wallet balances and transaction amounts.

### Concurrency Control

Wallet operations use pessimistic database locking to prevent conflicting balance updates from concurrent requests.

Pessimistic locking was chosen to serialize conflicting balance updates at the database level, avoiding the retry logic that optimistic locking may require when multiple transactions access the same wallet.

During transfers, wallet locks are acquired in a consistent UUID order to reduce the risk of deadlocks.

### Idempotency

Deposits and transfers require an `Idempotency-Key` header to prevent duplicate processing.

Repeated requests with the same key and matching data return the existing transaction without modifying balances again. Reusing the key with different request data returns HTTP 409 Conflict.

The API preserves the original HTTP 201 Created status on successful retries. Returning HTTP 200 OK for repeated requests is an alternative design.

### Transaction History

Transaction records are designed as append-only history to preserve traceability of deposits and transfers.

The application does not expose transaction update or deletion endpoints, and JPA mappings mark transaction fields as non-updatable. These restrictions are enforced at the application/ORM level rather than through database-level immutability controls.

### Authentication and Authorization

Spring Security and JWT provide stateless authentication.

Resource ownership is validated in the service layer to prevent unauthorized access and mitigate IDOR vulnerabilities.

Unauthorized access to another user's resources returns HTTP 403 Forbidden. Returning HTTP 404 Not Found is an alternative approach when hiding resource existence is a priority.

### Database and Testing Strategy

PostgreSQL is used for both application development and integration testing, avoiding behavioral differences introduced by in-memory databases.

Flyway manages database schema migrations, while Testcontainers provides isolated PostgreSQL instances for integration tests.

The automated suite contains 111 unit and integration tests covering business rules, transaction behavior, persistence, and HTTP authorization.
