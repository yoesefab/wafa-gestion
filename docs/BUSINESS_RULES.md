# WAFA Gestion - Business Rules

## 1. Purpose

This document defines the business rules for the WAFA Gestion MVP. The rules are
derived from `REQUIREMENTS.md` and `DATABASE.md` and are intended to guide API
behavior, service-layer validation, user-interface behavior, and acceptance
tests.

Rules use stable identifiers in the form `BR-XX`. Unless stated otherwise, a
rule must be enforced by the backend even when the frontend also validates it.

## 2. Authentication

- **BR-01:** Only an active user with valid credentials may authenticate.
- **BR-02:** A user may authenticate with either their username or email. Both
  identifiers are matched case-insensitively and must be unique.
- **BR-03:** Passwords must never be stored or logged in plain text. Only a
  secure one-way password hash may be persisted.
- **BR-04:** Successful authentication issues a time-limited JWT. Every business
  operation requires a valid, unexpired token.
- **BR-05:** All authenticated users have the same access to every application
  module. The MVP has no roles, permissions, public registration, or privilege
  levels.
- **BR-06:** Signing out removes the locally held authentication token. The MVP
  does not require server-side token revocation or refresh-token storage.

## 3. Common Validation and Calculations

- **BR-07:** Required text values must be trimmed and must contain at least one
  non-whitespace character.
- **BR-08:** User-provided text must respect the maximum lengths defined by the
  API and database schema. Values that exceed those limits must be rejected, not
  silently truncated.
- **BR-09:** An email address is optional for customers and suppliers, but when
  supplied it must have a valid email format. A user account email is required.
- **BR-10:** Phone numbers and ICE identifiers are stored as text so that leading
  zeros and formatting are preserved. The MVP does not perform external ICE or
  phone-number verification.
- **BR-11:** Monetary values use Moroccan dirhams (`MAD`) and decimal arithmetic.
  Prices, subtotals, taxes, and totals must never use binary floating-point
  calculations.
- **BR-12:** Unit prices and monetary totals must be zero or positive. Tax rates
  must be between `0.00` and `100.00`, inclusive.
- **BR-13:** Stock and order quantities are whole units. An order-line quantity
  must be greater than zero; a stock movement quantity must be non-zero.
- **BR-14:** Monetary calculations use the following formulas:
  `line subtotal = quantity x unit price`,
  `line tax = line subtotal x tax rate / 100`, and
  `line total = line subtotal + line tax`.
- **BR-15:** Tax values are rounded to two decimal places using
  `RoundingMode.HALF_UP`. Document subtotals, tax amounts, and totals are sums of
  the backend-calculated line values.
- **BR-16:** Values calculated by the backend, including stock balances and
  document totals, must not be accepted as authoritative input from the client.
- **BR-17:** Product SKUs, category names, usernames, and emails are compared
  case-insensitively for uniqueness. Sales order, purchase order, and invoice
  numbers must also be unique and immutable.
- **BR-18:** Validation errors must identify the rejected field or business
  operation without exposing passwords, tokens, or internal exception details.

## 4. Categories and Products

- **BR-19:** Every product must belong to exactly one category.
- **BR-20:** A new product may be assigned only to an active category.
- **BR-21:** A category name must be unique after trimming and
  case-normalization.
- **BR-22:** Every product must have a unique SKU/reference, a name, a supported
  unit of measure, a purchase price, a selling price, and a minimum stock
  threshold.
- **BR-23:** Product SKU uniqueness is evaluated after trimming and
  case-normalization. An existing SKU cannot be reused by a second product,
  including when the original product is inactive.
- **BR-24:** Purchase price, selling price, and minimum stock threshold must be
  zero or positive.
- **BR-25:** A newly created product always starts with a current stock of zero.
  Opening stock must be recorded later as a manual inbound stock movement.
- **BR-26:** Current stock is read-only in normal product creation and update
  operations. It may change only through the inventory workflows in section 7.
- **BR-27:** A product's current default purchase or selling price may be edited.
  The change applies only to future order lines and must not change prices stored
  on existing orders or invoices.
- **BR-28:** An inactive product cannot be selected for a new order line. An
  inactive category cannot be assigned to a new or edited product, but
  deactivating a category does not automatically deactivate its existing
  products.

## 5. Customers

- **BR-29:** A customer must have a non-blank person or company name.
- **BR-30:** ICE, contact person, email, phone, and address are optional customer
  details and are validated only when provided.
