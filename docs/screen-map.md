# JF MiniMart — Screen Map & Data Layout

Source of requirements: `../CPECAPS.txt`.

## 1. Status lifecycle

```
Order:    PENDING ─admin confirms/prices→ CONFIRMED ─courier→ PREPARING ─→ OUT_FOR_DELIVERY ─client scans QR→ DELIVERED
             └────────────── CANCELLED (admin, or client while PENDING)
Delivery: ASSIGNED → PREPARING → OUT_FOR_DELIVERY → DELIVERED | UNSUCCESSFUL(reason) → reschedule
On DELIVERED: delivered_at recorded, invoice created (due +30 days), shown in client history.
```

## 2. Schema changes (implemented in `backend/.../db/migration/V1__init.sql`)

- `users`: add `client_id` (FK, nullable; null for admin/courier) and `status` (`PENDING_APPROVAL` / `ACTIVE` / `DISABLED`).
- `clients_list`: add `delivery_address`; make `department` nullable.
- `inventory` → `products` (a price list; `quantity` / `last_shelved` optional).
- `orders`: add `reference_number` (unique, e.g. `JF-2026-000123`), `created_at`, `confirmed_at`, `delivered_at`, `cancelled_reason`.
- `order_items`: `unit_price` nullable (unlisted items await pricing); add `is_custom_request`.
- `deliveries`: add `qr_used_at`, `qr_expires_at`, `delivered_at`; make `qr_code_token` unique.
- `ldl_grocery` → `invoices`, with `sales_invoice_number`; `due_date = start_date + 30 days`.
- `payments`: unchanged. Outstanding balance is computed.
- One DB name everywhere: `jf_mart_db`.

## 3. Screens

### Auth (shared)
- **Login**: one screen, which routes by the role the backend returns. It replaces the separate Client and Courier logins.
- **Client Sign‑Up**: hospital/client name, contact person, number, email, delivery address, department (optional).
- **Pending Approval**: shown until the admin approves the account.

### Client: bottom nav Home · Order · Cart · Orders · Account
| Screen | Content |
|---|---|
| Home | Active orders with status chips, outstanding balance, next due date |
| Order (Catalog / Quotation) | Products with current prices, search, qty stepper, live estimated total, "Request unlisted item" |
| Request Item (bottom sheet) | Product name, qty, preferred unit, instructions |
| Cart | Priced lines + requested lines ("price TBD"), estimated total, special instructions, Submit |
| Order Confirmation | Reference number, summary, "View order" |
| Orders (History) | Filter by status; ref no., order date, delivered date, amount |
| Order Details | Items & qty, amounts, status timeline; **Scan QR** when Out for Delivery |
| QR Scanner | Camera (CameraX + ML Kit); a successful scan sets the order to Delivered |
| Account | Edit client info; **Statement** (invoices, due dates, paid/unpaid), which replaces the current Payments screen |

### Courier: bottom nav Deliveries · Profile
| Screen | Content |
|---|---|
| Deliveries | Assigned deliveries grouped Today / Upcoming / Done |
| Delivery Details | Client, address, scheduled date, items & qty; actions Preparing → Out for Delivery → **Show QR**; Mark Unsuccessful (reason) |
| Show QR | Full‑screen one‑time QR that updates to Delivered after the client scans it |
| Profile | Account info, logout |

### Admin (same screens whether in the app or on the web; location undecided)
- Dashboard: new orders, pending approvals, overdue balances
- Client Approvals
- Clients → Client Detail: info, addresses, order history, transactions, outstanding balance, **Export to Excel**
- Products: add/edit price, activate/deactivate
- Orders inbox → Order Review: edit items, price unlisted items, confirm/cancel, assign courier + schedule date
- Invoices & Payments: record payment, sales invoice number

## 4. Existing wireframes → target

| Existing file | Target |
|---|---|
| `client/ClientLoginScreen.kt`, `courier/CourierLoginScreen.kt` | merged into `ui/auth/LoginScreen` |
| `client/ClientDashboardScreen.kt` | Client Home |
| `client/ClientAvailableItemsScreen.kt` | Order (Catalog / Quotation) |
| `client/ClientCartScreen.kt` | Cart |
| `client/ClientOrderDetailsScreen.kt` | Order Details |
| `client/ClientPaymentsScreen.kt` | Statement, under Account |
| `client/ClientManageAccountScreen.kt` | Account |
| `courier/CourierDashboardScreen.kt` | Deliveries |
| `courier/CourierDeliveryDetailsScreen.kt` | Delivery Details |
| `courier/CourierProfile.kt` | Profile |
| *(new)* | Sign‑Up, Pending Approval, Request Item, Order Confirmation, Orders History, QR Scanner, Show QR, Admin screens |

## 5. Build order

1. ~~Restructure folders~~ (done).
2. ~~Backend skeleton: Gradle build, Flyway schema, security/JWT, auth + sign‑up/approval~~ (done, tested by hand).
3. ~~Products + orders API~~ (done, tested by hand); Android data layer; Client flow.
4. ~~Delivery + QR API~~ (done, tested by hand); Courier flow + client QR scanner in the app.
5. ~~Billing + Excel export~~ (done, tested by hand). Admin UI (once its location is decided).
