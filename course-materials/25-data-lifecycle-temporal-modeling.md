# Week 25: Data Lifecycle & Temporal Modeling

## The Story: "We Lost $2.4M Because We Couldn't Prove What the Price Was"

The litigation conference room was silent as the insurance company's attorney displayed the email timestamp: March 15, 2023, 2:47 PM.

"Your system shows the premium was $8,200 per month on March 15th," the attorney said. "But our client's contract, signed on March 15th at 2:30 PM, clearly states $6,500 per month. Someone in your company changed the data after the contract was signed."

Sarah, the CTO, felt her stomach drop. She knew the truth: the underwriter had updated the premium from $6,500 to $8,200 that afternoon after discovering a risk factor they'd missed. It was a legitimate business change. But their database had no history tracking. The old value was gone. Overwritten. Forever.

"We have no way to prove what the price was at 2:30 PM," she admitted quietly to her legal team during the break. "Our database just updates records in place. We don't keep history."

The company lost the lawsuit. $2.4 million settlement, plus legal fees, plus the reputation damage of looking like they'd tampered with data. All because their data model treated everything as "current state only."

Six months later, Sarah was rebuilding their entire data architecture with temporal modeling: system-versioned tables tracking every change, valid-time ranges showing when business facts were true, transaction timestamps showing when the database learned about changes, and complete audit trails linking every modification to a user and reason.

"We're not just storing data anymore," she told her team. "We're storing history. Truth over time. The ability to answer 'what did we know, and when did we know it?' Because in the real world, that question can cost you millions if you can't answer it."

---

## Introduction: Data Changes Over Time

In Weeks 23-24, you learned to model and optimize data structures. But we treated data as static snapshots: a customer has one address, a product has one price, an order has one status. In reality, data has a **lifecycle**:

- Customer addresses **change** when they move
- Product prices **evolve** based on market conditions
- Order statuses **transition** from Draft → Scheduled → In Progress → Completed
- Regulations require proving **what data looked like historically**
- Business users ask "Show me what this looked like on June 15th"
- Auditors demand "Who changed this and when?"

This week explores **temporal modeling**: techniques for tracking data over time, maintaining history, and answering questions about the past. You'll learn when different approaches are appropriate and how to implement them in production systems.

### Why This Matters for You

Your petroleum terminal operations background prepared you perfectly for temporal modeling:

- **Inventory levels change constantly** - tanks fill and empty, but you need historical volumes for reconciliation
- **Pricing changes** - commodity prices fluctuate, but invoices must reflect the price at the time of loading
- **Regulatory compliance** - you must prove tank volumes, product specifications, and safety checks at specific points in time
- **Operational decisions** - "Why did we schedule that load?" requires knowing what information was available when the decision was made

In your MBA studies, you've encountered this in accounting (historical cost vs. current value), supply chain (inventory snapshots), and strategy (competitive position over time).

As a backend engineer, you'll face this constantly:
- "Undo" functionality requires knowing previous states
- Audit logs are legal requirements in finance, healthcare, and industrial systems
- Debugging production issues requires "What did the data look like when the bug occurred?"
- Analytics teams need historical trends, not just current snapshots

---

## Core Concept 1: The Temporal Dimensions

Every piece of data exists in **two temporal dimensions**:

### Valid Time (Business Time)
**When the fact was true in the real world.**

- A customer lived at 123 Main St from Jan 1, 2020 to Mar 15, 2023
- A product's price was $150 from Apr 1 to Jun 30, 2024
- A tank held 50,000 barrels of diesel at 8:00 AM on July 15th

Valid time is determined by **domain events and business rules**, not database operations.

### Transaction Time (System Time)
**When the database learned about the fact.**

- The customer's address change was recorded in the database on March 16, 2023 at 9:00 AM (one day after they moved)
- The price change was entered on Mar 25, 2024 (retroactively set to Apr 1 effective date)
- The tank measurement was logged at 8:05 AM (5 minutes after the actual reading)

Transaction time is determined by **database operations** (INSERT, UPDATE, DELETE), not business events.

### Bi-Temporal Modeling: Both Dimensions

Real-world systems often need **both**:

**Question:** "What did we think the customer's address was on March 20, 2023?"
- Valid time: March 20, 2023
- Transaction time: March 20, 2023
- **Answer:** 123 Main St (we hadn't recorded the move yet)

**Question:** "What do we now know about where the customer lived on March 20, 2023?"
- Valid time: March 20, 2023
- Transaction time: Current (today)
- **Answer:** Still at 123 Main St (they moved on March 15, after March 20)

**Question:** "When did we learn that the customer moved?"
- Transaction time: March 16, 2023 at 9:00 AM

This separation enables **corrections** (fixing data entry errors) versus **updates** (recording new facts).

---

## Core Concept 2: Slowly Changing Dimensions (SCD)

Ralph Kimball's dimensional modeling introduced **Slowly Changing Dimensions**: attributes that change infrequently but must be tracked. There are six standard types:

### Type 0: Retain Original
**Never change the value, even if it's wrong.**

Use when: Historical integrity matters more than accuracy.

```sql
CREATE TABLE products (
    product_id UUID PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL,
    initial_classification VARCHAR(100) NOT NULL,  -- Never changes
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Example:** Product's original category at launch, even if you later reclassify it. Analytics need stable groupings.

### Type 1: Overwrite
**Update in place, losing history.**

Use when: Only current value matters, history is irrelevant.

```sql
UPDATE customers
SET phone_number = '555-0123'
WHERE customer_id = 'abc-123';

-- Old phone number is gone forever
```

**Example:** Correcting a typo in a customer's name. The old (incorrect) value has no business value.

**Petroleum Terminal Example:** Correcting a data entry error where a bay was assigned the wrong number. The error never reflected reality, so there's no history to preserve.

### Type 2: Add New Row
**Create a new version row, preserving all history.**

Use when: Full history is required, changes are significant business events.

```sql
CREATE TABLE customers (
    customer_key BIGSERIAL PRIMARY KEY,           -- Surrogate key (unique per row)
    customer_id UUID NOT NULL,                     -- Business key (same across versions)
    customer_name VARCHAR(255) NOT NULL,
    billing_address TEXT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,                                 -- NULL = current version
    is_current BOOLEAN DEFAULT true,
    version INT NOT NULL,

    CHECK (valid_to IS NULL OR valid_to > valid_from),
    CHECK ((is_current = true AND valid_to IS NULL) OR
           (is_current = false AND valid_to IS NOT NULL))
);

-- Customer moves on Mar 15, 2023
-- Before:
-- customer_key | customer_id | billing_address | valid_from | valid_to | is_current
-- 1            | abc-123     | 123 Main St     | 2020-01-01 | NULL     | true

-- After:
UPDATE customers
SET valid_to = '2023-03-15',
    is_current = false
WHERE customer_id = 'abc-123'
  AND is_current = true;

INSERT INTO customers (customer_id, customer_name, billing_address, valid_from, is_current, version)
VALUES ('abc-123', 'Acme Corp', '456 Oak Ave', '2023-03-15', true, 2);

-- Result:
-- customer_key | customer_id | billing_address | valid_from | valid_to   | is_current | version
-- 1            | abc-123     | 123 Main St     | 2020-01-01 | 2023-03-15 | false      | 1
-- 2            | abc-123     | 456 Oak Ave     | 2023-03-15 | NULL       | true       | 2
```

**Query current version:**
```sql
SELECT * FROM customers
WHERE customer_id = 'abc-123'
  AND is_current = true;
```

**Query historical version (as of date):**
```sql
SELECT * FROM customers
WHERE customer_id = 'abc-123'
  AND valid_from <= '2023-01-15'
  AND (valid_to IS NULL OR valid_to > '2023-01-15');
```

**Petroleum Terminal Example:** Customer contract terms. When a customer renegotiates their pricing or credit terms, you need both the old contract (for historical invoices) and the new contract (for future loads).

### Type 3: Add New Column
**Store both old and new values in separate columns.**

Use when: Only the previous value matters (not full history), and there are few changes.

```sql
CREATE TABLE products (
    product_id UUID PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL,
    current_category VARCHAR(100) NOT NULL,
    previous_category VARCHAR(100),
    category_changed_at TIMESTAMP
);

-- Product is reclassified
UPDATE products
SET previous_category = current_category,
    current_category = 'Hazmat Class 3',
    category_changed_at = CURRENT_TIMESTAMP
WHERE product_id = 'diesel-123';
```

**Example:** Product category changes are rare, and you only need to track "what was it before?" for impact analysis.

**Limitation:** Only stores one previous value. If it changes again, you lose history before that.

### Type 4: Add History Table
**Store current state in main table, full history in separate table.**

Use when: Current queries must be fast, but full history is occasionally needed.

```sql
-- Main table (current state only)
CREATE TABLE products (
    product_id UUID PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    price_per_barrel NUMERIC(10,2) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- History table (all changes)
CREATE TABLE products_history (
    history_id BIGSERIAL PRIMARY KEY,
    product_id UUID NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    price_per_barrel NUMERIC(10,2) NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP,
    changed_by UUID NOT NULL,
    change_reason TEXT,

    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- Trigger to automatically populate history on UPDATE
CREATE OR REPLACE FUNCTION record_product_history()
RETURNS TRIGGER AS $$
BEGIN
    -- Close the previous history record
    UPDATE products_history
    SET valid_to = CURRENT_TIMESTAMP
    WHERE product_id = OLD.product_id
      AND valid_to IS NULL;

    -- Insert new history record
    INSERT INTO products_history
        (product_id, product_code, product_name, price_per_barrel, valid_from, changed_by)
    VALUES
        (NEW.product_id, NEW.product_code, NEW.product_name,
         NEW.price_per_barrel, CURRENT_TIMESTAMP, current_user_id());

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER product_history_trigger
AFTER UPDATE ON products
FOR EACH ROW
EXECUTE FUNCTION record_product_history();
```

**Petroleum Terminal Example:** Product pricing. Most queries need current price (fast lookup on main table). Historical pricing analysis queries the history table separately.

### Type 6: Hybrid (1+2+3)
**Combine Type 1, 2, and 3: Current value in main table, full history in rows, plus previous value.**

```sql
CREATE TABLE customers (
    customer_key BIGSERIAL PRIMARY KEY,
    customer_id UUID NOT NULL,
    customer_name VARCHAR(255) NOT NULL,

    -- Type 2: Full history
    valid_from DATE NOT NULL,
    valid_to DATE,
    is_current BOOLEAN DEFAULT true,

    -- Type 3: Quick access to previous
    previous_credit_limit NUMERIC(10,2),

    -- Type 1: Always overwrite (corrections)
    email VARCHAR(255) NOT NULL,  -- Typos just get fixed

    version INT NOT NULL
);
```

Rarely used due to complexity, but valuable when different attributes have different history requirements.

---

## Core Concept 3: System-Versioned Temporal Tables

Modern databases (PostgreSQL 13+, SQL Server 2016+, Oracle 12c+) provide **built-in temporal table support**:

### PostgreSQL Temporal Tables (System Versioning)

```sql
-- Create main table with system versioning
CREATE TABLE load_orders (
    load_order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    requested_quantity_barrels NUMERIC(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    scheduled_date DATE NOT NULL,

    -- System versioning columns (automatically managed)
    sys_period tstzrange NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL)
);

-- Create history table (automatically populated)
CREATE TABLE load_orders_history (
    LIKE load_orders,
    PRIMARY KEY (load_order_id, sys_period)
);

-- Set up system versioning
ALTER TABLE load_orders
ADD COLUMN sys_start TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN sys_end TIMESTAMP DEFAULT 'infinity';

-- Trigger to automatically maintain history
CREATE OR REPLACE FUNCTION load_orders_versioning()
RETURNS TRIGGER AS $$
BEGIN
    -- Insert old row into history
    INSERT INTO load_orders_history
    SELECT OLD.*, CURRENT_TIMESTAMP, 'infinity';

    -- Update sys_start for new row
    NEW.sys_start := CURRENT_TIMESTAMP;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER load_orders_versioning_trigger
BEFORE UPDATE ON load_orders
FOR EACH ROW
EXECUTE FUNCTION load_orders_versioning();
```

**Querying temporal data:**

```sql
-- Current state (normal query)
SELECT * FROM load_orders
WHERE load_order_id = 'abc-123';

-- Historical state (as of timestamp)
SELECT * FROM load_orders_history
WHERE load_order_id = 'abc-123'
  AND sys_start <= '2024-06-15 14:30:00'
  AND sys_end > '2024-06-15 14:30:00';

-- All versions (audit trail)
SELECT * FROM load_orders_history
WHERE load_order_id = 'abc-123'
ORDER BY sys_start;

-- Changes between two timestamps
SELECT * FROM load_orders_history
WHERE load_order_id = 'abc-123'
  AND sys_start >= '2024-06-01'
  AND sys_start < '2024-07-01'
ORDER BY sys_start;
```

**Petroleum Terminal Example:** A load order starts as DRAFT, becomes SCHEDULED, transitions to IN_PROGRESS, then COMPLETED. System versioning automatically captures every status transition with exact timestamps, enabling:
- "What was the status at 2:30 PM?" (for dispute resolution)
- "How long did this order spend in each status?" (for process improvement)
- "Who changed the scheduled date?" (audit trail)

---

## Core Concept 4: Audit Trails

Audit trails answer: **"Who did what, when, and why?"**

### Comprehensive Audit Table

```sql
CREATE TABLE audit_log (
    audit_id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    record_id UUID NOT NULL,
    operation VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),

    -- Who
    user_id UUID NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    user_role VARCHAR(100),

    -- When
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- What changed
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],  -- ['status', 'scheduled_date']

    -- Why
    change_reason TEXT,
    business_context VARCHAR(100),  -- 'customer_request', 'emergency_reschedule', etc.

    -- How (technical context)
    ip_address INET,
    user_agent TEXT,
    application_version VARCHAR(50),

    -- Indexes for common queries
    CHECK (occurred_at <= CURRENT_TIMESTAMP)
);

CREATE INDEX idx_audit_table_record ON audit_log(table_name, record_id, occurred_at DESC);
CREATE INDEX idx_audit_user ON audit_log(user_id, occurred_at DESC);
CREATE INDEX idx_audit_operation ON audit_log(operation, occurred_at DESC);
CREATE INDEX idx_audit_occurred ON audit_log(occurred_at DESC);
```

### Generic Audit Trigger

```sql
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
DECLARE
    old_json JSONB;
    new_json JSONB;
    changed_fields TEXT[];
BEGIN
    -- Capture old and new values as JSON
    IF (TG_OP = 'DELETE') THEN
        old_json := row_to_json(OLD)::JSONB;
        new_json := NULL;
    ELSIF (TG_OP = 'INSERT') THEN
        old_json := NULL;
        new_json := row_to_json(NEW)::JSONB;
    ELSIF (TG_OP = 'UPDATE') THEN
        old_json := row_to_json(OLD)::JSONB;
        new_json := row_to_json(NEW)::JSONB;

        -- Identify changed fields
        SELECT array_agg(key)
        INTO changed_fields
        FROM jsonb_each(old_json) o
        WHERE o.value IS DISTINCT FROM (new_json->o.key);
    END IF;

    -- Insert audit record
    INSERT INTO audit_log
        (table_name, record_id, operation, user_id, user_name,
         old_values, new_values, changed_fields)
    VALUES
        (TG_TABLE_NAME,
         COALESCE(NEW.id, OLD.id),  -- Assumes 'id' column
         TG_OP,
         current_setting('app.current_user_id')::UUID,
         current_setting('app.current_user_name'),
         old_json,
         new_json,
         changed_fields);

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Apply to specific tables
CREATE TRIGGER load_orders_audit_trigger
AFTER INSERT OR UPDATE OR DELETE ON load_orders
FOR EACH ROW
EXECUTE FUNCTION audit_trigger_function();
```

### Application-Level Audit Context

In your Java application, set the user context:

```java
@Component
public class AuditContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            User user = (User) auth.getPrincipal();

            // Set session variables for trigger to use
            jdbcTemplate.execute(
                "SELECT set_config('app.current_user_id', '" + user.getId() + "', true)"
            );
            jdbcTemplate.execute(
                "SELECT set_config('app.current_user_name', '" + user.getUsername() + "', true)"
            );
        }

        chain.doFilter(request, response);
    }
}

