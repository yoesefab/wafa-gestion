# WAFA Gestion - Project Architecture

## 1. System Overview

WAFA Gestion uses a three-tier web architecture:

```text
+-------------------------+
| React web application   |
| Vite, Tailwind, shadcn  |
+------------+------------+
             |
             | HTTPS + JSON REST API under /api
             | Authorization: Bearer <JWT>
             v
+-------------------------+
| Spring Boot application |
| Modular monolith        |
| Java 21                 |
+------------+------------+
             |
             | JPA / JDBC transactions
             v
+-------------------------+
| PostgreSQL              |
| Schema managed by       |
| Flyway                  |
+-------------------------+
```

The React application never accesses the database directly. The Spring Boot
backend owns authentication, validation, calculations, state transitions,
transactions, and all data access. PostgreSQL is the durable source of
operational data.

The deployed system consists of one frontend application, one backend
application, and one PostgreSQL database. Development may run these as separate
local processes or containers without changing the logical architecture.

### 1.1 Architectural principles

- Organize backend code by business capability, not only by technical layer.
- Keep module dependencies explicit and one-directional.
- Expose module behavior through application services and query interfaces.
- Keep controllers thin; business rules belong in domain/application services.
- Never expose JPA entities through the REST API.
- Use one database transaction for one business operation.
- Make inventory the only authority allowed to change stock.
- Prefer simple synchronous calls while the application is one process.
- Use database constraints as a final integrity boundary, not as a replacement
  for clear application validation.

## 2. Why a Modular Monolith

A modular monolith is appropriate because WAFA Gestion has a small, closely
related domain and must be completed by one intern in approximately 20 working
days. Sales, purchases, products, stock, and invoices are separate business
capabilities, but their workflows frequently need strong consistency within one
database transaction.

The approach provides:

- one application to build, run, test, configure, and deploy;
- direct in-process calls between modules with no network failure modes;
- straightforward transactions across sales, purchases, inventory, and stock
  movements;
- one security configuration and one standard error model;
- clear business boundaries without distributed-system infrastructure;
- easier local development and demonstration; and
- a path to later extraction if a module eventually needs independent scaling or
  ownership.

"Monolith" does not mean unstructured code. Each module owns a defined set of
business responsibilities and persistence objects. Other modules interact with
its public application interface rather than reaching into its controllers,
repositories, or internal entities.

## 3. Why Microservices Are Not Necessary

Microservices would not solve a current WAFA Gestion problem. The MVP has one
authenticated user type, a small internal user base, one development team, one
database, and modest performance requirements. No module currently requires
independent deployment, scaling, technology, or availability.

Using microservices would introduce work that does not contribute to the MVP:

- service discovery, routing, and multiple deployments;
- network authentication and service-to-service authorization;
- distributed tracing and more complex monitoring;
- API compatibility between independently deployed services;
- eventual consistency, messaging, retries, and idempotent consumers;
- distributed transaction problems for order and stock updates; and
- more integration environments and failure scenarios.

The most important workflows require atomic order and inventory updates. A
single process and database make those guarantees direct and testable.
Microservices should be reconsidered only if there is evidence such as separate
teams, materially different scaling needs, independent release requirements, or
a module that must tolerate the rest of the system being unavailable.

## 4. Backend Module Boundaries

The backend follows package-by-module organization. Each business module may
contain its own API, application, domain, and infrastructure packages:

```text
module
|-- api              REST controllers and request/response mapping
|-- application      use cases, transactions, public facades
|-- domain           entities, value rules, status transitions
`-- infrastructure   JPA repositories and external adapters
```

This is a conceptual structure, not a requirement to create empty layers in
small modules. A layer is introduced only when it has a real responsibility.

### 4.1 Module responsibilities

| Module | Owns | May depend on |
| --- | --- | --- |
| `shared` | Standard errors, pagination, clocks, common configuration, technical utilities | Nothing in the business domain |
| `user` | User identity, active state, password hash persistence, user lookup | `shared` |
| `auth` | Login, password verification, JWT issue/validation, security filter, current-user endpoint | `user`, `shared` |
| `catalog` | Categories, product master data, prices, unit, stock threshold, product queries | `shared` |
| `partner` | Customers and suppliers, contact data, active-state rules | `shared` |
| `inventory` | Current-stock mutation workflow, product locks, stock movements, adjustments, low-stock queries, reconciliation | `catalog`, `user`, `shared` |
| `sales` | Sales order aggregate, lines, totals, draft editing, confirmation, cancellation, delivery | `partner`, `catalog`, `inventory`, `user`, `shared` |
| `purchase` | Purchase order aggregate, lines, totals, draft editing, ordering, receipt, cancellation | `partner`, `catalog`, `inventory`, `user`, `shared` |
| `invoice` | Invoice snapshots, invoice numbering, invoice queries, PDF rendering | `sales`, `user`, `shared` |
| `dashboard` | Read-only operational summaries and aggregations | Public query interfaces from `catalog`, `partner`, `inventory`, `sales`, `purchase`; `shared` |

The `ORDERED` purchase and `DELIVERED` sales states come from the API contract.
They must be added to the requirements, business rules, and database model before
implementation, as noted in `API.md`.

### 4.2 Dependency diagram

Arrows mean "depends on":

```text
auth ---------> user ---------------------------> shared

inventory ----> catalog ------------------------> shared
    |----------> user

sales --------> partner ------------------------> shared
  |------------> catalog
  |------------> inventory
  `------------> user

purchase -----> partner
   |-----------> catalog
   |-----------> inventory
   `-----------> user

invoice ------> sales
   `-----------> user

dashboard ----> catalog, partner, inventory, sales, purchase
```

All modules may use `shared`, but `shared` must not depend on a business module.
There are no dependencies from `catalog`, `partner`, or `inventory` back to
`sales` or `purchase`, so order workflows cannot create circular dependencies.

### 4.3 Public and internal module APIs

A module may expose:

- application commands for supported state changes;
- query services returning purpose-specific projections;
- small public DTOs or value records needed by callers; and
- repository interfaces only when they are a deliberate inter-module port.

A module must not expose its controllers as an internal API. Other modules must
not import its internal repository implementations, mutate its entities, or
write directly to its tables.

Examples:

- Sales asks `partner` and `catalog` for active customer/product data.
- Sales calls the public `InventoryService` to issue stock for a confirmation.
- Invoice asks the public sales query interface for an invoice-source snapshot.
  Sales assembles the customer and product presentation data behind that
  interface, so invoice does not need to traverse sales repositories.
- Dashboard calls public read/query interfaces and never invokes mutation
  commands.

The `shared` module is not a place for business objects used by many modules.
Stock rules stay in `inventory`, money calculations for orders stay with the
order aggregates, and authentication rules stay in `auth`/`user`.

### 4.4 Inventory authority rule

`InventoryService` is the central authority for every product stock change.
This is a mandatory architecture rule:

In this document, `Product.currentStock` refers to the stock quantity field that
may also be described as `Product.quantity` in code discussions and is stored as
`products.current_stock` in PostgreSQL.

```text
Allowed:
SalesApplicationService    ---> InventoryService ---> product balance + movement
PurchaseApplicationService ---> InventoryService ---> product balance + movement
Manual adjustment API      ---> InventoryService ---> product balance + movement

Forbidden:
Sales repository           -X-> Product.currentStock
Purchase repository        -X-> Product.currentStock
Catalog update endpoint    -X-> Product.currentStock
Any controller             -X-> Product.currentStock
```

The current balance is stored on the product row as specified in `DATABASE.md`,
but catalog create/update operations treat it as read-only. The catalog module
may expose a narrow lock/update persistence port used by inventory; this does not
transfer business authority to catalog. Only `InventoryService` may orchestrate
that port together with stock movement creation.

This rule should be enforced through package visibility where practical, code
review, and an architecture test that rejects stock-mutating dependencies from
sales and purchase.

## 5. Frontend Feature Boundaries

The React frontend follows feature-based organization that mirrors user
workflows rather than backend JPA entities:

```text
src/
|-- app/                 router, providers, layout, query client
|-- features/
|   |-- auth/
|   |-- catalog/         categories and products
|   |-- partners/        customers and suppliers
|   |-- inventory/
|   |-- sales/
|   |-- purchases/
|   |-- invoices/
|   `-- dashboard/
`-- shared/              UI primitives, Axios client, formatting, generic hooks
```

