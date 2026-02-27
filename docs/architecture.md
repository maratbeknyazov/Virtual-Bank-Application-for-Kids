# Virtual Bank Application for Kids - Architecture

## 1. Architectural Overview

The application follows a **Layered (3-Tier) Architecture** pattern, optimized for simplicity, testability, and extensibility in a standalone desktop application context.

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (JavaFX UI - Parent & Child Screens)   │
├─────────────────────────────────────────┤
│         Application Layer               │
│  (Controllers, Use Cases, Workflows)    │
├─────────────────────────────────────────┤
│           Domain Layer                  │
│    (Entities, Business Rules, Logic)    │
├─────────────────────────────────────────┤
│       Infrastructure Layer              │
│  (Repository, File I/O, Serialization)  │
├─────────────────────────────────────────┤
│         Data Layer (Local Files)        │
│    (JSON/CSV files on disk)             │
└─────────────────────────────────────────┘
```

### 1.1 Design Principles

1. **Separation of Concerns**: Each layer has a single responsibility
2. **Dependency Inversion**: High-level layers don't depend on low-level details
3. **Single Responsibility**: Each class does one thing well
4. **KISS (Keep It Simple)**: No unnecessary complexity or design patterns
5. **Domain-Driven Design**: Domain logic isolated from infrastructure
6. **Testability**: All layers independently testable via unit tests

## 2. Layer Responsibilities

### 2.1 Presentation Layer (UI)

**Responsibility**: Render user interface and capture user interactions.

**Technology**: JavaFX

**Components**:

- **Parent Dashboard Screen**
  - Display managed children
  - Create/manage child accounts
  - Create tasks
  - Approve task submissions
  - View transactions
  - Monitor savings goals

- **Child Dashboard Screen**
  - Display personal accounts (Current & Savings)
  - View account balance
  - View goals and progress
  - View assigned tasks

- **Transaction Screen (Child)**
  - Request withdrawal/deposit
  - View transaction history
  - Filter by date, type

- **Task Screen (Child)**
  - View open tasks
  - Submit completed task
  - View task history and rewards earned

- **Savings Goal Screen (Child)**
  - Create new goal
  - View goals and progress
  - View completed goals
  - Edit/delete goals

- **Common Components**
  - Login dialog (PIN entry)
  - Transaction confirmation dialog
  - Error/success toast notifications

**Responsibilities**:
- Render JavaFX nodes
- Capture user input (mouse, keyboard)
- Validate user input (client-side)
- Call application layer via controllers
- Display results from application layer
- Handle UI refresh events
- NO business logic in UI layer

**Key Classes**:
- `ParentDashboardController`, `ChildDashboardController`
- `TransactionViewController`, `TaskViewController`
- `GoalViewController`, `SavingsGoalViewController`
- `LoginController`
- UI utility classes for common widgets

### 2.2 Application Layer

**Responsibility**: Orchestrate use cases, coordinate domain logic, and manage application workflows.

**Components**:

- **Use Case Services** (Application Services)
  - `TransactionService`: Deposit, withdraw, transfer operations
  - `TaskService`: Create, submit, approve task workflows
  - `GoalService`: Create, update, complete savings goals
  - `AccountService`: Account management and querying
  - `UserService`: User creation, login, authentication
  - `OrganizationService`: Organization setup (MVP: not user-facing)

- **Workflow Coordinators**
  - `TaskApprovalWorkflow`: Orchestrates task approval → deposit creation
  - `TransferWorkflow`: Ensures atomic transfer (two linked transactions)

**Responsibilities**:
- Accept requests from UI controllers
- Coordinate domain objects
- Delegate business rule validation to domain layer
- Coordinate repository access (CRUD)
- Return DTOs (Data Transfer Objects) to UI
- Throw application-level exceptions (invalid input, not found, etc.)
- NO persistence logic; delegate to repositories
- NO UI rendering

**Key Classes**:
- `TransactionService`, `TaskService`, `GoalService`
- `AccountService`, `UserService`, `OrganizationService`
- Workflow coordinators
- DTOs for UI communication

### 2.3 Domain Layer

**Responsibility**: Encapsulate business logic, rules, and domain entities.

**Components**:

- **Domain Entities**
  - `Organization`, `User`, `BankAccount`, `Transaction`
  - `SavingsGoal`, `Task`

- **Value Objects** (as needed)
  - `Money` (encapsulate amount in cents with validation)
  - `UserId`, `OrganizationId` (ID wrappers for type safety)

- **Domain Services** (Domain Logic)
  - `TransactionValidator`: Validate withdrawal, deposit, transfer rules
  - `TaskRuleValidator`: Validate task state transitions, deadlines
  - `GoalCalculator`: Calculate goal progress, check completion
  - `BalanceCalculator`: Calculate account balance from transactions

- **Exceptions**
  - `InsufficientBalanceException`
  - `InvalidAmountException`
  - `InvalidStateTransitionException`
  - `UnauthorizedException`

**Responsibilities**:
- Define entity structure and invariants
- Enforce business rules via validation
- Implement domain-driven business logic
- Raise domain-specific exceptions
- NO infrastructure or database logic
- NO UI or presentation logic

**Key Classes**:
- `Organization`, `User`, `BankAccount`, `Transaction`, `SavingsGoal`, `Task`
- `TransactionValidator`, `TaskRuleValidator`, `GoalCalculator`
- Domain exceptions

### 2.4 Infrastructure Layer

**Responsibility**: Handle data persistence, file I/O, and serialization.

**Components**:

- **Repository Implementations**
  - `FileBasedRepository<T>`: Generic repository using JSON files
  - `OrganizationRepository`, `UserRepository`, `BankAccountRepository`
  - `TransactionRepository`, `SavingsGoalRepository`, `TaskRepository`

- **Serialization & Deserialization**
  - `JsonSerializer`: Convert entities to/from JSON
  - `CsvExporter`: Export transaction history to CSV (for parent review)

- **File Management**
  - `FileStorageManager`: Handle file paths, directory structure
  - Ensure data directory exists and is writable
  - Handle file locking (if concurrent access needed)

- **Data Access Objects (DAOs)**
  - Query builders for filtering by `organization_id`, `child_id`, etc.
  - Search and sort capabilities

**Responsibilities**:
- CRUD operations to file-based storage
- Serialize/deserialize JSON and CSV
- Manage data directory structure
- Handle file I/O errors gracefully
- Ensure referential integrity (for relational-like operations)
- NO business logic validation
- NO UI logic

**Key Classes**:
- `FileBasedRepository<T>`, `RepositoryFactory`
- `JsonSerializer`, `CsvExporter`
- `FileStorageManager`, `FilePathResolver`

### 2.5 Data Layer

**Responsibility**: Persistent storage of application data.

**Format**: JSON files (primary), CSV for exports

**Directory Structure**:
```
~/.virtualbank/
├── organizations.json
├── org_{organization_id}/
│   ├── users.json
│   ├── accounts.json
│   ├── transactions.json
│   ├── goals.json
│   └── tasks.json
└── exports/
    └── {organization_id}_{child_id}_{timestamp}.csv
