# QuickBite: JavaFX Food Ordering & Delivery Management System

**QuickBite** is a production-grade desktop food-ordering and delivery management application developed in Java and JavaFX. The project has been architected to showcase core academic topics: **Java Compilation & OOP**, **JavaFX GUI Engineering**, **Multithreading & Concurrency**, **SQLite/JDBC Relational Persistence**, and **JSON/External API Integration**.

---

## 1. Key Features & Role Dashboards

QuickBite features three role-based dashboards with independent workflows:

### 👤 Customer Features
- **Authentication**: Secure Login & Registration with input validation.
- **Restaurant & Menu Browser**: Browse restaurants (Italian, Burgers, Ramen, Salads) with dynamic category pills (`Pizza`, `Pasta`, `Burgers`, `Ramen`, `Salads`, `Dessert`, `Drinks`) and instant search.
- **Interactive Cart**: Live subtotal, delivery fee calculation, quantity stepper (+/-), and item removal.
- **Checkout Flow**: Select delivery address and payment methods (*Cash on Delivery*, *Credit/Debit Card*, *Mobile Banking*).
- **Live Order Tracking**: Real-time visual 6-stage lifecycle progress:
  $$\text{PLACED} \rightarrow \text{CONFIRMED} \rightarrow \text{PREPARING} \rightarrow \text{READY} \rightarrow \text{OUT\_FOR\_DELIVERY} \rightarrow \text{DELIVERED}$$
- **Smart Delivery Intelligence**: Real-time weather and road condition advisory fetched asynchronously via Java `HttpClient` from the Open-Meteo REST API.
- **Order History & Reviews**: Review previous orders, submit 1–5 star ratings and feedback to dynamically recalculate restaurant ratings.

### 🍳 Restaurant Admin Features
- **Incoming Orders Dispatch**: View real-time incoming orders and advance status (*Accept* $\rightarrow$ *Prepare* $\rightarrow$ *Ready*).
- **Menu Management (Full CRUD)**:
  - **Create**: Add new dishes with price, category, and description.
  - **Read**: Live table of all menu items.
  - **Update**: Edit dish pricing, descriptions, and categories.
  - **Delete**: Remove items with confirmation.
  - **Stock Toggle**: Instantly toggle availability between *In Stock* and *Sold Out*.
- **Revenue Statistics**: Live dashboard cards for *Total Orders*, *Total Revenue*, *Active Orders*, and *Average Rating*.
- **JSON Menu Import / Export**: Export full menus into formatted JSON and import external JSON menus into SQLite.

### 🛵 Delivery Staff Features
- **Assigned Orders Table**: View orders ready for courier pickup.
- **Status Progression**: Mark orders as *Picked Up (Out for Delivery)* and *Delivered*.
- **Synchronized Driver Pool**: Automatically releases the driver back into the shared pool upon delivery completion.
- **Delivery History**: Complete log of all past delivered orders.

### ⚡ Multithreading & Synchronization (Background Engine)
- **Shared Resource Synchronization**: `DeliveryDriverPool` protects driver allocation using `synchronized` blocks to eliminate race conditions.
- **Background Order Simulation**: `ExecutorService` with 4 worker threads manages order lifecycle progression without freezing the JavaFX UI.

---

## 2. Pre-Configured Demo Accounts

For fast grading and demonstration, click the **Quick Demonstration Logins** buttons on the login screen or enter:

| Role | Email | Password | Pre-Assigned Entity |
| :--- | :--- | :--- | :--- |
| **Customer** | `customer@quickbite.com` | `pass123` | Alice Customer |
| **Restaurant Admin** | `admin@quickbite.com` | `pass123` | Marco Rossi (Bella Italia Trattoria) |
| **Delivery Staff 1** | `driver1@quickbite.com` | `pass123` | Dave Rider (Motorbike) |
| **Delivery Staff 2** | `driver2@quickbite.com` | `pass123` | Elena Speed (Motorbike) |

---

## 3. Technology Stack & Architecture

