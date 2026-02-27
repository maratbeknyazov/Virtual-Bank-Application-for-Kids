# Virtual Bank Application for Kids - Domain Model

## 1. Overview

This document defines the business domain for the Virtual Bank Application for Kids. It specifies core entities, relationships, business rules, invariants, and validation rules that govern the system behavior.

## 2. Core Entities

### 2.1 Organization

Represents a family or organizational unit that operates independently within the system.

```
Organization
├── id: UUID (unique identifier)
├── name: String (max 255 characters)
├── organization_id: UUID (self-reference for multi-tenancy)
├── created_at: Instant
├── updated_at: Instant
└── is_active: Boolean (default: true)
```

**Business Rules**:
- Each organization is isolated. No cross-organization data sharing.
- Organization name must be unique within the system.
- Once created, `organization_id` cannot be changed.

**Invariants**:
- `id != null`
- `organization_id != null`
- `name != null && name.length > 0 && name.length ≤ 255`
- `created_at != null && created_at ≤ updated_at`

### 2.2 User

Represents a person (parent or child) within the system.

```
User
├── id: UUID
├── organization_id: UUID (foreign key)
├── username: String (max 50 characters, unique within organization)
├── name: String (max 255 characters)
├── role: Enum {PARENT, CHILD}
├── pin: String (hashed, 4-6 digits)
├── age: Integer (optional, for children)
├── account_locked: Boolean (default: false)
├── created_at: Instant
└── updated_at: Instant
```

**Business Rules**:
- Username must be unique within an organization.
- PIN must be 4-6 digits.
- PIN must be hashed using bcrypt or similar (never stored in plaintext).
- CHILD users cannot have PARENT privileges and vice versa.
- User cannot be deleted if they have active accounts (soft delete instead).
- Account can be locked after multiple failed PIN attempts (not in MVP, but design for it).

**Invariants**:
- `id != null`
- `organization_id != null`
- `username != null && username.length > 0 && username.length ≤ 50`
- `name != null && name.length > 0 && name.length ≤ 255`
- `role! = null && role in {PARENT, CHILD}`
- `pin != null && hashed`
- `created_at != null`

### 2.3 BankAccount

Represents a child's bank account (current or savings).

```
BankAccount
├── id: UUID
├── organization_id: UUID (foreign key)
├── child_id: UUID (foreign key to User(CHILD))
├── account_type: Enum {CURRENT, SAVINGS}
├── balance: Long (in cents, non-negative)
├── currency_code: String (default: "USD")
├── account_number: String (unique identifier for display)
├── is_active: Boolean (default: true)
├── created_at: Instant
└── updated_at: Instant
```

**Business Rules**:
- Each child can have exactly ONE current account and ONE savings account.
- Balance is stored in cents (smallest unit) to avoid floating-point errors.
- Balance can never be negative.
- Account number must be unique within organization.
- Account is immutable once created (except balance and active status).
- For SAVINGS accounts: only deposits and transfers allowed (no direct withdrawals).

**Invariants**:
- `id != null`
- `organization_id != null`
- `child_id != null`
- `account_type != null && account_type in {CURRENT, SAVINGS}`
- `balance >= 0 && balance is integer` (in cents)
- `currency_code != null`
- `account_number != null && unique within organization`
- `created_at != null`

### 2.4 Transaction

Represents a financial transaction (deposit, withdrawal, transfer).

```
Transaction
├── id: UUID
├── organization_id: UUID (foreign key)
├── account_id: UUID (foreign key to BankAccount)
├── transaction_type: Enum {DEPOSIT, WITHDRAWAL, TRANSFER}
├── amount: Long (in cents, always positive)
├── balance_after: Long (in cents, state at time of transaction)
├── description: String (optional, max 255 characters)
├── category: String (optional, e.g., "Task Reward", "Chore", "Gift")
├── related_transaction_id: UUID (optional, for transfers - links paired transactions)
├── status: Enum {PENDING, COMPLETED, FAILED, REJECTED}
├── created_at: Instant
└── updated_at: Instant
```

**Business Rules**:
- Transactions are **immutable** once created (COMPLETED or FAILED status).
- Amount is always positive; direction indicated by `transaction_type`.
- For TRANSFER transactions:
  - Create TWO transactions: one WITHDRAWAL on source, one DEPOSIT on destination
  - Link them via `related_transaction_id`
  - Both must succeed or both must fail (atomic operation)
- Pending transactions must be approved before becoming COMPLETED.
-Balance `balance_after` is a snapshot and cannot change.
- Only COMPLETED transactions affect account balance.

