# Virtual Bank Application for Kids - Non-Functional Requirements

## 1. Overview

This document defines non-functional requirements (NFRs) for the Virtual Bank Application for Kids. NFRs specify quality attributes and constraints rather than specific features, ensuring the application meets performance, reliability, security, and usability standards.

---

## 2. Performance Requirements

### 2.1 Response Time

| Operation | Target | Acceptable | Critical |
|-----------|--------|-----------|----------|
| User login (PIN check) | < 100ms | < 500ms | > 5 seconds |
| Display dashboard | < 500ms | < 1 second | > 5 seconds |
| Submit/approve transaction | < 100ms | < 500ms | > 2 seconds |
| Display transaction history (100 txns) | < 200ms | < 1 second | > 5 seconds |
| Create/approve task | < 100ms | < 500ms | > 2 seconds |
| Create/update savings goal | < 100ms | < 500ms | > 2 seconds |
| Search/filter transactions | < 200ms | < 1 second | > 5 seconds |

**Definition**:
- **Target**: Ideal response time (goal for happy path)
- **Acceptable**: Maximum acceptable response time (user satisfaction maintained)
- **Critical**: Response time considered unacceptable (optimization required)

### 2.2 Throughput

- **Concurrent Users (MVP)**: 1 (single user at a time)
- **Transactions per Session**: 100+ per account (no degradation)
- **Data Size Limit (MVP)**: 10,000 transactions per account (before optimization needed)

### 2.3 Application Startup Time

- **Cold Start (first launch)**: < 2 seconds
- **Warm Start (after initialization)**: < 1 second

**Measurement**: From application launch to main dashboard fully rendered and interactive

### 2.4 Memory Usage

- **Idle (no user action)**: < 100 MB
- **Normal Operation**: < 200 MB
- **Peak Usage (large dataset)**: < 500 MB

**Acceptable Limit**: Application should not exceed 500 MB under normal conditions

### 2.5 Disk I/O

- **File Write Duration**: < 50ms per transaction
- **File Read Duration**: < 100ms per repository query
- **Total Data File Size**: < 50 MB for typical usage (100+ transactions/account)

**Rationale**: JSON files must be written and read quickly without blocking UI

---

## 3. Reliability Requirements

### 3.1 Availability (MVP)

- **Uptime Target**: 99.9% (application running without crashes)
- **Mean Time Between Failures (MTBF)**: > 30 days
- **Mean Time To Recovery (MTTR)**: < 1 minute (restart application)

**Definition**: Reliability measured during normal offline usage without network/database dependencies

### 3.2 Error Recovery

| Error Type | Recovery Strategy | Target Recovery Time |
|-----------|-------------------|----------------------|
| Network error | N/A (offline app) | N/A |
| File write failure | Log error, notify user, allow retry | < 5 seconds |
| Corrupted data file | Restore from backup (future), show error | Manual intervention |
| Invalid state | Log and skip invalid transactions, continue | < 100ms |
| Out of memory | Graceful shutdown with save attempt | < 5 seconds |

**Philosophy**: Fail safely. Never lose data due to technical errors.

### 3.3 Crash Prevention

- Zero unhandled exceptions (except fatal OS-level errors)
- All exceptions caught and logged
- User-friendly error messages displayed
- Application continues running after recoverable errors

### 3.4 Data Durability

- Every transaction written to disk before UI confirms
- File sync to disk before returning success
- No in-memory-only changes lost on crash
- Transaction logs immutable (append-only semantics)

---

## 4. Data Integrity Requirements

### 4.1 Consistency Guarantees

| Invariant | Guarantee | Verification |
|-----------|-----------|--------------|
| Balance = Sum of transactions | Always true | Calculated on each query |
| User owns account | Always true | Checked before access |
| Organization isolation | No leakage | organization_id filters in all queries |
| Transaction immutability | Cannot be modified post-completion | Exception thrown if attempted |
| Multi-tenant data | No cross-org visibility | SQL WHERE org_id in all queries |

**Enforcement**: Invariants enforced at domain layer, verified in tests

### 4.2 Atomicity for Complex Operations

**Operations that must be atomic** (all-or-nothing):

1. **Transfer Transaction**
   - Either both withdrawal and deposit succeed, or both fail
   - No partial transfers
   - Implementation: Transaction wrapper or batch write