- **Language**: Java 21+ / Java 26
- **GUI**: JavaFX (Controls, Graphics, Base, Layouts: `BorderPane`, `VBox`, `HBox`, `FlowPane`, `ScrollPane`, `TableView`)
- **Styling**: Custom CSS (`src/main/resources/css/style.css`)
- **Database**: SQLite 3 (`database/QuickBite.db`) via JDBC (`org.xerial:sqlite-jdbc`)
- **Concurrency**: `ExecutorService`, `Thread`, `Runnable`, `synchronized`, `Platform.runLater()`
- **JSON & API**: Java `HttpClient`, asynchronous `CompletableFuture`, custom `JsonUtil` + Google `Gson`
- **Architecture**: Strict **MVC + DAO + Service Layer**:
  - Keep SQL queries exclusively in DAO classes.
  - Keep business rules in Service classes.
  - Keep Controllers focused on event handling and GUI views.

```
QuickBite/
├── pom.xml                                 # Maven project descriptor
├── run.bat                                 # Windows batch quick launcher
├── run.ps1                                 # PowerShell quick launcher
├── README.md                               # Project documentation
├── PROJECT_REPORT.md                       # Comprehensive 20-section academic report
├── database/
│   └── QuickBite.db                        # Self-initializing SQLite database
├── src/main/java/com/quickbite/
│   ├── AppLauncher.java                    # JavaFX entry point launcher (bypasses module check)
│   ├── Main.java                           # Application entry point & graceful shutdown
│   ├── config/
│   │   └── DatabaseConfig.java             # SQLite connection & schema initialization
│   ├── model/
│   │   ├── User.java                       # Abstract base class (OOP Abstraction/Encapsulation)
│   │   ├── Customer.java                   # Concrete customer subclass (Polymorphic showDashboard)
│   │   ├── RestaurantAdmin.java            # Concrete admin subclass
│   │   ├── DeliveryStaff.java              # Concrete driver subclass
│   │   ├── Restaurant.java                 # Restaurant entity
│   │   ├── FoodItem.java                   # Food item entity
│   │   ├── CartItem.java                   # Active shopping cart item
│   │   ├── Order.java                      # Order header & lifecycle states
│   │   ├── OrderItem.java                  # Order line items
│   │   ├── Delivery.java                   # Delivery tracking entity
│   │   ├── Review.java                     # Customer ratings & reviews
│   │   └── SmartDeliveryInfo.java          # Weather & ETA DTO
│   ├── dao/
│   │   ├── UserDAO.java                    # PreparedStatements for User table
│   │   ├── RestaurantDAO.java              # Restaurant queries
│   │   ├── FoodItemDAO.java                # Menu item CRUD
│   │   ├── OrderDAO.java                   # Transactional order placement & stats
│   │   ├── DeliveryDAO.java                # Delivery assignment & status updates
│   │   └── ReviewDAO.java                  # Review creation & rating aggregation
│   ├── service/
│   │   ├── AuthService.java                # Authentication & validation
│   │   ├── MenuService.java                # Menu operations & JSON import/export
│   │   ├── OrderService.java               # Cart checkout & order processing
│   │   └── DeliveryService.java            # Driver lifecycle & delivery dispatch
│   ├── concurrency/
│   │   ├── DeliveryDriverPool.java         # Synchronized shared driver pool
│   │   ├── OrderProcessingSimulator.java   # ExecutorService background pipeline
│   │   └── ConcurrencyMonitorService.java  # Thread telemetry & event bus
│   ├── api/
│   │   ├── WeatherApiClient.java           # Java HttpClient for Open-Meteo REST API
│   │   └── SmartDeliveryService.java       # Async ETA & weather advisory
│   ├── controller/
│   │   ├── LoginView.java                  # Sign In & Registration GUI
│   │   ├── CustomerDashboardView.java      # Customer shopping, tracking & reviews
│   │   ├── RestaurantDashboardView.java    # Admin orders, menu CRUD & stats
│   │   ├── DeliveryDashboardView.java      # Courier delivery management
│   │   └── ConcurrencyMonitorView.java     # Live multithreading monitor
│   └── util/
│       ├── JsonUtil.java                   # Robust JSON serializer/deserializer
│       ├── AlertUtil.java                  # Standardized dialog helpers
│       ├── SessionContext.java             # Session state holder
│       └── DatabaseSeeder.java             # Mock database seeder
└── src/main/resources/
    └── css/
        └── style.css                       # Modern CSS stylesheet
```

---

## 4. How to Compile and Run

### Option A: Using the Quick Launcher Scripts (Recommended)

1. Open a terminal or PowerShell in the `QuickBite` directory:
   ```powershell
   cd C:\Users\User\.gemini\antigravity\scratch\QuickBite
   ```
