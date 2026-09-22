# QUICKBITE: JavaFX Food Ordering & Delivery Management System
## Comprehensive Project Report & Technical Manual

---

## 1. Introduction
In recent years, desktop software engineering has evolved from monolithic single-threaded designs toward modular, event-driven, multithreaded desktop applications. **QuickBite** is an enterprise-grade desktop food ordering and delivery management system developed in Java using JavaFX, SQLite via JDBC, multithreaded concurrency pools, and asynchronous external REST API integration. The system serves three key user personas: Customers, Restaurant Administrators, and Delivery Couriers, providing an end-to-end interactive workflow from menu discovery to doorstep delivery.

---

## 2. Problem Statement
Traditional student software demonstrations frequently isolate academic concepts into trivial command-line scripts—such as isolated multithreading exercises, simplistic relational databases, or plain GUI mockups with no persistence. Real-world commercial applications require:
1. Seamless integration of relational data persistence with foreign key integrity.
2. Responsive graphical user interfaces that never freeze during network latency or complex computational tasks.
3. Thread-safe coordination over scarce physical resources (such as delivery drivers).
4. Asynchronous ingestion of external JSON web services.

QuickBite was built specifically to solve this gap by integrating all major curriculum topics into a unified food delivery platform.

---

## 3. Objectives
- **Demonstrate OOP Mastery**: Apply abstraction, inheritance, polymorphism, and encapsulation across a unified user hierarchy.
- **Engineer a Responsive JavaFX GUI**: Construct a modern, intuitive desktop interface with CSS styling, custom tables, badges, dialogs, and real-time trackers.
- **Implement Robust Relational Persistence**: Maintain 7 relational SQLite tables using JDBC `PreparedStatement` queries and ACID transactions.
- **Solve Concurrency Problems**: Use `ExecutorService`, background worker threads, and thread-safe `synchronized` blocks to eliminate race conditions in delivery driver assignment.
- **Integrate Real-World REST APIs**: Query weather and traffic conditions asynchronously using Java 11+ `HttpClient` and parse JSON into domain DTOs.
- **Maintain Clear Separation of Concerns**: Enforce the **MVC + DAO + Service** architectural pattern.

---

## 4. Proposed System
QuickBite provides three specialized role dashboards accessed via a unified authentication system:
- **Customer Dashboard**: Restaurant discovery, real-time category filtering, dynamic shopping cart, checkout with multiple payment methods, live 6-stage order tracking, and 5-star customer reviews.
- **Restaurant Admin Dashboard**: Incoming order dispatching (*Accept* $\rightarrow$ *Prepare* $\rightarrow$ *Ready*), full CRUD menu management, real-time revenue analytics, and JSON menu import/export.
- **Delivery Staff Dashboard**: Active delivery assignments, route status advancement (*Picked Up* $\rightarrow$ *Delivered*), and completed delivery logs.
- **Concurrency & Multithreading Monitor**: Real-time visual dashboard showcasing active worker threads, lock states, and shared driver pool queues.

---

## 5. Technologies Used
- **Programming Language**: Java (JDK 21 / Java 26)
- **GUI Toolkit**: JavaFX (Controls, Graphics, Base, FXML-ready)
- **Styling**: JavaFX CSS (`style.css`)
- **Database**: SQLite 3 (`QuickBite.db`)
- **Database Driver**: Xerial SQLite-JDBC Driver (`org.xerial:sqlite-jdbc`)
- **Networking**: Java 11+ `java.net.http.HttpClient`
- **External Web Service**: Open-Meteo REST API (Real-time weather data)
- **JSON Serialization**: Custom robust `JsonUtil` + Google Gson
- **Build System**: Apache Maven (`pom.xml`)

---

## 6. C vs Java Compilation

Understanding the differences between compilation models is fundamental to modern computer science:

```
[ C Compilation Model - Platform Dependent ]
C Source (.c) ──> Preprocessor ──> Compiler ──> Assembler ──> Linker ──> Machine Binary (.exe/.out)
(Directly targets specific OS & CPU hardware architecture)

[ Java Compilation Model - "Write Once, Run Anywhere" ]
Java Source (.java) ──> javac Compiler ──> Platform-Independent Bytecode (.class)
                                                    │
                                           ┌────────┴────────┐
                                           ▼                 ▼
                                    JVM (Windows)       JVM (Linux/macOS)
                                      (JIT / HotSpot interpreter to Native Machine Code)
```

