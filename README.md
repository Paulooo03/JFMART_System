# JF MiniMart System

Ordering and delivery system for Joyce Felice MiniMart (formerly LDL Grocery). Hospitals order products, the courier delivers them, the client confirms receipt by scanning a QR code, and invoices are tracked with 30-day payment terms. The requirements are in `CPECAPS.txt` and the screen map is in `docs/screen-map.md`.

| Part | Folder | Stack |
|---|---|---|
| Android app (client + courier) | `app/` | Kotlin, Jetpack Compose |
| Server | `backend/` | Spring Boot, Kotlin, MariaDB/MySQL |

The admin has no screens yet. Admin tasks (approving clients, pricing orders, scheduling deliveries, recording payments, Excel export) are done with the server's API, see [Admin tasks](#admin-tasks-without-an-admin-screen).

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
2. Approve it (see below), then sign in.
3. Order products, or request an item that is not listed.
4. As admin, price any requested items, confirm the order, and schedule a delivery.
5. Sign in as `courier`: start preparing, mark out for delivery, show the QR code.
6. Sign in as the client again: open the order and scan the QR code (or use "Type the code instead").
7. The order becomes Delivered and an invoice appears under Account > Statement.

## Admin tasks without an admin screen

In a second PowerShell window. Replace the `<...>` values.

```powershell
$base = "http://localhost:8080"
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType "application/json" -Body (@{ username = "admin"; password = "<admin-password>" } | ConvertTo-Json)
$a = @{ Authorization = "Bearer $($login.token)" }

# Clients waiting for approval, then approve one by its userId
Invoke-RestMethod -Uri "$base/api/admin/clients/pending" -Headers $a
Invoke-RestMethod -Method Post -Uri "$base/api/admin/clients/<userId>/approve" -Headers $a

# Add a product to the price list
Invoke-RestMethod -Method Post -Uri "$base/api/admin/products" -Headers $a -ContentType "application/json" -Body (@{ name = "Pork Liempo"; price = 320; unit = "kg" } | ConvertTo-Json)

# Pending orders; price a requested item; confirm; schedule delivery for today
Invoke-RestMethod -Uri "$base/api/admin/orders?status=PENDING" -Headers $a
Invoke-RestMethod -Method Put -Uri "$base/api/admin/orders/<orderId>/items/<itemId>" -Headers $a -ContentType "application/json" -Body (@{ unitPrice = 180 } | ConvertTo-Json)
Invoke-RestMethod -Method Post -Uri "$base/api/admin/orders/<orderId>/confirm" -Headers $a
Invoke-RestMethod -Method Post -Uri "$base/api/admin/orders/<orderId>/delivery" -Headers $a -ContentType "application/json" -Body (@{ scheduledDate = (Get-Date).ToString("yyyy-MM-dd") } | ConvertTo-Json)
```

Invoice and payment calls, client statements and the Excel export are `GET/POST /api/admin/invoices...` and `GET /api/admin/clients/{clientId}/export`.

## Troubleshooting

| Problem | Fix |
|---|---|
| App says "Can't reach the server" | Is the server running? Run the `adb reverse` command again. Check `jfmartApiUrl`. In Logcat, search `JFMartApi` for the exact cause. |
| `bootRun` says "JAVA_HOME is not set" | Set `$env:JAVA_HOME` in the same window. |
| `Task 'bootRun' not found` | You are in the project root. Run `cd backend` first. |
| Server fails with "Connection refused" | MySQL is not running. Start it in XAMPP. |
| Server fails with "Access denied for user" | `DB_PASSWORD` does not match the database user's password. |
| XAMPP MySQL errors about "Aria" or crashed tables | Stop MySQL, then repair the system tables with `aria_chk -r` and `myisamchk -r` in `C:\xampp\mysql\data\mysql`. Back up `data` first. |
| Emulator freezes or the app will not install | Device Manager > Cold Boot Now, or Wipe Data. |

Plain HTTP is allowed in debug builds only. A release build needs the server behind HTTPS.
