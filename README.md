# TripTrekker Flight Booking System - Repository Guidelines

## 📁 Project Structure
```
triptrekker/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/triptrekker/
│   │   │       ├── TriptrekkerApplication.java
│   │   │       ├── common/ # Shared across modules
│   │   │       │   ├── config/ # Redis, Jackson, etc.
│   │   │       │   ├── exception/ # Global exception handling
│   │   │       │   └── util/ # Shared utilities
│   │   │       └── modules/ # Business capabilities
│   │   │           ├── flightsearch/ # Flight search module
│   │   │           │   ├── FlightSearchModule.java # Module marker
│   │   │           │   ├── api/ # Public interfaces (other modules can use)
│   │   │           │   │   └── FlightSearchService.java
│   │   │           │   ├── internal/ # Private implementation (NO external access!)
│   │   │           │   │   ├── FlightSearchServiceImpl.java
│   │   │           │   │   ├── cache/
│   │   │           │   │   ├── provider/ # Amadeus integration
│   │   │           │   │   └── repository/
│   │   │           │   └── model/ # DTOs & entities (public)
│   │   │           │       ├── FlightSearchRequest.java
│   │   │           │       └── FlightSearchResponse.java
│   │   │           ├── booking/ # Booking module
│   │   │           │   ├── BookingModule.java
│   │   │           │   ├── api/
│   │   │           │   ├── internal/
│   │   │           │   └── model/
│   │   │           ├── payment/ # Payment module
│   │   │           │   ├── PaymentModule.java
│   │   │           │   ├── api/
│   │   │           │   ├── internal/
│   │   │           │   └── model/
│   │   │           ├── notification/ # Notification module
│   │   │           │   ├── NotificationModule.java
│   │   │           │   ├── api/
│   │   │           │   ├── internal/
│   │   │           │   └── model/
│   │   │           └── integration/ # External providers
│   │   │               ├── IntegrationModule.java
│   │   │               ├── api/
│   │   │               └── internal/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/ # Flyway scripts
│   └── test/
│       └── java/
└── docker-compose.yml
```

## 🎯 Core Principles

### 1. **Module Isolation (The Golden Rule)**
- **NEVER** import classes from another module's `internal` package
- **ONLY** use classes from `api` packages for cross-module communication
- Spring Modulith will **fail the build** if you violate this

### 2. **Access Modifiers by Package**

| Package | Access Modifier | Can be used by |
|---------|----------------|----------------|
| `modules/xxx/api/` | `public` | Any module |
| `modules/xxx/model/` | `public` | Any module (DTOs/Entities) |
| `modules/xxx/internal/` | **package-private (no modifier)** | Only within same module |
| `modules/xxx/` (Module class) | `public` | Spring container |
| `common/` | `public` | All modules |

### 3. **Module Communication Rules**

✅ **CORRECT - Using API:**
```java
// In booking module
package com.triptrekker.modules.booking.api;

import com.triptrekker.modules.flightsearch.api.FlightSearchService;  // OK - using API

@Service
public class BookingService {
    private final FlightSearchService flightSearch;  // OK - public interface
}
```

❌ **WRONG - Using Internal:**
```java
// In booking module
package com.triptrekker.modules.booking.api;

import com.triptrekker.modules.flightsearch.internal.FlightSearchServiceImpl;  // ERROR!

@Service
public class BookingService {
    private final FlightSearchServiceImpl impl;  // Will fail verification!
}
```

## 📝 Implementation Guidelines

### Creating a New Module
1. Create the package structure:
```bash
modules/newmodule/
├── NewModule.java           # Module marker
├── api/                     # Public interfaces
├── internal/                # Private implementation
└── model/                   # DTOs and entities
```

2. Create module marker class:
```java
package com.triptrekker.modules.newmodule;

import org.springframework.stereotype.Component;

@Component
public class NewModule {
    // Empty - just marks the module
}
```

3. Define API interface (public):
```java
package com.triptrekker.modules.newmodule.api;

public interface NewModuleService {
    SomeResponse doSomething(SomeRequest request);
}
```

4. Implement in internal (package-private):
```java
package com.triptrekker.modules.newmodule.internal;

import com.triptrekker.modules.newmodule.api.NewModuleService;
import org.springframework.stereotype.Service;

@Service
class NewModuleServiceImpl implements NewModuleService {  // NO public!

    @Override
    public SomeResponse doSomething(SomeRequest request) {
        // implementation
        return null;
    }
}
```

5. Create model classes (public):
```java
package com.triptrekker.modules.newmodule.model;

import lombok.Data;

@Data
public class SomeRequest {
    private String field;
}
```

## 🏗️ Building & Verification

Verify Module Boundaries:
```bash
./mvnw clean verify
```
This will fail if:
- Any module accesses another's internal package
- There are circular dependencies between modules
- Module boundaries are violated

Run Tests with Module Validation:
```bash
./mvnw test
```

## 🔄 Event Communication Between Modules
Preferred method: Spring Application Events

### Step 1: Publish event in source module
```java
// In flightsearch module
@Service
class FlightSearchServiceImpl implements FlightSearchService {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void searchCompleted(String query) {
        eventPublisher.publishEvent(new FlightSearchCompletedEvent(query));
    }
}
```

### Step 2: Listen in target module
```java
// In booking module (using API event class)
package com.triptrekker.modules.booking.internal;

import com.triptrekker.modules.flightsearch.api.events.FlightSearchCompletedEvent;

@Component
class BookingEventListener {
    
    @EventListener
    public void handleFlightSearch(FlightSearchCompletedEvent event) {
        // React to flight search completion
    }
}
```

### Event class (in API package)
```java
package com.triptrekker.modules.flightsearch.api.events;

public class FlightSearchCompletedEvent {
    private final String query;
    
    public FlightSearchCompletedEvent(String query) {
        this.query = query;
    }
    
    public String getQuery() { return query; }
}
```

## 📦 Database Conventions

### Schema Organization
- Each module may have its own tables
- Use module prefix for table names: `flightsearch_*`, `booking_*`, `payment_*`
- Foreign keys can cross modules but document dependencies



## 🚫 Common Mistakes to Avoid

### ❌ DO NOT:
- Make internal classes public
- Import from `*.internal.*` in other modules
- Create circular dependencies (Module A → B → A)
- Put business logic in common package
- Use `@ComponentScan` without specifying packages

### ✅ DO:
- Keep internal classes package-private
- Expose only interfaces in api packages
- Use events for cross-module communication
- Keep common for truly shared utilities only
- Let Spring Modulith auto-configure component scanning

## 🔧 Development Workflow

1. Local Development Setup
```bash
# Start dependencies
docker-compose up -d

# Run application
./mvnw spring-boot:run
```

2. Verification Before Commit
```bash
# Format code (if using formatter)
./mvnw spotless:apply

# Run tests and verify module boundaries
./mvnw clean verify
```

If verification fails, check for:
- Internal package imports
- Circular dependencies

### Common Verification Errors & Fixes
- Error: Module 'booking' accesses internal type of 'flightsearch'
    - Fix: Move the accessed class to api package or use events
- Error: Cycle detected: flightsearch -> booking -> flightsearch
    - Fix: Break cycle by moving shared logic to common or use events

## 📊 Module Dependency Graph
Allowed dependencies (simplified):

```
flightsearch ← booking ← payment
    ↓           ↓
notification ← integration
```

Rules:
- `common` can be used by any module
- No module depends on notification (it's a leaf)
- `integration` is only used by flightsearch initially
