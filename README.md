# 🍔 QuickBite: Food Ordering & Delivery Management System

QuickBite is a desktop-based **Food Ordering and Delivery Management System** developed using **Java, JavaFX, SQLite, REST APIs, JSON, and multithreading**.

The project is designed as an academic Java application demonstrating core Java programming concepts together with GUI development, database management, API integration, JSON processing, concurrency, and object-oriented software design.

---

## 📌 Project Overview

QuickBite provides a complete food ordering ecosystem connecting:

- 👤 Customers
- 🍽️ Restaurants
- 🛵 Delivery Drivers

 Under a single application. Customers can browse restaurants and menus, search for food, place orders, view order information, and receive smart delivery information.

Restaurants can manage their food items, availability, and incoming orders.

Administrators can manage users, restaurants, food items, and system data.

---

# ✨ Features

## 👤 Customer Features

- Customer registration and login
- Customer dashboard
- Browse restaurants
- Browse restaurant menus
- Search and filter food
- Add food items to cart
- Update cart quantities
- Remove items from cart
- Place food orders
- View order information and history
- View customer reviews
- Dish of the Day
- Weather-based smart delivery estimation
- Logout

## 🍽️ Restaurant Features

- Restaurant login
- Restaurant dashboard
- View food menu
- Add, update, and delete food items
- Toggle food availability
- Manage food categories
- View incoming orders
- Update order status
- Export menu data to JSON
- Import menu data from JSON

## 🛵 Driver Features

- Driver login
- View assigned deliveries
- View delivery information
- Update delivery status

---

# 🤖 Smart Features

## 🌤️ Weather-Based Smart Delivery

QuickBite integrates the **Open-Meteo API** to obtain current weather information.

The application retrieves:

- Temperature
- Weather condition
- Wind speed

The information is processed to provide:

- Estimated delivery time
- Weather advisory
- Current weather condition

The current implementation uses:

```text
Latitude: 22.8098
Longitude: 89.5644
```

These coordinates correspond to **Khulna, Bangladesh**.

### API

```text
https://api.open-meteo.com/
```

The weather request uses:

```text
current=temperature_2m,weather_code,wind_speed_10m
```

The request is handled asynchronously so the JavaFX interface remains responsive.

---

# 🍽️ Dish of the Day

QuickBite includes a **Dish of the Day** feature.

The system selects an available food item and applies a promotional discount based on configured discount levels.

Current discount levels include:

```text
10%
15%
20%
25%
30%
```

The Dish of the Day information is retrieved from the SQLite database.

---

# 🔌 API Integration

QuickBite demonstrates integration with external APIs using Java's built-in HTTP client.

The application uses:

```java
java.net.http.HttpClient
java.net.http.HttpRequest
java.net.http.HttpResponse
java.net.URI
```

### Current API Integration

| API | Purpose |
|---|---|
| Open-Meteo | Weather information |

---

# 🧵 Concurrency and Multithreading

QuickBite demonstrates Java concurrency concepts to prevent long-running operations from blocking the JavaFX UI thread.

The project uses:

```text
Thread
Runnable
ExecutorService
CompletableFuture
```

and a custom:

```text
ConcurrencyMonitorService
```

Background processing is used for operations such as:

- API requests
- Weather retrieval
- Smart delivery calculation
- Other time-consuming tasks

JavaFX UI updates are transferred safely back to the JavaFX Application Thread using:

```java
Platform.runLater(...)
```

---

# 🗄️ Database

QuickBite uses **SQLite** as its local relational database.

The database stores information such as:

- Users
- Customers
- Restaurants
- Drivers
- Food items
- Orders
- Order items
- Reviews

### Database

```text
SQLite
```

### Database file

```text
database/QuickBite.db
```

### JDBC URL

```text
jdbc:sqlite:database/QuickBite.db
```

The project does not require a separate MySQL or PostgreSQL server.

---

# 📦 Database Architecture

The project follows a DAO-based approach for database operations.

Example DAO components include:

```text
UserDAO
FoodItemDAO
RestaurantDAO
OrderDAO
```

The DAO layer separates database operations from business logic.

---

# 🏗️ Project Architecture

QuickBite follows a layered architecture:

```text
┌──────────────────────────────┐
│        JavaFX / FXML         │
│      User Interface Layer    │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│         Controllers          │
│   UI Events & Navigation     │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│          Services            │
│       Business Logic         │
└──────────────┬───────────────┘
               │
        ┌──────┴──────┐
        ▼             ▼
┌──────────────┐ ┌──────────────┐
│     DAO      │ │ External API │
│  Database    │ │ Integration  │
└──────┬───────┘ └──────────────┘
       │
       ▼
┌──────────────┐
│    SQLite    │
│   Database   │
└──────────────┘
```

---

# 🖥️ User Interface

The graphical user interface is developed using:

```text
JavaFX
```

The project is structured to support **FXML and Scene Builder**.

The intended architecture is:

```text
FXML
 ↓
Controller
 ↓
Service
 ↓
DAO
 ↓
SQLite
```

FXML separates visual UI design from Java application logic.

---

# 🎨 Scene Builder Compatibility

The FXML-based UI can be designed and edited using **JavaFX Scene Builder**.

FXML files define:

- Layouts
- Buttons
- Labels
- Text fields
- Tables
- Images
- Menus
- Other JavaFX controls

Controllers handle:

- Event handlers
- UI logic
- Navigation
- Service calls
- Database interaction

---

# 📁 Project Structure

```text
QuickBite/
│
├── database/
│   └── QuickBite.db
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── quickbite/
│   │   │           ├── api/
│   │   │           │   ├── WeatherApiClient.java
│   │   │           │   ├── SmartDeliveryService.java
│   │   │           │   └── DishOfTheDayService.java
│   │   │           ├── config/
│   │   │           ├── concurrency/
│   │   │           │   └── ConcurrencyMonitorService.java
│   │   │           ├── controller/
│   │   │           ├── dao/
│   │   │           │   ├── UserDAO.java
│   │   │           │   ├── FoodItemDAO.java
│   │   │           │   ├── RestaurantDAO.java
│   │   │           │   └── OrderDAO.java
│   │   │           ├── model/
│   │   │           ├── service/
│   │   │           │   └── MenuService.java
│   │   │           ├── util/
│   │   │           │   └── JsonUtil.java
│   │   │           └── ...
│   │   │
│   │   └── resources/
│   │       └── com/
│   │           └── quickbite/
│   │               ├── view/
│   │               │   ├── login.fxml
│   │               │   ├── customer-dashboard.fxml
│   │               │   ├── restaurant-dashboard.fxml
│   │               │   └── ...
│   │               ├── css/
│   │               └── images/
│   │
│   └── test/
│       └── java/
│
├── pom.xml
├── README.md
└── PROJECT_REPORT.md
```

---

# 🧩 Main Packages

## `api`

Responsible for external API communication.

Examples:

```text
WeatherApiClient
SmartDeliveryService
DishOfTheDayService
```

## `controller`

Contains JavaFX controllers responsible for:

- Handling UI events
- Connecting FXML components to Java
- Calling services
- Scene navigation
- Updating UI elements

## `dao`

The Data Access Object layer responsible for:

```text
INSERT
SELECT
UPDATE
DELETE
```

## `model`

Contains Java model classes representing system entities.

Examples:

```text
User
Customer
Restaurant
Driver
FoodItem
Order
OrderItem
Review
```

## `service`

Contains application business logic.

Example:

```text
MenuService
```

## `util`

Contains reusable utility classes.

Example:

```text
JsonUtil
```

## `concurrency`

Contains classes related to multithreading and asynchronous processing.

Example:

```text
ConcurrencyMonitorService
```

---

# 🧾 JSON Processing

QuickBite uses **Jackson Databind** for JSON processing.

Jackson is used for:

- JSON serialization
- JSON deserialization
- API response parsing
- Menu export
- Menu import

Example:

```java
ObjectMapper mapper = new ObjectMapper();
```

---

# 📤 Menu JSON Export

Restaurant menu data can be exported into JSON files.

Example:

```json
[
  {
    "id": 1,
    "name": "Zinger Burger",
    "price": 299.0,
    "category": "Burger",
    "available": true
  }
]
```

---

# 📥 Menu JSON Import

Previously exported menu files can be imported into QuickBite.

The import process is:

