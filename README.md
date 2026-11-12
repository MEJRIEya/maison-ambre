# Maison Ambre

A small multi-boutique perfume platform built for the Kubee Analytics technical exercise.
One shared catalogue is sold through three boutiques (Dubai in AED, Paris in EUR, Casablanca in MAD), each with its own stock and prices.
Customers browse and order from one boutique at a time, and a manager page shows how each boutique is doing.

**Stack:** Spring Boot 3.5 (Java 21) · PostgreSQL 16 · React 18 (Vite) · Docker Compose

## Run it

You need Docker (Docker Desktop on Windows or macOS).

```bash
docker compose up --build
```

Then open **http://localhost:3000**.

- The first build downloads Maven and npm dependencies, so it takes a few minutes. Later builds are quicker.
- The API starts once the database is healthy, and Flyway creates the schema and loads the seed data.
  If the page says it cannot reach the API, wait for `Started MaisonAmbreApplication` in the logs and reload.
- The API is reachable through the web container, for example http://localhost:3000/api/boutiques.
- To start again from fresh seed data: `docker compose down`, then `docker compose up`.
- If port 3000 is already used on your machine, change `"3000:80"` in `docker-compose.yml`.

### Run the tests

```bash
docker compose up -d db
docker compose run --rm api-tests
```

This runs `mvn test` in a Maven container against a separate `maison_ambre_test` database, so the demo data is never touched.
With JDK 21 and Maven installed, `mvn test` in `backend/` works too, given a PostgreSQL reachable at the URL in `TEST_DB_URL`.

The tests are in `backend/src/test/java/com/maisonambre/order/OrderApiTest.java` and call the order endpoint over HTTP:

1. A completed order turns a prospect into a client.
2. Ten buyers order the last five bottles at the same moment: exactly five orders succeed, five get `409`, and the stock ends at 0.
3. A cart that mixes two boutiques is rejected with `422` and no stock is taken.

## Project structure

```
backend/                 Spring Boot API
  src/main/java/com/maisonambre/
    boutique/            list boutiques
    catalog/             products, per-boutique inventory, stock updates
    customer/            prospects, clients, identification by email
    order/               checkout, complete/cancel, the conversion rule
    insights/            manager figures (plain SQL)
    common/              Money, error format
  src/main/resources/db/
    migration/           schema (Flyway)
    seed/                demo data (Flyway, not used by the tests)
frontend/                React app, served by nginx in Docker
docker/                  database init script (creates the test database)
docker-compose.yml
```

## API

| Method | Path | What it does | Success | Errors |
|---|---|---|---|---|
| GET | `/api/boutiques` | List boutiques | 200 | |
| GET | `/api/boutiques/{id}/products` | Catalogue of a boutique, with local price and availability | 200 | 404 boutique |
| GET | `/api/boutiques/{id}/products/{productId}` | Product detail in that boutique | 200 | 404 boutique or product not sold there |
| POST | `/api/customers` | Identify a visitor by email: creates a prospect or returns the existing prospect or client | 201 new / 200 existing | 400, 422 unknown boutique |
| GET | `/api/customers/{id}` | A customer with status `PROSPECT` or `CLIENT` | 200 | 404 |
| POST | `/api/orders` | Checkout: places and completes an order | 201 | 400, 409 not enough stock, 422 mixed boutiques / unknown customer / product not sold there |
| GET | `/api/orders/{id}` | One order | 200 | 404 |
| POST | `/api/orders/{id}/complete` | Complete a pending order | 200 | 404, 409 not pending |
| POST | `/api/orders/{id}/cancel` | Cancel a pending order and return its stock | 200 | 404, 409 not pending |
| GET | `/api/boutiques/{id}/insights` | Manager figures for a boutique | 200 | 404 |

Example checkout request:

```json
{
  "boutiqueId": 2,
  "customerId": 5,
  "lines": [
    { "boutiqueId": 2, "productId": 4, "quantity": 1 }
  ]
}
```