| Dimension | C Compilation Model | Java Compilation Model |
| :--- | :--- | :--- |
| **Output** | Native machine code (`.exe`, ELF binary). | Intermediate platform-neutral bytecode (`.class`). |
| **Portability** | Platform-dependent. Must recompile for each CPU/OS. | Fully portable across any platform hosting a standard JVM. |
| **Memory Management**| Manual via `malloc()` and `free()`. Vulnerable to leaks. | Automated via JVM Garbage Collector (GC). |
| **Pointers & Safety**| Raw memory pointers; risk of buffer overflows. | Secure object references without direct address manipulation. |
| **Execution** | Directly executed by the CPU hardware. | Interpreted and dynamically compiled by HotSpot JIT inside the JVM. |

---

## 7. JVM, JDK, and JRE Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ JDK (Java Development Kit)                                  │
│  - javac, javadoc, jar, jdb, debugging tools               │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ JRE (Java Runtime Environment)                          │ │
│ │  - Core Java Class Libraries (java.base, java.sql, etc.)│ │
│ │ ┌─────────────────────────────────────────────────────┐ │ │
│ │ │ JVM (Java Virtual Machine)                          │ │ │
│ │ │  - ClassLoader Subsystem (Loading, Linking, Init)   │ │ │
│ │ │  - Runtime Data Areas (Heap, Stack, Method Area)   │ │ │
│ │ │  - Execution Engine (Interpreter, JIT Compiler, GC) │ │ │
│ │ └─────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```
1. **JDK**: Contains the compiler (`javac`) and diagnostic tools necessary to author Java code.
2. **JRE**: Contains the core class libraries and execution environment needed to run compiled bytecode.
3. **JVM**: The abstract computing machine that executes bytecode instructions, performs Just-In-Time (JIT) optimization, and cleans unreferenced heap memory.

---

## 8. Object-Oriented Design Principles
QuickBite implements all four core OOP tenets:
1. **Abstraction**: The abstract class `User` establishes the template for all system accounts while hiding implementation specifics.
2. **Encapsulation**: Domain models (`User`, `Restaurant`, `FoodItem`, `Order`, `Delivery`, `Review`) maintain `private` fields accessible only through validated getters and setters.
3. **Inheritance**: `Customer`, `RestaurantAdmin`, and `DeliveryStaff` inherit common identity attributes (`id`, `name`, `email`, `password`, `phone`, `address`) from `User`.
4. **Polymorphism**: The abstract method `public abstract void showDashboard(Stage stage)` is implemented differently by each subclass:
   - `Customer.showDashboard()` launches `CustomerDashboardView`.
   - `RestaurantAdmin.showDashboard()` launches `RestaurantDashboardView`.
   - `DeliveryStaff.showDashboard()` launches `DeliveryDashboardView`.

---

## 9. System Architecture

```mermaid
flowchart TD
    subgraph UI ["Presentation Layer (JavaFX)"]
        LoginView["Login & Registration View"]
        CustView["Customer Dashboard & Cart"]
        AdminView["Restaurant Admin & Menu CRUD"]
        DelivView["Delivery Staff Dashboard"]
        MonitorView["Concurrency & Thread Monitor"]
    end

    subgraph ServiceLayer ["Service Layer"]
        AuthService["AuthService"]
        OrderService["OrderService"]
        MenuService["MenuService"]
        DelivService["DeliveryService"]
        SmartDeliv["SmartDeliveryService"]
    end

    subgraph ConcurrencyLayer ["Concurrency & Asynchronous Tasks"]
        ThreadPool["ExecutorService (4 Worker Threads)"]
        Sim["OrderProcessingSimulator"]
        DriverPool["DeliveryDriverPool (synchronized)"]
    end

    subgraph External ["External Services"]
        Http["Java HttpClient"]
        WeatherAPI["Open-Meteo Weather REST API"]
    end

    subgraph DataLayer ["Data Access Layer (DAO)"]
        UserDAO["UserDAO"]
        RestDAO["RestaurantDAO"]
        FoodDAO["FoodItemDAO"]
        OrderDAO["OrderDAO (Transactions)"]
        DelivDAO["DeliveryDAO"]
        RevDAO["ReviewDAO"]
    end

    subgraph Storage ["Persistence Layer"]
        DB[(SQLite Database: QuickBite.db)]
    end

    UI --> ServiceLayer
    ServiceLayer --> DataLayer
    ServiceLayer --> ConcurrencyLayer
    ConcurrencyLayer --> DataLayer
    ServiceLayer --> External
    External --> Http --> WeatherAPI
    DataLayer --> Storage