```text
JSON File
   ↓
Jackson ObjectMapper
   ↓
FoodItem Objects
   ↓
MenuService
   ↓
SQLite Database
```

---

# ☕ Technologies Used

| Technology | Purpose |
|---|---|
| Java | Main programming language |
| JavaFX | Desktop GUI |
| FXML | UI definition |
| Scene Builder | Visual UI design |
| SQLite | Local database |
| JDBC | Database connectivity |
| Maven | Dependency management |
| Jackson | JSON processing |
| Open-Meteo API | Weather information |
| Java HttpClient | HTTP requests |
| ExecutorService | Multithreading |
| CompletableFuture | Asynchronous processing |
| Git | Version control |
| GitHub | Source code hosting |

---

# ⚙️ Requirements

## Java

Use:

```text
JDK 21+
```

Check the installed version:

```bash
java -version
```

## Maven

Check Maven:

```bash
mvn -version
```

## JavaFX

JavaFX dependencies are managed through Maven.

## Scene Builder

Scene Builder is recommended for editing FXML files visually, but is not required to run the application.

---

# 📦 Maven Dependencies

The project uses Maven to manage dependencies.

Important dependencies include:

```text
JavaFX
SQLite JDBC
Jackson Databind
```

The dependency configuration is maintained in:

```text
pom.xml
```

---

# 🚀 Running the Project

## 1. Clone the Repository

```bash
git clone https://github.com/Was-iiiif/Quick-Bite.git
```

Move into the project directory:

```bash
cd Quick-Bite
```

## 2. Build the Project

```bash
mvn clean install
```

## 3. Run the JavaFX Application

If the JavaFX Maven plugin is configured:

```bash
mvn javafx:run
```

Alternatively, run the JavaFX `Application` class directly from IntelliJ IDEA.

The application entry point is the class that extends:

```java
javafx.application.Application
```

and implements:

```java
@Override
public void start(Stage stage)
```

---

# 🗄️ Database Initialization

QuickBite uses:

```text
database/QuickBite.db
```

If the database does not exist, the application's database initialization logic can create the required database structure.

SQLite is file-based, so no separate database server is required.

---

# 🧪 System Verification

The project includes verification/testing functionality for checking important system components.

Possible checks include:

```text
Database connection
Restaurant loading
Food item loading
Authentication
Driver functionality
Order functionality
API-related components
```

A verification class such as:

```text
VerifySystem
```

can be used during development.

> **Note:** Verification should not assume that predefined customer or administrator accounts exist unless those accounts have explicitly been seeded into the database.

---



# 🔄 Application Flow

A typical customer workflow is:

```text
Launch Application
       ↓
     Login
       ↓
Customer Dashboard
       ↓
Browse Restaurants
       ↓
Select Restaurant
       ↓
Browse Menu
       ↓
Search / Filter Food
       ↓
Add Items to Cart
       ↓
Review Cart
       ↓
Place Order
       ↓
Order Confirmation
       ↓
Delivery Processing
       ↓
Smart Delivery Information
       ↓
Order Delivered
```

---

# 🧠 Object-Oriented Programming

QuickBite demonstrates important Java OOP concepts.

## Encapsulation

Model classes keep data private and expose it through methods such as:

```java
getName()
setName()
getPrice()
setPrice()
```

## Inheritance

Different user types can share common user functionality through inheritance where appropriate.

Conceptually:

```text
User
 ├── Customer
 ├── Restaurant
 ├── Driver
 └── Admin
```

## Polymorphism

Different objects can implement or override common behavior.

## Abstraction

Service and DAO layers abstract business and database logic from the user interface.

## Classes and Objects

The application is structured around Java classes and objects.

Example:

```java
FoodItem food = new FoodItem();
Restaurant restaurant = new Restaurant();
Order order = new Order();
```

---

# 🧵 Threading Architecture

Expensive operations should not execute directly on the JavaFX Application Thread.

Example:

```text
JavaFX Application Thread
          │
          ▼
    User requests
    weather data
          │
          ▼
 Background Executor
          │
          ▼
     HTTP Request
          │
          ▼
    JSON Response
          │
          ▼
   Process Response
          │
          ▼
 Platform.runLater()
          │
          ▼
 Update JavaFX UI
```

This prevents the GUI from freezing during network operations.

---