Every error has the same shape ([RFC 9457 problem details](https://www.rfc-editor.org/rfc/rfc9457)) with a stable `code`:

```json
{ "status": 409, "title": "Conflict", "detail": "Not enough stock for Oud Royal in Maison Ambre Paris", "code": "INSUFFICIENT_STOCK" }
```

Status codes follow one rule: `400` the request is malformed, `404` the resource in the URL does not exist,
`409` the request conflicts with the current state (stock, order status), `422` the request is well formed but breaks a business rule.

## Design decisions

### Prospects and clients: two tables

`prospect` holds everyone who has given an email, with the boutique where they were first seen.
`client` holds one row per prospect who has completed an order: its primary key is `prospect_id`, plus `converted_at`.

Why two tables rather than one table with a status:

- **The database enforces the rules.** The primary key means a prospect can be converted only once, and the foreign key means every client is a prospect. With a status column, `status = 'CLIENT'` and an empty `converted_at` could drift apart unless an extra check is added.
- **"Prospects include clients" falls out of the model.** Kubee confirmed that the conversion rate counts everyone who has given an email. A client keeps their prospect row, so prospects are simply `COUNT(*)` on `prospect`, and the rate can never exceed 100%.
- **Conversion is an insert, not an update.** `INSERT ... ON CONFLICT (prospect_id) DO NOTHING` is safe to run on every completed order: the first one creates the client, later ones change nothing.
- **Client-only data has a home.** Anything that only applies to clients later (loyalty tier, lifetime value) goes in `client` without nullable columns on every prospect.

The cost is a join to know a customer's status. The API hides this: it talks about "customers" with a `status` field.

### The conversion happens in one place

`OrderService.complete()` is the only code that turns an order into a sale, and the only caller of `ClientRepository.convertToClient()`.
Both checkout and "complete a pending order" go through it.

**Attribution across boutiques** (agreed with Kubee): prospects and clients are both counted by the boutique where the person was **first seen**, so the conversion rate is consistent.
Sales are counted in the boutique where the order was **placed**.
In the seed data, Ines was first seen in Paris and first bought in Dubai: she counts as a Paris client, and her sale counts for Dubai.

### Money and currency

- Amounts are `NUMERIC(12,2)` in the database and `BigDecimal` in Java: exact decimals, never floating point.
- The currency belongs to the boutique. An order copies it when it is placed, and each line keeps the unit price at the time of purchase, so later price changes do not rewrite past sales.
- The API always returns money as `{ "amount": 120.00, "currency": "EUR" }`, so an amount is never separated from its currency.
- Prices never come from the client: the API reads them from the boutique's inventory.
- Each boutique reports in its own currency. Nothing is converted between currencies, so no figure mixes AED and EUR.

### Stock cannot go negative, even with orders at the same time

Stock is taken with a single conditional update:

```sql
UPDATE inventory SET stock = stock - :quantity WHERE id = :inventoryId AND stock >= :quantity
```

PostgreSQL locks the row during the update. If two orders want the last bottle, the second waits for the first to commit, then re-checks `stock >= quantity` against the new value and updates 0 rows.
The API then answers `409 INSUFFICIENT_STOCK` and the transaction rolls back, so no stock is taken for any line of that order.

Two more safety nets:

- Order lines are sorted by product id before taking stock, so two orders always lock rows in the same order and cannot deadlock each other.
- `CHECK (stock >= 0)` on the table: even a bug in the code could not store negative stock.

Cancelling a pending order locks the order row first (`SELECT ... FOR UPDATE`), so two cancels at the same time cannot return the stock twice.

### Mixed-boutique carts are rejected by the API

Each order line repeats the boutique it was added in. If any line's boutique differs from the order's boutique, the API answers `422 MIXED_BOUTIQUE_CART` before touching anything.
The frontend cannot build such a cart anyway, but the API does not trust it.

### Order lifecycle

`PENDING → COMPLETED` or `PENDING → CANCELLED`. Only `COMPLETED` orders count in the insights.

- Checkout (`POST /api/orders`) takes the stock and completes the order straight away, because there is no payment step (agreed with Kubee).
- Pending orders exist in the seed data and are handled through `/complete` and `/cancel`. A pending order has already reserved its stock; cancelling gives it back.

### Cart when the customer switches boutique: one cart per boutique

Each boutique keeps its own cart. Switching from Paris to Dubai shows the Dubai cart, and the Paris cart is still there when the customer comes back.

Why:

- **Nothing is lost.** Emptying the cart on a switch punishes a customer who is only comparing prices between boutiques.
- **A cart can never mix boutiques**, which matches the rule enforced by the API.
- **Prices stay honest.** Moving items into another boutique's cart would silently change their price and currency, and the product might not be sold or in stock there.

The risk is that a customer forgets a cart in another boutique, so the cart page says "You also have items in: Paris" with a link to switch.
Carts are stored in `localStorage` next to the selected boutique, so both survive a reload.

### Other choices

- **Boutique switcher** state is in `localStorage`. If the saved boutique no longer exists, the app falls back to the first one.
- **Customer identity is the email**, stored in lower case (`CHECK (email = lower(email))`), so `Sara@Example.com` and `sara@example.com` are the same person.
- **Insights are plain SQL** through Spring's `JdbcClient`: aggregates are easier to read and check in SQL than through JPA. The low-stock threshold is 5, set in `application.yml` (`app.insights.low-stock-threshold`). Top 3 is by units sold, then revenue, then name. The conversion rate is `null` (shown as "–") when a boutique has no prospects yet, because "no data" is not 0%.
- **Flyway owns the schema**; Hibernate never creates tables (`ddl-auto: none`).
- **Tests use a real PostgreSQL**, not an in-memory database, because the stock safety relies on PostgreSQL's locking.

## What I cut

Agreed with Kubee before starting, to fit around two university validations this week:

- Insights use plain tables, no charts.
- The interface is minimal.
- Seed data is limited to what the brief asks for.

The separate product page was cut in the agreed scope, then added back.

Not done:

- The three optional items: filter by fragrance notes, per-currency rounding rules, temporary unavailability in one boutique.
- Authentication on the manager page (out of scope).
- Frontend tests, and API tests beyond the order endpoint.

## What I would do next with one more week

- Use Testcontainers so the tests start their own database, and add tests for cancel (stock returned), validation errors and the insights figures.
- Add the optional features. Temporary unavailability would be an `active` flag on `inventory`, separate from stock.
- Expire pending orders after a delay so reserved stock comes back on its own.
- Protect the manager page and let a manager see only their boutique.
- Add a date range to the insights, and OpenAPI documentation for the API.
- Add a health check to the API container so the web app waits until the API is ready.

## Hours

Actual hours are compared with both estimates sent to Kubee: the first one (full scope and agreed reduced scope) and the revised one that takes AI assistance into account.

| Component | First estimate (full scope) | Agreed scope | Revised estimate (with AI) | Actual |
|---|---|---|---|---|
| Data model | 4h | 3h | 2h | 2h |
| API | 9h | 8h | 4h | 2.5h |
| Frontend | 7h | 5h | 2.5h | 1.5h |
| Insights page | 3h | 2h | 1h | 1h |
| Setup and README | 3h | 2h | 2h | 1h |
| Contingency | 3h | 2h | 1.5h | 1h |
| **Total** | **29h** | **22h** | **13h** | **9h** |

**What differed and why:** The actual total is 9h, against 13h in the revised estimate and 22h in the agreed scope. The data model and the insights page took what I expected. The API, the frontend and the setup came in under the revised estimate because the AI assistant wrote most of the code (see below), and less of the contingency was needed. The first Docker build took about 22 minutes because of slow downloads.

## Use of AI

Most of the code was written with an AI coding assistant (Claude), as allowed by the brief. The commits carry a `Co-Authored-By` line for that reason.
