# MealSynx V2 — Autonomous Campus Dining, Demand Forecasting & Waste Optimization Platform

**Author:** D Raghuram 

> **Download Updated V2 Archive & Resources:** [MealSynx V2 Google Drive Folder](https://drive.google.com/drive/folders/1WU5mLa1a5kv9KuL4wuhrsInbJYXuqK_C)

## Technology Stack

| Layer | Technologies Used | 
| ----- | ----- | 
| **Core Backend & Concurrency** | Java 21 LTS, `ExecutorService`, `ArrayBlockingQueue`, Embedded HTTP JSON REST Server (Port `8080`) | 
| **Desktop UI** | Java Swing (`MealSynxDesktopApp`) + Console CLI (`MealSynxConsoleCLI`) | 
| **Database & Persistence** | MySQL 8.4 LTS (`mealsynxv2db`), JDBC Connector/J 8.4, Custom Thread-Safe Connection Pool, `PersistentDataStore` (`.dat` failover) | 
| **Security** | Salted SHA-256 Cryptographic Hashing (`SecureRandom` + `MessageDigest.isEqual`) | 
| **Mobile & Big Data Layers** | Kotlin 1.9 (`MealSynxDtos`, `MealSynxAsyncApiClient`, `QrPaymentManager`), Scala (`DemandSparkJob` / Apache Spark) | 
| **Analytics & Intelligence** | Strategy Design Pattern (What-If Simulator + NL Parser), 5-Aspect NLP Sentiment Engine, Automated Monthly Payroll | 

## Database Configuration (`mealsynxf4a` / `mealsynxv2db`)

The SQL setup script is included in the repository at **`sql/MealSynxV2.sql`**.

* **MySQL Workbench Connection Name:** `mealsynxf4a or your connection name`

* **Database / Schema Name:** `mealsynxv2db or use any (just change it in sql too)`

* **Default Host / Port / User:** `localhost:3306` | `root`

### How to Configure or Customize

1. Run `sql/mealsynxf4a_mysql84_setup.sql` in MySQL Workbench (or let the app auto-bootstrap the tables on startup).

2. Open **`src/main/resources/application.properties`** (or **`src/main/java/com/mealsynx/db/DatabaseConfig.java`**) and update your MySQL password and optional defaults:

   ```properties
   db.connection.name=mealsynxf4a
   db.url=jdbc:mysql://localhost:3306/mealsynxv2db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
   db.username=root
   db.password=ENTER_YOUR_PASSWORD_HERE
   
   # Optional: Customize initial Admin & Default Canteen before first launch
   admin.default.username=admin
   admin.default.password=admin123*
   canteen.default.id=CAN-1
   canteen.default.name=Canteen 1
   ```

   *(If MySQL is not running locally, MealSynx V2 automatically falls back to `PersistentDataStore` so the app still runs without errors.)*

## Key Features Added in V2

1. **Salted SHA-256 Password & Recovery Hashing (`PasswordUtil`)**: All passwords and security-question answers are stored as salted SHA-256 hashes (`salt:hash`) instead of plain text.

2. **Clean-Slate Startup & Full Canteen/Worker CRUD**: Starts with only **1 Admin account** and **1 default Canteen (`Canteen 1`)**—zero pre-seeded demo students, workers, menus, inventory, or calendar events. Admin can add, rename (inline or via button), and remove canteens and workers.

3. **Security-Question Recovery & Admin-Only Wallet Top-Up**: Students register with a security question (favourite colour, animal, movie, actor, or birth city) for self-service **Forgot Password?** recovery. New student wallets start at `₹0.00` and can only be credited by the Admin.

4. **Full Shopping Cart with Special Notes**: Students can add dishes to **🛒 My Cart**, adjust quantities (`+1` / `-1`), attach per-item and order-level cooking notes, and checkout.

5. **Live Counter POS Queue Integration**: Walk-in counter orders start as `Order Placed` in **📥 Orders Received** so workers advance them step-by-step (`Order Placed ➔ Preparing ➔ Prepared ➔ Collected`).

6. **Upgraded 5-Aspect NLP & Verified Review Rules**: Negation- and phrase-aware sentiment (`"bad taste"`, `"poor taste"`, `"not good"`, etc.) across *Taste, Spiciness, Quantity, Hygiene,* and *Service*. Students can only review dishes they have ordered and can only edit their own reviews inline.

7. **Targeted What-If Simulator + Natural Language Parser**: Simulate by **Canteen** and **Food/Dish**, or ask a natural language question directly in the simulator bar.

8. **Automated Monthly Worker Payroll & Net Profit**: Sends monthly salary due reminders to Admin, credits salaries with one click, notifies workers, and updates each canteen's wages and net profit.

9. **Worker NLP & Operational Improvement Advisor**: Displays actionable improvement tips per canteen based on negative student NLP reviews, logged kitchen waste/mistakes, and expiring stock.

## Quick Start & How It Works

1. **Run the App**: Open the `MealSynx` folder in **IntelliJ IDEA** (JDK 21), set your MySQL password in `application.properties`, and run `src/main/java/com/mealsynx/MealSynxApplication.java`.

2. **Admin Setup**: Tick **Admin** on the login screen and sign in (`admin` / `admin123*`). Rename `Canteen 1` or add more canteens, add workers (with monthly salary), and credit student wallets in **💳 Update Balance**.

3. **Worker Setup**: Tick **Worker** and sign in with the worker account created by Admin. Add menu items in **🍔 Update Menu & Kitchen Plan**, add stock in **📦 Warehouse Inventory**, and manage incoming orders in **📥 Orders Received**.

4. **Student Flow**: Tick **Student**, click **Create a new account** (with a security question), browse menus, add dishes with special notes to **🛒 My Cart**, checkout, track orders live, and submit verified reviews in **⭐ Ratings and Feedback**.

## MealSynx V1 vs MealSynx V2 Comparison

| Feature / Area | MealSynx V1 (Academic Demo) | MealSynx V2 (GitHub Edition) | 
| ----- | ----- | ----- | 
| **Password Storage** | Unhashed demo passwords | Salted SHA-256 (`salt:hash`) via `PasswordUtil` | 
| **Account Recovery** | Not available | Security-question **Forgot Password?** recovery | 
| **Initial Users & Login** | Pre-seeded demo students & workers (`Cant1workA`, etc.) + pre-filled login fields | Clean-slate: **Admin-only seed** (`admin`) & empty login fields | 
| **Canteens & Staff CRUD** | 3 hardcoded canteens; add-only staff | 1 renameable default canteen (`Canteen 1`); full **Add / Rename / Remove** for Canteens & Workers | 
| **Menu, Inventory & Calendar** | 30 pre-built dishes, pre-filled stock & pre-marked calendar events | Empty initial menu, empty warehouse & clean calendar (populated by Workers/Admin) | 
| **Student Wallet** | Started with ₹500.00 demo balance | Starts at ₹0.00; **Admin top-up only** | 
| **Ordering & Cart** | Direct checkbox checkout | Full **Shopping Cart** (`+1`/`-1`, remove, per-item & order notes) | 
| **Live Counter POS** | Marked directly as `Collected` | Queued as `Order Placed` for worker progression | 
| **NLP & Reviews** | Basic 3-aspect keyword check; pre-seeded ratings | 5-aspect negation-aware NLP; **0 initial ratings**; ordered-only & author-only editable reviews | 
| **What-If Simulator** | Global canteen scenarios only | Filter by **Canteen + Dish** or ask **Natural Language** questions | 
| **Payroll & Worker Advisor** | Manual wage entry | Automated monthly salary reminders, one-click payout & **NLP Worker Improvement Advisor** | 
| **Database Schema** | `mealsynx` | `mealsynxv2db` (`mealsynxf4a` connection) + `.dat` persistent failover |

---

> **Original Version (V1):** Looking for the initial academic release? Check out [MealSynx V1 on GitHub](https://github.com/D-Raghuram-f4a/mealsynx).