```

**Responsibilities**:
- Persist data in JSON/CSV format
- Maintain data directory structure
- NO logic; just storage

**Format Examples**:

**organizations.json**:
```json
[
  {
    "id": "uuid-1",
    "name": "Smith Family",
    "organization_id": "uuid-1",
    "created_at": "2024-01-15T10:30:00Z",
    "updated_at": "2024-01-15T10:30:00Z",
    "is_active": true
  }
]
```

**users.json** (inside org_{organization_id}):
```json
[
  {
    "id": "uuid-2",
    "organization_id": "uuid-1",
    "username": "john_parent",
    "name": "John Smith",
    "role": "PARENT",
    "pin": "$2a$10$...", // bcrypt hash
    "age": null,
    "account_locked": false,
    "created_at": "2024-01-15T10:30:00Z",
    "updated_at": "2024-01-15T10:30:00Z"
  }
]
```

**accounts.json**:
```json
[
  {
    "id": "uuid-3",
    "organization_id": "uuid-1",
    "child_id": "uuid-4",
    "account_type": "CURRENT",
    "balance": 50000,  // in cents = $500.00
    "currency_code": "USD",
    "account_number": "ACC-001-CURRENT",
    "is_active": true,
    "created_at": "2024-01-15T10:35:00Z",
    "updated_at": "2024-01-16T14:22:00Z"
  }
]
```

## 3. Key Architectural Patterns

### 3.1 Repository Pattern

- **Purpose**: Abstract data access and provide CRUD operations
- **Implementation**: `FileBasedRepository<T>` for all entity types
- **Interface**:
  ```
  interface IRepository<T> {
    T create(T entity)
    T read(UUID id)
    List<T> readByOrganization(UUID organizationId)
    T update(T entity)
    void delete(UUID id)
    List<T> query(Query criteria)  // Advanced filtering
  }
  ```
- **Benefit**: Easy to swap file-based storage for database later

### 3.2 Service Layer (Application Services)

- **Purpose**: Coordinate domain logic and repositories
- **Pattern**:
  ```
  Service methods accept DTOs from UI
    ↓ validates input
    ↓ calls domain validators
    ↓ calls repositories to fetch entities
    ↓ calls domain services to transform
    ↓ calls repositories to persist
    ↓ returns DTO to UI
  ```
- **Benefit**: Clear separation between UI and domain logic

### 3.3 Dependency Injection (simple)

- **Purpose**: Decouple layer dependencies
- **Implementation**: Constructor injection, no framework (MVP)
  ```java
  class TransactionService {
    private final TransactionRepository repo;
    private final AccountRepository accountRepo;
    
    TransactionService(TransactionRepository repo, 
                      AccountRepository accountRepo) {
      this.repo = repo;
      this.accountRepo = accountRepo;
    }
  }
  ```
- **Benefit**: Easy testing, clear dependencies

### 3.4 DTO (Data Transfer Objects)

- **Purpose**: Decouple UI from domain entities
- **Pattern**:
  ```
  Domain: Transaction (internal entity, business rules)
    ↓ convert to DTO for UI
  UI: TransactionDTO (simple data holder, no logic)
  ```
- **Benefit**: UI doesn't need to know domain rules

### 3.5 Specification/Query Pattern

- **Purpose**: Build complex queries without hardcoding
- **Example**:
  ```java
  Query query = new Query()
    .withOrganizationId(orgId)
    .withChildId(childId)
    .withType(DEPOSIT)
    .withDateRange(start, end)
    .sortByDateDescending();
  
  List<Transaction> results = repo.query(query);
  ```
- **Benefit**: Flexible searching without SQL

## 4. Data Storage Strategy

### 4.1 File Format: JSON

**Advantages**:
- Human-readable (can inspect/edit files directly if needed)
- Structured (arrays, objects, primitives)
- No dependencies (no database server required)
- Easy serialization in Java (Jackson)
- Version control friendly (text-based)

**Disadvantages**:
- Slower than database for large datasets (MVP OK)
- No built-in indexing (MVP OK)
- No transaction support (mitigate via file locking)

### 4.2 File Organization

```
Data Root: ~/.virtualbank/

