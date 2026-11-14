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
