# Dubai Real Estate API

REST backend for Dubai DLD property sales and rent transactions, built with **Spring Boot 3**, **Java 21**, **PostgreSQL**, and **Hexagonal Architecture**. Includes an embedded **MCP server** (Spring AI) so an LLM agent can query the data directly.

---

## Architecture

```
com.abhay.dubairealestate
├── domain/          ← Pure Java models (SaleTransaction, RentTransaction, AreaAveragePrice)
├── application/
│   ├── port/in/     ← Use-case interfaces (inbound ports)
│   ├── port/out/    ← Repository & CSV-parse interfaces (outbound ports)
│   └── service/     ← Application services implementing the inbound ports
└── infrastructure/
    ├── persistence/ ← JPA entities, Spring Data repos, Specifications, Adapters
    ├── web/         ← REST controllers + response DTOs
    ├── csv/         ← OpenCSV-based parsers (implement outbound CSV ports)
    └── mcp/         ← Spring AI @Tool methods (MCP server tools)
```

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.9+ |
| PostgreSQL | 14+ |

---

## Quick Start

### 1. Create the database

```sql
CREATE DATABASE dubai_real_estate;
```

### 2. Configure credentials

Set environment variables (or edit `application.yml`):

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=yourpassword
```

### 3. Run

```bash
mvn spring-boot:run
```

Flyway will automatically apply the schema migration on startup.

---

## REST Endpoints

### Sales

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/sales/upload` | Upload DLD sales CSV (`multipart/form-data`, field: `file`) |
| `GET` | `/api/sales` | Query transactions (filters below) |
| `GET` | `/api/sales/averages` | Average price per area |
| `GET` | `/api/sales/averages/{area}` | Average price for a specific area |

**GET /api/sales query params:**

| Param | Type | Description |
|-------|------|-------------|
| `usage` | string | Filter by usage (e.g. `Residential`) |
| `minPrice` | decimal | Minimum `trans_value` |
| `maxPrice` | decimal | Maximum `trans_value` |
| `areaSearch` | string | Free-text search on area name |
| `projectSearch` | string | Free-text search on project name |
| `page` | int | Page number (0-based, default `0`) |
| `size` | int | Page size (default `20`) |

---

### Rents

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/rents/upload` | Upload DLD rents CSV (`multipart/form-data`, field: `file`) |
| `GET` | `/api/rents` | Query transactions (filters below) |
| `GET` | `/api/rents/averages` | Average annual rent per area |
| `GET` | `/api/rents/averages/{area}` | Average annual rent for a specific area |

**GET /api/rents query params:** same structure as sales, but `minPrice`/`maxPrice` filter on `annual_amount`.

---

## MCP Tools

The server exposes four MCP tools at `/sse` (Server-Sent Events transport):

| Tool | Description |
|------|-------------|
| `getSalesAreas` | List all distinct area names with sales transactions |
| `getRentsAreas` | List all distinct area names with rent transactions |
| `getLastTransactionsByArea` | Last 10 sales + last 10 rents for a given area |
| `getAreaAveragePrices` | Average sales price & average annual rent for a given area |

To connect an MCP client (e.g. Claude Desktop), point it to:
```
http://localhost:8080/sse
```

---

## CSV Column Mapping

The parsers accept the standard Dubai DLD column naming (case-insensitive). Key columns:

| Domain field | Accepted CSV headers |
|---|---|
| `transValue` | `TRANS_VALUE` |
| `annualAmount` | `ANNUAL_AMOUNT` |
| `areaName` | `AREA_EN`, `AREA_NAME_EN`, `AREA_NAME` |
| `projectName` | `PROJECT_EN`, `PROJECT_NAME` |
| `usage` | `USAGE_EN`, `USAGE`, `GROUP_EN` |

Rows that cannot be parsed are **skipped with a warning** — the import continues.

---

## Running Tests

```bash
mvn test
```
