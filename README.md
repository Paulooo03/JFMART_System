# JF MiniMart System

Ordering and delivery system for Joyce Felice MiniMart (formerly LDL Grocery). Hospitals order products, the courier delivers them, the client confirms receipt by scanning a QR code, and invoices are tracked with 30-day payment terms. The requirements are in `CPECAPS.txt` and the screen map is in `docs/screen-map.md`.

| Part | Folder | Stack |
|---|---|---|
| Android app (client + courier) | `app/` | Kotlin, Jetpack Compose |
| Server | `backend/` | Spring Boot, Kotlin, MariaDB/MySQL |
| Admin website | `admin-web/` | HTML, CSS and JavaScript, served by the server |

The admin works in a web browser: with the server running, open <http://localhost:8080> and sign in as `admin`. See [The admin website](#the-admin-website).

## What you need

- **Android Studio** (latest stable) with an emulator (Device Manager > Create Device, a recent Pixel is fine).
- **MySQL or MariaDB.** XAMPP works: install it and use its MySQL.
- **Git.**
- No separate JDK is needed. Android Studio ships one at `C:\Program Files\Android\Android Studio\jbr`.

## 1. Get the code

```powershell
git clone https://github.com/Paulooo03/JFMART_System.git
cd JFMART_System
```

Open the **project root** in Android Studio. It builds the app. Wait for the Gradle sync to finish.

The server is a separate Gradle build in `backend/`. You run it from a terminal, so you do not need to open it in the IDE.

## 2. Create the database (once)

Start MySQL (XAMPP Control Panel > Start next to MySQL), then:

```powershell
C:\xampp\mysql\bin\mysql.exe -u root
```
```sql
CREATE DATABASE jf_mart_db CHARACTER SET utf8mb4;
CREATE USER 'jfmart_app_user'@'localhost' IDENTIFIED BY '<choose-a-db-password>';
GRANT ALL PRIVILEGES ON jf_mart_db.* TO 'jfmart_app_user'@'localhost';
FLUSH PRIVILEGES;
```

The tables are created automatically when the server starts (Flyway migrations in `backend/src/main/resources/db/migration`).

## 3. Run the server

Use one PowerShell window and keep it open. The variables only last for that window.

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:DB_PASSWORD = "<the-db-password-from-step-2>"
$env:ADMIN_PASSWORD = "<choose-an-admin-password>"
$env:COURIER_PASSWORD = "<choose-a-courier-password>"
$env:JWT_SECRET = -join ((48..57)+(65..90)+(97..122) | Get-Random -Count 40 | ForEach-Object {[char]$_})
cd backend
.\gradlew.bat bootRun
```

- It is ready when you see `Started JfMartApplicationKt`. The progress bar staying at about 85% is normal.
- The admin and courier accounts (`admin` and `courier`) are created on the first start from the two passwords above. Changing the variables later does not change those passwords.
- A new `JWT_SECRET` on every start signs everyone out, which is fine for development.

## 4. Run the app

1. Start an emulator and wait for the home screen.
2. Let the emulator reach the server running on your PC:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080
   ```
   Run this again every time the emulator restarts.
3. Create the file `C:\Users\<you>\.gradle\gradle.properties` (not the one in the project) with:
   ```
   jfmartApiUrl=http://localhost:8080/
   ```
   Then **Sync Project with Gradle Files**. Without this line the app uses `http://10.0.2.2:8080/`, which works on some setups.
4. Press **Run**.

**Using a real phone instead:** put your PC's Wi-Fi address in that file (`jfmartApiUrl=http://192.168.1.50:8080/`), keep the phone on the same Wi-Fi, and allow Java through Windows Firewall on private networks.

## 5. Try the whole flow

1. In the app, **Create an account** as a client (a new hospital). It says the account is pending.
2. Sign in to the admin website and approve it under **Clients**, then sign in on the phone.
3. Order products, or request an item that is not listed.
4. On the admin website, open the order under **Orders**, price any requested items, confirm it, and schedule a delivery.
5. Sign in as `courier`: start preparing, mark out for delivery, show the QR code.
6. Sign in as the client again: open the order and scan the QR code (or use "Type the code instead").
7. The order becomes Delivered and an invoice appears under Account > Statement.

## The admin website

Start the server (step 3), then open <http://localhost:8080> in a browser. Sign in with the username `admin` and the `ADMIN_PASSWORD` you set. The pages are in `admin-web/` and the server copies them in when it starts, so **restart the server after changing anything in that folder**.

| Page | What the admin does there |
|---|---|
| Dashboard | See what needs attention: clients waiting for approval, new orders, confirmed orders without a delivery date, overdue balances |
| Clients | Approve or reject sign-ups, add or edit clients, enable or disable accounts, see balances and payment deadlines |
| Products | Add products, change prices, hide products from clients |
| Orders | Review an order: price requested items, change quantities, remove items, confirm or cancel, and schedule the delivery |
| Deliveries | See all deliveries, give waiting orders a delivery date, reschedule |
| Invoices & payments | Enter the sales invoice number, record payments, filter by client, status or overdue |
| Records | Pick a client to see their order history, invoices and payments, and download everything as an Excel file |

Notes:
- The admin site only talks to the server's `/api` addresses, and the login token is kept for the browser tab only.
- Text that clients type (hospital names, item names, notes) is always shown as plain text.
- A client can only be approved or added by the admin; clients cannot sign themselves in until then.

## Troubleshooting

| Problem | Fix |
|---|---|
| Admin site shows no data or sends you back to the login page | The token expired (it lasts 12 hours) or the server was restarted. Sign in again. If pages look old after you edited `admin-web/`, restart the server and press Ctrl+F5. |
| App says "Can't reach the server" | Is the server running? Run the `adb reverse` command again. Check `jfmartApiUrl`. In Logcat, search `JFMartApi` for the exact cause. |
| `bootRun` says "JAVA_HOME is not set" | Set `$env:JAVA_HOME` in the same window. |
| `Task 'bootRun' not found` | You are in the project root. Run `cd backend` first. |
| Server fails with "Connection refused" | MySQL is not running. Start it in XAMPP. |
| Server fails with "Access denied for user" | `DB_PASSWORD` does not match the database user's password. |
| XAMPP MySQL errors about "Aria" or crashed tables | Stop MySQL, then repair the system tables with `aria_chk -r` and `myisamchk -r` in `C:\xampp\mysql\data\mysql`. Back up `data` first. |
| Emulator freezes or the app will not install | Device Manager > Cold Boot Now, or Wipe Data. |

Plain HTTP is allowed in debug builds only. A release build needs the server behind HTTPS.