**Invariants**:
- `id != null && immutable`
- `organization_id != null`
- `account_id != null`
- `transaction_type != null && transaction_type in {DEPOSIT, WITHDRAWAL, TRANSFER}`
- `amount > 0` (in cents)
- `balance_after >= 0`
- `status != null && status in {PENDING, COMPLETED, FAILED, REJECTED}`
- `created_at != null`

### 2.5 SavingsGoal

Represents a savings target set by a child.

```
SavingsGoal
├── id: UUID
├── organization_id: UUID (foreign key)
├── child_id: UUID (foreign key to User(CHILD))
├── savings_account_id: UUID (foreign key to BankAccount)
├── goal_name: String (max 255 characters)
├── target_amount: Long (in cents)
├── current_progress: Long (in cents, = savings account balance)
├── deadline: LocalDate (optional)
├── category: String (optional, e.g., "toy", "gadget", "experience")
├── is_completed: Boolean (default: false)
├── completed_at: Instant (optional, set when target reached)
├── created_at: Instant
└── updated_at: Instant
```

**Business Rules**:
- Goal is created with 0 progress.
- `current_progress` = balance of linked `savings_account_id` at query time.
- Goal auto-completes when `current_progress >= target_amount`.
- Goal completion is immutable (cannot be undone).
- Deadline can be in the past (informational only, not enforced).
- Goal can be edited while not completed.
- Completed goals should be archived/hidden but not deleted.

**Invariants**:
- `id != null`
- `organization_id != null`
- `child_id != null`
- `savings_account_id != null`
- `goal_name != null && goal_name.length > 0 && goal_name.length ≤ 255`
- `target_amount > 0` (in cents)
- `current_progress >= 0` (in cents)
- `if is_completed then completed_at != null else completed_at == null`
- `created_at != null`

### 2.6 Task

Represents a task assigned by a parent to a child with a reward.

```
Task
├── id: UUID
├── organization_id: UUID (foreign key)
├── parent_id: UUID (foreign key to User(PARENT))
├── child_id: UUID (foreign key to User(CHILD))
├── task_description: String (max 500 characters)
├── reward_amount: Long (in cents)
├── task_status: Enum {OPEN, SUBMITTED, APPROVED, REJECTED, COMPLETED, EXPIRED}
├── category: String (optional, e.g., "chore", "learning", "behavior")
├── deadline: LocalDate (optional)
├── submitted_at: Instant (optional, when child marks as done)
├── approved_at: Instant (optional, when parent approves)
├── created_at: Instant
└── updated_at: Instant
```

**Business Rules**:
- Task created by PARENT in OPEN status.
- Child can SUBMIT task (changes to SUBMITTED status).
- Parent can APPROVE (→ COMPLETED) or REJECT (→ REJECTED) submitted task.
- Parent can also REJECT tasks in OPEN/SUBMITTED status.
- Upon APPROVAL → COMPLETED:
  - Create DEPOSIT transaction to child's account (reward_amount)
  - Deposit must complete automatically (not pending)
- Task expires after deadline if not submitted (status → EXPIRED).
- Reward is credited only on APPROVAL; if REJECTED, no reward credited.
- Task cannot be edited after SUBMITTED status.

**Invariants**:
- `id != null`
- `organization_id != null`
- `parent_id != null`
- `child_id != null`
- `task_description != null && task_description.length > 0 && task_description.length ≤ 500`
- `reward_amount > 0` (in cents)
- `task_status != null && task_status in {OPEN, SUBMITTED, APPROVED, REJECTED, COMPLETED, EXPIRED}`
- `if task_status == SUBMITTED then submitted_at != null else submitted_at == null`
- `if task_status in {APPROVED, COMPLETED} then approved_at != null else approved_at == null`
- `created_at != null`

## 3. Relationships Between Entities

```
Organization (1) ─── (Many) User
                ├─── (Many) BankAccount
                ├─── (Many) Transaction
                ├─── (Many) SavingsGoal
                └─── (Many) Task

User(PARENT) (1) ─── (Many) Task (as parent_id)
User(CHILD) (1) ─── (Many) BankAccount
            ├─── (Many) SavingsGoal
            ├─── (Many) Task (as child_id)
            └─── (Many) Transaction (ownership)

BankAccount (1) ─── (Many) Transaction
              ├─── (1) SavingsGoal

Task (1) ──→ creates (1..Many) Transaction (DEPOSIT on completion)
```

## 4. Business Rules

### 4.1 Account Management