organizations.json
├─ Store all organizations

per-organization data:
org_{organization_id}/
├─ users.json              # All users in org
├─ accounts.json           # All bank accounts in org
├─ transactions.json       # All transactions in org
├─ goals.json              # All savings goals in org
└─ tasks.json              # All tasks in org

exports/
└─ {exports go here}
```

**Rationale**: Organize by organization to enable future multi-tenancy separation (even in filesystem).

### 4.3 Data Consistency

**Single Source of Truth**:
- Files are the authoritative source
- In-memory entities are caches
- Changes written to disk immediately (especially transactions)

**Atomic Operations**:
- Use file locking for concurrent access prevention (if needed)
- Transaction completion is atomic (write to file, then modify balance)

**Backup Strategy** (out of MVP):
- Manual export to CSV (parent feature)
- Automatic backup to USB drive (future)

### 4.4 CSV Export

**Purpose**: Allow parents to export transaction history for record-keeping

**Format**:
```csv
Date,Type,Amount,Description,Balance After
2024-01-15,DEPOSIT,50.00,Weekly Allowance,500.00
2024-01-16,WITHDRAWAL,10.00,Snacks,490.00
...
```

**Implementation**: `CsvExporter` service

## 5. Error Handling Strategy

### 5.1 Exception Hierarchy

```
RuntimeException
├── ApplicationException (application-level)
│   ├── EntityNotFoundException
│   ├── DuplicateEntityException
│   ├── InvalidInputException
│   └── UnauthorizedException
├── DomainException (domain-level)
│   ├── InsufficientBalanceException
│   ├── InvalidAmountException
│   ├── InvalidStateTransitionException
│   └── DeadlineExpiredException
└── InfrastructureException (infrastructure-level)
    ├── FileAccessException
    ├── SerializationException
    └── DataCorruptionException