2. **Task Approval**
   - Either task approved AND reward deposited, or both fail
   - No orphaned deposits or tasks
   - Implementation: Coordinated write to tasks.json and transactions.json

3. **Account Creation**
   - Either both CURRENT and SAVINGS accounts created, or both fail
   - No orphaned accounts
   - Implementation: Coordinated write to accounts.json

### 4.3 Referential Integrity

- Foreign key references must exist before creation
- Cascading soft-deletes (mark is_active = false)
- Validation on every related entity access
- Tests verify integrity constraints

### 4.4 Monetary Accuracy

- All amounts stored as `Long` (cents, integer)
- No floating-point arithmetic
- Rounding rules: Round to nearest cent (0.5 rounds up)
- Formula verification:
  - `Account.balance = Sum(deposits) - Sum(withdrawals)`
  - Must equal actual balance in file

**Example**:
```
Initial balance: $0
Deposit $50.50 → 5050 cents ✅
Withdraw $10.25 → 1025 cents ✅
Final balance: $40.25 → 4025 cents ✅
```

### 4.5 Data Validation Rules

**At Write Time**:
- All required fields populated
- All monetary values positive (except balance, which is unsigned)
- All timestamps valid and consistent (created_at ≤ updated_at)
- All IDs and foreign keys valid