// In your service layer, add change reason
public void rescheduleLoadOrder(UUID loadOrderId, LocalDate newDate, String reason) {
    LoadOrder order = repository.findById(loadOrderId);

    // Set change reason in session variable
    jdbcTemplate.execute(
        "SELECT set_config('app.change_reason', '" + reason + "', true)"
    );

    order.setScheduledDate(newDate);
    repository.save(order);  // Trigger fires, captures reason
}
```

**Petroleum Terminal Example:** When a load order is rescheduled, the audit log captures:
- **Who:** Logan (Logistics Coordinator)
- **What:** Changed `scheduled_date` from June 15 to June 16
- **When:** 2024-06-10 14:23:17
- **Why:** "Customer requested delay due to driver availability"
- **How:** Web application v2.3.1, IP 192.168.1.45

---

## Core Concept 5: Soft Deletes vs Hard Deletes

### Hard Delete: Permanent Removal

```sql
DELETE FROM load_orders
WHERE load_order_id = 'abc-123';

-- Data is gone forever (unless you have database backups)
```

**Use when:**
- Legal requirement (GDPR "right to be forgotten")
- Test data cleanup
- No business value in retention

**Risk:** Can break referential integrity, lose audit trail, cannot answer "what was deleted?"

### Soft Delete: Mark as Deleted

```sql
ALTER TABLE load_orders
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID,
ADD COLUMN deletion_reason TEXT;