```

---

## 10. Use Case Diagram

```mermaid
flowchart LR
    Customer((Customer))
    Admin((Restaurant Admin))
    Driver((Delivery Staff))

    subgraph QuickBite ["QuickBite System"]
        UC1[Sign In / Register]
        UC2[Browse Restaurants & Menus]
        UC3[Manage Cart & Checkout]
        UC4[Live Order Tracking & Weather ETA]
        UC5[Submit Ratings & Reviews]
        
        UC6[Manage Food Menu - Full CRUD]
        UC7[Accept & Prepare Orders]
        UC8[View Revenue Analytics]
        UC9[Import / Export Menu JSON]
        
        UC10[View Assigned Deliveries]
        UC11[Update Delivery Status]
        UC12[Complete Delivery & Return to Pool]
    end

    Customer --> UC1
    Customer --> UC2
    Customer --> UC3
    Customer --> UC4
    Customer --> UC5

    Admin --> UC1
    Admin --> UC6
    Admin --> UC7
    Admin --> UC8
    Admin --> UC9

    Driver --> UC1
    Driver --> UC10
    Driver --> UC11
    Driver --> UC12
```

---

## 11. Activity Diagram: Order Placement & Fulfillment

```mermaid
flowchart TD
    Start([Customer Selects Restaurant]) --> AddCart[Add Dishes to Cart]
    AddCart --> ViewCart[Review Cart & Subtotal]
    ViewCart --> Checkout[Click Checkout & Enter Address]
    Checkout --> OrderDB[OrderDAO Inserts Order & Items in DB Transaction]
    OrderDB --> AsyncWeather[Async Fetch Weather & Road Conditions]
    OrderDB --> AdminQueue[Order Enters Admin Queue as PLACED]
    AdminQueue --> AcceptOrder{Admin Confirms?}
    AcceptOrder -- No --> Cancelled[Order CANCELLED]
    AcceptOrder -- Yes --> Prep[Kitchen Sets Status to PREPARING]
    Prep --> Ready[Kitchen Sets Status to READY]
    Ready --> AcquireDriver[Acquire Driver from Synchronized Driver Pool]
    AcquireDriver --> OutForDeliv[Driver Marks PICKED_UP]
    OutForDeliv --> Delivered[Driver Marks DELIVERED]
    Delivered --> ReleaseDriver[Driver Released Back to Pool]
    Delivered --> CustomerReview[Customer Submits 5-Star Review]
    CustomerReview --> End([End])
    Cancelled --> End
```

---

## 12. Class Diagram: Domain & Inheritance Hierarchy

```mermaid
classDiagram
    class User {
        <<abstract>>
        #int id
        #String name
        #String email
        #String password
        #String phone
        #String address
        #String role
        #String createdAt
        +showDashboard(Stage stage)* void
    }

    class Customer {
        +showDashboard(Stage stage) void
    }

    class RestaurantAdmin {
        -int restaurantId
        +getRestaurantId() int
        +showDashboard(Stage stage) void
    }

    class DeliveryStaff {
        -boolean available
        -String vehicleType
        +isAvailable() boolean
        +showDashboard(Stage stage) void
    }

    class Restaurant {
        -int id
        -String name
        -String description
        -String address
        -String phone
        -double rating
        -String imageUrl
    }

    class FoodItem {
        -int id
        -int restaurantId
        -String name
        -String description
        -String category
        -double price
        -boolean available
    }

    class CartItem {
        -FoodItem foodItem
        -int quantity
        +getSubtotal() double
    }

    class Order {
        -int id
        -int userId
        -int restaurantId
        -double totalAmount
        -double deliveryFee
        -String status
        -String deliveryAddress
        -String paymentMethod
        -String createdAt
    }

    class OrderItem {
        -int id
        -int orderId
        -int foodId
        -String foodName
        -int quantity
        -double unitPrice
        +getSubtotal() double
    }

    class Delivery {
        -int id
        -int orderId
        -int deliveryStaffId
        -String status
        -String assignedAt
        -String pickedUpAt
        -String deliveredAt
    }

    class Review {
        -int id
        -int userId
        -int restaurantId
        -int orderId
        -int rating
        -String comment
    }

    User <|-- Customer
    User <|-- RestaurantAdmin
    User <|-- DeliveryStaff
    Restaurant "1" *-- "N" FoodItem
    CartItem o-- FoodItem
    Order "1" *-- "N" OrderItem
    Order "1" -- "1" Delivery
    Restaurant "1" -- "N" Review
    User "1" -- "N" Order
