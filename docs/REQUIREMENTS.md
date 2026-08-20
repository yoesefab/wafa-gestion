# WAFA Gestion - Software Requirements Specification

## 1. Context

WAFA BUREAU is a Moroccan company that sells office supplies, office furniture,
and office equipment. Its daily operations require the management of products,
business partners, stock, purchases, sales, and invoices.

WAFA Gestion is an internal web application developed as an internship project.
It is an ERP-lite: it centralizes the company's main commercial and inventory
activities without attempting to replace a complete accounting, warehouse, or
customer relationship management system.

The application will be used by a small internal team from desktop and laptop
computers. It has one authenticated user type. Every authenticated user has the
same permissions and can access all application modules.

### 1.1 Technical context

- Backend: Java 21, Spring Boot, Spring Web, Spring Data JPA, Spring Security,
  Bean Validation, JWT, PostgreSQL, Flyway, and Swagger/OpenAPI.
- Frontend: React, Vite, Tailwind CSS, shadcn/ui, TanStack Query, and Axios.
- Architecture: modular monolith with one backend application, one frontend
  application, and one PostgreSQL database.
- Target duration: approximately 20 working days for one intern.

## 2. Problem

When commercial and inventory data is maintained in separate spreadsheets,
paper documents, or informal records, the company cannot reliably answer basic
operational questions such as:

- Which products are currently available?
- Which products need to be reordered?
- What was sold or purchased during a given period?
- Which customer or supplier is associated with an order?
- Why did the stock quantity of a product change?
- Which sales orders have been invoiced?

This fragmentation creates duplicate data, inconsistent stock figures, limited
traceability, and time-consuming manual reporting. WAFA Gestion must provide a
single, consistent source of operational data while remaining small enough to
design, implement, test, and demonstrate within the internship period.

## 3. Objectives

### 3.1 Primary objectives

1. Secure access to the application with a simple authentication mechanism.
2. Centralize product, category, customer, and supplier records.
3. Maintain a reliable current stock quantity for every product.
4. Record and trace all stock increases and decreases.
5. Manage basic sales and purchase order lifecycles.
6. Generate simple invoices from confirmed sales orders.
7. Provide a dashboard with useful operational indicators.
8. Expose a documented REST API and provide a responsive, usable web interface.

### 3.2 Success criteria

The MVP is successful when an authenticated user can:

- create the master data needed for transactions;
- create and confirm purchase orders that increase stock;
- create and confirm sales orders that decrease stock;
- inspect the resulting stock movement history;
- generate and view an invoice for a confirmed sales order; and
- see current stock alerts and basic activity summaries on the dashboard.

## 4. Functional Requirements

The identifiers below are intended to support implementation tracking and
acceptance testing.

### 4.1 Authentication

- **FR-AUTH-01:** The system shall allow a user to sign in with a username or
  email and password.
- **FR-AUTH-02:** The backend shall issue a JWT after successful authentication.
- **FR-AUTH-03:** The frontend shall send the JWT when accessing protected API
  endpoints.
- **FR-AUTH-04:** All business endpoints shall reject unauthenticated requests.
- **FR-AUTH-05:** The system shall allow the authenticated user to sign out.
- **FR-AUTH-06:** Passwords shall be stored as secure one-way hashes, never as
  plain text.
- **FR-AUTH-07:** One initial user account shall be created through seed data or
  a controlled setup process. Public registration and role management are not
  required.

### 4.2 Common data-management behavior

- **FR-COM-01:** List screens shall support pagination.
- **FR-COM-02:** Master-data list screens shall support a simple text search by
  the most useful fields, such as name, reference, email, or phone number.
- **FR-COM-03:** Forms shall display clear validation messages for invalid or
  missing data.
- **FR-COM-04:** Destructive actions shall require user confirmation.
- **FR-COM-05:** Records referenced by transactions shall not be physically
  deleted. Where applicable, they shall be deactivated instead.
- **FR-COM-06:** The user interface shall display loading, empty, success, and
  error states for API operations.

### 4.3 Categories

- **FR-CAT-01:** The user shall be able to list, search, create, update, and
  deactivate categories.
- **FR-CAT-02:** A category shall contain a unique name and an optional
  description.
- **FR-CAT-03:** A category assigned to at least one product shall not be
  physically deleted.

### 4.4 Products

- **FR-PROD-01:** The user shall be able to list, search, create, view, update,
  and deactivate products.
- **FR-PROD-02:** A product shall contain a unique reference/SKU, name,
  category, purchase price, selling price, current stock quantity, minimum stock
  threshold, unit of measure, and active status.