-- "Delete" operation
UPDATE load_orders
SET deleted_at = CURRENT_TIMESTAMP,
    deleted_by = 'user-123',
    deletion_reason = 'Duplicate entry created by error'
WHERE load_order_id = 'abc-123';

-- Query active records only
SELECT * FROM load_orders
WHERE deleted_at IS NULL;

-- Query including deleted (for audit)
SELECT * FROM load_orders
WHERE load_order_id = 'abc-123';  -- Sees deleted records

-- "Undelete" operation
UPDATE load_orders
SET deleted_at = NULL,
    deleted_by = NULL,
    deletion_reason = NULL
WHERE load_order_id = 'abc-123';
```

**Use when:**
- Regulatory compliance requires retention
- Accidental deletions are likely (undo capability)
- Deleted records have foreign key relationships
- Analytics need complete history

**Implementation Best Practice: Make it transparent with a view**

```sql
-- View that filters out soft-deleted records
CREATE VIEW active_load_orders AS
SELECT * FROM load_orders
WHERE deleted_at IS NULL;

-- Application code queries view instead of table
SELECT * FROM active_load_orders
WHERE customer_id = 'customer-456';
```

**Petroleum Terminal Example:** A load order is cancelled after the customer calls. Instead of hard deleting (losing the scheduling history and contract reference), soft delete it. If the customer calls back saying "Actually, we do want that load," you can undelete it. The audit trail shows the entire lifecycle: created → scheduled → cancelled → reactivated → completed.

---

## Core Concept 6: Event Sourcing

Instead of storing **current state**, store **all events** that led to that state.

### Event Store

```sql
CREATE TABLE load_order_events (
    event_id BIGSERIAL PRIMARY KEY,
    load_order_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id UUID NOT NULL,
    sequence_number INT NOT NULL,

    UNIQUE (load_order_id, sequence_number),
    CHECK (occurred_at <= CURRENT_TIMESTAMP)
);