```

---

## 13. Entity-Relationship (ER) Diagram

```mermaid
erDiagram
    USERS ||--o{ ORDERS : places
    USERS ||--o{ DELIVERIES : handles
    USERS ||--o{ REVIEWS : writes
    RESTAURANTS ||--o{ FOOD_ITEMS : offers
    RESTAURANTS ||--o{ ORDERS : receives
    RESTAURANTS ||--o{ REVIEWS : evaluated_by
    ORDERS ||--|{ ORDER_ITEMS : contains
    ORDERS ||--o| DELIVERIES : tracked_by
    FOOD_ITEMS ||--o{ ORDER_ITEMS : ordered_in

    USERS {
        int id PK
        string name
        string email UK
        string password
        string phone
        string address
        string role
        string created_at
    }

    RESTAURANTS {
        int id PK
        string name
        string description
        string address
        string phone
        real rating
        string image_url
    }

    FOOD_ITEMS {
        int id PK
        int restaurant_id FK
        string name
        string description
        string category
        real price
        int available
        string image_url
    }

    ORDERS {
        int id PK
        int user_id FK
        int restaurant_id FK
        real total_amount
        real delivery_fee
        string status
        string delivery_address
        string payment_method
        string created_at
    }

    ORDER_ITEMS {
        int id PK
        int order_id FK
        int food_id FK
        int quantity
        real unit_price
    }

    DELIVERIES {
        int id PK
        int order_id FK
        int delivery_staff_id FK
        string status
        string assigned_at
        string picked_up_at
        string delivered_at
    }

    REVIEWS {
        int id PK
        int user_id FK
        int restaurant_id FK
        int order_id FK
        int rating
        string comment
        string created_at
    }
```

---

## 14. Sequence Diagram: Customer Order Placement & Transaction

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Customer
    participant GUI as CustomerDashboardView
    participant OS as OrderService
    participant ODAO as OrderDAO
    participant DB as SQLite (QuickBite.db)
    participant Sim as OrderProcessingSimulator

    Customer->>GUI: Clicks "Place Order Now"
    GUI->>OS: placeOrder(userId, restId, cartItems, address, payment, fee, true)
    OS->>ODAO: createOrder(order)
    ODAO->>DB: conn.setAutoCommit(false) [BEGIN TRANSACTION]
    ODAO->>DB: INSERT INTO orders VALUES (...)
    ODAO->>DB: Batch INSERT INTO order_items VALUES (...)
    ODAO->>DB: conn.commit() [COMMIT TRANSACTION]
    DB-->>ODAO: Transaction Success
    ODAO-->>OS: Returns generated Order ID
    OS->>Sim: startOrderSimulation(orderId, false)
    Sim-->>OS: Async Worker Task Submitted to ExecutorService
    OS-->>GUI: Order placed successfully
    GUI->>GUI: Opens showOrderTrackingModal() & clears cart
```

---

## 15. Sequence Diagram: Asynchronous Weather API Integration

```mermaid
sequenceDiagram
    autonumber
    participant GUI as OrderTrackingModal
    participant SD as SmartDeliveryService
    participant WC as WeatherApiClient
    participant Http as java.net.http.HttpClient
    participant API as Open-Meteo REST API

    GUI->>SD: fetchSmartDeliveryInfoAsync(callback)
    Note over SD: Spawns CompletableFuture on Background Worker Thread
    SD->>WC: fetchCurrentWeatherJson()
    WC->>Http: send(HttpRequest.GET)
    Http->>API: HTTP GET /v1/forecast?current=temperature_2m,weather_code
    API-->>Http: HTTP 200 OK (JSON Payload)
    Http-->>WC: Response String
    WC-->>SD: Raw JSON String
    Note over SD: Parses temperature, WMO weather code & calculates ETA
    SD->>GUI: Platform.runLater(() -> callback.accept(SmartDeliveryInfo))
    GUI->>GUI: Updates Weather Badge & Travel Safety Advisory
```

---

## 16. Multithreading & Concurrency Architecture

### Thread Pool Architecture
QuickBite avoids ad-hoc thread creation by maintaining a reusable `ExecutorService` configured with 4 worker daemon threads:
```java
private final ExecutorService executorService = Executors.newFixedThreadPool(4, new ThreadFactory() {
    private int count = 1;
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "QuickBite-Worker-" + (count++));
        t.setDaemon(true);
        return t;
    }
});
```

### Shared Resource Synchronization
Delivery drivers represent a finite physical resource. When multiple customer orders transition to `READY` at the same time, multiple threads attempt to claim couriers. QuickBite guards driver pool mutations using `synchronized` critical sections:
```java
public synchronized int acquireDriver(int orderId) {
    if (availableDriverIds.isEmpty()) {
        return -1; // Resource exhausted, order enters queue
    }
    Iterator<Integer> it = availableDriverIds.iterator();
    int selectedDriverId = it.next();
    it.remove();
    activeDriverToOrderMap.put(selectedDriverId, orderId);
    deliveryDAO.createOrAssignDelivery(orderId, selectedDriverId);
    return selectedDriverId;
}
```

---

## 17. Database Design & CRUD Operations
QuickBite interacts with SQLite via JDBC using `PreparedStatement` to ensure strict parameterization:
```sql
SELECT * FROM food_items WHERE restaurant_id = ?;
INSERT INTO food_items (restaurant_id, name, description, category, price, available, image_url) VALUES (?, ?, ?, ?, ?, ?, ?);
UPDATE food_items SET name = ?, price = ?, available = ? WHERE id = ?;
DELETE FROM food_items WHERE id = ?;
```
Foreign keys are enforced upon every database connection via:
```sql
PRAGMA foreign_keys = ON;
```

---

## 18. Testing & Validation Plan
1. **Authentication Tests**:
   - Valid credentials correctly load user dashboards.
   - Duplicate email registration triggers an error dialog.
   - Empty input fields trigger informative validation alerts.
2. **Cart & Pricing Tests**:
   - Quantity increments update item subtotals immediately.
   - Removing items recalculates overall order total and delivery fees.
3. **Transactional Integrity Tests**:
   - `OrderDAO.createOrder()` writes both `orders` and `order_items` atomically or rolls back entirely if interrupted.
4. **Concurrency & Race Condition Tests**:
   - The "Fire 3 Concurrent Orders" test in the Concurrency Monitor creates 3 parallel threads placing orders simultaneously to confirm thread-safe driver allocation.
5. **API & Fallback Tests**:
   - Live network conditions retrieve live weather from Open-Meteo.
   - Simulating offline execution triggers the local safety advisory fallback without throwing unhandled exceptions.

---

## 19. Limitations
- **Single Host Deployment**: SQLite is optimized for single-host desktop deployments. High-concurrency enterprise workloads with thousands of simultaneous remote write transactions would benefit from PostgreSQL or MySQL.
- **Mock Payment Processing**: Payment methods (*Cash on Delivery*, *Card*, *Mobile Banking*) record status without real-world banking gateway integration (e.g., Stripe SDK).

---

## 20. Conclusion
QuickBite demonstrates how Java and JavaFX can deliver a responsive, multithreaded desktop experience. By uniting **Java OOP**, **JavaFX GUI Engineering**, **SQLite Relational Persistence**, **Thread-Safe Synchronization**, and **External REST API Ingestion** under an **MVC + DAO + Service** architecture, QuickBite provides an exemplary software engineering project fulfilling all academic and professional standards.
