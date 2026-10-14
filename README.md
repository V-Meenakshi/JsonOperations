# JSON Dataset Service

A Spring Boot REST service that stores JSON records in named datasets (relational database) and lets you
query a dataset with **group-by** and **sort-by** operators.

* Java 17, Spring Boot 4.1.1, Spring Data JPA, Jackson 3
* Database: **H2 in-memory by default** (zero setup) or **PostgreSQL** via a profile
* Interactive API docs with **Swagger UI** (springdoc-openapi); also testable with Postman / curl (collection in `docs/`)

---

## Run it

**Prerequisites:** JDK 17+ (Maven is provided by the wrapper).

```bash
git clone <your-repo-url>
cd json-dataset-service
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

The API is then available at `http://localhost:8080`.

| What | URL |
|------|-----|
| **Swagger UI** (try the API in the browser) | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| H2 console | http://localhost:8080/h2-console |

H2 console: `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:datasetdb`, user `sa`, empty password). Data is lost on restart with H2.

### With PostgreSQL

```bash
docker run -d --name datasetdb -p 5432:5432 -e POSTGRES_DB=datasetdb -e POSTGRES_PASSWORD=postgres postgres:16
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Override the connection with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` environment variables.

### Run the tests

```bash
./mvnw test
```

Unit tests (services, strategies, DTOs), a `@DataJpaTest` repository test, `@WebMvcTest` controller tests and an
end-to-end `@SpringBootTest` that replays the examples from the assignment.

---

## API

The same contract is browsable and executable in Swagger UI at `/swagger-ui.html`.

Base path: `/api/dataset`. Dataset names: 1-100 characters, letters/digits/`_`/`-`. A dataset is created
implicitly by its first record.

### 1. Insert a record

`POST /api/dataset/{datasetName}/record` - `Content-Type: application/json`

```bash
curl -X POST http://localhost:8080/api/dataset/employee_dataset/record \
  -H "Content-Type: application/json" \
  -d '{"id":1,"name":"John Doe","age":30,"department":"Engineering"}'
```

**201 Created**
```json
{ "message": "Record added successfully", "dataset": "employee_dataset", "recordId": 1 }
```

Rules: the body must be a JSON object with an integer `id`; `id` must be unique **within the dataset**
(the same id may exist in different datasets). All other fields are free-form.

### 2. Query a dataset

`GET /api/dataset/{datasetName}/query`

| Parameter | Required | Description |
|-----------|----------|-------------|
| `groupBy` | no | Field to group records by |
| `sortBy`  | no | Field to sort by |
| `order`   | no | `asc` (default) or `desc`, case-insensitive |

**Group by** - `GET .../query?groupBy=department`
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

**Sort by** - `GET .../query?sortBy=age&order=asc`
```json
{
  "sortedRecords": [
    { "id": 2, "name": "Jane Smith", "age": 25, "department": "Engineering" },
    { "id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing" },
    { "id": 1, "name": "John Doe", "age": 30, "department": "Engineering" }
  ]
}
```

**Both** - `GET .../query?groupBy=department&sortBy=age&order=desc` returns `groupedRecords`, with the records
sorted inside each group.

**Neither** - `GET .../query` returns `{ "records": [ ... ] }` in insertion order.

Semantics:
* Groups appear in order of first occurrence. Group keys are the text form of the value (`30` -> `"30"`).
* Records missing the field (or holding `null`) go to the group `"null"` and are placed **last** when sorting,
  in both directions.
* Numbers sort numerically, strings lexicographically (case-sensitive). Mixed types never fail:
  numbers < booleans < strings < other.
* Sorting is stable: records with equal values keep their insertion order.

### Errors

Errors use RFC 9457 `application/problem+json`:

```json
{ "type": "about:blank", "title": "Not Found", "status": 404,
  "detail": "Dataset 'foo' does not exist.", "instance": "/api/dataset/foo/query" }
```

| Status | When |
|--------|------|
| 400 | Malformed JSON / body not an object; missing or non-integer `id`; invalid dataset name; unknown `groupBy`/`sortBy` field; invalid `order` |
| 404 | Querying a dataset that has no records |
| 405 / 415 | Unsupported HTTP method / content type |
| 409 | `id` already exists in the dataset |
| 500 | Unexpected error (details are logged, never returned) |

---

## Design

```
controller  DatasetController            HTTP mapping, input validation, DTOs in/out
dto         DatasetQuery, SortOrder,     Immutable request/response types
            InsertRecordResponse, QueryResponse
service     DatasetService(+Impl)        Orchestration (facade over the pieces below)
            GroupingService              Group-by logic
            SortingService               Chooses a SortStrategy by SortOrder
            RecordFieldValidator         "Field exists in dataset?" check
strategy    SortStrategy                 Strategy pattern:
            AscendingSortStrategy,         one implementation per direction,
            DescendingSortStrategy         auto-registered by Spring
            FieldValueComparator         Type-safe ordering of arbitrary JSON values
repository  RecordRepository             Spring Data JPA
entity      DatasetRecord                (dataset_name, record_id) unique; JSON stored as TEXT
exception   *Exception, GlobalExceptionHandler
```

* **Strategy pattern** for sort direction - add a new direction/behaviour by adding a `@Component`
  implementing `SortStrategy`; nothing else changes.
* **Storage model:** one table, `dataset_record`, with the whole record as JSON text. This works on every relational
  database. The unique constraint `(dataset_name, record_id)` is the real guard against duplicates (the
  service also maps a race-condition violation to 409).
* **Trade-off:** group/sort run in memory after loading the dataset. That is simple and fully dynamic for any field,
  but for very large datasets you would push the work into the database (e.g. PostgreSQL `jsonb` operators) and
  add pagination.

## Postman

Import `docs/json-dataset-service.postman_collection.json`. Run the three "Insert" requests first, then the queries.