CREATE INDEX idx_load_order_events_order ON load_order_events(load_order_id, sequence_number);
```

### Storing Events

```java
// Domain Events
public abstract class LoadOrderEvent {
    private final UUID loadOrderId;
    private final Instant occurredAt;
    private final UUID userId;
}

public class LoadOrderCreated extends LoadOrderEvent {
    private final UUID customerId;
    private final String productCode;
    private final BigDecimal requestedQuantity;
    private final LocalDate scheduledDate;
}

public class LoadOrderScheduled extends LoadOrderEvent {
    private final LocalDate scheduledDate;
    private final LocalTime scheduledTime;
}

public class LoadOrderStarted extends LoadOrderEvent {
    private final UUID bayId;
    private final UUID operatorId;
}

public class LoadOrderCompleted extends LoadOrderEvent {
    private final BigDecimal actualQuantity;
    private final Instant completedAt;
}

// Event Store Repository
@Repository
public class LoadOrderEventStore {

    public void append(UUID loadOrderId, LoadOrderEvent event) {
        int nextSequence = getNextSequenceNumber(loadOrderId);

        jdbcTemplate.update(
            "INSERT INTO load_order_events " +
            "(load_order_id, event_type, event_data, occurred_at, user_id, sequence_number) " +
            "VALUES (?, ?, ?::jsonb, ?, ?, ?)",
            loadOrderId,
            event.getClass().getSimpleName(),
            objectMapper.writeValueAsString(event),
            event.getOccurredAt(),
            event.getUserId(),
            nextSequence
        );
    }

    public List<LoadOrderEvent> getEvents(UUID loadOrderId) {
        return jdbcTemplate.query(
            "SELECT * FROM load_order_events " +
            "WHERE load_order_id = ? " +
            "ORDER BY sequence_number",
            new Object[]{loadOrderId},
            this::mapToEvent
        );
    }
}
```

### Reconstructing State from Events

```java
public class LoadOrder {
    private UUID id;
    private UUID customerId;
    private String productCode;
    private BigDecimal requestedQuantity;
    private LocalDate scheduledDate;
    private LoadOrderStatus status;
    private BigDecimal actualQuantity;

    // Reconstruct from events (event sourcing)
    public static LoadOrder fromEvents(List<LoadOrderEvent> events) {
        LoadOrder order = new LoadOrder();

        for (LoadOrderEvent event : events) {
            order.apply(event);  // Apply each event in sequence
        }

        return order;
    }

    private void apply(LoadOrderEvent event) {
        if (event instanceof LoadOrderCreated e) {
            this.id = e.getLoadOrderId();
            this.customerId = e.getCustomerId();
            this.productCode = e.getProductCode();
            this.requestedQuantity = e.getRequestedQuantity();
            this.status = LoadOrderStatus.DRAFT;
        } else if (event instanceof LoadOrderScheduled e) {
            this.scheduledDate = e.getScheduledDate();
            this.status = LoadOrderStatus.SCHEDULED;
        } else if (event instanceof LoadOrderStarted e) {
            this.status = LoadOrderStatus.IN_PROGRESS;
        } else if (event instanceof LoadOrderCompleted e) {
            this.actualQuantity = e.getActualQuantity();
            this.status = LoadOrderStatus.COMPLETED;
        }
    }
}

// Usage
List<LoadOrderEvent> events = eventStore.getEvents(loadOrderId);
LoadOrder order = LoadOrder.fromEvents(events);
```

### Projections (Read Models)

Event sourcing is write-optimized. For reads, create **projections**:

```sql
-- Materialized view (projection) for fast queries
CREATE MATERIALIZED VIEW load_orders_current_state AS
SELECT
    lo.load_order_id,
    (e.event_data->>'customerId')::UUID as customer_id,
    e.event_data->>'productCode' as product_code,
    (e.event_data->>'requestedQuantity')::NUMERIC as requested_quantity,
    COALESCE(
        (SELECT event_data->>'status'
         FROM load_order_events
         WHERE load_order_id = lo.load_order_id
         ORDER BY sequence_number DESC
         LIMIT 1),
        'DRAFT'
    ) as current_status,
    (SELECT MAX(occurred_at)
     FROM load_order_events
     WHERE load_order_id = lo.load_order_id) as last_updated
FROM (SELECT DISTINCT load_order_id FROM load_order_events) lo
JOIN LATERAL (
    SELECT * FROM load_order_events
    WHERE load_order_id = lo.load_order_id
      AND event_type = 'LoadOrderCreated'
    LIMIT 1
) e ON true;