```

### 5.2 Error Handling in Each Layer

**Presentation Layer** (UI):
- Catch application/domain exceptions
- Display user-friendly error messages
- Log errors for debugging
- Example: "Insufficient balance. You have $10.00 but need $15.00"

**Application Layer**:
- Catch domain exceptions
- Convert to application exceptions
- Log with context (userId, transactionId, etc.)
- Example: Catch `InsufficientBalanceException` → throw `TransactionFailedException`

**Domain Layer**:
- Throw domain exceptions with clear messages
- NO try-catch (let caller handle)

**Infrastructure Layer**:
- Handle file I/O exceptions
- Throw `InfrastructureException` with root cause
- Log all I/O errors
- Example: FileNotFoundException → `FileAccessException`

### 5.3 Logging Strategy

**Log Levels**:
- `ERROR`: Unrecoverable errors (file corruption, critical logic failure)
- `WARN`: Recoverable issues (file not found, retry-able operations)
- `INFO`: Significant events (user login, large transactions, system startup)
- `DEBUG`: Detailed flow information (function entry/exit, parameter values)
- `TRACE`: Very detailed (object state, serialization details)

**Configuration**: 
- Log to file: `~/.virtualbank/logs/app.log`
- Console output for dev environment
- Configurable log level via properties

**Sample Log Entry**:
```
[2024-01-15 14:30:22.123] [INFO]  [TransactionService] Deposit processed: txn_id=uuid-5, amount=$50.00, account=ACC-001, balance=$500.00
[2024-01-15 14:30:22.456] [ERROR] [FileBasedRepository] Failed to persist transaction: IOException on file write, file=/home/user/.virtualbank/org_uuid-1/transactions.json
```

## 6. Validation Approach

### 6.1 Client-Side Validation (UI)

**In Presentation Layer**:
- Validate input format (non-empty, numeric, etc.)
- Provide immediate user feedback
- Example: "Amount must be a number greater than $0.01"

### 6.2 Server-Side Validation (Application/Domain)

**In Application Layer**:
- Validate input is present and in valid format
- Throw `InvalidInputException` if not

**In Domain Layer**:
- Validate business rules
- Throw domain exceptions (e.g., `InsufficientBalanceException`)

### 6.3 Validation Examples

**Deposit**:
1. UI: Check amount > 0 and numeric
2. Application: Check user permission
3. Domain: Check transaction rules
4. Infrastructure: Write to file

**Withdrawal**:
1. UI: Check amount > 0 and numeric
2. Application: Fetch account, check authorization
3. Domain: Check sufficient balance, account type
4. Infrastructure: Write to file

## 7. Testing Approach

### 7.1 Testing Strategy

**Target**: 90%+ code coverage with focus on unit and integration tests

**Test Pyramid**:
```
       Integration Tests (20-30%)
      ╱                    ╲
     ╱  Unit Tests (60-70%) ╲
    ╱_________________________╲
   E2E / Manual Testing (10%)
```

### 7.2 Unit Tests

**Scope**: Test single class in isolation

**Framework**: JUnit 5, Mockito

**Coverage Areas**:
- Domain entity invariants (`BankAccount`, `Transaction`, etc.)
- Domain validators (`TransactionValidator`, `TaskRuleValidator`)
- Service methods with mocked repositories
- Utility functions

**Example Test**:
```java
@Test
void testWithdrawInsufficientBalance() {
  BankAccount account = new BankAccount(UUID.randomUUID(), CURRENT, 1000);
  
  assertThrows(InsufficientBalanceException.class, () -> {
    TransactionValidator.validateWithdrawal(account, 2000);
  });
}
```

### 7.3 Integration Tests

**Scope**: Test interaction between layers (without database)

**Coverage Areas**:
- Service + Repository integration
- Transaction workflows (deposit → balance update)
- Task approval workflow (task created → deposit → reward)
- Goal auto-completion workflow

**Example Test**:
```java
@Test
void testDepositUpdatesBankAccountBalance() {
  TransactionService service = new TransactionService(
    new FileBasedRepository<>(), 
    new FileBasedRepository<>()
  );
  
  BankAccount account = service.deposit(accountId, 5000);
  
  assertEquals(5000, account.getBalance());
}
```

### 7.4 E2E Tests (Manual)

**Scope**: Test full application workflows

**Scenarios**:
- User login with PIN
- Create child account
- Perform deposit
- Submit and approve task
- Create savings goal and track progress
- Export transaction history

**Tool**: Manual testing (Java desktop testing tools like TestFX optional)

### 7.5 Test Data

**Strategy**: Create test fixtures and builders
```java
class TestDataBuilder {
  static Organization createOrganization() { ... }
  static User createParent() { ... }
  static User createChild() { ... }
  static BankAccount createAccount(UUID childId) { ... }
}
```

## 8. Extensibility Strategy

### 8.1 Extension Points

**Currently Designed for Easy Extension**:

1. **New User Roles**: Add `User.role` enum value → update validation rules
2. **New Transaction Types**: Add `Transaction.type` enum value → extend validators
3. **New File Formats**: Add `CsvSerializer`, `XmlSerializer` → implement `Serializer` interface
4. **New Persistence**: Replace `FileBasedRepository` with `DatabaseRepository` → implement `IRepository<T>`
5. **New Reports**: Add `ReportService` → generate custom CSV reports
6. **New Validation Rules**: Add domain validators → call from service layer

### 8.2 Design for Extension

**Principle**: Open for extension, closed for modification

**Example: Adding a new notification system (future)**:
```java
// Define interface
interface NotificationService {
  void notify(User user, String message);
}