- **BR-31:** Only an active customer may be selected for a new sales order.
- **BR-32:** A draft sales order cannot be confirmed if its customer has become
  inactive since the draft was created.
- **BR-33:** Editing customer data affects future use of that customer but must
  not alter an already generated invoice snapshot.

## 6. Suppliers

- **BR-34:** A supplier must have a non-blank person or company name.
- **BR-35:** ICE, contact person, email, phone, and address are optional supplier
  details and are validated only when provided.
- **BR-36:** Only an active supplier may be selected for a new purchase order.
- **BR-37:** A draft purchase order cannot be received if its supplier has
  become inactive since the draft was created.
- **BR-38:** Editing supplier data must not change the supplier reference, line
  values, or totals stored on an existing purchase order.

## 7. Inventory and Stock Movements

- **BR-39:** Current stock must never be negative.
- **BR-40:** A product is considered low stock when
  `current stock <= minimum stock threshold`.
- **BR-41:** Every stock change must create exactly one immutable stock movement
  per affected product and source line. Direct stock edits are prohibited.
- **BR-42:** A movement records the product, signed quantity delta, balance before
  the movement, balance after the movement, movement type, timestamp, creator,
  and its source when applicable.
- **BR-43:** A `SALE` movement has a negative quantity and references exactly one
  sales order line. A `PURCHASE` movement has a positive quantity and references
  exactly one purchase order line.
- **BR-44:** A `MANUAL_IN` movement has a positive quantity and a `MANUAL_OUT`
  movement has a negative quantity. Both require a reason and have no order-line
  source.
- **BR-45:** A manual adjustment may be created only for an active product. Its
  quantity is entered as a positive magnitude; the selected direction determines
  the stored sign.
- **BR-46:** A manual outbound adjustment must be rejected when the requested
  quantity is greater than the current stock.
- **BR-47:** For every movement,
  `stock after = stock before + quantity delta`, and both balances must be zero
  or positive.
- **BR-48:** Stock validation, product balance updates, movement creation, and
  the related order status change must commit in one database transaction. A
  failure must roll back the complete operation.
- **BR-49:** Concurrent operations affecting the same product must be serialized
  with product-row locking so that both operations cannot spend the same stock.
- **BR-50:** Repeating a sales confirmation or purchase receipt must not create
  duplicate movements or apply stock twice.
- **BR-51:** Stock movements cannot be edited, archived, or deleted. An error in
  stock is corrected by a new, explained manual movement, not by rewriting
  history.
- **BR-52:** For each product, current stock must equal the sum of all its stock
  movement deltas. Any mismatch is a data-integrity incident requiring
  investigation.

## 8. Sales Orders

- **BR-53:** A sales order is created in `DRAFT` status with a unique, immutable
  order number, an active customer, an order date, and at least one line.
- **BR-54:** Each sales line contains one active product, a positive whole-unit
  quantity, a non-negative unit selling price, a tax rate, and calculated totals.
- **BR-55:** The same product may appear at most once in a sales order. The user
  must update the existing line quantity instead of adding a duplicate line.
- **BR-56:** A product's current selling price supplies the initial line price.
  The saved line price is a historical transaction value and does not change
  when the product price changes.
- **BR-57:** Only a `DRAFT` sales order may have its customer, date, note, or lines
  edited.
- **BR-58:** The only allowed sales status transitions are `DRAFT` to
  `CONFIRMED` and `DRAFT` to `CANCELLED`.
- **BR-59:** Confirming a sales order requires an active customer, at least one
  valid line, active products, valid recalculated totals, and sufficient current
  stock for every line.
- **BR-60:** Stock sufficiency must be evaluated for the complete sales order
  while all affected product rows are locked. If any product has insufficient
  stock, no stock or status change may be committed.
- **BR-61:** Confirming a sales order decreases stock once for each line, creates
  one `SALE` movement per line, changes the status to `CONFIRMED`, and records the
  confirmation timestamp in the same transaction.
- **BR-62:** A `CONFIRMED` sales order and its lines are read-only and cannot be
  cancelled or deleted in the MVP.
- **BR-63:** A `CANCELLED` sales order is read-only, has no stock effect, and
  cannot return to `DRAFT`.
- **BR-64:** The MVP does not reserve stock for draft sales orders. Availability
  is checked only when confirmation is requested.