-- Refresh projection after events are added
REFRESH MATERIALIZED VIEW CONCURRENTLY load_orders_current_state;
```

**Benefits of Event Sourcing:**
- **Complete audit trail** - Every change is an event
- **Time travel** - Reconstruct state at any point by replaying events up to that time
- **Debugging** - "Why is this in this state?" → Look at event sequence
- **Business insights** - Analyze event patterns (how long between Created and Scheduled?)
- **Replayability** - Change business logic and replay events to see different outcomes

**Drawbacks:**
- **Complexity** - More code, more infrastructure
- **Query difficulty** - Need projections for efficient reads
- **Storage** - Events accumulate (but storage is cheap, data loss is expensive)

**Petroleum Terminal Example:** Load order lifecycle is perfectly suited for event sourcing:
1. `LoadOrderCreated` (customer request)
2. `LoadOrderScheduled` (logistics coordinator assigns time slot)
3. `LoadOrderRescheduled` (customer delays)
4. `LoadOrderStarted` (driver arrives, loading begins)
5. `MeterReadingRecorded` (every 30 seconds during loading)
6. `LoadOrderCompleted` (loading finished)
7. `CarrierTicketGenerated` (documentation created)

You can answer: "Why did this load take 45 minutes when it usually takes 30?" by examining the sequence of MeterReadingRecorded events and finding the 10-minute gap where loading was paused.

---

## Domain Example: Petroleum Terminal - Comprehensive Temporal Model

Let's design a complete temporal data model for petroleum terminal operations:

### Tank Inventory (Bi-Temporal)

```sql
CREATE TABLE tank_inventory_measurements (
    measurement_id BIGSERIAL PRIMARY KEY,
    tank_id UUID NOT NULL,

    -- Valid time (when measurement was taken in real world)
    measured_at TIMESTAMP NOT NULL,

    -- Transaction time (when recorded in database)
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    corrected_at TIMESTAMP,  -- If measurement was later corrected

    -- Measurements
    volume_barrels NUMERIC(10,2) NOT NULL,
    temperature_fahrenheit NUMERIC(5,2),
    water_content_percentage NUMERIC(5,4),

    -- Audit
    measured_by UUID NOT NULL,  -- Operator who took reading
    measurement_method VARCHAR(50),  -- 'AUTOMATIC_GAUGE', 'MANUAL_STICK', etc.

    is_current_measurement BOOLEAN DEFAULT true,
    superseded_by BIGINT REFERENCES tank_inventory_measurements(measurement_id),

    FOREIGN KEY (tank_id) REFERENCES tanks(tank_id),
    CHECK (volume_barrels >= 0),
    CHECK (temperature_fahrenheit BETWEEN -60 AND 200),
    CHECK (water_content_percentage BETWEEN 0 AND 100)
);

CREATE INDEX idx_tank_inventory_valid_time
    ON tank_inventory_measurements(tank_id, measured_at DESC);
CREATE INDEX idx_tank_inventory_transaction_time
    ON tank_inventory_measurements(tank_id, recorded_at DESC);
```

**Query: "What was the tank volume at 8:00 AM on July 15th?" (valid time)**
```sql
SELECT volume_barrels
FROM tank_inventory_measurements
WHERE tank_id = 'tank-001'
  AND measured_at <= '2024-07-15 08:00:00'
ORDER BY measured_at DESC
LIMIT 1;
```

**Query: "What did we think the tank volume was at 8:00 AM when we ran the report at 9:00 AM?" (bi-temporal)**
```sql
SELECT volume_barrels
FROM tank_inventory_measurements
WHERE tank_id = 'tank-001'
  AND measured_at <= '2024-07-15 08:00:00'
  AND recorded_at <= '2024-07-15 09:00:00'
  AND is_current_measurement = true
ORDER BY measured_at DESC
LIMIT 1;
```

**Correction scenario:**
```sql
-- Original measurement (later found to be wrong)
INSERT INTO tank_inventory_measurements
    (tank_id, measured_at, volume_barrels, measured_by, measurement_method)
VALUES
    ('tank-001', '2024-07-15 08:00:00', 50000, 'operator-123', 'MANUAL_STICK');
-- measurement_id = 1001, recorded_at = 2024-07-15 08:05:00

-- Later, discover measurement was wrong (should have been 48,500)
-- Insert corrected measurement
INSERT INTO tank_inventory_measurements
    (tank_id, measured_at, volume_barrels, measured_by, measurement_method)
VALUES
    ('tank-001', '2024-07-15 08:00:00', 48500, 'operator-123', 'AUTOMATIC_GAUGE');
-- measurement_id = 1002, recorded_at = 2024-07-15 14:30:00 (when correction was made)

-- Mark original as superseded
UPDATE tank_inventory_measurements
SET is_current_measurement = false,
    superseded_by = 1002,
    corrected_at = CURRENT_TIMESTAMP
