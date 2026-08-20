# WAFA Gestion - REST API Contract

## 1. Scope

This document defines the REST API used by the WAFA Gestion React frontend. The
backend is a Spring Boot application and all routes are relative to:

```text
/api
```

The contract is intentionally small. It exposes the workflows required by the
MVP and does not expose JPA entities, generic CRUD operations, role management,
or internal persistence details.

### 1.1 Workflow alignment decision

The requested API includes `deliver` for sales and `mark ordered` for purchases.
Those actions extend the earlier requirements and business rules, which currently
define only `DRAFT`, `CONFIRMED`, and `CANCELLED` for sales and only `DRAFT`,
`RECEIVED`, and `CANCELLED` for purchases.

This API therefore adopts these minimal extended workflows:

| Resource | From | To | Stock effect |
| --- | --- | --- | --- |
| Sales order | `DRAFT` | `CONFIRMED` | Deduct stock |
| Sales order | `DRAFT` | `CANCELLED` | None |
| Sales order | `CONFIRMED` | `DELIVERED` | None |
| Purchase order | `DRAFT` | `ORDERED` | None |
| Purchase order | `DRAFT` | `CANCELLED` | None |
| Purchase order | `ORDERED` | `RECEIVED` | Add stock |
| Purchase order | `ORDERED` | `CANCELLED` | None |

- Sales confirmation deducts stock. Delivery records completion only and has no
  additional stock effect.
- Marking a purchase as ordered has no stock effect and makes its commercial
  fields read-only. Receipt increases stock.
- A sales order may be cancelled only from `DRAFT` because confirmation has
  already changed stock.
- A purchase order may be cancelled from `DRAFT` or `ORDERED` because neither
  state has changed stock.
- `DELIVERED` sales and `RECEIVED` purchases are immutable terminal states.

Before implementation, `REQUIREMENTS.md`, `DATABASE.md`, and
`BUSINESS_RULES.md` must be updated to include `DELIVERED` and `ORDERED`. In
particular, this decision extends or supersedes BR-58, BR-62, BR-65, BR-71,
BR-72, BR-73, BR-74, and BR-76 only where those rules describe statuses and
delivery/ordering. All stock-integrity and immutability rules remain applicable.

## 2. General Conventions

### 2.1 Authentication and content types

- `POST /api/auth/login` is public.
- Every other endpoint requires `Authorization: Bearer <token>`.
- JSON requests use `Content-Type: application/json`.
- JSON responses use `Content-Type: application/json`.
- Errors use `Content-Type: application/problem+json`.
- Invoice downloads use `Content-Type: application/pdf`.
- There is one authenticated user type and no authorization role checks.

### 2.2 JSON conventions

- Properties use `camelCase`.
- Database identifiers are JSON integers.
- Dates use ISO 8601 `YYYY-MM-DD` strings.
- Timestamps use ISO 8601 UTC strings, for example
  `2026-08-16T14:30:00Z`.
- Monetary values and tax rates are JSON decimal numbers mapped to Java
  `BigDecimal`; they are never calculated with `double` or `float`.
- Quantities are positive whole JSON integers unless a signed movement delta is
  explicitly returned.
- Optional properties may be `null`. The API does not distinguish omitted and
  null optional fields on full-update requests.
- Unknown request properties should be rejected to detect frontend/backend
  contract drift early.

### 2.3 Successful response envelopes

A single JSON resource is wrapped in `data`:

```json
{
  "data": {
    "id": 42,
    "name": "Example"
  }
}
```

A paginated collection uses a zero-based page number:

```json
{
  "data": [],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

Collection endpoints accept `page` (default `0`) and `size` (default `20`,
maximum `100`). Invalid page or size values return `400`. Sorting is endpoint
specific and uses `sort=field,asc|desc`. Default sorting is stable and includes
`id` as a final tie-breaker internally.

Optional filters are combined with logical AND. An omitted filter does not
restrict results. Blank search text is treated as omitted after trimming. Date
ranges are inclusive and use the business date named by the endpoint.

### 2.4 Optimistic concurrency

Mutable resources expose a numeric `version`. Full updates and archive actions
must send the last version read by the client. A stale version returns
`409 VERSION_CONFLICT`, allowing TanStack Query to invalidate and reload the
resource instead of silently overwriting another update.

State-transition actions validate the current persisted state atomically. They
use an empty request body and return `409 INVALID_STATE_TRANSITION` if another
request has already changed the state.

## 3. Standard Error Response

All non-success JSON responses use this structure:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "detail": "One or more fields are invalid.",
  "instance": "/api/products",
  "timestamp": "2026-08-16T14:30:00Z",
  "traceId": "01J5A8Y7D7K4P3Q2M1N0",
  "fieldErrors": [
    {
      "field": "sellingPrice",
      "code": "PositiveOrZero",
      "message": "must be greater than or equal to 0"
    }
  ]
}
```

`fieldErrors` is an empty array for errors not tied to request fields. `detail`
must be safe to display and must not expose stack traces, SQL, credentials, or
token data.

### 3.1 Standard status codes

| Status | Meaning |
| --- | --- |
| `200 OK` | Read, update, or action completed |
| `201 Created` | Resource created; `Location` is returned when a canonical detail endpoint exists |
| `400 Bad Request` | Malformed JSON, invalid parameter, or field validation failure |
| `401 Unauthorized` | Missing, invalid, or expired token; also invalid login |
| `404 Not Found` | Requested resource does not exist |
| `409 Conflict` | Uniqueness, version, current-state, stock, archive, or other business conflict |
| `500 Internal Server Error` | Unexpected server failure with safe problem details |

Common error codes include `VALIDATION_ERROR`, `INVALID_CREDENTIALS`,
`UNAUTHENTICATED`, `RESOURCE_NOT_FOUND`, `DUPLICATE_RESOURCE`,
`VERSION_CONFLICT`, `INVALID_STATE_TRANSITION`, `INSUFFICIENT_STOCK`,
`INACTIVE_REFERENCE`, `RESOURCE_IN_USE`, `INVOICE_ALREADY_EXISTS`, and
`INTERNAL_ERROR`.

## 4. Shared Representations

The following shapes are referenced by endpoint definitions. Response-only
calculated and audit fields must not be accepted in write requests.

### 4.1 References and audit data

```json
{
  "id": 7,
  "name": "Office Supplies"
}
```

Named references use `id` and `name`. Product references additionally include
`sku`. User references include `id` and `username`.

```json
{
  "createdAt": "2026-08-16T14:30:00Z",
  "updatedAt": "2026-08-16T14:30:00Z",
  "version": 0
}
```

### 4.2 Category

`CategoryWriteRequest`:

```json
{
  "name": "Office Supplies",
  "description": "Paper, filing and desk supplies"
}
```

`CategoryUpdateRequest` adds the required `version`. `CategoryResponse` adds
`id`, `active`, `deactivatedAt`, and audit data.

### 4.3 Product

`ProductWriteRequest`:

```json
{
  "sku": "PAP-A4-80",
  "name": "A4 Paper 80 gsm",
  "categoryId": 7,
  "unitOfMeasure": "PACK",
  "purchasePrice": 42.50,
  "sellingPrice": 55.00,
  "minimumStock": 10
}
```

`ProductUpdateRequest` adds the required `version`. `ProductResponse` adds
`id`, a category reference, `currentStock`, `lowStock`, `active`,
`deactivatedAt`, and audit data. `currentStock`, `lowStock`, and `active` are not
accepted by product create/update endpoints.

### 4.4 Customer and supplier

`PartnerWriteRequest` is used as the shape for customer and supplier writes:

```json
{
  "name": "Atlas Services SARL",
  "ice": "001234567000089",
  "contactPerson": "Salma Idrissi",
  "email": "contact@atlas.example",
  "phone": "+212522000000",
  "address": "Casablanca, Morocco"
}
```

