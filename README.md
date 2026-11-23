# Hospital Billing Service

This service records medicine bills for a hospital system. It exposes a REST API to create a bill, mark it paid, or cancel it, stores each bill in MySQL with Spring Data JPA, and publishes payment and prescription messages to Kafka when a bill is paid.

The pharmacy service that would consume those messages is not in this repository. Nothing in this service charges a card or calls a payment gateway. "Pay" updates the stored bill and emits Kafka messages.

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Java 17 (`java.version` in `pom.xml`) |
| Framework | Spring Boot 3.1.0 |
| HTTP API | Spring Web |
| Persistence | Spring Data JPA, Hibernate, MySQL (`com.mysql.cj.jdbc.Driver`, `MySQL8Dialect`) |
| Messaging | Spring Kafka |
| Build | Maven Wrapper (`mvnw`, Maven 3.9.9) |

The Maven wrapper script in git is not marked executable. On Linux and macOS, run it with `bash ./mvnw`. On Windows, use `mvnw.cmd`.

## What a bill contains

Bills are stored in the `billings` table. Each record has:

- `id` — generated database identity
- `patientId`, `patientName`
- `medicineId`, `medicineName`
- `quantity` (integer)
- `pricePerUnit` and `totalAmount` (`BigDecimal`)
- `status` — `PENDING`, `PAID`, or `CANCELLED`
- `billingDate` — set when the bill is created
- `paymentDate` — set when the bill is marked paid; otherwise null

There is no bean validation on the request. A create call that cannot be saved returns HTTP 500 with an empty body.

### Create

`POST /api/billing` saves a new bill:

- `totalAmount` is `pricePerUnit` multiplied by `quantity`, using `BigDecimal`.
- `status` is `PENDING`.
- `billingDate` is the current local date-time.
- No Kafka message is sent.

The response is HTTP 201 and the saved bill.

### Pay

`POST /api/billing/{id}/pay` takes no body.

- Unknown id: HTTP 404 and an empty body.
- Status is already `PAID`: HTTP 200 and the existing bill. No Kafka messages are sent.
- Any other status, including `CANCELLED`: status becomes `PAID`, `paymentDate` is set to now, the row is saved, then one message is sent to `payment-topic` and one to `prescription-topic`. The response is HTTP 200 and the saved bill.

`paymentStatus` on the payment message is always `true`. The prescription message carries the bill's patient, medicine, and quantity.

`KafkaTemplate.send` is asynchronous. The send calls sit in the same `@Transactional` method as the database update, and this project does not configure a Kafka transaction or an outbox. A successful HTTP response means the bill row was saved. Delivery to the broker is separate.

### Cancel

`POST /api/billing/{id}/cancel` takes no body.

- Unknown id: HTTP 404 and an empty body.
- Status is `PAID`: HTTP 400 and an empty body. The row is left unchanged.
- Any other status, including an already `CANCELLED` bill: status is set to `CANCELLED` and the row is saved. The response is HTTP 200. No Kafka message is sent.

## REST API

Base path: `/api/billing`. The server port is Spring Boot's default, `8080`, unless you set `SERVER_PORT`.

| Method | Path | Success | Other results |
| --- | --- | --- | --- |
| POST | `/api/billing` | 201, created bill | 500 empty body |
| POST | `/api/billing/{id}/pay` | 200, paid bill | 404 unknown id, 500 empty body |
| POST | `/api/billing/{id}/cancel` | 200, cancelled bill | 404 unknown id, 400 if the bill is `PAID`, 500 empty body |
| GET | `/api/billing` | 200, every bill | |
| GET | `/api/billing/{id}` | 200, one bill | 404 if that id does not exist |
| GET | `/api/billing/patient/{patientId}` | 200, bills for that patient (possibly empty) | |
| GET | `/api/billing/status/{status}` | 200, bills with that status | 400 if `status` is not a status name |

`{status}` is matched case-insensitively to `PENDING`, `PAID`, or `CANCELLED`.

### Create a bill

```bash
curl -s -X POST http://localhost:8080/api/billing \
  -H 'Content-Type: application/json' \
  -d '{
    "patientId": "DEMO-P001",
    "patientName": "Demo Patient",
    "medicineId": "DEMO-M001",
    "medicineName": "Demo Medicine",
    "quantity": 2,
    "pricePerUnit": 25.50
  }'
```

Request fields are `patientId`, `patientName`, `medicineId`, `medicineName`, `quantity`, and `pricePerUnit`. With those numbers, `totalAmount` is `51.00` and `status` is `PENDING`. `id`, `billingDate`, and `paymentDate` are produced by the service (`paymentDate` is null until the bill is paid).

### Pay a bill

Use the `id` returned at create time. This call has no request body.

```bash
curl -s -X POST http://localhost:8080/api/billing/1/pay
```

### Cancel a bill

This call has no request body. Cancel before pay if you need the bill to stay unpaid. A bill that is already `PAID` is rejected.

```bash
curl -s -X POST http://localhost:8080/api/billing/1/cancel
```

### Read bills

