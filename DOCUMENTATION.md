# Dubai Real Estate — Full Application Documentation

> A production-ready REST backend for Dubai Land Department (DLD) property **sales** and **rent** transaction data, built with **Java 21**, **Spring Boot 3.4.3**, **PostgreSQL**, and **Hexagonal (Ports & Adapters) Architecture**. The application also ships an embedded **MCP (Model Context Protocol) server** powered by Spring AI so that any LLM agent (e.g. Claude Desktop) can query real-estate data through natural-language tool calls.

---

## Table of Contents

1. [Project Origin & Requirements](#1-project-origin--requirements)
2. [Technology Stack](#2-technology-stack)
3. [Architecture — Hexagonal (Ports & Adapters)](#3-architecture--hexagonal-ports--adapters)
4. [Project Structure](#4-project-structure)
5. [Domain Model](#5-domain-model)
6. [Application Layer — Ports & Use Cases](#6-application-layer--ports--use-cases)
   - [Inbound Ports (Use Cases)](#61-inbound-ports-use-cases)
   - [Outbound Ports](#62-outbound-ports)
   - [Application Services](#63-application-services)
7. [Infrastructure Layer](#7-infrastructure-layer)
   - [CSV Parsing Adapter](#71-csv-parsing-adapter)
   - [Persistence Adapter](#72-persistence-adapter)
   - [Web (REST) Adapter](#73-web-rest-adapter)
   - [MCP Tools Adapter](#74-mcp-tools-adapter)
8. [REST API — Complete Reference](#8-rest-api--complete-reference)
   - [Sales Endpoints](#81-sales-endpoints)
   - [Rents Endpoints](#82-rents-endpoints)
9. [MCP Server & Tools](#9-mcp-server--tools)
10. [Database Schema & Migrations](#10-database-schema--migrations)
11. [Duplicate Handling Strategy](#11-duplicate-handling-strategy)
12. [Configuration Reference](#12-configuration-reference)
13. [How to Run](#13-how-to-run)
14. [Postman Collection](#14-postman-collection)
15. [Key Design Decisions & Features Summary](#15-key-design-decisions--features-summary)

---

## 1. Project Origin & Requirements

This backend was built as a technical exercise with the following explicit requirements:

- **CSV Upload & Parsing** — Accept publicly available Dubai DLD sales and rent transaction CSV files, parse them, and persist them into a relational database.
- **Filtered Querying** — Expose paginated query endpoints for both sales and rents, supporting filters by usage, min/max price, and free-text search on area and project name.
- **Average Prices per Area** — Compute and expose average sales transaction value and average annual rent amount, grouped by area.
- **MCP Tools** — Integrate a Model Context Protocol server exposing four tools:
  1. Get available sales areas
  2. Get available rents areas
  3. View last transactions of an area (both sales and rents)
  4. View average sales and rent prices for a given area
- **Hexagonal Architecture** — Strict layering using the Ports & Adapters pattern (as described by Tom Hombergs in *Get Your Hands Dirty on Clean Architecture*).
- **Postman Collection** — A ready-to-use Postman collection for all endpoints.

---

## 2. Technology Stack

| Category | Technology | Version |
|---|---|---|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.4.3 |
| AI / MCP | Spring AI | 1.0.0 |
| Persistence | Spring Data JPA + Hibernate | via Boot BOM |
| Database | PostgreSQL | 14+ recommended |
| DB Migrations | Flyway | via Boot BOM |
| CSV Parsing | OpenCSV | 5.9 |
| Boilerplate Reduction | Lombok | via Boot BOM |
| Env Config | spring-dotenv | 4.0.0 |
| Build Tool | Apache Maven | 3.9+ |
| Serialisation | Jackson (via Spring Web) | via Boot BOM |

**Notable Maven compiler flag:** `-parameters` is set on the compiler plugin so that Spring AI can introspect actual parameter names in `@Tool`-annotated methods at runtime.

---

## 3. Architecture — Hexagonal (Ports & Adapters)

The application strictly follows **Hexagonal Architecture** (also known as Ports & Adapters), which decouples the core business logic from all external concerns (database, HTTP, CSV files, AI tools).

```
┌─────────────────────────────────────────────────────────────────┐
│                         INFRASTRUCTURE                          │
│  ┌──────────┐  ┌──────────────┐  ┌───────────┐  ┌──────────┐  │
│  │  REST    │  │  CSV Parsers │  │  JPA/JDBC │  │  MCP     │  │
│  │Controllers│  │ (OpenCSV)   │  │  Adapters │  │  Tools   │  │
│  └────┬─────┘  └──────┬───────┘  └─────┬─────┘  └────┬─────┘  │
│       │ (calls)       │ (implements)    │ (implements) │        │
│ ══════╪═══════════════╪════════════════╪══════════════╪════════│
│       │            INBOUND PORTS   OUTBOUND PORTS              │
│  ┌────▼─────────────────────────────────────────────────────┐  │
│  │                   APPLICATION CORE                       │  │
│  │       ┌──────────────────────────────────────┐           │  │
│  │       │          APPLICATION SERVICES         │           │  │
│  │       │  SalesTransactionService              │           │  │
│  │       │  RentsTransactionService              │           │  │
│  │       └──────────────┬───────────────────────┘           │  │
│  │                      │ (uses)                             │  │
│  │       ┌──────────────▼───────────────────────┐           │  │
│  │       │           DOMAIN MODEL                │           │  │
│  │       │  SaleTransaction  RentTransaction     │           │  │
│  │       │  AreaAveragePrice                     │           │  │
│  │       └──────────────────────────────────────┘           │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

**Core principle:** The domain and application layers have **zero dependencies** on Spring, JPA, or any infrastructure library. All communication crosses through port interfaces.

---

## 4. Project Structure

```
src/main/java/com/abhay/dubairealestate/
│
├── DubaiRealEstateApplication.java          ← Spring Boot entry point
│
├── domain/
│   └── model/
│       ├── SaleTransaction.java             ← Immutable domain record (sales)
│       ├── RentTransaction.java             ← Immutable domain record (rents)
│       └── AreaAveragePrice.java            ← Value object for avg price result
│
├── application/
│   ├── port/
│   │   ├── in/                              ← INBOUND PORTS (driven by infrastructure)
│   │   │   ├── UploadSalesUseCase.java
│   │   │   ├── UploadRentsUseCase.java
│   │   │   ├── QuerySalesUseCase.java
│   │   │   ├── QueryRentsUseCase.java
│   │   │   ├── QuerySalesAreasUseCase.java
│   │   │   ├── QueryRentsAreasUseCase.java
│   │   │   ├── QuerySalesAveragePricesUseCase.java
│   │   │   ├── QueryRentsAveragePricesUseCase.java
│   │   │   ├── QueryLastSalesTransactionsUseCase.java
│   │   │   ├── QueryLastRentsTransactionsUseCase.java
│   │   │   ├── SalesFilter.java             ← Filter record (sales queries)
│   │   │   └── RentsFilter.java             ← Filter record (rents queries)
│   │   └── out/                             ← OUTBOUND PORTS (drive infrastructure)
│   │       ├── ParseSalesCsvPort.java
│   │       ├── ParseRentsCsvPort.java
│   │       ├── SaveSalesTransactionsPort.java
│   │       ├── SaveRentsTransactionsPort.java
│   │       ├── LoadSalesTransactionsPort.java
│   │       └── LoadRentsTransactionsPort.java
│   └── service/
│       ├── SalesTransactionService.java     ← Implements all sales use cases
│       └── RentsTransactionService.java     ← Implements all rents use cases
│
├── configuration/
│   └── BeanConfiguration.java              ← Registers MCP ToolCallbackProvider
│
└── infrastructure/
    ├── csv/
    │   ├── SalesCsvParser.java              ← Implements ParseSalesCsvPort
    │   ├── RentsCsvParser.java              ← Implements ParseRentsCsvPort
    │   ├── CsvUtils.java                    ← Shared CSV helper utilities
    │   └── CsvParseException.java           ← Unchecked CSV parse error
    ├── mcp/
    │   └── RealEstateMcpTools.java          ← @Tool-annotated MCP methods
    ├── persistence/
    │   ├── adapter/
    │   │   ├── SalesPersistenceAdapter.java ← Implements Save + Load sales ports
    │   │   └── RentsPersistenceAdapter.java ← Implements Save + Load rents ports
    │   ├── entity/
    │   │   ├── SaleTransactionEntity.java   ← JPA entity for sale_transactions
    │   │   └── RentTransactionEntity.java   ← JPA entity for rent_transactions
    │   ├── projection/
    │   │   └── AreaAveragePriceProjection.java ← JPA interface projection
    │   ├── repository/
    │   │   ├── SaleTransactionJpaRepository.java
    │   │   └── RentTransactionJpaRepository.java
    │   └── specification/
    │       ├── SaleTransactionSpecification.java ← JPA Criteria API dynamic filters
    │       └── RentTransactionSpecification.java
    └── web/
        ├── controller/
        │   ├── SalesController.java
        │   ├── RentsController.java
        │   └── GlobalExceptionHandler.java
        └── dto/
            ├── SaleTransactionResponse.java
            ├── RentTransactionResponse.java
            ├── AreaAveragePriceResponse.java
            └── UploadResponse.java

src/main/resources/
├── application.properties
└── db/migration/
    ├── V1__create_tables.sql
    ├── V2__add_unique_constraints.sql
    └── V3__fix_unique_constraints_nulls_not_distinct.sql
```

---

## 5. Domain Model

All domain objects are **immutable Java records** — no framework annotations, no dependencies on Spring or JPA.

### `SaleTransaction`
Represents a single DLD property sale transaction.

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Database-assigned identifier |
| `transId` | `String` | DLD transaction reference number |
| `transDate` | `LocalDate` | Date of transaction |
| `transValue` | `BigDecimal` | Total sale price (AED) |
| `areaName` | `String` | Dubai area name |
| `projectName` | `String` | Development / project name |
| `usage` | `String` | Property usage (e.g. Residential, Commercial) |
| `registrationType` | `String` | DLD registration type |
| `propertyType` | `String` | Property type (e.g. Apartment, Villa) |
| `rooms` | `String` | Room configuration |
| `actualArea` | `BigDecimal` | Built-up area in sq. metres |
| `meterSalePrice` | `BigDecimal` | Price per sq. metre (computed if absent) |

### `RentTransaction`
Represents a single DLD rent/lease contract.

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Database-assigned identifier |
| `contractId` | `String` | DLD contract reference number |
| `contractDate` | `LocalDate` | Contract registration date |
| `annualAmount` | `BigDecimal` | Annual rent amount (AED) |
| `startDate` | `LocalDate` | Lease start date |
| `endDate` | `LocalDate` | Lease end date |
| `areaName` | `String` | Dubai area name |
| `projectName` | `String` | Development / project name |
| `usage` | `String` | Property usage |
| `propertyType` | `String` | Property type |
| `rooms` | `String` | Room configuration |
| `actualArea` | `BigDecimal` | Built-up area in sq. metres |

### `AreaAveragePrice`
A lightweight value object returned by average-price queries.

| Field | Type | Description |
|---|---|---|
| `areaName` | `String` | Dubai area name |
| `averagePrice` | `BigDecimal` | Average transaction value or annual rent |
| `transactionCount` | `long` | Number of transactions contributing to the average |

---

## 6. Application Layer — Ports & Use Cases

### 6.1 Inbound Ports (Use Cases)

These interfaces define **what the application can do**. They are called by the web layer, MCP tools, or any other driving adapter.

| Interface | Method(s) | Description |
|---|---|---|
| `UploadSalesUseCase` | `uploadSales(InputStream)` → `int` | Parse & persist a sales CSV; returns record count |
| `UploadRentsUseCase` | `uploadRents(InputStream)` → `int` | Parse & persist a rents CSV; returns record count |
| `QuerySalesUseCase` | `querySales(SalesFilter, Pageable)` → `Page<SaleTransaction>` | Paginated filtered sales query |
| `QueryRentsUseCase` | `queryRents(RentsFilter, Pageable)` → `Page<RentTransaction>` | Paginated filtered rents query |
| `QuerySalesAreasUseCase` | `getSalesAreas()` → `List<String>` | All distinct area names with sales data |
| `QueryRentsAreasUseCase` | `getRentsAreas()` → `List<String>` | All distinct area names with rents data |
| `QuerySalesAveragePricesUseCase` | `getAverageSalesPrices()`, `getAverageSalesPriceByArea(String)` | Sales averages (all areas or one) |
| `QueryRentsAveragePricesUseCase` | `getAverageRentsPrices()`, `getAverageRentsPriceByArea(String)` | Rent averages (all areas or one) |
| `QueryLastSalesTransactionsUseCase` | `getLastSalesByArea(String, int)` | Latest N sales for an area |
| `QueryLastRentsTransactionsUseCase` | `getLastRentsByArea(String, int)` | Latest N rents for an area |

#### Filter Records

**`SalesFilter`**
```
usage         — exact match on usage type (case-insensitive)
minPrice      — minimum trans_value (BigDecimal)
maxPrice      — maximum trans_value (BigDecimal)
areaSearch    — LIKE wildcard search on area_name
projectSearch — LIKE wildcard search on project_name
```

**`RentsFilter`**
```
usage         — exact match on usage type (case-insensitive)
minPrice      — minimum annual_amount (BigDecimal)
maxPrice      — maximum annual_amount (BigDecimal)
areaSearch    — LIKE wildcard search on area_name
projectSearch — LIKE wildcard search on project_name
```

### 6.2 Outbound Ports

These interfaces define **what the application needs** from the outside world. The application core depends only on these abstractions.

| Interface | Implemented by |
|---|---|
| `ParseSalesCsvPort` | `SalesCsvParser` |
| `ParseRentsCsvPort` | `RentsCsvParser` |
| `SaveSalesTransactionsPort` | `SalesPersistenceAdapter` |
| `SaveRentsTransactionsPort` | `RentsPersistenceAdapter` |
| `LoadSalesTransactionsPort` | `SalesPersistenceAdapter` |
| `LoadRentsTransactionsPort` | `RentsPersistenceAdapter` |

### 6.3 Application Services

**`SalesTransactionService`** implements:
`UploadSalesUseCase`, `QuerySalesUseCase`, `QuerySalesAreasUseCase`, `QuerySalesAveragePricesUseCase`, `QueryLastSalesTransactionsUseCase`

**`RentsTransactionService`** implements:
`UploadRentsUseCase`, `QueryRentsUseCase`, `QueryRentsAreasUseCase`, `QueryRentsAveragePricesUseCase`, `QueryLastRentsTransactionsUseCase`

Both services are annotated with `@Transactional` on write operations, and `@Transactional(readOnly = true)` on read operations, ensuring proper connection management and potential query plan optimisation.

---

## 7. Infrastructure Layer

### 7.1 CSV Parsing Adapter

The CSV parsers implement the outbound `ParseSalesCsvPort` / `ParseRentsCsvPort` interfaces using **OpenCSV** with `CSVReaderHeaderAware` (header-based column mapping, case-insensitive after normalisation).

#### `CsvUtils` (shared helpers)
- `normaliseKeys(row)` — trims and upper-cases all header keys for robust matching
- `getString(row, ...keys)` — returns first non-blank value matched by any of the given key aliases
- `parseBigDecimal(value)` — strips commas and parses; returns `null` on blank/invalid
- `parseDate(value, formatters)` — tries multiple `DateTimeFormatter` patterns:  
  `yyyy-MM-dd`, `dd/MM/yyyy`, `MM/dd/yyyy`, `d/M/yyyy`, `yyyy/MM/dd`

#### Column Alias Mapping

**Sales CSV supported headers:**

| Domain Field | Accepted CSV Columns |
|---|---|
| `transId` | `TRANS_ID`, `INSTANCE_ID`, `TRANSACTION_NUMBER` |
| `transDate` | `INSTANCE_DATE`, `TRANS_DATE` |
| `transValue` | `TRANS_VALUE` |
| `areaName` | `AREA_EN`, `AREA_NAME_EN`, `AREA_NAME` |
| `projectName` | `PROJECT_EN`, `PROJECT_NAME` |
| `usage` | `USAGE_EN`, `USAGE`, `GROUP_EN` |
| `registrationType` | `REG_TYPE_EN`, `REGISTRATION_TYPE`, `PROCEDURE_EN` |
| `propertyType` | `PROP_TYPE_EN`, `PROPERTY_TYPE` |
| `rooms` | `ROOMS_EN`, `ROOMS` |
| `actualArea` | `ACTUAL_AREA`, `AREA` |
| `meterSalePrice` | `METER_SALE_PRICE`, `PRICE_PER_SQM` (computed if absent) |

> If `meterSalePrice` is absent in the CSV, it is **automatically computed** as `transValue / actualArea` (rounded HALF_UP to 2 decimal places).

**Rents CSV supported headers:**

| Domain Field | Accepted CSV Columns |
|---|---|
| `contractId` | `CONTRACT_NUM`, `CONTRACT_ID` |
| `contractDate` | `INSTANCE_DATE`, `CONTRACT_DATE`, `REGISTRATION_DATE` |
| `annualAmount` | `ANNUAL_AMOUNT` |
| `startDate` | `START_DATE` |
| `endDate` | `END_DATE` |
| `areaName` | `AREA_EN`, `AREA_NAME_EN`, `AREA_NAME` |
| `projectName` | `PROJECT_EN`, `PROJECT_NAME` |
| `usage` | `USAGE_EN`, `USAGE` |
| `propertyType` | `PROP_TYPE_EN`, `PROPERTY_TYPE` |
| `rooms` | `ROOMS_EN`, `ROOMS` |
| `actualArea` | `ACTUAL_AREA`, `AREA` |

**Resilience:** Individual rows that fail to parse are **skipped with a WARN log**; the import continues processing the remaining rows. Only a catastrophic file-level failure throws `CsvParseException` (HTTP 422).

### 7.2 Persistence Adapter

Both `SalesPersistenceAdapter` and `RentsPersistenceAdapter` implement their respective Save and Load outbound ports.

#### Write Path — Batched Upsert
- Uses raw `JdbcTemplate.batchUpdate()` with chunked batches of **500 records** at a time for high throughput.
- SQL uses `INSERT ... ON CONFLICT ON CONSTRAINT <unique_constraint> DO NOTHING` — duplicate records are silently ignored (idempotent imports).
- The `reWriteBatchedInserts=true` JDBC URL parameter enables PostgreSQL wire-level batch rewriting for maximum insert performance.

#### Read Path — JPA + Specifications
- Paginated filtered queries use **JPA Criteria API Specifications** (`SaleTransactionSpecification`, `RentTransactionSpecification`) for composable, type-safe dynamic predicates.
- Average price aggregations are handled by custom **JPQL `@Query`** methods using `AVG()` and `COUNT()` with `GROUP BY`.
- "Last N transactions" queries use Spring Data's `Pageable` with `PageRequest.of(0, limit)` and `ORDER BY date DESC`.
- An **`AreaAveragePriceProjection`** interface projection is used to extract aggregation results without loading full entities.

#### Entity → Domain Mapping
Each adapter has a private `toDomain()` mapper that converts JPA entities to pure domain records, keeping the domain layer free of JPA annotations.

### 7.3 Web (REST) Adapter

Controllers depend only on use-case interfaces (inbound ports), never on services directly.

**Response formats:**
- `SaleTransactionResponse` and `RentTransactionResponse` use `@JsonInclude(NON_NULL)` to omit null fields in the JSON output.
- Pagination is serialised via Spring Data's DTO mode (`PageSerializationMode.VIA_DTO`), providing a clean JSON structure without HAL/hypermedia overhead.
- Both query endpoints default to **page 0, size 20**, sorted by `transDate` / `contractDate` descending.

**Global Exception Handling (`GlobalExceptionHandler`):**

| Exception | HTTP Status | Description |
|---|---|---|
| `CsvParseException` | `422 Unprocessable Entity` | CSV file could not be parsed at all |
| `IOException` | `400 Bad Request` | Problem reading uploaded file stream |
| `MaxUploadSizeExceededException` | `413 Payload Too Large` | File exceeds 100 MB limit |
| `IllegalArgumentException` | `400 Bad Request` | Invalid request parameter |

All error responses have the shape: `{ "error": "<type>", "detail": "<message>" }`.

### 7.4 MCP Tools Adapter

`RealEstateMcpTools` is registered as a Spring `@Component` and wired into Spring AI's `MethodToolCallbackProvider` via `BeanConfiguration`. The `-parameters` Java compiler flag ensures parameter names are preserved at runtime so Spring AI can auto-document tool signatures.

The MCP server is exposed at `/sse` (Server-Sent Events transport) on the same HTTP port as the REST API.

---

## 8. REST API — Complete Reference

### 8.1 Sales Endpoints

#### `POST /api/sales/upload`
Upload a Dubai DLD sales transactions CSV file.

- **Content-Type:** `multipart/form-data`
- **Form field:** `file` (the CSV file)
- **Max file size:** 100 MB

**Response `200 OK`:**
```json
{
  "recordsImported": 15842,
  "message": "Sales CSV processed successfully. New records saved; duplicates automatically skipped."
}
```

---

#### `GET /api/sales`
Query sales transactions with optional filters. Results are paginated and sorted newest-first.

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `usage` | `string` | No | — | Filter by usage type (case-insensitive exact match) |
| `minPrice` | `decimal` | No | — | Minimum `trans_value` (AED) |
| `maxPrice` | `decimal` | No | — | Maximum `trans_value` (AED) |
| `areaSearch` | `string` | No | — | Free-text LIKE search on `area_name` |
| `projectSearch` | `string` | No | — | Free-text LIKE search on `project_name` |
| `page` | `int` | No | `0` | Page number (0-based) |
| `size` | `int` | No | `20` | Page size |

**Response `200 OK`:** Paginated `Page<SaleTransactionResponse>`
```json
{
  "content": [
    {
      "id": 1,
      "transId": "2024-ABC-001",
      "transDate": "2024-11-15",
      "transValue": 2500000.00,
      "areaName": "DUBAI MARINA",
      "projectName": "MARINA RESIDENCES",
      "usage": "Residential",
      "registrationType": "Sales",
      "propertyType": "Apartment",
      "rooms": "2 B/R",
      "actualArea": 102.50,
      "meterSalePrice": 24390.24
    }
  ],
  "totalElements": 15842,
  "totalPages": 793,
  "size": 20,
  "number": 0
}
```

---

#### `GET /api/sales/averages`
Get average sales transaction value per area across all areas.

**Response `200 OK`:** `List<AreaAveragePriceResponse>`
```json
[
  { "areaName": "BUSINESS BAY", "averagePrice": 1850000.50, "transactionCount": 4210 },
  { "areaName": "DOWNTOWN DUBAI", "averagePrice": 3200000.00, "transactionCount": 2105 }
]
```

---

#### `GET /api/sales/averages/{area}`
Get average sales price for a specific area name.

**Path Variable:** `area` — the area name (case-insensitive)

**Response `200 OK`:**
```json
{ "areaName": "DOWNTOWN DUBAI", "averagePrice": 3200000.00, "transactionCount": 2105 }
```

**Response `404 Not Found`:** if no sales data exists for the area.

---

### 8.2 Rents Endpoints

#### `POST /api/rents/upload`
Upload a Dubai DLD rent transactions CSV file.

- **Content-Type:** `multipart/form-data`
- **Form field:** `file`
- **Max file size:** 100 MB

**Response `200 OK`:**
```json
{
  "recordsImported": 9315,
  "message": "Rents CSV processed successfully. New records saved; duplicates automatically skipped."
}
```

---

#### `GET /api/rents`
Query rent transactions with optional filters. Results are paginated and sorted newest-first by `contractDate`.

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `usage` | `string` | No | — | Filter by usage type (case-insensitive exact match) |
| `minPrice` | `decimal` | No | — | Minimum `annual_amount` (AED) |
| `maxPrice` | `decimal` | No | — | Maximum `annual_amount` (AED) |
| `areaSearch` | `string` | No | — | Free-text LIKE search on `area_name` |
| `projectSearch` | `string` | No | — | Free-text LIKE search on `project_name` |
| `page` | `int` | No | `0` | Page number (0-based) |
| `size` | `int` | No | `20` | Page size |

**Response `200 OK`:** Paginated `Page<RentTransactionResponse>`
```json
{
  "content": [
    {
      "id": 1,
      "contractId": "2024-RENT-001",
      "contractDate": "2024-11-01",
      "annualAmount": 120000.00,
      "startDate": "2024-11-01",
      "endDate": "2025-10-31",
      "areaName": "JUMEIRAH VILLAGE CIRCLE",
      "projectName": "DIAMOND VIEWS",
      "usage": "Residential",
      "propertyType": "Apartment",
      "rooms": "1 B/R",
      "actualArea": 75.00
    }
  ],
  "totalElements": 9315,
  "totalPages": 466,
  "size": 20,
  "number": 0
}
```

---

#### `GET /api/rents/averages`
Get average annual rent per area across all areas.

**Response `200 OK`:** `List<AreaAveragePriceResponse>`

---

#### `GET /api/rents/averages/{area}`
Get average annual rent for a specific area.

**Response `200 OK`:** `AreaAveragePriceResponse`  
**Response `404 Not Found`:** if no rent data exists for the area.

---

## 9. MCP Server & Tools

The application runs an embedded **Model Context Protocol (MCP) server** using Spring AI's `spring-ai-starter-mcp-server-webmvc`. This allows any MCP-compatible LLM client to call the backend as a tools provider.

**Server Config:**
```
Name:    dubai-real-estate-mcp
Version: 1.0.0
SSE URL: http://localhost:8080/sse
```

### Available Tools

#### `getSalesAreas`
> Get the list of all distinct area names that have sales transactions in the Dubai DLD database.

- **Parameters:** none
- **Returns:** `List<String>` — sorted list of area names

---

#### `getRentsAreas`
> Get the list of all distinct area names that have rent transactions in the Dubai DLD database.

- **Parameters:** none
- **Returns:** `List<String>` — sorted list of area names

---

#### `getLastTransactionsByArea`
> Get the most recent property transactions (both sales and rents) for a specific area. Returns up to 10 of the latest sales and up to 10 of the latest rent transactions.

- **Parameters:** `areaName` (`String`) — the Dubai area name
- **Returns:**
```json
{
  "area": "DUBAI MARINA",
  "lastSales": [
    {
      "id": 100,
      "transDate": "2024-11-15",
      "transValue": "2500000.00",
      "areaName": "DUBAI MARINA",
      "projectName": "MARINA RESIDENCES",
      "propertyType": "Apartment",
      "rooms": "2 B/R"
    }
  ],
  "lastRents": [ ... ]
}
```

---

#### `getAreaAveragePrices`
> Get the average sales price and average annual rent price for a given area. Returns average trans_value (sales) and average annual_amount (rents) along with transaction counts.

- **Parameters:** `areaName` (`String`) — the Dubai area name
- **Returns:**
```json
{
  "area": "DUBAI MARINA",
  "averageSalesPrice": 2850000.00,
  "salesTransactionCount": 3412,
  "averageAnnualRent": 115000.00,
  "rentTransactionCount": 1987
}
```

> Returns `0` (not an error) for averages when no data exists for the requested area in either category.

### Connecting an MCP Client
Point your MCP client (e.g. Claude Desktop, or any SSE-compatible client) to:
```
http://localhost:8080/sse
```

---

## 10. Database Schema & Migrations

Flyway manages schema evolution automatically on startup. Migrations are located in `src/main/resources/db/migration/`.

### V1 — Initial Schema

Creates both tables and all performance indexes.

**`sale_transactions`**

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` `PRIMARY KEY` | Auto-increment |
| `trans_id` | `VARCHAR(100)` | DLD transaction ID |
| `trans_date` | `DATE` | Transaction date |
| `trans_value` | `NUMERIC(20,2)` | Sale price in AED |
| `area_name` | `VARCHAR(200)` | Dubai area |
| `project_name` | `VARCHAR(200)` | Project name |
| `usage` | `VARCHAR(100)` | Residential / Commercial etc. |
| `registration_type` | `VARCHAR(100)` | DLD registration type |
| `property_type` | `VARCHAR(100)` | Apartment / Villa etc. |
| `rooms` | `VARCHAR(50)` | Room configuration |
| `actual_area` | `NUMERIC(15,2)` | Area in sq.m |
| `meter_sale_price` | `NUMERIC(20,2)` | Price per sq.m |
| `created_at` | `TIMESTAMP NOT NULL DEFAULT NOW()` | Audit timestamp |

**`rent_transactions`**

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` `PRIMARY KEY` | Auto-increment |
| `contract_id` | `VARCHAR(100)` | DLD contract ID |
| `contract_date` | `DATE` | Registration date |
| `annual_amount` | `NUMERIC(20,2)` | Annual rent in AED |
| `start_date` | `DATE` | Lease start |
| `end_date` | `DATE` | Lease end |
| `area_name` | `VARCHAR(200)` | Dubai area |
| `project_name` | `VARCHAR(200)` | Project name |
| `usage` | `VARCHAR(100)` | Property usage |
| `property_type` | `VARCHAR(100)` | Property type |
| `rooms` | `VARCHAR(50)` | Room configuration |
| `actual_area` | `NUMERIC(15,2)` | Area in sq.m |
| `created_at` | `TIMESTAMP NOT NULL DEFAULT NOW()` | Audit timestamp |

**Indexes created in V1:**
- B-tree on `area_name`, `trans_date`/`contract_date`, `usage`, `project_name` (for filtered queries)
- **GIN trigram indexes** (`pg_trgm` extension) on `area_name` and `project_name` of both tables — enables fast `ILIKE %keyword%` free-text search

### V2 — Unique Constraints

Adds composite unique constraints to prevent duplicate row insertion on re-import.

- `sale_transactions`: `UNIQUE (trans_id, actual_area)` — `uq_sale_trans_id_area`
- `rent_transactions`: `UNIQUE (contract_date, start_date, end_date, area_name, actual_area, annual_amount, project_name)` — `uq_rent_dedup`

### V3 — NULLS NOT DISTINCT

Recreates the V2 constraints with `NULLS NOT DISTINCT` (PostgreSQL 15+ feature), ensuring that rows containing `NULL` values in any of the constraint columns are still treated as duplicates of each other — preventing spurious duplicate rows when source data is incomplete.

---

## 11. Duplicate Handling Strategy

Duplicate detection is a first-class concern. The same CSV can be uploaded multiple times safely:

1. **Database level:** Unique constraints (`NULLS NOT DISTINCT`) on both tables.
2. **SQL level:** `INSERT ... ON CONFLICT ON CONSTRAINT ... DO NOTHING` — duplicates are silently skipped, not errored.
3. **Batch level:** Inserts are chunked into 500-record batches using `JdbcTemplate.batchUpdate()` for throughput.
4. **JDBC level:** `reWriteBatchedInserts=true` in the connection URL enables PostgreSQL wire-protocol batch rewriting.
5. **Response level:** The upload response returns only the count of **rows read from CSV**, with a message clarifying that duplicates are skipped.

---

## 12. Configuration Reference

All configuration is in `src/main/resources/application.properties`. Sensitive values are externalised via environment variables (with defaults for local development). The `spring-dotenv` library also supports a `.env` file for local development.

| Property | Default | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/dubai_real_estate?reWriteBatchedInserts=true` | PostgreSQL JDBC URL |
| `spring.datasource.username` | `postgres` | DB username (`DB_USERNAME` env var) |
| `spring.datasource.password` | `postgres` | DB password (`DB_PASSWORD` env var) |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate DDL — schema managed by Flyway only |
| `spring.jpa.show-sql` | `false` | SQL logging (set `true` for debugging) |
| `spring.flyway.enabled` | `true` | Flyway auto-migration on startup |
| `spring.servlet.multipart.max-file-size` | `100MB` | Max single file upload size |
| `spring.servlet.multipart.max-request-size` | `100MB` | Max total multipart request size |
| `spring.ai.mcp.server.enabled` | `true` | Enable the MCP server |
| `spring.ai.mcp.server.name` | `dubai-real-estate-mcp` | MCP server identifier |
| `spring.ai.mcp.server.version` | `1.0.0` | MCP server version |
| `server.port` | `8080` | HTTP port (`SERVER_PORT` env var) |
| `logging.level.com.abhay.dubairealestate` | `DEBUG` | Application log level |

---

## 13. How to Run

### Prerequisites

| Tool | Version |
|---|---|
| Java JDK | 21+ |
| Apache Maven | 3.9+ |
| PostgreSQL | 14+ (15+ recommended for `NULLS NOT DISTINCT`) |

### Steps

**1. Create the PostgreSQL database:**
```sql
CREATE DATABASE dubai_real_estate;
```

**2. Set environment variables** (or create a `.env` file in the project root):
```bash
DB_URL=jdbc:postgresql://localhost:5432/dubai_real_estate?reWriteBatchedInserts=true
DB_USERNAME=postgres
DB_PASSWORD=yourpassword
SERVER_PORT=8080
```

**3. Build and run:**
```bash
mvn spring-boot:run
```

Flyway applies all migrations (V1 → V3) automatically on startup. The application is ready at `http://localhost:8080`.

**4. Upload the CSV data:**
```bash
# Upload sales transactions
curl -X POST http://localhost:8080/api/sales/upload \
  -F "file=@transactions-2026-02-19.csv"

# Upload rent transactions
curl -X POST http://localhost:8080/api/rents/upload \
  -F "file=@rents-2026-02-19.csv"
```

---

## 14. Postman Collection

A full Postman collection is included in the repository root: `Dubai_Real_Estate.postman_collection.json`

It covers all 8 REST endpoints with pre-configured request bodies, form-data for file uploads, and example query parameters.

**Import:** In Postman, go to **File → Import** and select the `.json` file.

---

## 15. Key Design Decisions & Features Summary

| # | Feature / Decision | Detail |
|---|---|---|
| 1 | **Hexagonal Architecture** | Strict Ports & Adapters — domain and application core have zero infrastructure dependencies |
| 2 | **Java Records for Domain** | `SaleTransaction`, `RentTransaction`, `AreaAveragePrice` are immutable records — clean, concise, thread-safe |
| 3 | **10 Granular Use Case Interfaces** | Each capability is its own port interface, enabling precise dependency injection and testability |
| 4 | **Dual-adapter pattern** | Single persistence adapter class per domain entity implements both the Save and Load outbound ports |
| 5 | **JdbcTemplate for Writes** | Bypasses JPA for batch inserts — uses raw SQL with `ON CONFLICT DO NOTHING` for idempotent upserts |
| 6 | **JPA Specifications for Reads** | Composable, type-safe dynamic query building for filtered paginated reads |
| 7 | **Flyway DB Migrations** | Full schema lifecycle management across 3 migration versions |
| 8 | **GIN Trigram Indexes** | `pg_trgm` extension + GIN indexes on `area_name` and `project_name` enable fast `ILIKE` free-text search |
| 9 | **NULLS NOT DISTINCT constraints** | PostgreSQL 15+ feature used in V3 migration — makes unique constraints treat NULLs as equal to prevent duplicate rows with null fields |
| 10 | **CSV Column Aliasing** | Multiple accepted column header variants per field, normalised to uppercase — handles DLD format variations without code changes |
| 11 | **Auto-computed `meterSalePrice`** | Computed as `transValue / actualArea` when absent from the CSV source |
| 12 | **Resilient CSV row skipping** | Individual unparseable rows are WARN-logged and skipped; the import continues |
| 13 | **Spring AI MCP Integration** | 4 `@Tool`-annotated methods expose the backend as an AI-callable tool server over SSE |
| 14 | **`-parameters` compiler flag** | Preserves parameter names at runtime for Spring AI `@Tool` method introspection |
| 15 | **Batch size 500 + reWriteBatchedInserts** | Optimal throughput for large CSV imports into PostgreSQL |
| 16 | **spring-dotenv support** | Local `.env` file support for developer experience without modifying properties files |
| 17 | **Page serialisation via DTO** | `@EnableSpringDataWebSupport(PageSerializationMode.VIA_DTO)` — clean JSON pagination without HAL/HATEOAS overhead |
| 18 | **Global exception handler** | Unified `@RestControllerAdvice` maps all known exceptions to appropriate HTTP status codes with structured error bodies |
| 19 | **`@JsonInclude(NON_NULL)`** on responses | Null fields are omitted from JSON output — cleaner API responses |
| 20 | **`open-in-view=false`** | Disabled OSIV — no lazy loading outside transaction boundaries; predictable performance |
