# JSON Dataset Service

A Spring Boot REST API that stores schema-free JSON records in named **datasets** (relational database) and lets clients query any dataset with dynamic **group-by** and **sort-by** operators.

| | |
|---|---|
| **Live API** | https://jsonoperations.onrender.com |
| **Swagger UI** | https://jsonoperations.onrender.com/swagger-ui/index.html |
| **OpenAPI JSON** | https://jsonoperations.onrender.com/v3/api-docs |

> The service runs on a free Render instance, so the first request after a period of inactivity can take 30-60 seconds while it wakes up.

---

## Approach

**What I built.** A layered Spring Boot 4 / Java 17 service with two REST endpoints:

1. **Insert Record** - `POST /api/dataset/{datasetName}/record` stores any JSON object in a dataset. A dataset is created implicitly by its first record.
2. **Query** - `GET /api/dataset/{datasetName}/query` returns the dataset's records, optionally grouped by a field (`groupBy`), sorted by a field (`sortBy`, `order=asc|desc`), or both.

**How it works.**

- **Storage.** One relational table, `dataset_record`, holds `dataset_name`, `record_id` and the whole record as JSON text. This keeps the schema flexible (any fields are allowed) while staying portable across relational databases. A unique constraint on `(dataset_name, record_id)` is the real guard against duplicate ids, so concurrent inserts are safe.
- **Dynamic operators.** Group-by and sort-by field names arrive at request time. Records are loaded in insertion order, validated, then grouped and sorted in the service layer.
- **Design patterns.** Sorting uses the **Strategy** pattern (`AscendingSortStrategy`, `DescendingSortStrategy`, auto-registered by Spring), so a new behaviour is added by writing one `@Component` without touching existing code. `DatasetService` acts as a **facade** over small single-purpose collaborators (`GroupingService`, `SortingService`, `RecordFieldValidator`, `RecordJsonMapper`).
- **Robustness.** Input is validated up front and a bad request never does partial work. All errors are returned as RFC 9457 `application/problem+json` with correct status codes, and unexpected failures never leak internals.
- **Mixed-type safety.** Sorting uses a total-order comparator, so a field holding numbers, strings and booleans never causes a `ClassCastException`.
- **Quality.** Built test-first with JUnit 5: unit tests, a repository test, controller slice tests and end-to-end tests that replay the assignment's examples. API documentation is generated with springdoc-openapi (Swagger UI), and a Postman collection is included.

## Tech stack

| Area | Choice |
|------|--------|
| Language | Java 17 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator) |
| Database | PostgreSQL (Neon) in deployment; in-memory H2 fallback for zero-setup local runs |
| API docs | springdoc-openapi (Swagger UI / OpenAPI 3) |
| Build | Maven (wrapper included) |
| Tests | JUnit 5, Mockito, Spring Test (MockMvc, `@DataJpaTest`) |
| Deployment | Docker on Render |

---

## API reference

Base path: `/api/dataset`. Dataset names are 1-100 characters: letters, digits, `_` or `-`.

### 1. Insert a record

`POST /api/dataset/{datasetName}/record` with `Content-Type: application/json`

```bash
curl -X POST https://jsonoperations.onrender.com/api/dataset/employee_dataset/record \
  -H "Content-Type: application/json" \
  -d '{"id": 1, "name": "John Doe", "age": 30, "department": "Engineering"}'
```

**`201 Created`**

```json
{
  "message": "Record added successfully",
  "dataset": "employee_dataset",
  "recordId": 1
}
```

Rules:

- The body must be a JSON object containing an integer `id`.
- `id` must be unique **within the dataset** (the same `id` may exist in different datasets).
- All other fields are free-form.

### 2. Query a dataset

`GET /api/dataset/{datasetName}/query`

| Parameter | Required | Description |
|-----------|----------|-------------|
| `groupBy` | No | Field to group records by |
| `sortBy`  | No | Field to sort records by |
| `order`   | No | `asc` (default) or `desc`, case-insensitive |

| Parameters supplied | Response property |
|---------------------|-------------------|
| `groupBy` only | `groupedRecords` |
| `sortBy` only | `sortedRecords` |
| `groupBy` + `sortBy` | `groupedRecords`, with records sorted inside each group |
| none | `records` (insertion order) |

**Group by** - `GET /api/dataset/employee_dataset/query?groupBy=department`

```json
{
  "groupedRecords": {
    "Engineering": [
      { "id": 1, "name": "John Doe", "age": 30, "department": "Engineering" },
      { "id": 2, "name": "Jane Smith", "age": 25, "department": "Engineering" }
    ],
    "Marketing": [
      { "id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing" }
    ]
  }
}
```

**Sort by** - `GET /api/dataset/employee_dataset/query?sortBy=age&order=asc`

```json
{
  "sortedRecords": [
    { "id": 2, "name": "Jane Smith", "age": 25, "department": "Engineering" },
    { "id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing" },
    { "id": 1, "name": "John Doe", "age": 30, "department": "Engineering" }
  ]
}
```

**Behaviour details**

- Groups appear in order of first occurrence. Group keys are the text form of the value (`30` becomes `"30"`).
- Records missing the field (or holding `null`) fall into the group `"null"` and are placed **last** when sorting, in both directions.
- Numbers sort numerically and strings lexicographically (case-sensitive). Mixed types never fail: numbers < booleans < strings < other.
- Sorting is stable: records with equal values keep their insertion order.