```bash
curl -s http://localhost:8080/api/billing
curl -s http://localhost:8080/api/billing/1
curl -s http://localhost:8080/api/billing/patient/DEMO-P001
curl -s http://localhost:8080/api/billing/status/PENDING
```

## Kafka

Producer settings:

- Bootstrap servers come from `KAFKA_BOOTSTRAP_SERVERS` (default `kafka:29092`).
- Message keys use `StringSerializer`. This service calls `send` with the topic and value only, so the record key is null.
- Message values use Spring's `JsonSerializer`.

On startup the application declares these topics with 1 partition and replication factor 1:

| Topic property | Default topic name | When a message is sent |
| --- | --- | --- |
| `kafka.topic.payment` | `payment-topic` | A bill is marked paid, and it was not already `PAID` |
| `kafka.topic.prescription` | `prescription-topic` | Same moment as the payment message |

`payment-topic` value:

```json
{
  "patientId": "DEMO-P001",
  "patientName": "Demo Patient",
  "paymentStatus": true
}
```

`prescription-topic` value:

```json
{
  "patientId": "DEMO-P001",
  "medicineId": "DEMO-M001",
  "medicineName": "Demo Medicine",
  "quantity": 2
}
```

If the broker cannot be contacted while topics are declared, that failure is logged. The admin client in this service is left at its default, which does not abort startup solely because topic creation failed. The cluster describe call waits up to 30 seconds, so an unreachable broker can delay startup by that long. Hibernate still needs MySQL at startup because `spring.jpa.hibernate.ddl-auto` is `update`.

## Environment variables

`src/main/resources/application.properties` reads connection settings from the process environment. The application does not load a `.env` file on its own. Export the variables in the shell, or use another mechanism that injects them into the process environment, before you start the JVM.

| Variable | Property | Default when unset |
| --- | --- | --- |
| `DB_URL` | `spring.datasource.url` | `jdbc:mysql://mysql:3306/billingdb` |
| `DB_USERNAME` | `spring.datasource.username` | `billinguser` |
| `DB_PASSWORD` | `spring.datasource.password` | empty |
| `KAFKA_BOOTSTRAP_SERVERS` | `spring.kafka.bootstrap-servers` | `kafka:29092` |

The defaults for the JDBC URL, database user, and Kafka host are the same connection targets the service used before. `DB_PASSWORD` has an empty default. Set it to the password of your own database user.

`.env.example` lists those names and has no values. Copy it to `.env`, which is gitignored, and fill in local values there:

```bash
cp .env.example .env
```

`.env` is for your machine. Do not commit it.

Example of exporting values for a database and broker on the same machine. Replace the password with the one you configured in MySQL:

```bash
export DB_URL='jdbc:mysql://localhost:3306/billingdb'
export DB_USERNAME='billinguser'
export DB_PASSWORD='your-database-password'
export KAFKA_BOOTSTRAP_SERVERS='localhost:9092'
```

`SERVER_PORT` is a normal Spring Boot setting if you need a port other than 8080. It is not referenced by `application.properties`.

The hostnames `mysql` and `kafka` are the built-in defaults. They resolve on a Docker network that uses those service names. A process started directly on your machine needs the `localhost` values above (or whatever host and port your MySQL and Kafka actually use). Kafka's listener port inside a container is often `29092`; a broker published on the host is often `9092`. Use the address your client can reach.

## Running locally

This repository does not include Docker Compose. Run MySQL and Kafka yourself, then start the service.

You need:

- JDK 17
- A MySQL server
- A Kafka broker
- The Maven wrapper (no separate Maven install)

Create the database named by `DB_URL`. With the default URL that name is `billingdb`. Create a MySQL user that matches `DB_USERNAME` and set `DB_PASSWORD` to that user's password. Grant that user access to `billingdb`.

```sql
CREATE DATABASE billingdb;
```

Hibernate creates or updates the `billings` table on startup (`ddl-auto=update`).

Make sure the broker will accept the topics `payment-topic` and `prescription-topic`, or allow the application's admin client to create them (1 partition, replication factor 1).

Load your environment variables, then start the service from the repository root:

```bash
set -a
. ./.env
set +a
bash ./mvnw spring-boot:run
```

Windows (`mvnw.cmd`), after setting the same variables in that shell:

```bat
mvnw.cmd spring-boot:run
```

The API is then at `http://localhost:8080/api/billing` when the port is 8080.

## Tests

```bash
bash ./mvnw test
```

`BillingServiceApplicationTests` loads the application context. The test points at `BillingServiceApplication` and replaces the database with in-memory H2 and the broker with Spring's embedded Kafka. It does not use your `DB_PASSWORD` or a running MySQL or Kafka.

## Project layout

```text
src/main/java/com/hospital/billing/
├── BillingServiceApplication.java
├── config/KafkaConfig.java          # producer, admin client, topic declarations
├── controller/BillingController.java
├── dto/                             # create request and Kafka message bodies
├── kafka/KafkaProducer.java
├── model/                           # Billing entity and BillingStatus
├── repository/BillingRepository.java
└── service/BillingService.java
src/main/resources/application.properties
src/test/java/com/example/billing_service/BillingServiceApplicationTests.java
.env.example
```