- **FR-PROD-03:** Prices and stock thresholds shall be zero or positive.
- **FR-PROD-04:** The current stock quantity shall be read-only on the normal
  product edit form and changed only through stock operations.
- **FR-PROD-05:** A newly created product shall start with zero stock. Any
  initial stock shall be entered as a traceable manual adjustment.
- **FR-PROD-06:** Product lists shall be filterable by category, active status,
  and low-stock status.
- **FR-PROD-07:** A product used in an order or stock movement shall not be
  physically deleted.

### 4.5 Customers

- **FR-CUST-01:** The user shall be able to list, search, create, view, update,
  and deactivate customers.
- **FR-CUST-02:** A customer shall contain a name or company name and may contain
  ICE/tax identifier, contact person, email, phone number, and address.
- **FR-CUST-03:** Email format shall be validated when an email is provided.
- **FR-CUST-04:** A customer referenced by a sales order or invoice shall not be
  physically deleted.

### 4.6 Suppliers

- **FR-SUP-01:** The user shall be able to list, search, create, view, update,
  and deactivate suppliers.
- **FR-SUP-02:** A supplier shall contain a name or company name and may contain
  ICE/tax identifier, contact person, email, phone number, and address.
- **FR-SUP-03:** Email format shall be validated when an email is provided.
- **FR-SUP-04:** A supplier referenced by a purchase order shall not be
  physically deleted.

### 4.7 Inventory and stock movements

- **FR-INV-01:** The system shall display each active product's current stock,
  minimum stock threshold, and stock status.
- **FR-INV-02:** A product shall be marked as low stock when its current stock is
  less than or equal to its minimum stock threshold.
- **FR-INV-03:** The user shall be able to record a manual stock adjustment with
  product, quantity, direction (increase or decrease), reason, and optional note.
- **FR-INV-04:** Every confirmed purchase, confirmed sale, or manual adjustment
  shall create an immutable stock movement.
- **FR-INV-05:** A stock movement shall record its type, product, signed
  quantity, date and time, optional source document, note, and creator.
- **FR-INV-06:** Stock movement history shall be filterable by product, movement
  type, and date range.
- **FR-INV-07:** Confirming a stock decrease shall fail when it would make stock
  negative.
- **FR-INV-08:** Stock updates and their corresponding movements shall be saved
  atomically in one database transaction.
- **FR-INV-09:** Confirmed stock movements shall not be edited or deleted.

### 4.8 Sales orders

- **FR-SALE-01:** The user shall be able to list, search, create, view, and edit
  draft sales orders.
- **FR-SALE-02:** A sales order shall contain a unique order number, customer,
  order date, status, one or more lines, optional note, subtotal, tax amount, and
  total amount.
- **FR-SALE-03:** Each sales order line shall contain a product, positive
  quantity, non-negative unit selling price, tax rate, and calculated line total.
- **FR-SALE-04:** Totals shall be calculated by the backend and shall not rely on
  values submitted by the frontend.
- **FR-SALE-05:** MVP statuses shall be `DRAFT`, `CONFIRMED`, and `CANCELLED`.
- **FR-SALE-06:** Confirming a sales order shall validate stock for every line,
  decrease stock, and create outbound stock movements exactly once.
- **FR-SALE-07:** A sales order cannot be confirmed when any ordered product has
  insufficient stock.
- **FR-SALE-08:** Only draft sales orders may be edited or cancelled.
- **FR-SALE-09:** Confirmed sales orders shall remain read-only. Returns and
  post-confirmation cancellation are outside the MVP.

### 4.9 Purchase orders

- **FR-PUR-01:** The user shall be able to list, search, create, view, and edit
  draft purchase orders.
- **FR-PUR-02:** A purchase order shall contain a unique order number, supplier,
  order date, status, one or more lines, optional note, subtotal, tax amount, and
  total amount.
- **FR-PUR-03:** Each purchase order line shall contain a product, positive
  quantity, non-negative unit purchase price, tax rate, and calculated line total.
- **FR-PUR-04:** Totals shall be calculated by the backend.
- **FR-PUR-05:** MVP statuses shall be `DRAFT`, `RECEIVED`, and `CANCELLED`.
- **FR-PUR-06:** Marking a purchase order as received shall increase stock and
  create inbound stock movements exactly once.
- **FR-PUR-07:** Only draft purchase orders may be edited or cancelled.
- **FR-PUR-08:** Received purchase orders shall remain read-only. Partial
  receipts are outside the MVP.

