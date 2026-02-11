Fivver Share and earn program

## What Works

### Fully Implemented

1. **URL Shortening**
   - Base62 encoding using auto-increment database ID
   - Short codes: ID 1 → "1", ID 10 → "A", ID 62 → "10"
   - Deterministic and collision-free

2. **Duplicate Prevention**
   - Database UNIQUE constraint on target_url
   - Application-level duplicate check
   - Race condition handling with DataIntegrityViolationException

3. **Link Creation API**
   - POST /links endpoint with JSON validation
   - Bean Validation (@NotBlank, @Size max 2048)
   - Returns short code, short URL, and target URL
   - HTTP 400 for validation errors, 200 on success

4. **Redirect & Click Tracking**
   - GET /:shortCode endpoint with 302 redirect
   - Automatic click recording on every access
   - Fraud detection before recording
   - Click data: timestamp, is_valid flag, earnings

5. **Fraud Detection (Simulated)**
   - 100ms delay per click (Thread.sleep)
   - 90% valid rate, 10% fraud rate (random)
   - Valid clicks: $0.05, fraudulent clicks: $0.00

6. **Statistics & Analytics**
   - GET /stats endpoint with pagination (page, size parameters)
   - Per-link statistics: total clicks, earnings, monthly breakdown
   - Custom JPQL query using PostgreSQL TO_CHAR for monthly grouping
   - Spring Data Pagination with full metadata

7. **Database Schema**
   - shortened_links table: id, short_code, target_url, created_at
   - clicks table: id, link_id, clicked_at, is_valid, earnings
   - Indexes on short_code, target_url, link_id, clicked_at
   - Foreign key with ON DELETE CASCADE

8. **Testing Suite**
   - 37 JUnit tests, 100% pass rate
   - Unit tests with Mockito
   - Integration tests with H2 in-memory database
   - Test categories: Base62Encoder (12), FraudDetectionService (4), LinkService (7), Controllers (13+7)

9. **Spring Boot Architecture**
   - Layered: Controller → Service → Repository → Entity
   - Dependency injection with constructor-based injection
   - Transaction management with @Transactional
   - REST API with proper HTTP status codes

---

## What is Missing

### Not Implemented (Out of Scope)

1. **Authentication & Authorization** - No user management, API keys, or rate limiting. Anyone can create links and view all statistics.

2. **Custom Short Codes** - Users cannot specify their own vanity URLs. Only auto-generated Base62 codes.

3. **Link Expiration** - Links never expire, no TTL configuration, no automatic cleanup.

4. **Real Fraud Detection** - Only simulated. No IP tracking, user agent analysis, bot detection, or ML models.

5. **Analytics Dashboard** - No web UI, only JSON API responses.

6. **Link Management** - Cannot update or delete links via API. No bulk operations, categories, or tags.

7. **Custom Domain Support** - Hardcoded base URL (localhost:8080), no multi-domain support.

---

## Database Justification

### Schema Design

**Two-Table Approach:**
- shortened_links: Stores URL mappings
- clicks: Stores individual click events (one-to-many relationship)

**Why separate tables?**
- Preserves complete click history for auditing
- Enables monthly aggregation without data loss
- Prevents UPDATE race conditions (only INSERTs)
- Supports future analytics (hourly trends, patterns)

**Indexes:**
- idx_short_code: Fast redirect lookups (most frequent operation)
- idx_target_url: Duplicate detection on insert
- idx_link_id: JOIN performance for statistics
- idx_clicked_at: Time-based query optimization

---

## Design Decisions & Trade-offs

### 1. URL Shortening: Base62 vs Hash

**Chosen: Base62 encoding of auto-increment ID**

**Pros:**
- Zero collisions (deterministic)
- Simple implementation
- Sequential codes

**Cons:**
- Sequential codes leak business metrics
- 7 characters vs 6 for billions of URLs

**Alternative: Hash-based (MD5, SHA)**
- Rejected: Requires collision handling, more complex, no benefit for this use case

**Time:** Base62 took 30 minutes, hash-based would take 2 hours

---

### 2. Fraud Detection: Synchronous vs Asynchronous

**Chosen: Synchronous with Thread.sleep**

**Pros:**
- Simple implementation
- Easy to test
- No infrastructure overhead

**Cons:**
- Blocks thread for 100ms
- Limits throughput to ~10 requests/second per thread
- Thread pool exhaustion under heavy load

**Alternative: Async with queue (RabbitMQ, Kafka)**
- Rejected: Infrastructure complexity, overkill for demo scope