### Error responses

Errors use RFC 9457 `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Dataset 'foo' does not exist.",
  "instance": "/api/dataset/foo/query"
}
```

| Status | When |
|--------|------|
| `400 Bad Request` | Malformed JSON or body is not an object; missing / non-integer `id`; invalid dataset name; unknown `groupBy` / `sortBy` field; invalid `order` |
| `404 Not Found` | Querying a dataset that has no records |
| `405` / `415` | Unsupported HTTP method / content type |
| `409 Conflict` | `id` already exists in the dataset |
| `500 Internal Server Error` | Unexpected failure (details are logged, never returned) |

---

## API documentation and testing tools

- **Swagger UI** - https://jsonoperations.onrender.com/swagger-ui/index.html. Browse every endpoint, parameter, example and error response, and execute requests from the browser.
- **OpenAPI 3 JSON** - https://jsonoperations.onrender.com/v3/api-docs. Import into Postman, Insomnia or a client generator.
- **Postman collection** - [`docs/json-dataset-service.postman_collection.json`](docs/json-dataset-service.postman_collection.json). Import it, then run the folders in order: *1. Insert*, *2. Query*, *3. Error cases*. Change the `baseUrl` collection variable to `https://jsonoperations.onrender.com` to test the deployed service (default is `http://localhost:8080`).

---

## Run locally

**Prerequisites:** JDK 17 or newer. Maven is provided by the wrapper.

```bash
git clone <your-repo-url>
cd json-dataset-service
./mvnw spring-boot:run            # Windows: mvnw.cmd spring-boot:run
```

With no extra configuration the app starts on `http://localhost:8080` using an **in-memory H2 database** (data is lost on restart). Swagger UI is at `http://localhost:8080/swagger-ui/index.html`.

### Use PostgreSQL locally

Create a local, git-ignored file `src/main/resources/application-postgres.properties` using the template `application-postgres.properties.example`, fill in your database URL and credentials, and run:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Credentials are never committed to the repository. The `dataset_record` table is created automatically on startup.

### Run the tests

```bash
./mvnw test
```

Tests always run against an isolated in-memory H2 database (configured in `src/test/resources/application.properties`), so they never touch a real database.

| Test type | Scope |
|-----------|-------|
| Unit | Grouping and sorting services, sort strategies, value comparator, JSON mapper, `DatasetQuery`, service orchestration (Mockito) |
| `@DataJpaTest` | Repository queries and the unique constraint |
| `@WebMvcTest` | Controller contract: status codes, bodies, validation, error mapping |
| `@SpringBootTest` | End-to-end replay of the assignment examples, plus the OpenAPI / Swagger endpoints |

---

## Deployment

The service is containerised with the included `Dockerfile` (multi-stage build) and deployed to **Render** with a **Neon PostgreSQL** database. Database settings are supplied as environment variables, so no secrets live in the repository:

| Variable | Purpose |
|----------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host>/<database>?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | Database password |

`PORT` is injected by Render and honoured by the application, and forwarded headers are enabled so Swagger UI works correctly behind Render's HTTPS proxy.

---

## Architecture

```
Client -> DatasetController -> DatasetService (facade) -> GroupingService
                                                      -> SortingService -> SortStrategy (Asc / Desc)
                                                      -> RecordFieldValidator
                                                      -> RecordRepository -> PostgreSQL
```

| Layer | Classes | Responsibility |
|-------|---------|----------------|
| controller | `DatasetController` | HTTP mapping, path / query validation, OpenAPI annotations |
| dto | `DatasetQuery`, `SortOrder`, `InsertRecordResponse`, `QueryResponse` | Immutable request / response types (Java records) |
| service | `DatasetService`, `DatasetServiceImpl` | Orchestrates the insert and query use cases |
| | `GroupingService`, `SortingService`, `RecordFieldValidator`, `RecordJsonMapper` | Single-purpose collaborators |
| strategy | `SortStrategy`, `AscendingSortStrategy`, `DescendingSortStrategy`, `FieldValueComparator` | Sort behaviour and type-safe value ordering |
| repository | `RecordRepository` | Spring Data JPA access |
| entity | `DatasetRecord` | Persistence model |
| exception | `GlobalExceptionHandler` and domain exceptions | Maps errors to HTTP problem responses |
| config | `OpenApiConfig` | Swagger / OpenAPI metadata |

## Project structure

```
json-dataset-service
├── docs/                                      Postman collection
├── Dockerfile
├── src/main/java/com/assignment/jsondataset
│   ├── config/  controller/  dto/  entity/
│   └── exception/  repository/  service/  strategy/
├── src/main/resources
│   ├── application.properties
│   └── application-postgres.properties.example
├── src/test/java/...                          unit, slice and integration tests
├── src/test/resources/application.properties  H2 config for tests
└── pom.xml
```

## Design decisions and limitations

- **JSON as text.** Storing the record as JSON text in a single table is portable across relational databases and supports arbitrary fields. Native `jsonb` columns would allow database-side filtering but tie the schema to PostgreSQL.
- **In-memory grouping and sorting.** Operators run in the application after loading the dataset. This is simple and fully dynamic for any field. For very large datasets the work should move into the database (for example PostgreSQL `jsonb` operators) together with pagination.
- **No authentication.** Out of scope for this assignment.