The corresponding update request adds `version`. `CustomerResponse` and
`SupplierResponse` add `id`, `active`, `deactivatedAt`, and audit data.

### 4.5 Archive request

```json
{
  "version": 3
}
```

`ArchiveRequest` is used for category, product, customer, and supplier archive
actions. Reactivation is not part of the MVP API.

### 4.6 Order write requests

`SalesOrderWriteRequest`:

```json
{
  "customerId": 15,
  "orderDate": "2026-08-16",
  "note": "Deliver during business hours",
  "lines": [
    {
      "productId": 42,
      "quantity": 3,
      "unitPrice": 55.00,
      "taxRate": 20.00
    }
  ]
}
```

`PurchaseOrderWriteRequest` has the same structure with `supplierId` replacing
`customerId`; `unitPrice` is the purchase price. Update requests add the order's
required `version`. Order number, status, line totals, header totals, stock, and
timestamps are server-managed and are not accepted.

### 4.7 Order responses

An order summary contains:

```json
{
  "id": 81,
  "orderNumber": "SO-2026-000081",
  "party": {
    "id": 15,
    "name": "Atlas Services SARL"
  },
  "orderDate": "2026-08-16",
  "status": "DRAFT",
  "subtotal": 165.00,
  "taxAmount": 33.00,
  "totalAmount": 198.00,
  "version": 0
}
```

`SalesOrderDetailResponse` uses `customer` instead of `party` and adds `note`,
`confirmedAt`, `deliveredAt`, `createdBy`, audit data, and `lines`.
`PurchaseOrderDetailResponse` uses `supplier` and adds `orderedAt`, `receivedAt`,
`createdBy`, audit data, and `lines`.

Each response line contains `id`, a product reference, `quantity`, `unitPrice`,
`taxRate`, `lineSubtotal`, `lineTax`, and `lineTotal`.

### 4.8 Invoice response

`InvoiceResponse` contains `id`, `invoiceNumber`, `salesOrderId`,
`salesOrderNumber`, `issueDate`, the customer snapshot, `subtotal`, `taxAmount`,
`totalAmount`, `createdBy`, and `createdAt`. Invoice detail additionally includes
snapshot lines with `lineNumber`, `productSku`, `productName`, `unitOfMeasure`,
`quantity`, `unitPrice`, `taxRate`, `lineSubtotal`, `lineTax`, and `lineTotal`.

## 5. Authentication Endpoints

| Method and path | Purpose | Request body | Response body | Validation | Status codes | Business rules |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /api/auth/login` | Authenticate and issue a JWT. | `{ "login": "admin", "password": "..." }` | `data`: `{ "accessToken", "tokenType": "Bearer", "expiresAt", "user": { "id", "username", "email" } }` | `login` and `password` required and non-blank; inactive and unknown users return the same error. | `200`, `400`, `401`, `500` | BR-01 to BR-05, BR-07, BR-18 |
| `GET /api/auth/me` | Return the current authenticated user. | None | `data`: `{ "id", "username", "email", "active" }` | Valid JWT and active current user required. | `200`, `401`, `500` | BR-01, BR-04, BR-05 |

There is no backend logout, registration, role, permission, or token-refresh
endpoint in the MVP (BR-05 and BR-06).

## 6. Category Endpoints

| Method and path | Purpose | Request body | Response body | Validation | Status codes | Business rules |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/categories` | List and search categories. Query: `page`, `size`, `search`, `active`, `sort=name,asc`. | None | Paginated `CategoryResponse` list. | `search` max 120 characters; `active` boolean; sort fields: `name`, `createdAt`. | `200`, `400`, `401` | BR-07, BR-08, BR-21, BR-28, BR-85, BR-86 |
| `GET /api/categories/{id}` | Get category details. | None | `data`: `CategoryResponse`. | Positive numeric `id`. | `200`, `400`, `401`, `404` | BR-21, BR-28, BR-86 |
| `POST /api/categories` | Create an active category. | `CategoryWriteRequest` | `data`: created `CategoryResponse`. | Name required, trimmed, max 120 and case-insensitively unique; description max 500. | `201`, `400`, `401`, `409` | BR-07, BR-08, BR-21 |
| `PUT /api/categories/{id}` | Fully update a category. | `CategoryUpdateRequest` | `data`: updated `CategoryResponse`. | Create validation plus matching current `version`; inactive category may still be renamed. | `200`, `400`, `401`, `404`, `409` | BR-07, BR-08, BR-21, BR-87 |
| `POST /api/categories/{id}/archive` | Archive a category. | `ArchiveRequest` | `data`: updated `CategoryResponse`. | Version required; category must be active; archive does not alter products. | `200`, `400`, `401`, `404`, `409` | BR-21, BR-28, BR-85 to BR-88, BR-94 |