### 4.10 Invoices

- **FR-INVCE-01:** The user shall be able to generate one invoice from a
  confirmed sales order.
- **FR-INVCE-02:** An invoice shall receive a unique invoice number and shall
  copy the customer details, order lines, prices, taxes, totals, and issue date
  at the time of generation.
- **FR-INVCE-03:** Changes to master data after generation shall not alter the
  invoice snapshot.
- **FR-INVCE-04:** The user shall be able to list, search, and view invoices.
- **FR-INVCE-05:** Generated invoices shall be read-only in the MVP.
- **FR-INVCE-06:** The invoice view shall be suitable for browser printing.
- **FR-INVCE-07:** Payment collection, credit notes, legal e-invoicing, and
  accounting journal entries are outside the MVP.

### 4.11 Dashboard

- **FR-DASH-01:** The dashboard shall show counts of active products, customers,
  and suppliers.
- **FR-DASH-02:** The dashboard shall show the number of products currently at
  or below their minimum stock threshold.
- **FR-DASH-03:** The dashboard shall show the number and total value of sales
  orders confirmed during the current month.
- **FR-DASH-04:** The dashboard shall show the number and total value of purchase
  orders received during the current month.
- **FR-DASH-05:** The dashboard shall show a short list of low-stock products and
  recent stock movements.
- **FR-DASH-06:** Dashboard data shall be calculated from persisted operational
  data and refreshed when the page is loaded.

## 5. Non-Functional Requirements

### 5.1 Security

- **NFR-SEC-01:** All protected communication in a deployed environment shall
  use HTTPS.
- **NFR-SEC-02:** JWTs shall have a limited lifetime and be signed with a secret
  supplied through environment configuration.
- **NFR-SEC-03:** Authentication failures shall not reveal whether a username
  exists.
- **NFR-SEC-04:** The backend shall validate all inputs independently of frontend
  validation.
- **NFR-SEC-05:** Secrets and production credentials shall not be committed to
  source control.
- **NFR-SEC-06:** Cross-origin access shall be restricted to configured frontend
  origins.

### 5.2 Reliability and data integrity

- **NFR-REL-01:** Database schema changes shall be versioned with Flyway.
- **NFR-REL-02:** Foreign keys, unique constraints, and transactions shall
  enforce critical business invariants.
- **NFR-REL-03:** Confirming the same order more than once shall not duplicate
  stock movements or stock changes.
- **NFR-REL-04:** API errors shall use a consistent response structure with an
  HTTP status, message, and field validation details where relevant.
- **NFR-REL-05:** Dates and times shall be stored consistently and presented in
  a format suitable for users in Morocco.
- **NFR-REL-06:** Monetary values shall use decimal arithmetic and the Moroccan
  dirham (`MAD`) as the MVP currency.

### 5.3 Performance

- **NFR-PERF-01:** Under normal internship demonstration loads, standard list and
  detail API requests should respond within one second, excluding network delay.
- **NFR-PERF-02:** List endpoints shall be paginated to avoid loading unbounded
  result sets.
- **NFR-PERF-03:** Database indexes shall support identifiers, foreign keys, and
  fields used frequently for search and filtering.

### 5.4 Usability and accessibility

- **NFR-UX-01:** The interface shall be usable on current desktop browsers and
  remain functional on tablet-sized screens.
- **NFR-UX-02:** Navigation and forms shall use consistent labels, layouts, and
  feedback patterns.
- **NFR-UX-03:** Interactive controls shall be keyboard accessible, have visible
  focus states, and use labels understandable by assistive technology.
- **NFR-UX-04:** Tables shall clearly distinguish loading, empty, and error states.
- **NFR-UX-05:** The MVP interface shall use one consistent display language.
  Full internationalization is optional.

### 5.5 Maintainability and testability

- **NFR-MNT-01:** Backend code shall be organized by business module inside a
  modular monolith, with clear boundaries between web, application, and
  persistence responsibilities.
- **NFR-MNT-02:** The frontend shall organize API access, query hooks, reusable
  components, and module pages consistently.
- **NFR-MNT-03:** Public REST endpoints shall be documented with OpenAPI and
  accessible through Swagger UI in development.
- **NFR-MNT-04:** Core business rules, especially order confirmation and stock
  updates, shall have automated backend tests.
- **NFR-MNT-05:** The project shall include instructions for local setup,
  configuration, database migration, and application startup.