**At Read Time**:
- Deserialize only valid JSON schemas
- Skip corrupted records with error log (don't crash)
- Validate data types match expected (catch type conversion errors)

---

## 5. Security Requirements

### 5.1 Authentication & Access Control

| Requirement | Implementation | Notes |
|-------------|-----------------|-------|
| PIN-based login | bcrypt hashing | 4-6 digit PIN hashed, never plaintext |
| Account lockout | 5 failed attempts → lock 15 min | Future: implement in v1.1 |
| Session timeout | 30 minutes idle | Auto-logout after inactivity |
| Multi-child management | Parent can manage multiple children | Parent sees all children accounts |
| Parent privacy | Child cannot see parent PIN | Different login screens |

### 5.2 Data Protection

| Requirement | Approach | Status |
|-------------|----------|--------|
| PIN storage | bcrypt hashing (not plaintext) | Mandatory |
| Data encryption at rest | Files stored plaintext (MVP) | Future: encryption optional |
| Transmission security | Offline only (no transmission) | N/A |
| Backup security | Manual exports stored securely | User responsibility |

**Security Philosophy for MVP**:
- Focus on preventing accidental access (PIN login)
- Not designed for malicious user on same system
- Suitable for home environment (low external threat)
- Future version can add encryption if needed

### 5.3 Multi-Tenant Security

- **Data Isolation**: Every query filters by `organization_id`
- **Verification**: Tests confirm no data leakage across orgs
- **Code Review**: Critical paths reviewed for isolation
- **Testing**: Specific test cases for multi-tenant scenarios

**Example Violation** (must be prevented):
```
// ❌ WRONG - No organization filter
List<Transaction> allTransactions = repo.findAll();

// ✅ CORRECT - Filter by organization
List<Transaction> txns = repo.findByOrganization(organizationId);
```

### 5.4 Input Validation & Injection Prevention

**Application is offline, so primary threats are**:
- Malformed data in JSON files (data corruption)
- Buffer overflow from large inputs (theoretical)
- Integer overflow in monetary amounts

**Mitigation**:
- Validate all input before processing
- Limit input sizes (max 255 chars for names, etc.)
- Use bounded integers for amounts (Long vs BigDecimal)
- Type safety via Jackson JSON deserialization

### 5.5 Audit Trail

**What Gets Logged**:
- User login/logout with timestamp
- Transaction creation with details (amount, account, initiator)
- Task submission/approval with details
- Failed operations (insufficient balance, invalid state, etc.)
- System errors (file I/O, corruption)

**Log Destination**: `~/.virtualbank/logs/app.log`

**Log Retention**: Keep 7 days of logs (configurable)

**Log Format**:
```
[2024-01-15 14:30:22.123] [INFO] [TransactionService] Deposit processed: txn_id=uuid-5, amount=5000, account=ACC-001
[2024-01-15 14:30:22.456] [ERROR] [FileRepository] Read failed: file=/path/transactions.json, error=IOException
```

### 5.6 Security Testing

- [ ] Unit tests for PIN hashing (bcrypt)
- [ ] Tests for multi-tenant isolation
- [ ] Tests for authorization checks
- [ ] Tests for input validation
- [ ] Security code review checklist (before release)

---

## 6. Maintainability Requirements

### 6.1 Code Quality Standards

| Metric | Target | Verification |
|--------|--------|--------------|
| Code coverage | 90%+ | Jacoco coverage report |
| Cyclomatic complexity | < 10 per method | SonarQube analysis |
| Duplication | < 5% | SonarQube analysis |
| Documentation | 100% of public API | JavaDoc coverage |
| Code style | Consistent | Checkstyle enforcement |

### 6.2 Naming Conventions

- **Classes**: PascalCase (e.g., `TransactionService`)
- **Methods**: camelCase (e.g., `depositMoney()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_PIN_LENGTH`)
- **Variables**: camelCase (e.g., `accountBalance`)
- **Packages**: lowercase.dots (e.g., `com.virtualbank.domain`)

### 6.3 Documentation Standards

**Java Code**:
- Public classes/methods have JavaDoc comments
- Complex logic has inline comments
- Assumptions documented (e.g., "Amount in cents")
- Examples provided for public APIs

**Example**:
```java
/**
 * Deposits money into an account.
 *
 * @param account the target account
 * @param amountInCents amount to deposit (positive long)
 * @return updated account with new balance
 * @throws InvalidAmountException if amount <= 0
 */
public BankAccount deposit(BankAccount account, long amountInCents) {
  // Implementation...
}
```

### 6.4 Version Control

- **Repository**: Git
- **Branching**: Feature branches, merge via pull request
- **Commit Messages**: Clear, descriptive, conventional commits
- **History**: Maintain clean, navigable history

**Example Commit Message**:
```
feat(transaction): implement deposit with balance validation

- Add Deposit operation to TransactionService
- Validate amount > 0 and account exists
- Update account balance and persist transaction
- Add unit tests (90% coverage)

Fixes #123
```

### 6.5 Refactoring & Technical Debt

- **Code Review**: Every change reviewed before merge
- **Debt Tracking**: Document known tech debt with priority
- **Debt Paydown**: Allocate 20% of sprint capacity to tech debt
- **Deprecation**: Mark deprecated APIs, provide migration path

---

## 7. Scalability Requirements

### 7.1 Data Growth (MVP)

| Scenario | Support Level | Action if Exceeded |
|----------|---------------|--------------------|
| 100 transactions per account | ✅ Full support | No action needed |
| 1,000 transactions per account | ✅ Full support | No action needed |
| 10,000 transactions per account | ✅ Supported | Optimize file I/O |
| 100,000+ transactions per account | ❌ Not supported | Requires database, deferred to v2 |

**Storage**: 10,000 transactions ≈ 5-10 MB per account (JSON format)

**Performance Assumption**: File I/O acceptable up to 10K transactions

### 7.2 User Growth (MVP)

| Users | Support Level | Constraint |
|-------|---------------|----|
| 1 parent + 5 children | ✅ Full support | Typical family |
| 10 users (1 org) | ✅ Supported | Larger family |
| 100+ users (1 org) | ⚠️ Limited | Not tested, may be slow |
| Multiple orgs | ❌ MVP | Multi-org UI not implemented |

**MVP Assumption**: Designed for single family per installation

### 7.3 Future Scalability (Post-MVP)

**Planned for Version 2.0** (not MVP):
- Database option (PostgreSQL, MySQL)
- Distributed file storage (cloud)
- Caching layer (Redis)
- Load balancing for concurrent users
- Horizontal scaling via microservices

**Design Decision**: Current architecture allows database swap without major refactoring (Repository pattern)

---

## 8. Usability Requirements

### 8.1 User Interface Standards

| Aspect | Requirement | Implementation |
|--------|-------------|-----------------|
| **Response** | UI never freezes | Async file I/O, threading |
| **Feedback** | User knows what's happening | Toast messages, progress indicators |
| **Errors** | Clear error messages in user language | No error codes, plain English |
| **Navigation** | Intuitive flow between screens | Clear menus, back buttons |
| **Accessibility** | Readable text, visible buttons | Min 12pt font, high contrast |

### 8.2 Child-Friendly Interface

- **Language**: Simple, age-appropriate (8-15 years old)
- **Colors**: Engaging but not distracting
- **Animations**: Fun but not overwhelming (smooth transitions)
- **Icons**: Clear, recognizable symbols
- **Confirmations**: Ask before destructive actions

**Example Text**:
- ✅ "You have $50.00 in your Current Account"
- ❌ "Account balance: 5000 cents"

### 8.3 Parent Assessment Dashboard

- **Clear Information**: Balance, transaction history at a glance
- **Quick Controls**: Easy to create tasks, approve submissions
- **Reports**: Transaction exports for personal records

### 8.4 First-Time User Experience

- **Onboarding**: Guide user through account setup (5 minutes)
- **Defaults**: Sensible defaults (e.g., USD currency, no PIN initially)
- **Help**: Tooltips, short help text on screens
- **Tutorial**: Optional walkthrough for first task

---

## 9. Compatibility & Platform Requirements

### 9.1 Platform Support

| Platform | Version | Status | Notes |
|----------|---------|--------|-------|
| Windows | 10, 11, Server 2019+ | ✅ Required | Win32 API, .NET runtime (JavaFX handles) |
| macOS | 10.15+ | ✅ Required | Intel & Apple Silicon (Java 21 universal) |
| Linux | Ubuntu 18.04+, Debian 10+ | ✅ Required | GTK+ 3+ required by JavaFX |

### 9.2 Java/JavaFX Requirements

- **Java Version**: 21 LTS (minimum, latest preferred)
- **JavaFX Version**: 21.0.x (matching Java version)
- **JVM Mode**: 64-bit (no 32-bit support)
- **Heap Memory**: Minimum 256 MB, recommended 512 MB

### 9.3 Disk Space Requirements

- **Installation**: 200 MB (Java runtime + JavaFX + app)
- **Data Storage**: 10 MB per 1000 transactions (JSON format)
- **Logs**: 5 MB (7-day retention)
- **Total Minimum**: 300 MB free space recommended

### 9.4 Display Requirements

| Aspect | Requirement | Notes |
|--------|-------------|-------|
| **Resolution** | Minimum 1024x768 | Most common netbook size |
| **Resolution** | Recommended 1920x1080 | Full desktop experience |
| **DPI Scaling** | Auto-scaling supported | Windows, macOS, Linux |
| **Color Depth** | 24-bit color minimum | Modern displays standard |

### 9.5 Localization

**MVP Scope**: English only

**Future (v2.0)**: Support for Spanish, French, Mandarin (estimated 2-3 weeks per language)

---

## 10. Technical Constraints

### 10.1 Offline-Only Constraint

- **No Internet**: Application must work completely without internet
- **No External Dependencies**: Cannot call external APIs
- **No Auto-Update**: Manual download for new versions
- **No Cloud Sync**: Future feature, not MVP

**Verification**: Test with network cable disconnected, firewall blocking app

### 10.2 No Database Constraint

- **JSON Only**: JSON files for all data (not CSV, not database)
- **Future Option**: Database can be swapped in v2.0 (Repository pattern)
- **File Format**: UTF-8 encoded, valid JSON schema

**Rationale**: Simplicity, no server setup, inspectable data files

### 10.3 Standalone Constraint

- **Single JAR/Executable**: Ideally single downloadable file
- **No Installation**: Extract and run (or installer for convenience)
- **No Dependencies**: All dependencies bundled or included
- **No Services**: No background services or daemons

### 10.4 Desktop-Only Constraint

- **No Mobile**: Smartphone/tablet apps not in MVP
- **No Web**: Web-based version not in MVP
- **No Embedded**: Not for IoT or embedded devices

---

## 11. Compliance & Standards

### 11.1 Data Protection

**COPPA Compliance** (Children's Online Privacy Protection Act - USA):
- MVP is offline only, so COPPA not directly applicable
- However, design with child privacy in mind:
  - No external data sharing
  - No analytics tracking
  - No third-party services
  - Clear parental consent via parent login

**GDPR Compliance** (Europe):
- Right to be forgotten: Manual data deletion (export, file deletion)
- Data portability: Export to CSV supported
- Transparency: Clear privacy in user manual

**Recommendation**: Legal review before international release (post-MVP)

### 11.2 WCAG Accessibility (Future)

**MVP Status**: Baseline accessibility

**Post-MVP WCAG 2.1 Level AA**:
- Screen reader support (JavaFX FX CSS)
- Keyboard navigation
- Color contrast (WCAG standards)
- Alternative text for icons

---

## 12. Development & Testing Standards

### 12.1 Code Review Checklist

Before every merge, verify:
- ✅ Code follows naming conventions and style guide
- ✅ Complex logic has comments
- ✅ Public API has JavaDoc
- ✅ Unit tests written (90%+ coverage)
- ✅ No compiler warnings
- ✅ No hardcoded secrets (PINs, tokens)
- ✅ No sensitive data in logs (PINs, account numbers)
- ✅ Multi-tenant isolation verified (for data access)
- ✅ Edge cases tested (zero amounts, null handling)

### 12.2 Testing Standards

**Unit Tests**:
- Framework: JUnit 5
- Mocking: Mockito
- Target: 90%+ code coverage (domain + application layers)

**Integration Tests**:
- Test layer interactions (service + repository)
- Test workflows (task approval, transfer)
- Test data persistence

**E2E Tests**:
- Manual testing of user scenarios
- Cross-platform testing (Windows, Mac, Linux)
- Performance testing under load

**Test Data**:
- Use builders and fixtures
- No production data in tests
- Isolated test state (no cross-test dependencies)

### 12.3 Build & Deployment Standards

**Build Process**:
1. Compile Java code
2. Run unit + integration tests
3. Generate test coverage report
4. Analyze code quality (SonarQube optional)
5. Build JAR artifact
6. Package with JavaFX runtime

**Pre-Release Checklist**:
- [ ] All tests passing (90%+ coverage)
- [ ] No compiler warnings
- [ ] Performance tested
- [ ] Multi-tenant isolation verified
- [ ] Platform testing (Windows, Mac, Linux)
- [ ] User manual reviewed
- [ ] Release notes prepared
- [ ] Backup/recovery tested
- [ ] Security review completed

---

## 13. Summary Table

| Category | Requirement | Target | Status |
|----------|-------------|--------|--------|
| **Performance** | Transaction processing | < 100ms | ⏳ To be verified |
| **Reliability** | Uptime | 99.9% | ⏳ To be verified |
| **Data Integrity** | Data loss incidents | 0 | ⏳ To be verified |
| **Security** | Authentication | PIN-based bcrypt | ⏳ To be implemented |
| **Maintainability** | Code coverage | 90%+ | ⏳ Target for Week 16 |
| **Scalability** | Max transactions | 10,000+ per account | ⏳ To be tested |
| **Usability** | Setup time | < 5 minutes | ⏳ To be verified |
| **Compatibility** | Platforms | Windows, macOS, Linux | ⏳ To be tested |

---

## 14. Success Criteria (NFRs)

The application successfully meets NFR requirements when:

- ✅ **Performance**: All operations complete within acceptable time
- ✅ **Reliability**: No crashes, graceful error handling
- ✅ **Data Integrity**: Zero data loss or corruption cases
- ✅ **Security**: PIN authentication works, multi-tenant isolation verified
- ✅ **Maintainability**: 90%+ test coverage, clean code, documented
- ✅ **Scalability**: Supports 10K transactions per account without issues
- ✅ **Usability**: New user understands app within 5 minutes
- ✅ **Compatibility**: Works on Windows, macOS, Linux without issues
- ✅ **Offline**: Application fully functional without internet

---

## 15. Non-Functional Testing Plan

| NFR Category | Testing Method | Success Criteria |
|-------------|-----------------|------------------|
| Performance | Load testing with 1000 txns | All operations < 100ms |
| Reliability | Crash testing (OS signals) | Graceful shutdown, data saved |
| Data Integrity | Corruption testing (manual file edit) | Detected and logged |
| Security | PIN testing (bcrypt verification) | Correct hashing, no plaintext |
| Maintainability | Code analysis (SonarQube) | 90%+ coverage, <5% duplication |
| Scalability | Stress testing (large datasets) | No degradation up to 10K txns |
| Usability | User testing (5+ children/parents) | All complete setup in < 5 min |
| Compatibility | Cross-platform testing | Works on all 3 platforms |

---

## 16. Future NFR Enhancements (Post-MVP)

- [ ] End-to-end encryption for data files
- [ ] Automated cloud backup option
- [ ] Advanced caching for performance
- [ ] Real-time collaboration (parent + child)
- [ ] Analytics and reporting dashboards
- [ ] Mobile app (iOS/Android)
- [ ] Web dashboard (read-only)
- [ ] WCAG 2.1 Level AA accessibility
- [ ] Multi-language support
- [ ] Email notifications