// Implement for file-based (MVP)
class LogBasedNotificationService implements NotificationService { ... }

// Future: email-based
class EmailNotificationService implements NotificationService { ... }

// Inject via constructor
class TaskService {
  TaskService(TaskRepository repo, NotificationService notifier) { ... }
}
```

### 8.3 Version Compatibility

**Strategy** (for future data migrations):
- Add `version` field to entities
- Implement migration interface
- Example: If file format changes in v2, migrator converts v1 → v2

```java
interface DataMigration {
  void migrate(Path dataDirectory);
}

class V1toV2Migration implements DataMigration { ... }
```

## 9. Module Structure

### 9.1 Suggested Project Structure

```
virtual-bank-app/
├── src/main/java/
│   └── com/virtualbank/
│       ├── presentation/        # JavaFX UI
│       │   ├── controller/
│       │   ├── view/
│       │   └── util/
│       ├── application/         # Services, use cases
│       │   ├── service/
│       │   ├── dto/
│       │   └── workflow/
│       ├── domain/              # Entities, business logic
│       │   ├── entity/
│       │   ├── validator/
│       │   ├── service/
│       │   └── exception/
│       ├── infrastructure/      # Repositories, I/O
│       │   ├── repository/
│       │   ├── serializer/
│       │   ├── storage/
│       │   └── exception/
│       └── main/
│           └── App.java         # Application entry point
│
├── src/test/java/
│   └── com/virtualbank/
│       ├── domain/
│       │   └── [entity/validator tests]
│       ├── application/
│       │   └── [service tests]
│       └── infrastructure/
│           └── [repository tests]
│
├── pom.xml
└── README.md
```

### 9.2 Dependencies (Maven)

```xml
<!-- JavaFX -->
<dependency>
  <groupId>org.openjfx</groupId>
  <artifactId>javafx-controls</artifactId>
  <version>21.0.x</version>
</dependency>

<!-- JSON Serialization -->
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>2.15.x</version>
</dependency>

<!-- Password Hashing -->
<dependency>
  <groupId>org.mindrot</groupId>
  <artifactId>jbcrypt</artifactId>
  <version>0.4</version>
</dependency>

<!-- Testing -->
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>5.9.x</version>
  <scope>test</scope>
</dependency>

<dependency>
  <groupId>org.mockito</groupId>
  <artifactId>mockito-core</artifactId>
  <version>5.x</version>
  <scope>test</scope>
</dependency>
```

## 10. Summary

| Aspect | Approach |
|--------|----------|
| **Architecture** | Layered (3-tier): Presentation → Application → Domain → Infrastructure |
| **UI Framework** | JavaFX, MVC controllers |
| **Data Storage** | JSON files (no database) |
| **Validation** | Client-side (UI) + Server-side (domain) |
| **Error Handling** | Domain exceptions → Application exceptions → User-friendly UI messages |
| **Testing** | Unit (60-70%) + Integration (20-30%) + E2E (10%); target 90%+ coverage |
| **Design Patterns** | Repository, Service, DTO, Dependency Injection |
| **Extensibility** | Interface-based, easy to swap implementations |
| **Complexity** | Simple and pragmatic, no over-engineering |