## 7. Product Endpoints

| Method and path | Purpose | Request body | Response body | Validation | Status codes | Business rules |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/products` | Paginate, search, and filter products. Query: `page`, `size`, `search`, `categoryId`, `active`, `lowStock`, `sort=name,asc`. `search` matches name or SKU/reference case-insensitively. `lowStock=true` is the low-stock product listing. | None | Paginated `ProductResponse` list. | `categoryId` positive; `active` and `lowStock` boolean; `search` max 180; sort fields: `name`, `sku`, `currentStock`, `createdAt`. | `200`, `400`, `401` | BR-23, BR-28, BR-40, BR-85, BR-86 |
| `GET /api/products/{id}` | Get one product including current stock. | None | `data`: `ProductResponse`. | Positive numeric `id`. | `200`, `400`, `401`, `404` | BR-22 to BR-28, BR-40 |
| `POST /api/products` | Create an active zero-stock product. | `ProductWriteRequest` | `data`: created `ProductResponse` with `currentStock: 0`. | Required fields; active category; unique normalized SKU; supported unit; prices and minimum stock non-negative. | `201`, `400`, `401`, `404`, `409` | BR-19, BR-20, BR-22 to BR-26 |
| `PUT /api/products/{id}` | Fully update product master data without changing stock. | `ProductUpdateRequest` | `data`: updated `ProductResponse`. | Create validation plus current `version`; body cannot contain current stock; category must be active. | `200`, `400`, `401`, `404`, `409` | BR-19, BR-20, BR-22 to BR-28 |
| `POST /api/products/{id}/archive` | Archive a product. | `ArchiveRequest` | `data`: updated `ProductResponse`. | Version required; product must be active; no historical record is changed. | `200`, `400`, `401`, `404`, `409` | BR-28, BR-85 to BR-87, BR-89, BR-94 |

No product endpoint accepts a stock balance. Stock changes use section 11.

## 8. Customer Endpoints

| Method and path | Purpose | Request body | Response body | Validation | Status codes | Business rules |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/customers` | Paginate and search customers. Query: `page`, `size`, `search`, `active`, `sort=name,asc`. Search matches name, ICE, email, or phone. | None | Paginated `CustomerResponse` list. | `search` max 254; `active` boolean; sort fields: `name`, `createdAt`. | `200`, `400`, `401` | BR-29, BR-30, BR-85, BR-86 |
| `GET /api/customers/{id}` | Get customer details. | None | `data`: `CustomerResponse`. | Positive numeric `id`. | `200`, `400`, `401`, `404` | BR-29, BR-30, BR-33, BR-86 |
| `POST /api/customers` | Create an active customer. | `PartnerWriteRequest` | `data`: created `CustomerResponse`. | Name required/max 180; optional fields bounded; email valid when provided. | `201`, `400`, `401` | BR-07 to BR-10, BR-29, BR-30 |
| `PUT /api/customers/{id}` | Fully update a customer. | Partner update request with `version` | `data`: updated `CustomerResponse`. | Create validation plus current version; invoice snapshots remain unchanged. | `200`, `400`, `401`, `404`, `409` | BR-29, BR-30, BR-33, BR-81 |
| `POST /api/customers/{id}/archive` | Archive a customer. | `ArchiveRequest` | `data`: updated `CustomerResponse`. | Version required; customer must be active; related orders and invoices remain unchanged. | `200`, `400`, `401`, `404`, `409` | BR-31, BR-32, BR-85 to BR-87, BR-90, BR-94 |