- **BR-65:** Partial confirmation, backorders, delivery processing, returns, and
  post-confirmation reversal are outside the MVP.

## 9. Purchase Orders

- **BR-66:** A purchase order is created in `DRAFT` status with a unique,
  immutable order number, an active supplier, an order date, and at least one
  line.
- **BR-67:** Each purchase line contains one active product, a positive
  whole-unit quantity, a non-negative unit purchase price, a tax rate, and
  calculated totals.
- **BR-68:** The same product may appear at most once in a purchase order. The
  user must update the existing line quantity instead of adding a duplicate line.
- **BR-69:** A product's current purchase price supplies the initial line price.
  The saved line price is a historical transaction value and does not change
  when the product price changes.
- **BR-70:** Only a `DRAFT` purchase order may have its supplier, date, note, or
  lines edited.
- **BR-71:** The only allowed purchase status transitions are `DRAFT` to
  `RECEIVED` and `DRAFT` to `CANCELLED`.
- **BR-72:** Receiving a purchase order requires an active supplier, at least one
  valid line, active products, and valid recalculated totals.
- **BR-73:** Receiving a purchase order increases stock once for each line,
  creates one `PURCHASE` movement per line, changes the status to `RECEIVED`, and
  records the receipt timestamp in the same transaction.
- **BR-74:** A `RECEIVED` purchase order and its lines are read-only and cannot be
  cancelled or deleted in the MVP.
- **BR-75:** A `CANCELLED` purchase order is read-only, has no stock effect, and
  cannot return to `DRAFT`.
- **BR-76:** Partial receipt, supplier invoices, purchase returns, and
  post-receipt reversal are outside the MVP.

## 10. Invoices

- **BR-77:** An invoice may be generated only from a `CONFIRMED` sales order.
- **BR-78:** A sales order may have at most one invoice, and every invoice must
  reference exactly one sales order.
- **BR-79:** Invoice generation assigns a unique, immutable invoice number and an
  issue date. Number generation must be concurrency-safe; it must not use
  `MAX(number) + 1`.
- **BR-80:** An invoice copies the customer identity and contact details, product
  SKU, product name, unit of measure, quantities, prices, tax rates, and totals
  from the confirmed sale at generation time.
- **BR-81:** Invoice values are snapshots. Later changes to customers, products,
  prices, or category data must not alter an existing invoice.
- **BR-82:** An invoice and all its lines are immutable and cannot be deleted,
  cancelled, or regenerated with different data in the MVP.
- **BR-83:** Generating or printing an invoice has no additional stock effect.
  Stock was already deducted when the sales order was confirmed.
- **BR-84:** Invoice payment status, credit notes, refunds, supplier invoices,
  legal e-invoicing, and accounting entries are outside the MVP.

## 11. Archive and Deletion Behavior

- **BR-85:** Categories, products, customers, suppliers, and users are archived
  by setting them inactive. Archiving preserves their identifiers and historical
  relationships.
- **BR-86:** An inactive master record cannot be selected for a new transaction,
  but it remains visible when viewing existing orders, stock movements, and
  invoices.
- **BR-87:** Archiving a master record must not cascade to or modify any related
  record.
- **BR-88:** A category may be physically deleted only when it has never been
  referenced by a product; otherwise it must be archived.
- **BR-89:** A product may be physically deleted only when it has never been
  referenced by an order or stock movement and its current stock is zero;
  otherwise it must be archived.
- **BR-90:** A customer or supplier may be physically deleted only when it has
  never been referenced by a transaction; otherwise it must be archived.
- **BR-91:** A user referenced by audit or movement history must not be physically
  deleted and may only be deactivated.
- **BR-92:** Draft orders should normally be cancelled. Physical deletion is
  permitted only before the draft has produced any external effect, and its owned
  draft lines must be removed with it.
- **BR-93:** Confirmed sales orders, received purchase orders, their lines, stock
  movements, invoices, and invoice lines are permanent history and must never be
  physically deleted.
- **BR-94:** Every destructive action exposed by the user interface requires an
  explicit confirmation from the user. Confirmation does not override any rule
  that prohibits deletion.

## 12. MVP Boundary

These rules deliberately support one user type, one stock balance per product,
one currency, whole-unit quantities, full order confirmation/receipt, and simple
printable invoices. Features such as roles, multiple warehouses, reservations,
partial fulfillment, returns, payments, credit notes, accounting, and a complete
edit audit log require new business rules and are not implicit in this document.
