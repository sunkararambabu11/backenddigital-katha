# Digital Katha - Backend API

REST API backend for the **Kirana Credit App** (Digital Khata Book) — a digital ledger for small Indian shopkeepers to track customer credit/debit transactions.

Built with **Spring Boot 3.4.4**, **MySQL**, **Spring Security + JWT**, and **iText PDF**.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.4.4 (Java 17) |
| Database | MySQL 8+ |
| ORM | Spring Data JPA / Hibernate 6 |
| Auth | Spring Security + JWT (jjwt 0.12.6) |
| PDF | iText 7.2.5 |
| Build | Maven (mvnw wrapper included) |

---

## Prerequisites

- **Java 17** (Temurin/OpenJDK recommended)
- **MySQL 8.0+**
- **Maven 3.8+** (or use the included `./mvnw` wrapper)

---

## Setup

### 1. Install and start MySQL

**macOS (Homebrew):**
```bash
brew install mysql
brew services start mysql
```

**Ubuntu/Debian:**
```bash
sudo apt install mysql-server
sudo systemctl start mysql
```

**Windows:**
Download from https://dev.mysql.com/downloads/installer/

### 2. Create the database

```bash
mysql -u root
```

```sql
CREATE DATABASE digitalkatha;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root@123';
FLUSH PRIVILEGES;
EXIT;
```

### 3. Clone and build

```bash
git clone https://github.com/sunkararambabu11/backenddigital-katha.git
cd backenddigital-katha
./mvnw clean package -DskipTests
```

### 4. Run

```bash
java -jar target/springbootapp-0.0.1-SNAPSHOT.jar
```

The server starts on **http://localhost:8081**. Hibernate will auto-create all tables on first run.

---

## Configuration

All config is in `src/main/resources/application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8081` | HTTP port |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/digitalkatha` | MySQL connection URL |
| `spring.datasource.username` | `root` | DB username |
| `spring.datasource.password` | `root@123` | DB password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Auto-create/update tables |
| `openai.api.key` | `YOUR_API_KEY_HERE` | OpenAI key for AI features (optional) |

---

## API Endpoints

All endpoints except auth require a JWT token in the `Authorization: Bearer <token>` header.

### Authentication

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login (returns JWT) | No |

**Register body:**
```json
{
  "ownerName": "Shop Owner",
  "shopName": "My Kirana",
  "email": "owner@email.com",
  "mobile": "9876543210",
  "password": "mypass",
  "confirmPassword": "mypass"
}
```

**Login body:**
```json
{
  "username": "9876543210",
  "password": "mypass"
}
```

**Login response:**
```json
{
  "userId": 1,
  "shopName": "My Kirana",
  "token": "eyJhbGci...",
  "message": "Login successfully"
}
```

### Customers

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/customers` | List all customers for logged-in user |
| POST | `/api/customers` | Create a customer |
| GET | `/api/customers/{id}` | Get single customer |
| PUT | `/api/customers/{id}` | Update customer |
| DELETE | `/api/customers/{id}` | Delete customer |

**Customer fields:** `name`, `mobile`, `openingBalance`, `description`

### Transactions

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions` | Create transaction |
| GET | `/api/transactions/customer/{customerId}` | Get transactions for a customer |
| PUT | `/api/transactions/{id}` | Update transaction |
| DELETE | `/api/transactions/{id}` | Delete transaction |

**Transaction body:**
```json
{
  "customerId": 1,
  "type": "DEBIT",
  "amount": 1500.00,
  "description": "Rice and dal purchase"
}
```

Type must be `DEBIT` or `CREDIT`. The `balanceAfter` field is auto-calculated.

### Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard` | Basic stats (4 fields) |
| GET | `/api/dashboard/summary` | Full summary (11 fields — recommended) |
| GET | `/api/dashboard/top-debtors` | Top debtors list |
| GET | `/api/dashboard/recent` | Recent transactions |
| GET | `/api/dashboard/monthly` | Monthly totals |