## 9. Supplier Endpoints

| Method and path | Purpose | Request body | Response body | Validation | Status codes | Business rules |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/suppliers` | Paginate and search suppliers. Query: `page`, `size`, `search`, `active`, `sort=name,asc`. Search matches name, ICE, email, or phone. | None | Paginated `SupplierResponse` list. | `search` max 254; `active` boolean; sort fields: `name`, `createdAt`. | `200`, `400`, `401` | BR-34, BR-35, BR-85, BR-86 |
| `GET /api/suppliers/{id}` | Get supplier details. | None | `data`: `SupplierResponse`. | Positive numeric `id`. | `200`, `400`, `401`, `404` | BR-34, BR-35, BR-38, BR-86 |
| `POST /api/suppliers` | Create an active supplier. | `PartnerWriteRequest` | `data`: created `SupplierResponse`. | Name required/max 180; optional fields bounded; email valid when provided. | `201`, `400`, `401` | BR-07 to BR-10, BR-34, BR-35 |
| `PUT /api/suppliers/{id}` | Fully update a supplier. | Partner update request with `version` | `data`: updated `SupplierResponse`. | Create validation plus current version; existing order values remain unchanged. | `200`, `400`, `401`, `404`, `409` | BR-34, BR-35, BR-38 |
| `POST /api/suppliers/{id}/archive` | Archive a supplier. | `ArchiveRequest` | `data`: updated `SupplierResponse`. | Version required; supplier must be active; related purchase orders remain unchanged. | `200`, `400`, `401`, `404`, `409` | BR-36, BR-37, BR-85 to BR-87, BR-90, BR-94 |

## 10. Stock Movement Representations

`StockMovementResponse`:

```json
{
  "id": 1204,
  "product": {
    "id": 42,
    "sku": "PAP-A4-80",
    "name": "A4 Paper 80 gsm"
  },
  "movementType": "MANUAL_IN",
  "quantityDelta": 25,
  "stockBefore": 5,
  "stockAfter": 30,
  "source": null,
  "reason": "Opening stock count",
  "note": null,
  "createdBy": {
    "id": 1,
    "username": "admin"
  },
  "occurredAt": "2026-08-16T14:30:00Z"
}
```

For order-driven movements, `source` contains `documentType`, `documentId`,
`documentNumber`, and `lineId`.

`ManualStockAdjustmentRequest`:

```json
{
  "productId": 42,
  "direction": "IN",
  "quantity": 25,
  "reason": "Opening stock count",
  "note": null
}
```

## 11. Stock Movement Endpoints

| Method and path | Purpose | Request body | Response body | Validation | Status codes | Business rules |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/stock-movements` | List movement history. Query: `page`, `size`, `productId`, `type`, `dateFrom`, `dateTo`, `sort=occurredAt,desc`. | None | Paginated `StockMovementResponse` list. | Product ID positive; type in `SALE`, `PURCHASE`, `MANUAL_IN`, `MANUAL_OUT`; ISO dates; `dateFrom <= dateTo`; sort only by `occurredAt`. | `200`, `400`, `401` | BR-41 to BR-44, BR-47, BR-51, BR-52, BR-86 |
| `POST /api/stock-movements/adjustments` | Apply one manual stock adjustment. | `ManualStockAdjustmentRequest` | `data`: created `StockMovementResponse`. | Active existing product; direction `IN` or `OUT`; quantity positive; reason required/max 180; note max 1000; outbound result cannot be negative. | `201`, `400`, `401`, `404`, `409` | BR-13, BR-39, BR-41, BR-42, BR-44 to BR-52 |

Stock movements have no update, archive, or delete endpoint (BR-51 and BR-93).