**Rule BA-001**: Multi-Tenant Isolation
- Every entity contains `organization_id`
- Queries MUST filter by `organization_id`
- No data leakage between organizations

**Rule BA-002**: Child Account Creation
- When a CHILD user is created, automatically create:
  - 1 CURRENT account (balance = 0)
  - 1 SAVINGS account (balance = 0)

**Rule BA-003**: Initial Balance
- New accounts start with 0 balance (unless parent provides initial deposit)

**Rule BA-004**: Account Uniqueness
- Child can have exactly 1 CURRENT and 1 SAVINGS account
- Cannot create duplicate accounts

### 4.2 Transaction Processing

**Rule TP-001**: Balance Validation (Withdrawal)
- Before WITHDRAWAL: `account.balance >= withdrawal_amount`
- If validation fails: transaction → FAILED, account balance unchanged

**Rule TP-002**: Amount Validation
- All amounts must be positive integers (cents)
- Amount = 0 is invalid

**Rule TP-003**: Transfer Atomicity
- TRANSFER must create 2 linked transactions (WITHDRAWAL + DEPOSIT)
- Both transactions must COMPLETE or both must FAIL
- No partial transfers

**Rule TP-004**: Balance Immutability
- `balance_after` is a snapshot of the account state at transaction time
- Cannot be modified after transaction creation

**Rule TP-005**: Transaction Order
- Transactions are ordered by `created_at` timestamp
- For same-second transactions: order by `id` (deterministic)

**Rule TP-006**: SAVINGS Account Restrictions
- SAVINGS accounts cannot have direct WITHDRAWAL transactions
- Only DEPOSIT and TRANSFER (to CURRENT) are allowed
- Attempting withdrawal from SAVINGS → FAILED transaction

### 4.3 Task and Reward System

**Rule TR-001**: Task Creation
- Only PARENT users can create tasks
- Task starts in OPEN status
- Reward amount must be > 0
- Deadline can be null (open-ended task)

**Rule TR-002**: Task Submission
- Only CHILD assigned to task can submit it
- Can submit only if status = OPEN
- Changes status to SUBMITTED
- Records `submitted_at` timestamp

**Rule TR-003**: Task Approval and Rejection
- Only assigned PARENT can approve/reject
- Approval actions:
  - OPEN/SUBMITTED → APPROVED → COMPLETED
  - On COMPLETED: Create DEPOSIT transaction automatically
  - Deposit MUST succeed (no insufficient funds scenario)
  - Records `approved_at` timestamp
- Rejection actions:
  - SUBMITTED → REJECTED (no reward credited)
  - OPEN → CANCELLED (via rejection)
  - No transaction created

**Rule TR-004**: Task Expiry
- If deadline has passed and status is OPEN/SUBMITTED → EXPIRED
- Expired tasks cannot be submitted or approved
- No reward for expired tasks

**Rule TR-005**: Reward Deposit
- Deposit created by approving task is AUTOMATIC (not requiring separate approval)
- Deposit amount = `task.reward_amount`
- Target account = child's CURRENT account
- Deposit must complete immediately (non-nullable flow)

### 4.4 Savings Goals

**Rule SG-001**: Goal Initialization
- Goal starts with `current_progress = 0`
- Goal linked to child's SAVINGS account
- Target amount must be > 0

**Rule SG-002**: Progress Tracking
- `current_progress` = current balance of linked SAVINGS account (at query time)
- Not stored, but calculated on-the-fly

**Rule SG-003**: Auto-Completion
- When SAVINGS account balance ≥ `target_amount`:
  - Goal auto-transitions to `is_completed = true`
  - `completed_at` timestamp recorded
  - Goal becomes immutable (read-only)

**Rule SG-004**: Goal Archival
- Completed goals remain in the system but marked as archived
- Can be displayed in a "Past Goals" section
- Child can create new replacement goals

### 4.5 Data Integrity