- **NFR-MNT-06:** Application logs shall record startup failures and unexpected
  server errors without exposing passwords, tokens, or other secrets.

## 6. Modules

The modular monolith shall contain the following business modules:

| Module | Responsibility | Main dependencies |
| --- | --- | --- |
| Authentication | Login, JWT issuance and validation, current user | None |
| Categories | Product classification | Authentication |
| Products | Product catalog, prices, stock threshold | Categories |
| Customers | Customer master data | Authentication |
| Suppliers | Supplier master data | Authentication |
| Inventory | Current product stock and low-stock view | Products |
| Stock movements | Immutable stock change history | Inventory, Products |
| Sales orders | Customer orders, totals, confirmation | Customers, Products, Inventory |
| Purchase orders | Supplier orders, totals, receipt | Suppliers, Products, Inventory |
| Invoices | Invoice snapshots generated from sales | Sales orders, Customers |
| Dashboard | Aggregated operational indicators | Other business modules, read-only |

Dependencies are logical boundaries inside one deployable backend. Modules may
coordinate through application services, but business rules shall not be placed
in controllers or duplicated in the frontend.

## 7. MVP Scope

### 7.1 Included

The 20-day MVP includes:

- one seeded user and JWT-based login/logout;
- category, product, customer, and supplier management;
- pagination, basic search, and essential filters;
- current stock display and low-stock detection;
- manual stock increases and decreases with an audit trail;
- draft and confirmation workflows for sales orders;
- draft and receipt workflows for purchase orders;
- atomic stock updates and immutable stock movements;
- one read-only, printable invoice per confirmed sales order;
- a dashboard with the indicators listed in section 4.11;
- backend validation, consistent API errors, Flyway migrations, and OpenAPI
  documentation;
- focused automated tests for authentication and critical inventory/order rules;
- a responsive internal web interface with essential feedback states; and
- local setup and run documentation.

### 7.2 Explicitly excluded from the MVP

To keep delivery realistic, the following are not part of the initial build:

- self-registration, multiple roles, and fine-grained permissions;
- multiple warehouses, storage locations, lots, serial numbers, or barcodes;
- reservations, backorders, partial deliveries, and partial purchase receipts;
- editing or reversing confirmed orders and stock movements;
- product variants, bundles, images, and multiple units of measure;
- quotations, delivery notes, returns, refunds, and credit notes;
- invoice payment tracking, accounting, tax declarations, and bank integration;
- supplier invoices and automatic procurement suggestions;
- multiple currencies and exchange rates;
- email, SMS, or push notifications;
- advanced analytics, report builders, or data warehouses;
- native mobile applications, offline mode, and real-time collaboration;
- cloud deployment automation and enterprise-grade observability; and
- import/export beyond any small fixture or seed data needed for demonstration.

### 7.3 Suggested 20-day delivery plan

| Days | Deliverable |
| --- | --- |
| 1-2 | Project setup, database, module skeleton, security design, Flyway baseline |
| 3-4 | Authentication and shared API/frontend foundations |
| 5-7 | Categories, products, customers, and suppliers |
| 8-10 | Inventory, manual adjustments, stock movement history |
| 11-13 | Sales order workflow and stock deduction |
| 14-15 | Purchase order workflow and stock increase |
| 16 | Invoice generation and printable view |
| 17 | Dashboard |
| 18-19 | Integration testing, validation, error handling, UI refinement |
| 20 | Documentation, sample data, final verification, and demonstration preparation |

The schedule assumes one currency, one warehouse-equivalent stock quantity per
product, no public deployment requirement, and timely access to stakeholder
feedback. Optional features shall only begin after the full MVP workflow is
working and tested.

## 8. Optional Features

The following features may be considered after the MVP, in priority order:

1. PDF invoice export using a server-side or browser print solution.
2. CSV import/export for products, customers, suppliers, and stock reports.
3. Order status extensions such as partially received, delivered, and returned.
4. Stock reservations for draft or confirmed sales orders.
5. Automatic low-stock notifications and purchase suggestions.
6. Quotations and delivery notes linked to sales orders.
7. Payment status and due-date tracking for invoices.
8. Multiple users with administrator and employee roles.
9. Audit history for edits to important master data.
10. Product images, barcode support, and barcode label printing.
11. Multiple warehouses and stock transfers.
12. French and Arabic interface localization.
13. Richer charts, period comparisons, and downloadable reports.
14. Automated backups, deployment pipeline, monitoring, and production hosting.

Optional features are not acceptance criteria for the internship MVP and must
not delay the implementation or verification of the core workflows.