## 12. Sales Order Endpoints

Allowed list statuses are `DRAFT`, `CONFIRMED`, `DELIVERED`, and `CANCELLED`.

| Method and path | Purpose | Request body | Response body | Validation | Status codes | Business rules |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/sales-orders` | List sales orders. Query: `page`, `size`, `search`, `status`, `customerId`, `dateFrom`, `dateTo`, `sort=orderDate,desc`. Search matches order number or customer name; dates filter `orderDate`. | None | Paginated sales order summaries. | Valid status; positive customer ID; ISO dates with valid range; search max 180; sort fields: `orderDate`, `orderNumber`, `totalAmount`, `status`. | `200`, `400`, `401` | BR-53 to BR-64, BR-86; API workflow decision supersedes BR-65 for delivery |
| `GET /api/sales-orders/{id}` | Get a sales order and all lines. | None | `data`: `SalesOrderDetailResponse`. | Positive numeric `id`. | `200`, `400`, `401`, `404` | BR-53 to BR-64, BR-86; API workflow decision supersedes BR-65 for delivery |
| `POST /api/sales-orders` | Create a draft sales order. | `SalesOrderWriteRequest` | `data`: created `SalesOrderDetailResponse`; `Location` identifies it. | Active customer; at least one line; active products; no duplicate product; positive quantities; valid prices/rates; note max 1000; totals recalculated. | `201`, `400`, `401`, `404`, `409` | BR-11 to BR-18, BR-31, BR-53 to BR-56, BR-64 |
| `PUT /api/sales-orders/{id}` | Replace editable data and lines of a draft. | Sales order update request including `version` | `data`: updated `SalesOrderDetailResponse`. | Current status must be `DRAFT`; current version; same validation as create. | `200`, `400`, `401`, `404`, `409` | BR-11 to BR-18, BR-31, BR-53 to BR-57, BR-64 |
| `POST /api/sales-orders/{id}/confirm` | Confirm the complete sale and deduct stock. | None | `data`: updated `SalesOrderDetailResponse` in `CONFIRMED`. | Must be `DRAFT`; customer/products active; valid lines/totals; sufficient stock for all lines under locks. | `200`, `400`, `401`, `404`, `409` | BR-32, BR-39 to BR-43, BR-47 to BR-50, BR-58 to BR-61 |
| `POST /api/sales-orders/{id}/cancel` | Cancel a draft with no stock effect. | None | `data`: updated `SalesOrderDetailResponse` in `CANCELLED`. | Must be `DRAFT`. | `200`, `401`, `404`, `409` | BR-58, BR-62, BR-63, BR-94 |
| `POST /api/sales-orders/{id}/deliver` | Mark a confirmed sale delivered. No stock is changed. | None | `data`: updated `SalesOrderDetailResponse` in `DELIVERED` with `deliveredAt`. | Must be `CONFIRMED`; order content remains immutable. | `200`, `401`, `404`, `409` | API workflow decision; preserves BR-41, BR-50, BR-62 and BR-93; extends BR-58 and BR-65 |

Calling an action in its target or another invalid state returns `409`; it is not
treated as a second successful action. This makes accidental duplicate requests
visible while database constraints still prevent duplicate stock effects.

## 13. Purchase Order Endpoints

Allowed list statuses are `DRAFT`, `ORDERED`, `RECEIVED`, and `CANCELLED`.

| Method and path | Purpose | Request body | Response body | Validation | Status codes | Business rules |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/purchase-orders` | List purchase orders. Query: `page`, `size`, `search`, `status`, `supplierId`, `dateFrom`, `dateTo`, `sort=orderDate,desc`. Search matches order number or supplier name; dates filter `orderDate`. | None | Paginated purchase order summaries. | Valid status; positive supplier ID; ISO dates with valid range; search max 180; sort fields: `orderDate`, `orderNumber`, `totalAmount`, `status`. | `200`, `400`, `401` | BR-66 to BR-75, BR-86; API workflow decision extends BR-71 to BR-74 |
| `GET /api/purchase-orders/{id}` | Get a purchase order and all lines. | None | `data`: `PurchaseOrderDetailResponse`. | Positive numeric `id`. | `200`, `400`, `401`, `404` | BR-66 to BR-75, BR-86; API workflow decision extends BR-71 to BR-74 |
| `POST /api/purchase-orders` | Create a draft purchase order. | `PurchaseOrderWriteRequest` | `data`: created `PurchaseOrderDetailResponse`; `Location` identifies it. | Active supplier; at least one line; active products; no duplicate product; positive quantities; valid prices/rates; note max 1000; totals recalculated. | `201`, `400`, `401`, `404`, `409` | BR-11 to BR-18, BR-36, BR-66 to BR-69 |
| `PUT /api/purchase-orders/{id}` | Replace editable data and lines of a draft. | Purchase order update request including `version` | `data`: updated `PurchaseOrderDetailResponse`. | Current status must be `DRAFT`; current version; same validation as create. | `200`, `400`, `401`, `404`, `409` | BR-11 to BR-18, BR-36, BR-66 to BR-70 |
| `POST /api/purchase-orders/{id}/order` | Mark a complete draft as sent/ordered. No stock is changed. | None | `data`: updated `PurchaseOrderDetailResponse` in `ORDERED` with `orderedAt`. | Must be `DRAFT`; active supplier/products; valid lines and recalculated totals. | `200`, `400`, `401`, `404`, `409` | API workflow decision; preserves BR-66 to BR-70 and extends BR-71 |
| `POST /api/purchase-orders/{id}/receive` | Receive the complete order and increase stock. | None | `data`: updated `PurchaseOrderDetailResponse` in `RECEIVED` with `receivedAt`. | Must be `ORDERED`; supplier/products active; valid lines/totals; all product updates locked and atomic. | `200`, `400`, `401`, `404`, `409` | BR-37, BR-39 to BR-43, BR-47 to BR-50, BR-72 to BR-74, extended for `ORDERED` |
| `POST /api/purchase-orders/{id}/cancel` | Cancel a draft or ordered purchase with no stock effect. | None | `data`: updated `PurchaseOrderDetailResponse` in `CANCELLED`. | Current status must be `DRAFT` or `ORDERED`; `RECEIVED` cannot be cancelled. | `200`, `401`, `404`, `409` | API workflow decision; preserves BR-74, BR-75, BR-93 and extends BR-71 |