# 🌐 HTTP Request Flow

The weather API request follows:

```text
WeatherApiClient
       ↓
HttpClient
       ↓
HttpRequest
       ↓
Open-Meteo API
       ↓
HttpResponse<String>
       ↓
JSON Parser
       ↓
Weather Information
       ↓
SmartDeliveryService
       ↓
Customer Dashboard
```

---

# 🛒 Food Ordering System

A simplified relationship between the main entities is:

```text
Customer
   │
   │ places
   ▼
 Order
   │
   │ contains
   ▼
OrderItem
   │
   │ references
   ▼
FoodItem
   │
   │ belongs to
   ▼
Restaurant
```

---

# 🔍 Food Search

Customers can search for food items based on:

```text
Food name
Category
Restaurant
Availability
```

---

# 📱 Customer Dashboard

The Customer Dashboard provides access to:

```text
Restaurants
Menu
Search
Cart
Orders
Dish of the Day
Reviews
Smart Delivery
Logout
```

---

# 🧑‍💻 Development Workflow

A recommended workflow is:

```text
1. Design UI in Scene Builder
            ↓
2. Save FXML
            ↓
3. Create / update Controller
            ↓
4. Connect Controller to Services
            ↓
5. Connect Services to DAO
            ↓
6. Test SQLite operations
            ↓
7. Test API integration
            ↓
8. Test JavaFX UI
```

---

# 🌿 Git Workflow

Typical Git commands:

```bash
git status
git add .
git commit -m "Updated QuickBite features"
git push
```

For a separate development branch:

```bash
git checkout -b demo01
```

Push the branch:

```bash
git push -u origin demo01
```

---

# 📚 Academic Concepts Demonstrated

## Java Fundamentals

- Variables
- Data types
- Operators
- Conditional statements
- Loops
- Methods
- Classes
- Objects

## Object-Oriented Programming

- Encapsulation
- Inheritance
- Polymorphism
- Abstraction
- Interfaces

## JavaFX

- Stage
- Scene
- Controls
- Layouts
- Events
- FXML
- Scene Builder
- Controllers

## Database

- SQLite
- JDBC
- SQL
- CRUD operations
- DAO pattern

## JSON

- Serialization
- Deserialization
- JSON parsing
- API response processing
- File import/export

## Networking

- HTTP
- REST API
- URI
- HttpClient
- HttpRequest
- HttpResponse

## Multithreading

- Thread
- Runnable
- ExecutorService
- CompletableFuture
- Synchronization
- Background tasks

---

# 🎯 Project Objectives

The main objectives of QuickBite are:

1. To develop a complete desktop food ordering system using Java.
2. To demonstrate object-oriented programming principles.
3. To develop a graphical user interface using JavaFX.
4. To separate UI design from application logic using FXML.
5. To integrate a relational SQLite database.
6. To implement CRUD operations using JDBC.
7. To demonstrate JSON serialization and deserialization.
8. To integrate an external REST API.
9. To implement asynchronous API operations.
10. To demonstrate Java multithreading and concurrency.
11. To provide different interfaces for different user roles.
12. To develop a maintainable layered software architecture.

---

# 🔮 Future Improvements

Possible future improvements include:

- Real-time order tracking
- Google Maps integration
- GPS-based driver tracking
- Online payment integration
- Push notifications
- More restaurant integrations
- AI-powered food recommendations
- Personalized customer recommendations
- Demand prediction
- Delivery route optimization
- Advanced restaurant analytics
- Customer loyalty system
- Mobile application
- Cloud database
- Secure password hashing
- More comprehensive automated testing

---

# 👨‍💻 Developer

**Md Wasif Rahman**

Computer Science & Engineering  
Khulna University of Engineering & Technology (KUET)

GitHub:

https://github.com/Was-iiiif/Quick-Bite

---

# 📄 License

This project was developed primarily for **academic and educational purposes**.

The project may be modified and extended for learning, experimentation, and demonstration purposes.

---

# ⭐ Acknowledgements

Special thanks to the open-source technologies and services used in this project, including:

- Java
- JavaFX
- SQLite
- Jackson
- Open-Meteo
- Maven
- Scene Builder
- GitHub

---

## 🍔 QuickBite

> **Order Smart. Eat Better.**