**Time:** Synchronous took 1 hour, async would take 6 hours

**Production:** Switch to async for >1000 requests/second

---

### 3. Statistics: Real-time vs Pre-aggregated

**Chosen: Real-time aggregation with JPQL**

**Pros:**
- Always accurate
- No additional storage
- Simple implementation

**Cons:**
- Expensive for large datasets
- Database CPU intensive
- Slow for millions of clicks

**Alternative: Pre-aggregated tables**
- Rejected: Requires background jobs, data staleness, dual writes

**Time:** Real-time took 2 hours, pre-aggregation would take 8 hours

**Performance:** Works up to ~1M clicks, recommend pre-aggregation beyond that

---

### 4. Duplicate Prevention: Hybrid Approach

**Chosen: Database constraint + Application check**

**Implementation:**
1. Check for existing URL in application
2. Try to insert new record
3. Catch DataIntegrityViolationException for race conditions

**Pros:**
- Fast path for known duplicates
- Safe under concurrent load
- No distributed lock needed

**Cons:**
- Two database queries in race condition
- More complex code

**Time:** Hybrid took 3 hours, simpler approaches would take 1 hour but risk data integrity

---

### 5. Testing: Unit + Integration vs E2E

**Chosen: Unit tests (Mockito) + Integration tests (H2)**

**Coverage:**
- Unit: Mock dependencies, test business logic, fast (<1s per test)
- Integration: H2 database, full Spring context, test endpoints (2s per test)

**Not Implemented: E2E tests**
- Would require full PostgreSQL + application
- Much slower (30+ seconds per test)

**Time:** Current suite took 8 hours, E2E would add 4 hours

---

### 6. Configuration: Properties File with Overrides

**Chosen: application.properties with environment variable overrides**

**Pros:**
- Sensible defaults for development
- Easy local setup
- Production can override sensitive values

**Cons:**
- Secrets visible in properties file (mitigated by .gitignore)

**Alternative: All environment variables**
- Rejected: Harder to set up locally

**Time:** Properties approach took 30 minutes

---

### 7. Error Handling: ResponseStatusException

**Chosen: Spring's ResponseStatusException**

**Example:**
```java
throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Short link not found");
```

**Pros:**
- Built-in Spring Boot support
- Automatic HTTP status mapping
- Less boilerplate

**Cons:**
- Less control over error format
- No custom error codes

**Alternative: Custom exception hierarchy + @ExceptionHandler**
- Rejected: More boilerplate (3 hours vs 30 minutes)

---

### 8. Validation: Bean Validation

**Chosen: Jakarta Validation API**

**Example:**
```java
@NotBlank(message = "Target URL is required")
@Size(max = 2048, message = "Target URL must not exceed 2048 characters")
private String targetUrl;
```

**Pros:**
- Declarative and clean
- Automatic validation
- Standard Java API

**Cons:**
- Additional dependency
- Learning curve for complex validations

**Alternative: Manual validation**
- Rejected: More code, harder to maintain (2 hours vs 1 hour)

---

## AI Usage & Prompts

#### Prompt 1: Initial Implementation

```
Implement the following plan:

create program that will do the following: the idea is shareable links. 
short, clean, trackable URLs that can point to any seller-owned page on fivver. 
a seller generates a short link for their gig->they post it on social media -> when someone clicks 
the link fivver redirects them to the original page and reward the seller with 0.05 dollars for each valid click. 
requirments: 
POST /links creates an endpoint to accept a target URL and return a unique short URL, if multiple requests are made for the same URL return the existing. 
GET /:short_code clicking the short code link must redirect to target URL, links must pass a fraud validation (simulate this with a function that takes 100ms to complete) 
record the link if passed validation. 
GET /stats- return paginated list of all generated links, for each one include original URL, total valid checks, total earnings, and a breakdown of links grouped by month. 

use postgres for DB 
```


#### Prompt 2: Database Cleanup

```
did you create a new database? where is it? and if the names table we previously
built is unnecessary- delete it
```


#### Prompt 3: Testing Implementation

```
testing: verify using postman provide an automated test suite that covers core functionalities (using JUnit)
```


#### Prompt 4: Documentation

```
add two documentation files:
the first is README.md contains: setup- steps to install and run project locally
(including env vars), architecture- the overall code structure and how main components
interact, testing- instructions for verifying functionality, AI env setup- plugind
rules and other costum inviorment. the second file is MANIFEST.mf contains: what works
in the program, what is missing, DB justifiation, trade off in the design decisions
taked due to time, AI usage and promts, copy the main promts used
```