2. Double click or execute `run.bat` or `run.ps1`:
   ```powershell
   .\run.bat
   ```
   *or*
   ```powershell
   .\run.ps1
   ```

### Option B: Using IntelliJ IDEA

1. Launch **IntelliJ IDEA**.
2. Click **File $\rightarrow$ Open...** and select `C:\Users\User\.gemini\antigravity\scratch\QuickBite`.
3. IntelliJ will automatically detect `pom.xml` and resolve JavaFX dependencies.
4. Navigate to `src/main/java/com/quickbite/Main.java`.
5. Right-click and choose **Run 'Main.main()'**.

---

## 5. End-to-End Demonstration Scenario for Evaluators

1. **Launch App**: Click `run.bat` or `run.ps1`. The SQLite database initializes and seeds automatically.
2. **Login as Customer**:
   - Enter `customer@quickbite.com` and password `pass123`. Click **Sign In**.
   - Browse menus across Bella Italia, Burger Bistro 99, Tokyo Ramen, and Green Garden Bowls.
   - Filter by categories (*Pizza*, *Burgers*, *Ramen*) or search for dishes.
   - Add dishes to the cart and notice live subtotal & fee updates.
3. **Checkout & Smart Weather API**:
   - Click **Proceed to Checkout**.
   - Check **Enable background asynchronous delivery simulation** and click **Place Order Now**.
   - The **Live Order Tracking** window opens. Observe the **Smart Delivery Intelligence** box: real-time weather and estimated delivery duration are fetched in the background without freezing the GUI.
4. **Restaurant Admin Actions**:
   - Log out and enter `admin@quickbite.com` and password `pass123`. Click **Sign In**.
   - View the incoming order in the table. Click **Start Preparing**, then **Ready for Delivery**.
   - Switch to the **Menu Management** tab. Add a new dish, edit an item, or export/import menu items via JSON.
5. **Delivery Staff Actions**:
   - Log out and enter `driver1@quickbite.com` and password `pass123`. Click **Sign In**.
   - Select the assigned delivery. Click **Picked Up**, then **Mark Delivered**. Notice the driver is released back to the available pool.
6. **Customer Review**:
   - Log back into Customer (`customer@quickbite.com`). Go to **📦 My Orders**. Click **Rate & Review** and submit a 5-star review. Notice the restaurant's average rating updates immediately.

---

## 6. Academic Syllabus Mapping

| Course Topic | Implementation in QuickBite |
| :--- | :--- |
| **C vs Java Compilation** | Source code `.java` compiles via `javac` into bytecode `.class` executed platform-independently on the JVM. (Detailed in [PROJECT_REPORT.md](file:///C:/Users/User/.gemini/antigravity/scratch/QuickBite/PROJECT_REPORT.md)). |
| **JVM / JDK / JRE** | Utilizes JDK 21+ compiler, standard JRE runtime libraries, garbage collection, and Just-In-Time (JIT) compilation. |
| **Java OOP & Inheritance** | Base abstract class `User` extended by `Customer`, `RestaurantAdmin`, and `DeliveryStaff`. Protected fields, getters/setters (encapsulation). |
| **Polymorphism** | Abstract method `public abstract void showDashboard(Stage stage)` overridden across user subclasses to route dashboards polymorphically. |
| **JavaFX GUI** | Layouts (`BorderPane`, `FlowPane`, `TableView`, `VBox`, `HBox`), event handlers, responsive tables, modals, and CSS styling (`style.css`). |
| **Thread & Runnable** | `OrderProcessingSimulator` submits asynchronous `Runnable` tasks to background threads. |
| **ExecutorService** | Reusable 4-worker daemon thread pool (`Executors.newFixedThreadPool(4)`). |
| **Synchronization** | `DeliveryDriverPool` uses `synchronized` methods to protect shared driver availability and eliminate race conditions during concurrent order placement. |
| **UI Thread Safety** | `Platform.runLater()` safely updates JavaFX UI components from background threads. |
| **SQLite & JDBC** | 7 relational tables, foreign key enforcement (`PRAGMA foreign_keys = ON;`), and `PreparedStatement` parameterized queries. |
| **Transactions & ACID** | `OrderDAO.createOrder()` uses `conn.setAutoCommit(false)`, batch item insertion, commit, and rollback on error. |
| **JSON & External API** | Asynchronous HTTP GET requests via `java.net.http.HttpClient` to the Open-Meteo REST API, JSON serialization, and menu import/export. |