Each feature owns its pages, feature-specific components, API functions, TanStack
Query keys/hooks, form schemas, and frontend DTO types. A feature exposes a small
public entry point; another feature must not deep-import its private components
or query implementation.

Expected dependencies are:

- `app` composes routes and all features;
- features may use `shared` UI and HTTP infrastructure;
- sales may use public catalog and partner selectors;
- purchases may use public catalog and partner selectors;
- invoice pages consume invoice API representations, not sales form state; and
- dashboard uses its dedicated aggregation endpoints rather than combining many
  unrelated browser requests.

Server data remains in TanStack Query. Local component state manages forms,
dialogs, filters, and transient UI behavior. A second global server-state store
is unnecessary. Query keys are feature-owned and include normalized filters so
cache entries cannot be confused.

The frontend must not duplicate authoritative business rules. It may validate
forms early and disable impossible actions for usability, but the backend still
recalculates totals, validates statuses, checks active references, and controls
stock.

## 6. REST Communication

The React application communicates with the backend only through the `/api`
contract in `API.md`:

```text
React page
   |
   v
TanStack Query hook
   |
   v
Feature API function
   |
   v
Shared Axios instance ---- HTTPS/JSON ----> Spring REST controller
                                                |
                                                v
                                      Application service / DTO mapper
```

REST controllers are adapters. Their responsibilities are limited to:

- parsing path, query, and body parameters;
- Bean Validation of request DTOs;
- resolving the authenticated user;
- invoking one application use case;
- mapping results to response DTOs; and
- selecting the documented HTTP status and headers.

Controllers do not calculate totals, modify order statuses directly, open custom
transactions, or access repositories.

JSON uses consistent single-resource and paginated envelopes. Errors use the
standard `application/problem+json` body. Invoice downloads are PDF responses;
all other contract responses are JSON. OpenAPI annotations or generated schema
metadata must describe the same contract without exposing persistence entities.

Axios is configured once with the base URL, JSON defaults, timeout, and token
interceptor. Feature API functions return typed response data and do not each
create a separate Axios instance.

## 7. Authentication Flow

Authentication is stateless JWT authentication with no role system:

```text
1. React -> POST /api/auth/login { login, password }
2. Auth module -> User lookup -> password hash verification
3. Auth module -> signed, expiring JWT
4. React stores token for the browser session
5. Axios -> Authorization: Bearer <JWT> on protected requests
6. JWT filter validates signature/expiry and builds authenticated principal
7. React -> GET /api/auth/me to restore/verify current session identity
```

For the MVP, the client should keep the token in memory and mirror it to
`sessionStorage` when reload persistence is needed. `sessionStorage` is cleared
when the browser tab session ends and is preferable to long-lived
`localStorage`. Because JavaScript can access either store, frontend XSS
prevention remains important.

On `401`, the Axios response interceptor clears the token and redirects to the
login screen. Logout is a client-side token removal because the MVP has no
refresh tokens or revocation list. The backend still checks that the referenced
user is active.

Passwords are accepted only by the login endpoint, compared with a secure hash,
and never logged. JWT signing secrets and lifetimes come from environment
configuration. Production traffic uses HTTPS, and CORS permits only configured
frontend origins.

## 8. Transaction Boundaries

Transactions belong to application-service use cases, not controllers or
repositories. Each command is atomic and normally uses Spring's default
`REQUIRED` propagation.

| Use case | Transaction contents |
| --- | --- |
| Create/update draft order | Validate references, calculate lines/totals, save aggregate |
| Confirm sales order | Lock order/products, validate, deduct stock, append movements, update status |
| Mark sale delivered | Validate `CONFIRMED`, set `DELIVERED` and timestamp; no stock call |
| Mark purchase ordered | Validate draft, freeze calculated content, set `ORDERED`; no stock call |
| Receive purchase order | Lock order/products, validate, add stock, append movements, update status |
| Cancel eligible order | Validate current state, set `CANCELLED`; no stock call |
| Manual adjustment | Lock product, validate result, update balance, append movement |
| Generate invoice | Validate eligible sale and uniqueness, copy snapshot, save invoice and lines |
| Archive master data | Validate version and archive rules, update active state |