Partial receipt is not supported. The `receive` action always applies every line
exactly once.

## 14. Dashboard Representations

`DashboardSummaryResponse`:

```json
{
  "activeProducts": 320,
  "activeCustomers": 74,
  "activeSuppliers": 18,
  "lowStockProducts": 12,
  "currentMonthSales": {
    "orderCount": 26,
    "totalAmount": 84500.00
  },
  "currentMonthPurchases": {
    "orderCount": 8,
    "totalAmount": 31200.00
  },
  "recentMovements": []
}
```

`MonthlySalesPoint` contains `month` (`YYYY-MM`), `orderCount`, and
`totalAmount`. `TopProductResponse` contains a product reference,
`quantitySold`, and `revenue`. Dashboard sales include both `CONFIRMED` and
`DELIVERED` orders because delivery is a later state of an already confirmed
sale; each order is counted once. Sales periods use `confirmedAt`, purchase
periods use `receivedAt`, and calendar boundaries use `Africa/Casablanca`.

## 15. Dashboard Endpoints

| Method and path | Purpose | Request body | Response body | Validation | Status codes | Business rules |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/dashboard/summary` | Return current master counts, low-stock count, current-month sales/purchases, and five recent movements. | None | `data`: `DashboardSummaryResponse`. | None beyond authentication. | `200`, `401`, `500` | BR-40 to BR-43, BR-52, BR-61, BR-73, BR-85, BR-86; API workflow decision |
| `GET /api/dashboard/monthly-sales` | Return one point per month for a year. Query: `year`, default current year. | None | `data`: `{ "year": 2026, "currency": "MAD", "months": [MonthlySalesPoint x 12] }`. Missing months contain zeros. | `year` from 2000 to 2100. | `200`, `400`, `401` | BR-11, BR-15, BR-61; API workflow decision for delivered sales |
| `GET /api/dashboard/top-products` | Rank sold products for a period. Query: `dateFrom`, `dateTo`, `limit` (default 5, max 20). Defaults to current month. | None | `data`: list of `TopProductResponse`, ordered by quantity sold descending then product ID. | ISO dates; `dateFrom <= dateTo`; `limit` 1-20. | `200`, `400`, `401` | BR-11, BR-15, BR-41, BR-43, BR-61, BR-86 |
| `GET /api/dashboard/low-stock-products` | Return the most urgent active low-stock products. Query: `limit` (default 10, max 100). | None | `data`: list of `ProductResponse`, ordered by `currentStock - minimumStock` ascending then name. | `limit` 1-100. | `200`, `400`, `401` | BR-28, BR-39, BR-40, BR-52, BR-85 |

Dashboard endpoints are read-only aggregations and are not paginated because
their result sizes are fixed or explicitly limited.

## 16. Invoice Endpoints

`InvoiceSummaryResponse` omits snapshot lines. `InvoiceDetailResponse` includes
them as defined in section 4.8.

| Method and path | Purpose | Request body | Response body | Validation | Status codes | Business rules |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/invoices` | List invoices. Query: `page`, `size`, `search`, `dateFrom`, `dateTo`, `sort=issueDate,desc`. Search matches invoice number, sales order number, or customer snapshot name; dates filter `issueDate`. | None | Paginated `InvoiceSummaryResponse` list. | ISO dates with valid range; search max 180; sort fields: `issueDate`, `invoiceNumber`, `totalAmount`. | `200`, `400`, `401` | BR-77 to BR-84, BR-86 |
| `GET /api/invoices/{id}` | View one invoice snapshot and its lines. | None | `data`: `InvoiceDetailResponse`. | Positive numeric `id`. | `200`, `400`, `401`, `404` | BR-77 to BR-83, BR-86 |
| `POST /api/sales-orders/{salesOrderId}/invoice` | Generate the one invoice allowed for an eligible sale. | None | `data`: created `InvoiceDetailResponse`; `Location` points to `/api/invoices/{id}`. | Sales order must be `CONFIRMED` or `DELIVERED`; no invoice may already exist; snapshot and totals created atomically. | `201`, `401`, `404`, `409` | BR-77 to BR-83; API workflow decision treats `DELIVERED` as an already confirmed sale |
| `GET /api/sales-orders/{salesOrderId}/invoice/download` | Download the generated invoice for a sales order. | None | PDF bytes with `Content-Disposition: attachment; filename="<invoice-number>.pdf"`. This endpoint does not generate or mutate an invoice. | Sales order and generated invoice must exist. | `200`, `401`, `404`, `500` | BR-78, BR-80 to BR-83 |

Invoice generation and download are separate so that a safe `GET` never creates
data. Invoices have no update, cancel, archive, or delete endpoint.

## 17. Endpoint Inventory

The final contract contains only the following operations:

| Module | Operations |
| --- | --- |
| Authentication | Login, current user |
| Categories | List, detail, create, update, archive |
| Products | List/filter, detail, create, update, archive |
| Customers | List/search, detail, create, update, archive |
| Suppliers | List/search, detail, create, update, archive |
| Stock movements | List/filter, manual adjustment |
| Sales orders | List, detail, create draft, update draft, confirm, cancel, deliver |
| Purchase orders | List, detail, create draft, update draft, mark ordered, receive, cancel |
| Dashboard | Summary, monthly sales, top products, low stock |
| Invoices | List, detail, generate for sale, download for sale |

There are deliberately no generic delete endpoints, bulk operations, role APIs,
stock-balance update endpoints, partial fulfillment endpoints, payment APIs, or
public registration endpoints.