WHERE measurement_id = 1001;
```

Now queries using `is_current_measurement = true` get the corrected value, but the original incorrect measurement is preserved for audit purposes.

### Product Pricing (SCD Type 2)

```sql
CREATE TABLE product_pricing (
    pricing_id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL,

    -- Valid time range
    effective_from DATE NOT NULL,
    effective_to DATE,  -- NULL = current

    -- Pricing
    base_price_per_barrel NUMERIC(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',

    -- Version tracking
    is_current BOOLEAN DEFAULT true,
    version INT NOT NULL,

    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    reason_for_change TEXT,

    FOREIGN KEY (product_code) REFERENCES products(product_code),
    CHECK (effective_to IS NULL OR effective_to > effective_from),
    CHECK ((is_current = true AND effective_to IS NULL) OR
           (is_current = false AND effective_to IS NOT NULL))
);

-- Price history for diesel
INSERT INTO product_pricing
    (product_code, effective_from, base_price_per_barrel, version, created_by, reason_for_change)
VALUES
    ('DIESEL', '2024-01-01', 85.50, 1, 'user-123', 'Q1 2024 pricing'),
    ('DIESEL', '2024-04-01', 92.75, 2, 'user-123', 'Q2 2024 pricing - market increase');

-- Close previous version when new price is set
UPDATE product_pricing
SET effective_to = '2024-04-01',
    is_current = false
WHERE product_code = 'DIESEL'
  AND version = 1;
```

**Query: "What was the diesel price on March 15, 2024?"**
```sql
SELECT base_price_per_barrel
FROM product_pricing
WHERE product_code = 'DIESEL'
  AND effective_from <= '2024-03-15'
  AND (effective_to IS NULL OR effective_to > '2024-03-15');
-- Returns: 85.50
```

**Query: "Price history for diesel"**
```sql
SELECT effective_from, effective_to, base_price_per_barrel, reason_for_change
FROM product_pricing
WHERE product_code = 'DIESEL'
ORDER BY version;
```

### Load Orders (Event Sourcing + Audit Trail)

Already covered in detail above. The combination provides:
- **Event store**: Complete state transition history
- **Audit log**: Who/what/when/why for compliance
- **Current state projection**: Fast queries
- **Soft deletes**: Cancelled orders retained

---

## Hands-On Exercise: Build a Comprehensive Temporal Model

### Scenario

You're building the data model for a **fleet management system** for your petroleum terminal. Trucks and drivers are scheduled for loading operations, but this data changes constantly:

- **Trucks** have maintenance status that changes (active → maintenance → active)
- **Driver certifications** expire and must be renewed
- **Assignments** of drivers to trucks change
- **Regulatory audits** require proving "Which trucks were certified for hazmat on June 15, 2024?"
- **Billing disputes** require proving "What was the rate for truck rental on March 10?"

### Deliverables

**1. Design temporal tables for:**

a. **Trucks** (Type 2 SCD)
   - truck_id (business key)
   - license_plate
   - status (ACTIVE, MAINTENANCE, RETIRED)
   - valid_from, valid_to
   - Capture full history of status changes

b. **Driver Certifications** (Bi-temporal)
   - driver_id
   - certification_type (HAZMAT, TANKER, CDL_A)
   - issued_date (valid time - when cert was issued)
   - expiration_date (valid time - when cert expires)
   - recorded_at (transaction time - when added to database)
   - Allow for corrections if expiration date was recorded wrong

c. **Truck Rental Rates** (Type 4: Current + History)
   - Main table: current rates only (fast queries)
   - History table: all rate changes
   - Trigger to maintain history automatically

d. **Fleet Events** (Event Sourcing)
   - Event store for: TruckRegistered, TruckAssignedToDriver, MaintenanceStarted, MaintenanceCompleted, TruckRetired
   - Include projection for current truck-driver assignments

**2. Write SQL DDL for all tables**
   - Include all constraints (NOT NULL, CHECK, FK)
   - Add appropriate indexes
   - Create triggers for automatic history tracking where needed

**3. Implement soft delete for trucks**
   - Add deleted_at, deleted_by, deletion_reason
   - Create view that filters out deleted trucks
   - Show example of delete and undelete operations

**4. Create comprehensive audit log**
   - Generic audit_log table
   - Trigger function that works on any table
   - Apply to trucks and driver_certifications tables

**5. Write queries demonstrating temporal capabilities:**
   - "Which trucks were active on March 15, 2024?"
   - "Show the complete status history for truck T-001"
   - "What did we think the expiration date for driver D-123's HAZMAT cert was on April 1?" (recorded_at <= April 1)
   - "What is the actual expiration date for driver D-123's HAZMAT cert?" (current corrected value)
   - "Show all changes to truck T-001 in the audit log"
   - "What was the rental rate for truck type 'TANKER_5000GAL' on Feb 20, 2024?"

**6. Document your design decisions:**
   - Why did you choose Type 2 SCD for trucks vs Type 4 for rental rates?
   - When would you use hard delete vs soft delete for trucks?
   - What are the trade-offs of event sourcing for fleet events?
   - How would you handle GDPR "right to be forgotten" requests for driver data?

### Bonus Challenges

1. **Implement a complete event sourcing system** with:
   - Event store table
   - Java classes for events
   - Event store repository
   - State reconstruction from events
   - Materialized view projection

2. **Design a bi-temporal model for fuel pricing** where:
   - Valid time = when the price was effective in the market
   - Transaction time = when the price was recorded in your system
   - Handle late-arriving data (price change effective last week, but you learn about it today)

3. **Create a data retention policy** that:
   - Hard deletes audit logs older than 7 years (compliance requirement)
   - Archives event store to cold storage after 2 years
   - Keeps current state projections forever
   - Write SQL for automated cleanup jobs

---

## Reflection Questions

1. **Operations Experience:** In your petroleum terminal work, what data did you wish you could "go back in time" to see? How would temporal modeling have helped?

2. **MBA Program:** In your accounting coursework, you learned about historical cost vs. fair value. How does this map to transaction time vs. valid time in bi-temporal modeling?

3. **Industrial Systems:** Why is event sourcing particularly valuable for industrial automation systems where equipment state changes frequently?

4. **Compliance:** Your Navy background exposed you to strict documentation requirements. How do audit trails and temporal modeling support regulatory compliance in industrial systems?

5. **Design Trade-offs:** When would you choose Type 1 SCD (overwrite) vs Type 2 SCD (full history)? What business factors influence this decision?

---

## Key Takeaways

1. **Data has a lifecycle** - It's created, updated, corrected, and eventually deleted or archived. Your model must accommodate this.

2. **Two temporal dimensions** - Valid time (when fact was true) vs Transaction time (when database learned about it). Many systems need both.

3. **SCD types are patterns, not rules** - Choose based on business requirements: Type 1 when only current matters, Type 2 when full history matters, Type 4 when you need both performance and history.

4. **Audit trails are non-negotiable** in regulated industries - Healthcare, finance, energy, defense all require "who did what when and why."

5. **Soft deletes preserve relationships** - Hard deletes break foreign keys and lose context. Soft deletes enable undo and maintain referential integrity.

6. **Event sourcing provides ultimate audit trail** - Every state change is an event. Reconstruct any historical state by replaying events.

7. **System-versioned temporal tables** - Modern databases provide built-in temporal support. Use it instead of reinventing.

8. **Query patterns drive physical design** - "Show current state" needs different indexes than "Show history" or "Show as of date."

9. **Projections optimize reads** - Event sourcing is write-optimized. Create materialized views for read performance.

10. **Retention policies are part of the design** - Data grows forever. Plan archiving, partitioning, and cleanup from day one.

---

## Connection to Next Week

Week 26 brings everything together: **Domain-Specific Data Models + PROJECT 6**. You'll integrate all six courses:

- **Systems Thinking** (Courses 1): Understanding complex domain relationships in data models
- **Product Discovery** (Course 2): Identifying what data to model based on user needs
- **Requirements** (Course 3): Translating business rules into constraints and relationships
- **DDD** (Course 4): Aggregates become tables, events become event stores, ubiquitous language becomes column names
- **BPMN** (Course 5): Process state is persisted data that workflows read and update
- **Data Modeling** (Course 6): All the techniques you've learned come together

Your final project will be a **complete data architecture** for a real-world domain, demonstrating mastery of conceptual/logical/physical modeling, normalization, optimization, and temporal modeling.

---

## Additional Resources

### Books
- **"Temporal Data & the Relational Model"** by C.J. Date, Hugh Darwen, Nikos Lorentzos - Definitive guide to temporal databases
- **"The Data Warehouse Toolkit"** by Ralph Kimball - SCD types and dimensional modeling
- **"Implementing Domain-Driven Design"** by Vaughn Vernon - Chapter on event sourcing
- **"Event Sourcing in Practice"** by Michael Plöd - Practical guide with code examples

### Database Documentation
- **PostgreSQL Temporal Tables**: https://www.postgresql.org/docs/current/rangetypes.html
- **SQL Server Temporal Tables**: https://docs.microsoft.com/en-us/sql/relational-databases/tables/temporal-tables
- **Oracle Temporal Validity**: https://docs.oracle.com/en/database/oracle/oracle-database/

### Articles
- **Martin Fowler - "Temporal Patterns"**: https://martinfowler.com/eaaDev/timeNarrative.html
- **Slowly Changing Dimensions Explained**: https://en.wikipedia.org/wiki/Slowly_changing_dimension
- **Event Sourcing Pattern**: https://microservices.io/patterns/data/event-sourcing.html

### Tools
- **EventStoreDB**: Purpose-built database for event sourcing (https://www.eventstore.com/)
- **Axon Framework**: Java framework for event sourcing and CQRS (https://axoniq.io/)
- **Flyway**: Database migration tool for managing schema evolution (https://flywaydb.org/)

---

**Well done completing Week 25!** You now understand how to model data that changes over time, maintain complete audit trails, and answer historical queries. This is essential for enterprise and industrial systems where "what did we know when?" can have regulatory, legal, and business significance.

Next week, you'll apply everything you've learned across all 26 weeks to design a complete domain-specific data architecture.