### 8.1 Sales confirmation

```text
SalesApplicationService.confirm(orderId)
  BEGIN TRANSACTION
  -> load order and verify DRAFT
  -> validate customer, lines, totals
  -> InventoryService.issueSale(order lines, actor)
       -> lock affected products in ascending ID order
       -> validate active products and all available quantities
       -> update each current balance
       -> append one SALE movement per line
  -> set order CONFIRMED and confirmedAt
  COMMIT
```

### 8.2 Purchase receipt

```text
PurchaseApplicationService.receive(orderId)
  BEGIN TRANSACTION
  -> load order and verify ORDERED
  -> validate supplier, lines, totals
  -> InventoryService.receivePurchase(order lines, actor)
       -> lock affected products in ascending ID order
       -> validate active products
       -> update each current balance
       -> append one PURCHASE movement per line
  -> set order RECEIVED and receivedAt
  COMMIT
```

`InventoryService` participates in the caller's existing transaction. It must
not use `REQUIRES_NEW`, because committing stock separately from the order status
would violate atomicity. Any validation, constraint, or persistence failure rolls
back the whole use case.

Read-only list, detail, and dashboard queries may use read-only transactions.
They must not depend on lazy loading after the service transaction has ended;
repositories should return projections or services should map fully initialized
data to DTOs inside the transaction.

## 9. Inventory Consistency Strategy

WAFA Gestion uses a current balance plus an append-only ledger:

```text
products.current_stock = SUM(stock_movements.quantity_delta)
```

The balance supports fast availability checks and list screens. Stock movements
explain every change and provide transaction history. New products start at zero,
so opening stock is also a traceable manual movement.

Inventory consistency relies on all of the following controls:

1. **Single writer:** only `InventoryService` changes current stock or creates
   movements.
2. **Atomic writes:** balance, movements, and related order status commit in one
   transaction.
3. **Pessimistic product locks:** affected product rows are locked before the
   availability check and update.
4. **Deterministic lock order:** multi-product operations lock ascending product
   IDs to reduce deadlock risk.
5. **Complete validation:** a sale checks every line before applying any line.
6. **Non-negative constraint:** both application checks and database constraints
   reject a negative balance.
7. **Idempotency constraint:** each source order line can create at most one
   stock movement.
8. **Immutable ledger:** stock movements are never updated or deleted.
9. **Reconciliation:** a query or integration test compares balances with summed
   movement deltas.

Optimistic `@Version` fields protect normal aggregate edits. They do not replace
pessimistic locks for stock, because sales confirmation is a concurrent
read-check-write operation.

Draft sales orders do not reserve stock. Stock is checked at confirmation.
Delivery and purchase ordering have no stock effect. The MVP does not support
partial confirmation, partial receipt, returns, or reversal; those features
would require explicit compensating inventory commands and new business rules.

## 10. Error Handling

The backend uses one global exception-to-response mapper, implemented with
Spring's controller advice mechanism. It produces the standard Problem Details
shape from `API.md` for:

- request binding and Bean Validation failures;
- missing resources;
- duplicate unique values;
- optimistic locking conflicts;
- invalid state transitions;
- inactive references;
- insufficient stock;
- archive/resource-in-use conflicts; and
- unexpected server errors.

Domain and application services throw typed exceptions with stable error codes.
They do not depend on HTTP status classes. The web adapter maps those exceptions
to `400`, `404`, or `409` as defined by the API contract. Authentication failures
are handled consistently as `401`; there is no role-based `403` behavior in the
MVP.

Unexpected errors receive a trace ID, are logged once with useful server-side
context, and return a safe generic message. Logs must not contain passwords,
JWTs, SQL parameter secrets, or full sensitive request bodies.

The frontend Axios layer converts problem responses into one typed error shape.
TanStack Query pages show loading, empty, and retryable error states. Forms map
`fieldErrors` to their controls, while conflicts such as stale versions or
insufficient stock are shown as operation-level messages. A `401` triggers the
authentication flow described in section 7.