**Summary response shape:**
```json
{
  "totalCustomers": 2,
  "activeCustomers": 2,
  "totalDebit": 3500.00,
  "totalCredit": 300.00,
  "totalOutstanding": 3200.00,
  "todayTransactionCount": 3,
  "todayDebit": 3500.00,
  "todayCredit": 300.00,
  "topDebtors": [{ "name": "Priya Sharma", "balance": 2000.00 }],
  "recentTransactions": [{ "name": "Amit Patel", "type": "CREDIT", "amount": 300.00, "date": "2026-04-15 21:35", "description": "Cash payment" }],
  "monthlyData": { "APRIL": 3800.00 }
}
```

### PDF Reports

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/pdf/customers` | Download all customers as PDF |
| POST | `/api/pdf/transactions` | Download filtered transactions as PDF |

**Transaction PDF body:**
```json
{
  "customerId": 1,
  "fromDate": "2026-04-01",
  "toDate": "2026-04-30"
}
```

---

## Project Structure

```
src/main/java/com/springbootexample/
  SpringbootappApplication.java        # Main entry point
  config/
    CorsConfig.java                     # CORS settings (allows localhost:4200)
    JwtFilter.java                      # JWT authentication filter
    SecurityConfig.java                 # Spring Security config
  controller/
    AuthController.java                 # /api/auth/*
    CustomerController.java             # /api/customers/*
    TransactionController.java          # /api/transactions/*
    DashboardController.java            # /api/dashboard/*
    PdfController.java                  # /api/pdf/*
    AiController.java                   # /api/ai/* (AI assistant)
  entity/
    User.java                           # Shop owner account
    Customer.java                       # Customer (khata entry)
    Transaction.java                    # Debit/credit transaction
  dto/
    LoginRequest.java / LoginResponse.java
    RegisterRequest.java
    DashboardDTO.java / DashboardSummaryDTO.java
    TopDebtorDTO.java / RecentTransactionDTO.java
    TransactionPdfRequest.java
  repository/
    UserRepository.java
    CustomerRepository.java
    TransactionRepository.java
  security/
    JwtUtil.java                        # JWT token generation/validation
  service/
    AuthService.java / AuthServiceImpl.java
    CustomerService.java / CustomerImpl.java
    TransactionService.java / TransactionServiceImpl.java
    DashboardService.java / DashboardServiceImpl.java
    PdfService.java / PdfServiceImpl.java
    AiService.java
```

---

## Database Schema

Hibernate auto-creates these tables:

**users**
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT (PK) | Auto-increment |
| shop_name | VARCHAR(255) | |
| owner_name | VARCHAR(255) | |
| email | VARCHAR(255) | |
| mobile | VARCHAR(255) | Unique |
| password | VARCHAR(255) | Plain text (dev mode) |
| created_at | DATETIME | |

**customers**
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT (PK) | Auto-increment |
| name | VARCHAR(255) | |
| mobile | VARCHAR(255) | Unique |
| user_id | BIGINT | Owner's user ID |
| opening_balance | DOUBLE | Starting balance |
| current_balance | DOUBLE | Running balance |
| description | VARCHAR(255) | |
| created_at | DATETIME | |
| updated_at | DATETIME | |

**transactions**
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT (PK) | Auto-increment |
| customer_id | BIGINT | |
| user_id | BIGINT | |
| type | VARCHAR(255) | DEBIT or CREDIT |
| amount | DOUBLE | |
| balance_after | DOUBLE | Auto-calculated |
| description | VARCHAR(255) | |
| created_at | DATETIME | |

---

## Quick Test

After starting the server, verify it works:

```bash
# Register
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"ownerName":"Test","shopName":"Test Shop","email":"t@t.com","mobile":"9000000000","password":"pass","confirmPassword":"pass"}'

# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"9000000000","password":"pass"}'

# Use the token from login response for all other endpoints
```

---

## Related

- **Frontend**: [digitalkatha-frontend](https://github.com/sunkararambabu11/digitalkatha-frontend) — Angular 19 app (IBM Carbon Design System)