**Rule DI-001**: Referential Integrity
- Foreign key relationships must always reference existing entities
- Cascading deletes: When parent entity deleted, child entities soft-deleted
- (Soft delete: set `is_active = false`, don't physically remove)

**Rule DI-002**: Uniqueness Constraints
- Across organization:
  - `User.username` (unique per organization)
  - `BankAccount.account_number` (unique per organization)

**Rule DI-003**: Temporal Consistency
- `updated_at >= created_at` always
- `created_at` is immutable
- `updated_at` changes only when entity is modified

**Rule DI-004**: Monetary Accuracy
- All amounts stored in cents (Long integer)
- No floating-point arithmetic
- Sum of DEPOSIT transactions - sum of WITHDRAWAL transactions = account balance
  (Invariant must hold for all accounts at all times)

## 5. Validation Rules

### 5.1 User Validation

| Field | Rule | Example |
|-------|------|---------|
| `username` | 3-50 alphanumeric + underscore, unique/org | ✅ `john_doe`, ❌ `jo`, ❌ `john doe` |
| `name` | 1-255 characters, any characters allowed | ✅ `John Doe`, ❌ empty string |
| `pin` | 4-6 numeric digits, hashed before storage | ✅ `1234`, ❌ `12`, ❌ `12345678` |
| `age` | 1-120 (optional, for CHILD) | ✅ `10`, ❌ `200` |
| `role` | PARENT or CHILD, immutable after creation | ✅ `PARENT`, ❌ `ADMIN` |

### 5.2 Transaction Validation

| Field | Rule | Applies To |
|-------|------|------------|
| `amount` | > 0, integer (cents) | All types |
| `amount` | ≤ account balance | WITHDRAWAL, TRANSFER |
| `amount` | account.balance - amount ≥ 0 | WITHDRAWAL, TRANSFER |
| `description` | Optional, ≤ 255 chars | All types |
| `status` | Valid state transition (see state machine) | All types |

### 5.3 Savings Goal Validation

| Field | Rule |
|-------|------|
| `goal_name` | 1-255 non-empty string |
| `target_amount` | > 0, integer (cents) |
| `deadline` | Optional, can be past date |
| `current_progress` | ≤ target_amount when goal NOT completed |

### 5.4 Task Validation

| Field | Rule |
|-------|------|
| `task_description` | 1-500 non-empty string |
| `reward_amount` | > 0, integer (cents) |
| `deadline` | Optional LocalDate |
| `category` | Optional, suggest predefined values |

## 6. Domain Constraints

### 6.1 Multi-Tenancy Constraint
- **Every query, every operation** must include `organization_id` filter
- No cross-organization data access, even for READ operations
- Exception: System admin operations (out of MVP scope)

### 6.2 Immutability Constraints
- Transactions (once COMPLETED/FAILED): Immutable
- Task completion: Immutable
- Goal completion: Immutable
- User `created_at`, Organization `created_at`: Immutable
- Account `created_at`, BankAccount `created_at`: Immutable

### 6.3 State Machine Constraints

**Transaction Status Flow**:
```
PENDING → COMPLETED
       → FAILED / REJECTED
```

**Task Status Flow**:
```
OPEN → SUBMITTED → APPROVED → COMPLETED
    ↓        ↓
    └─→ REJECTED
OPEN / SUBMITTED → EXPIRED (if deadline passed)
```

### 6.4 Authorization Constraints
- CHILD cannot create, approve, or reject tasks
- CHILD cannot view other children's accounts or goals
- PARENT can only view/manage their own children and organization
- (MVP assumes single parent per organization; expand in future)

## 7. Exception Handling

### 7.1 Insufficient Balance
```
Exception: InsufficientBalanceException
Trigger: WITHDRAWAL amount > account balance
Action: Transaction → FAILED status
        Balance unchanged
        User feedback: "Insufficient balance. Available: $X.XX"
```

### 7.2 Invalid Amount
```
Exception: InvalidAmountException
Trigger: amount ≤ 0 or non-integer
Action: Transaction NOT created
        User feedback: "Amount must be greater than $0.01"
```

### 7.3 Unauthorized Access
```
Exception: UnauthorizedException
Trigger: User attempts to access/modify another user's data
Action: Operation aborted
        Audit log: Log unauthorized access attempt
        User feedback: "You don't have permission to access this"
```

### 7.4 Invalid State Transition
```
Exception: InvalidStateTransitionException
Trigger: Task status transition not allowed by state machine
Action: Operation rejected
        User feedback: "This action is not allowed in the current state"
```

## 8. Summary of Key Invariants

| Invariant | Scope |
|-----------|-------|
| Every entity has `organization_id` | Global |
| `created_at ≤ updated_at` | All entities |
| `balance ≥ 0` | BankAccount |
| Transaction is IMMUTABLE after creation | Transaction |
| Task deposit is AUTOMATIC on approval | Task |
| SAVINGS account cannot have direct withdrawal | BankAccount(SAVINGS) |
| Transfers are ATOMIC (2 linked transactions) | Transaction |
| Goal auto-completes when progress ≥ target | SavingsGoal |
| Transfer creates 2 linked transactions | Transaction |
| Amount always stored in cents (no decimals) | Transaction, BankAccount, Task, SavingsGoal |