## 11. Flyway Database Migration Strategy

Flyway is the only owner of persistent schema evolution. Hibernate validates the
schema but does not create or update it in normal development or deployment.

Migration rules:

- Use versioned migrations with descriptive names, for example
  `V1__create_core_schema.sql` and `V2__add_order_workflow_statuses.sql`.
- Create tables in dependency order, then foreign keys, checks, unique
  constraints, and indexes.
- Make every migration forward-only. Never edit a migration that has already
  been applied to a shared environment.
- Keep structural migrations separate from development/demo seed data.
- Seed the initial user through a controlled migration or setup mechanism with a
  precomputed password hash and no plain-text password in source control.
- Use explicit names for constraints and indexes so production errors are
  diagnosable.
- Add database enum/status `CHECK` changes in the same release as the matching
  Java enum and workflow behavior.
- Backfill data before adding a new non-null constraint to an existing table.
- Test migrations from an empty database and from the previous supported schema
  version.

The application should fail startup when a migration fails or Hibernate schema
validation detects a mapping mismatch. Production-like environments use a
dedicated application database user with only the permissions it needs; schema
migration permissions may be separated later if deployment requirements justify
it.

## 12. Testing Strategy

Testing effort is proportional to business risk. Stock integrity, order state
transitions, totals, authentication, and database constraints receive deeper
coverage than simple display mapping.

### 12.1 Backend unit tests

Fast unit tests cover:

- monetary calculations and rounding;
- sales and purchase status transitions;
- duplicate product-line validation;
- active/reference rules;
- stock delta and non-negative balance rules;
- invoice snapshot construction; and
- error-code selection for business failures.

Unit tests do not mock JPA internals. They test domain calculations and
application decisions through small public interfaces.

### 12.2 Backend integration tests

Integration tests use PostgreSQL through Testcontainers rather than an in-memory
database for behavior that depends on PostgreSQL, JPA locking, or constraints.
Priority scenarios are:

- Flyway starts successfully from an empty database;
- login, protected endpoints, expired/invalid JWTs, and inactive users;
- unique, check, and foreign-key constraints;
- draft create/update and every legal/illegal order transition;
- successful multi-line sale confirmation;
- insufficient stock rolls back every product, movement, and order change;
- concurrent sales cannot make stock negative;
- retrying confirmation/receipt cannot duplicate movements;
- purchase receipt atomically adds stock;
- manual outbound adjustment rejects insufficient stock;
- reconciliation balance equals the movement sum; and
- one immutable invoice snapshot per eligible sales order.

Controller tests verify endpoint paths, validation, JSON envelopes, status codes,
and the standard problem response. They complement, rather than duplicate, the
transactional integration tests.

### 12.3 Architecture tests

Lightweight ArchUnit or equivalent tests should verify that:

- controllers do not access repositories;
- business modules do not depend on controller packages;
- `shared` does not depend on business modules;
- `catalog`, `partner`, and `inventory` do not depend on sales or purchase;
- sales and purchase stock workflows call the inventory application boundary;
  and
- JPA entities are not used as REST response types.

### 12.4 Frontend tests

Vitest and React Testing Library cover important component and hook behavior:

- login and `401` handling;
- list filters and pagination parameters;
- field-error rendering;
- draft order calculations shown to the user;
- action availability for each order state;
- stale-version and insufficient-stock messages; and
- loading, empty, error, and success states.

API calls should be mocked at the HTTP boundary with stable fixtures matching
`API.md`, rather than mocking every hook implementation.

### 12.5 End-to-end acceptance tests

A small Playwright suite covers the highest-value workflows:

1. Log in and load the current user.
2. Create category/product/partner master data.
3. Add opening stock through a manual adjustment.
4. Create and confirm a sale, verify stock decreases, then deliver it.
5. Create, order, and receive a purchase, then verify stock increases.
6. Generate and download an invoice.
7. Verify dashboard and low-stock data reflect persisted operations.

For the 20-day MVP, these focused tests are preferable to pursuing exhaustive
coverage. Critical transaction and database behavior must be automated; purely
visual variations and optional features can remain outside the initial suite.
