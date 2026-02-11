Fivver Share and earn program

A Spring Boot application for Fiverr sellers to create short, shareable URLs for their gigs, track clicks, and calculate earnings ($0.05 per valid click).

## Features

- URL Shortening with Base62 encoding
- Duplicate Prevention with database constraints
- Click Tracking with fraud detection (100ms delay)
- Monthly Analytics with pagination
- REST API endpoints

---

## Setup

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker (for PostgreSQL)

### Installation

**1. Start PostgreSQL:**
```bash
docker run -d --name demo-postgres \
  -e POSTGRES_PASSWORD=secret \
  -e POSTGRES_USER=postgres \
  -p 5433:5432 postgres:16
```

**2. Build and Run:**
```bash
mvnw.cmd clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

Application runs on http://localhost:8080

### Environment Variables

Configure in `src/main/resources/application.properties` or override with environment variables:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5433/postgres
spring.datasource.username=postgres
spring.datasource.password=secret

# Application
app.base-url=http://localhost:8080

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

## Architecture

### Project Structure

```
src/main/java/com/fiverr/demo/
├── controller/          # REST endpoints
│   ├── LinkController.java      # POST /links, GET /stats
│   └── RedirectController.java  # GET /:shortCode
├── service/            # Business logic
│   ├── LinkService.java
│   └── FraudDetectionService.java
├── repository/         # Data access
│   ├── ShortenedLinkRepository.java
│   └── ClickRepository.java
├── entity/            # JPA entities
│   ├── ShortenedLink.java
│   └── Click.java
├── dto/               # Data transfer objects
└── util/
    └── Base62Encoder.java
```

### Component Flow

**Link Creation:**
```
POST /links → LinkController → LinkService
  → Check duplicate (findByTargetUrl)
  → Save to DB (get auto-increment ID)
  → Encode ID to Base62
  → Return short code
```

**Click Tracking:**
```
GET /:shortCode → RedirectController → LinkService
  → Find link (or 404)
  → FraudDetectionService (100ms delay, 90% valid)
  → Save click (is_valid, earnings)
  → Return 302 redirect
```

**Statistics:**
```
GET /stats → LinkController → LinkService
  → Fetch paginated links
  → Count valid clicks per link
  → Calculate earnings ($0.05 × valid clicks)
  → Get monthly breakdown (PostgreSQL TO_CHAR)
  → Return paginated results
```

### Database Schema

```sql
CREATE TABLE shortened_links (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) UNIQUE NOT NULL,
    target_url VARCHAR(2048) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE clicks (
    id BIGSERIAL PRIMARY KEY,
    link_id BIGINT NOT NULL REFERENCES shortened_links(id),
    clicked_at TIMESTAMP NOT NULL,
    is_valid BOOLEAN NOT NULL,
    earnings DECIMAL(10,2) NOT NULL
);
```

---

## API Endpoints

### POST /links
Create short link.

**Request:**
```json
{"targetUrl": "https://fiverr.com/seller/gig123"}
```

**Response:**
```json
{
  "shortCode": "1",
  "shortUrl": "http://localhost:8080/1",
  "targetUrl": "https://fiverr.com/seller/gig123"
}
```

### GET /:shortCode
Redirect to target URL. Records click and returns 302 redirect.

### GET /stats?page=0&size=10
Get paginated statistics.

**Response:**
```json
{
  "content": [{
    "shortCode": "1",
    "targetUrl": "https://fiverr.com/seller/gig123",
    "totalClicks": 15,
    "totalEarnings": 0.75,
    "monthlyBreakdown": {"2026-02": 10, "2026-01": 5}
  }],
  "totalPages": 1,
  "totalElements": 1
}
```

---

## Testing

### Automated Tests

37 JUnit tests covering all components.

```bash
# Run all tests
mvnw.cmd clean test

# Output: Tests run: 37, Failures: 0, Errors: 0
```

**Test Classes:**
- Base62EncoderTest (12 tests) - Encoding/decoding
- FraudDetectionServiceTest (4 tests) - Timing and validation
- LinkServiceTest (7 tests) - Business logic with mocks
- LinkControllerIntegrationTest (13 tests) - POST /links, GET /stats
- RedirectControllerIntegrationTest (7 tests) - GET /:shortCode

### Manual Testing

**Create Link:**
```bash
curl -X POST http://localhost:8080/links \
  -H "Content-Type: application/json" \
  -d '{"targetUrl":"https://fiverr.com/seller/test"}'
```

**Test Redirect:**
```bash
curl -L http://localhost:8080/1
```

**View Statistics:**
```bash
curl http://localhost:8080/stats?page=0&size=10
```

**Database Verification:**
```bash
docker exec demo-postgres psql -U postgres -d postgres \
  -c "SELECT * FROM shortened_links;"

docker exec demo-postgres psql -U postgres -d postgres \
  -c "SELECT * FROM clicks;"
```

---

## AI Environment Setup

This project was developed using Claude Code CLI (Anthropic).

### Code Generation Rules

**Architecture:**
- Follow Spring Boot layered architecture (Controller → Service → Repository → Entity)
- Use Spring Data JPA (no raw JDBC)
- Apply Bean Validation for input validation
- Include @Transactional for data operations

**Testing:**
- Unit tests with Mockito for services
- Integration tests with @SpringBootTest and H2 database
- Test happy paths and error scenarios

**Design Patterns:**
- Base62 encoding for URL shortening (deterministic, collision-free)
- Separate clicks table for historical tracking
- Database constraints for duplicate prevention
- Synchronous fraud detection with Thread.sleep

### Plugin Recommendations

- Spring Boot Plugin - Auto-completion
- JPA Plugin - Entity visualization
- REST Client - Endpoint testing
- Database Plugin - Schema viewing

### Context for AI Assistants

```bash
export PROJECT_TYPE="Spring Boot 3.5.10 with Java 17"
export DB_TYPE="PostgreSQL 16"
export ARCHITECTURE="Layered (Controller-Service-Repository)"
export TEST_FRAMEWORK="JUnit 5 + Mockito + Spring Boot Test"
```

---

## Troubleshooting

**Port 8080 in use:**
```bash
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Database connection failed:**
```bash
docker start demo-postgres
docker logs demo-postgres
```

**Tests failing:**
```bash
mvnw.cmd clean install
```

---

## Production Considerations

Before production deployment:

1. Security: Move credentials to env vars, add HTTPS, implement rate limiting
2. Performance: Add Redis caching, use read replicas, async click processing
3. Monitoring: Add Spring Boot Actuator, centralized logging
4. Configuration: Set ddl-auto=validate, use Flyway/Liquibase migrations
5. Scalability: Distributed cache, database sharding
