# Hospital Billing Service

A billing microservice developed as part of a team-based hospital management project.

This repository contains the **billing component only**, rather than the complete hospital system. It provides REST APIs for medicine bills and publishes payment and prescription messages through Kafka for integration with other services.

## My Contribution

My work focused on the billing backend:

- Implementing REST APIs for creating, retrieving, paying, and cancelling bills.
- Calculating bill totals using `BigDecimal`.
- Persisting billing records with Spring Data JPA and MySQL.
- Publishing payment and prescription messages through Kafka.

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.1.0 |
| REST API | Spring Web |
| Persistence | Spring Data JPA, MySQL |
| Messaging | Spring Kafka, Apache Kafka |
| Build tool | Maven Wrapper |

## Implemented Features

### Bill Creation

Create a bill containing patient details, medicine details, quantity, and price per unit.

The service calculates the total:

```text
totalAmount = pricePerUnit × quantity
```

Monetary calculations use Java `BigDecimal`. Newly created bills have the status `PENDING`.

### Bill Retrieval

Retrieve:

- All bills.
- A bill by its ID.
- Bills belonging to a patient.
- Bills filtered by status.

### Payment Processing

The payment endpoint:

1. Finds the requested bill.
2. Updates its status to `PAID`.
3. Records the payment timestamp.
4. Publishes payment and prescription messages to Kafka.

Calling the endpoint again for an already-paid bill returns the existing bill without publishing those messages again.

This endpoint records payment status within the project. It does not charge a card or connect to a real payment gateway.

### Bill Cancellation

Cancel a bill that has not been paid.

The service rejects cancellation of a bill with the status `PAID`.

## Service Integration

```text
API Client
    |
    v
Billing REST API
    |
    v
Billing Service ------> MySQL
    |
    v
Kafka
    |
    +---- payment-topic
    |
    +---- prescription-topic
```

The Kafka messages support integration with the project's pharmacy workflow. Downstream services are outside this repository.

## API Endpoints

Base path: `/api/billing`

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/billing` | Create a bill |
| GET | `/api/billing` | Retrieve all bills |
| GET | `/api/billing/{id}` | Retrieve a bill by ID |
| GET | `/api/billing/patient/{patientId}` | Retrieve bills for a patient |
| GET | `/api/billing/status/{status}` | Retrieve bills by status |
| POST | `/api/billing/{id}/pay` | Mark a bill as paid and publish messages |
| POST | `/api/billing/{id}/cancel` | Cancel an unpaid bill |

Supported statuses:

```text
PENDING
PAID
CANCELLED
```

## Example Request

Send a `POST` request to `/api/billing` with the header:

```text
Content-Type: application/json
```

Example body using fictional data:

```json
{
  "patientId": "DEMO-P001",
  "patientName": "Demo Patient",
  "medicineId": "DEMO-M001",
  "medicineName": "Demo Medicine",
  "quantity": 2,
  "pricePerUnit": 25.50
}
```

The calculated total is `51.00`, and the initial status is `PENDING`.

Use the bill ID returned by the API for subsequent requests:

```text
GET  /api/billing/{id}
POST /api/billing/{id}/pay
POST /api/billing/{id}/cancel
```

The payment and cancellation endpoints do not require a request body.

## Running Locally

### Requirements

- JDK 17.
- A running MySQL server.
- A running Kafka broker.
- A development database and database credentials.

The Maven Wrapper is included. MySQL and Kafka must be provided separately; this repository does not include a Docker Compose setup for them.

### 1. Clone the Repository

```bash
git clone https://github.com/jeffza666/Billing-Service.git
cd Billing-Service
```

### 2. Create a Development Database

Using a MySQL account with permission to create databases:

```sql
CREATE DATABASE billing_demo;
```

### 3. Configure the Service

Configuration is defined in:

```text
src/main/resources/application.properties
```

You can override connection settings with environment variables.

For example, in PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/billing_demo"
$env:SPRING_DATASOURCE_USERNAME = "YOUR_DEV_DB_USER"
$env:SPRING_DATASOURCE_PASSWORD = "YOUR_DEV_DB_PASSWORD"
$env:SPRING_KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:SERVER_PORT = "8080"
```

Replace the example values with your development settings.

The repository's Kafka address, `kafka:29092`, requires a network where the hostname `kafka` resolves. Override it with your broker's reachable address when running locally.

Ensure these topics exist, or enable automatic topic creation on your development broker:

```text
payment-topic
prescription-topic
```

The current JPA configuration uses `ddl-auto=update` to create or update database tables.

### 4. Start the Application

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

With the example port setting, the API is available at:

```text
http://localhost:8080/api/billing
```

## Project Structure

```text
src/main/java/com/hospital/billing/
├── controller/    # REST endpoints
├── dto/           # Request and Kafka message objects
├── kafka/         # Kafka message producer
├── model/         # Billing entity and status enum
├── repository/    # Database access
└── service/       # Billing calculations and workflow

src/main/resources/
└── application.properties
```

## Verification

Test sources are included in the repository. To run the available tests:

Windows:

```powershell
.\mvnw.cmd test
```

macOS/Linux:

```bash
./mvnw test
```

Tests that load the application context may require database and Kafka configuration.

For manual API verification:

1. Create a bill and check its calculated total and `PENDING` status.
2. Retrieve it by ID and patient ID.
3. Mark it as paid and check its status and payment timestamp.
4. Inspect the payment and prescription messages in Kafka.
5. Repeat the payment request and check that no additional messages are published.
6. Create another bill and cancel it.
7. Confirm that cancelling a paid bill is rejected.

## Scope and Further Improvements

This repository represents one service within a coursework system. The complete hospital application, its user interface, and downstream services are maintained outside this repository.

Potential improvements include:

- Stronger request validation.
- Consistent API error responses.
- Stricter billing status-transition rules.
- Integration tests covering database and Kafka behavior.
- Reliable coordination between database updates and Kafka publication, such as a transactional outbox.

The current implementation does not guarantee atomic database updates and Kafka delivery.
