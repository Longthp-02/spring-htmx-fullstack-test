# Spring + HTMX Fullstack Test

A Kotlin/Spring Boot fullstack sample that demonstrates:
- Startup data ingestion from external JSON into Postgres
- Server-rendered UI with Thymeleaf + HTMX
- Product table load/search/add/update/delete flows without full page reloads
- CSV export for filtered products

This project is built to match the fullstack test requirements.

## Tech Stack
- Kotlin + Spring Boot 4
- Postgres
- Flyway migrations
- Spring `JdbcClient` for SQL access
- Thymeleaf + HTMX
- Web Awesome components + design tokens
- Gradle Kotlin DSL

## What This App Does
### Data bootstrap on startup
- Scheduled job runs immediately (`@Scheduled(initialDelay = 0)`)
- Fetches from `https://famme.no/products.json`
- Saves max 50 products
- Stores products + variants in relational tables
- Upserts products and replaces variants

### Main UI (`/`)
- `Load products` button loads table from DB using HTMX fragment swap
- Search box does active filtering while typing (`keyup changed delay:300ms`)
- Add product form inserts into DB and refreshes table via HTMX
- Row actions:
  - `Update` -> opens dedicated edit page (`/products/{id}/edit`)
  - `Delete` -> uses `<dialog>` confirmation, then HTMX `DELETE`
- `Export CSV` downloads current (optionally filtered) product data

### Update page (`/products/{id}/edit`)
- Server-rendered form for editing title/handle/product type
- Saves changes and redirects back to same edit page

## Project Structure
- `src/main/kotlin/com/longpham/springhtmxfullstacktest/web/ProductPageController.kt`
  - UI endpoints (`/`, table fragment, add/delete, edit page, CSV export)
- `src/main/kotlin/com/longpham/springhtmxfullstacktest/product/ProductRepository.kt`
  - SQL operations with `JdbcClient`
- `src/main/kotlin/com/longpham/springhtmxfullstacktest/product/ProductBootstrapScheduler.kt`
  - Startup schedule trigger
- `src/main/kotlin/com/longpham/springhtmxfullstacktest/product/ProductBootstrapService.kt`
  - Bootstrap orchestration
- `src/main/kotlin/com/longpham/springhtmxfullstacktest/product/ProductBootstrapClient.kt`
  - External HTTP fetch (`RestClient`)
- `src/main/kotlin/com/longpham/springhtmxfullstacktest/product/ProductBootstrapMapper.kt`
  - DTO -> domain mapping
- `src/main/resources/templates/`
  - `index.html`, `products-edit.html`, `fragments/products-table.html`
- `src/main/resources/static/css/app.css`
  - Token-based UI styling

## Setup & Run
## 1) Start Postgres
```bash
docker compose up -d
```

## 2) Run app (dev profile)
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Open:
- `http://localhost:8080`

## Configuration
- `src/main/resources/application-dev.properties`
  - DB URL: `jdbc:postgresql://localhost:5435/spring_htmx_test`
- `src/main/resources/application.properties`
  - Flyway enabled

## Endpoints Summary
- `GET /` main page
- `GET /products/table?q=...` table fragment
- `POST /products` add product
- `DELETE /products/{id}?q=...` delete product and refresh fragment
- `GET /products/{id}/edit` update page
- `POST /products/{id}/edit` save update
- `GET /products/export.csv?q=...` CSV export
