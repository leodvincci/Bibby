# Week 24: Logical and Physical Modeling

**Course 6: Data Modeling in Real Domains — Week 24 of 26**

---

## The Query That Took 45 Minutes

In 2017, a logistics company launched their new shipment tracking system. The database design looked good on paper—all the entities, relationships, and constraints from their conceptual model (Week 23).

**Week 1 of production:** The system was unusably slow.

A simple query—"Show me all shipments for customer XYZ from last month"—took **45 minutes**.

**The problem?** The database team had focused only on **logical correctness** (proper normalization, relationships, constraints) but ignored **physical optimization**:

- ❌ **No indexes** on frequently queried columns (customer_id, shipment_date)
- ❌ **Over-normalized** — Even simple queries required 8-table joins
- ❌ **Wrong data types** — VARCHAR(255) for everything, including numeric IDs
- ❌ **No partitioning** — 50M rows in one table
- ❌ **Missing denormalization** — Customer name queried on every shipment display, required join

**The fix cost $3M and took 4 months:**
- Add indexes on high-cardinality columns
- Denormalize frequently accessed data (customer name in shipments table)
- Partition large tables by date
- Optimize data types
- Add covering indexes for common query patterns

**Performance after fixes:** Same query ran in **200 milliseconds**.

**The lesson:** **Logical correctness** (normalization, relationships) is necessary but **not sufficient**. **Physical optimization** (indexes, denormalization, partitioning) makes databases actually usable.

Welcome to **Week 24: Logical and Physical Modeling**.

---

## Logical Modeling: Normalization

**Normalization** is organizing data to **minimize redundancy** and **prevent anomalies**.

### Why Normalize?

**Un-normalized data has problems:**

**Example: Customer Orders (Un-normalized)**

```
Orders Table:
┌────────┬──────────┬──────────────┬────────────────┬─────────┬────────────┬────────┐
│order_id│customer  │customer_email│customer_phone  │product  │product_cat │quantity│
├────────┼──────────┼──────────────┼────────────────┼─────────┼────────────┼────────┤
│ORD-001│Acme Corp │acme@ex.com   │555-1234        │Laptop   │Electronics │   2    │
│ORD-002│Acme Corp │acme@ex.com   │555-1234        │Mouse    │Electronics │   5    │
│ORD-003│Beta Inc  │beta@ex.com   │555-5678        │Laptop   │Electronics │   1    │
│ORD-004│Acme Corp │acme@MAIL.com │555-1234        │Keyboard │Electronics │   3    │
└────────┴──────────┴──────────────┴────────────────┴─────────┴────────────┴────────┘
```

**Problems:**

1. **Update Anomaly:** Acme's email changed (ORD-004). But ORD-001 and ORD-002 still have old email. Data is inconsistent.

2. **Insertion Anomaly:** Can't store a new customer without an order.

3. **Deletion Anomaly:** If we delete Beta's only order (ORD-003), we lose all Beta's data.

4. **Redundancy:** "Acme Corp" and "555-1234" repeated 3 times. Wastes space and creates inconsistency risk.

### Normal Forms

**Normal forms** are rules for organizing data to eliminate these problems.

#### First Normal Form (1NF)

**Rule:** **No repeating groups or arrays.** Each cell contains **atomic** (single) values.

**Violation:**

```
Orders Table:
┌────────┬──────────┬───────────────────────────┐
│order_id│customer  │products                   │
├────────┼──────────┼───────────────────────────┤
│ORD-001│Acme Corp │Laptop, Mouse, Keyboard    │  ← Multiple products in one cell
│ORD-002│Beta Inc  │Laptop                     │
└────────┴──────────┴───────────────────────────┘
```

**Fixed (1NF):**

```
Orders Table:
┌────────┬──────────┬────────┐
│order_id│customer  │product │
├────────┼──────────┼────────┤
│ORD-001│Acme Corp │Laptop  │
│ORD-001│Acme Corp │Mouse   │
│ORD-001│Acme Corp │Keyboard│
│ORD-002│Beta Inc  │Laptop  │
└────────┴──────────┴────────┘
```

**Better (1NF with proper structure):**

```
Orders Table:                    OrderItems Table:
┌────────┬──────────┐           ┌──────────┬────────┬────────┬────────┐
│order_id│customer  │           │item_id   │order_id│product │quantity│
├────────┼──────────┤           ├──────────┼────────┼────────┼────────┤
│ORD-001│Acme Corp │           │ITEM-001  │ORD-001 │Laptop  │   1    │
│ORD-002│Beta Inc  │           │ITEM-002  │ORD-001 │Mouse   │   1    │
└────────┴──────────┘           │ITEM-003  │ORD-001 │Keyboard│   1    │
                                │ITEM-004  │ORD-002 │Laptop  │   1    │
                                └──────────┴────────┴────────┴────────┘
```

#### Second Normal Form (2NF)

**Rule:** Must be in 1NF, **and** all non-key attributes must depend on the **entire** primary key (not part of it).

**Only applies to tables with composite keys.**

**Violation (composite key: order_id + product_id):**

```
OrderItems Table:
┌────────┬──────────┬────────────┬────────┐
│order_id│product_id│product_name│quantity│  ← product_name depends only on product_id,
├────────┼──────────┼────────────┼────────┤     not the full key (order_id + product_id)
│ORD-001│PROD-100  │Laptop      │   2    │
│ORD-001│PROD-200  │Mouse       │   5    │
│ORD-002│PROD-100  │Laptop      │   1    │
└────────┴──────────┴────────────┴────────┘
PK: (order_id, product_id)
```

**Problem:** `product_name` depends only on `product_id`, not on `order_id`. This creates redundancy (Laptop repeated).

**Fixed (2NF):**

```
OrderItems Table:                Products Table:
┌────────┬──────────┬────────┐   ┌──────────┬────────────┐
│order_id│product_id│quantity│   │product_id│product_name│
├────────┼──────────┼────────┤   ├──────────┼────────────┤
│ORD-001│PROD-100  │   2    │   │PROD-100  │Laptop      │
│ORD-001│PROD-200  │   5    │   │PROD-200  │Mouse       │
│ORD-002│PROD-100  │   1    │   └──────────┴────────────┘
└────────┴──────────┴────────┘
PK: (order_id, product_id)      PK: product_id
```

Now `product_name` is stored once per product, not once per order item.

#### Third Normal Form (3NF)

**Rule:** Must be in 2NF, **and** no **transitive dependencies** (non-key attribute depending on another non-key attribute).

**Violation:**

```
Employees Table:
┌───────────┬─────────────┬─────────────┬─────────────────┐
│employee_id│employee_name│department_id│department_name  │  ← department_name depends on department_id,
├───────────┼─────────────┼─────────────┼─────────────────┤     which depends on employee_id (transitive)
│EMP-001    │Alice        │DEPT-10      │Sales            │
│EMP-002    │Bob          │DEPT-10      │Sales            │
│EMP-003    │Carol        │DEPT-20      │Engineering      │
└───────────┴─────────────┴─────────────┴─────────────────┘
```

**Problem:** `department_name` depends on `department_id`, not directly on `employee_id`. If Sales changes name to "Revenue", must update multiple rows.

**Fixed (3NF):**

```
Employees Table:                Departments Table:
┌───────────┬─────────────┬─────────────┐   ┌─────────────┬─────────────────┐
│employee_id│employee_name│department_id│   │department_id│department_name  │
├───────────┼─────────────┼─────────────┤   ├─────────────┼─────────────────┤
│EMP-001    │Alice        │DEPT-10      │   │DEPT-10      │Sales            │
│EMP-002    │Bob          │DEPT-10      │   │DEPT-20      │Engineering      │
│EMP-003    │Carol        │DEPT-20      │   └─────────────┴─────────────────┘
└───────────┴─────────────┴─────────────┘
PK: employee_id                            PK: department_id
```

#### Boyce-Codd Normal Form (BCNF)

**Rule:** Must be in 3NF, **and** every determinant (attribute that determines another) must be a candidate key.

**Violation (rare, but possible):**

```
CourseInstructors Table:
┌──────────┬────────────┬──────────┐
│student_id│course      │instructor│
├──────────┼────────────┼──────────┤
│STU-001   │Math        │Prof. Lee │  ← instructor determines course (each instructor teaches only one course)
│STU-001   │Physics     │Prof. Kim │     but instructor is not a candidate key
│STU-002   │Math        │Prof. Lee │
└──────────┴────────────┴──────────┘
PK: (student_id, course)
```

**Problem:** `instructor` determines `course` (Prof. Lee teaches only Math), but `instructor` isn't a candidate key.

**Fixed (BCNF):**

```
StudentEnrollments:              InstructorCourses:
┌──────────┬──────────┐          ┌──────────┬────────┐
│student_id│instructor│          │instructor│course  │
├──────────┼──────────┤          ├──────────┼────────┤
│STU-001   │Prof. Lee │          │Prof. Lee │Math    │
│STU-001   │Prof. Kim │          │Prof. Kim │Physics │
│STU-002   │Prof. Lee │          └──────────┴────────┘
└──────────┴──────────┘          PK: instructor
PK: (student_id, instructor)
```

### Petroleum Terminal: Normalization Example

**Un-normalized:**

```
LoadOrders Table:
┌──────────┬──────────┬──────────────┬──────────────┬────────┬──────────┬──────────┬──────┐
│order_id  │customer  │customer_email│customer_phone│product │product_cat│quantity  │bay   │
├──────────┼──────────┼──────────────┼──────────────┼────────┼──────────┼──────────┼──────┤
│LO-001    │Acme Corp │acme@ex.com   │555-1234      │Diesel  │Fuel      │5000      │Bay 3 │
│LO-002    │Acme Corp │acme@ex.com   │555-1234      │Gasoline│Fuel      │8000      │Bay 1 │
└──────────┴──────────┴──────────────┴──────────────┴────────┴──────────┴──────────┴──────┘
```

**Fully normalized (3NF):**

```
Customers:                       Products:                      Bays:
┌───────────┬──────────┬────────┐ ┌────────┬──────────┬─────┐ ┌──────┬────────┐
│customer_id│name      │email   │ │prod_cod│prod_name │cat  │ │bay_id│bay_name│
├───────────┼──────────┼────────┤ ├────────┼──────────┼─────┤ ├──────┼────────┤
│CUST-001   │Acme Corp │acme@...│ │DIES    │Diesel    │Fuel │ │BAY-01│Bay 1   │
└───────────┴──────────┴────────┘ │GAS     │Gasoline  │Fuel │ │BAY-03│Bay 3   │
                                  └────────┴──────────┴─────┘ └──────┴────────┘

LoadOrders:
┌──────────┬───────────┬────────┬──────┬────────┐
│order_id  │customer_id│prod_cod│bay_id│quantity│
├──────────┼───────────┼────────┼──────┼────────┤
│LO-001    │CUST-001   │DIES    │BAY-03│5000    │
│LO-002    │CUST-001   │GAS     │BAY-01│8000    │
└──────────┴───────────┴────────┴──────┴────────┘
```

**Benefits:**
- Customer data stored once
- Product data stored once
- Update customer email in one place
- Can add products without orders
- Can add bays without orders

---

## When to Denormalize

**Denormalization** is intentionally introducing redundancy for **performance**.

### Trade-offs

| Normalization | Denormalization |
|---------------|-----------------|
| ✅ No redundancy | ❌ Redundant data |
| ✅ Easy updates (one place) | ❌ Complex updates (multiple places) |
| ✅ Data integrity | ❌ Risk of inconsistency |
| ❌ More joins (slower reads) | ✅ Fewer joins (faster reads) |
| ❌ Complex queries | ✅ Simple queries |

### When to Denormalize

**Denormalize when:**
1. **Read-heavy workload** (many more reads than writes)
2. **Query performance is critical** (must be fast)
3. **Redundant data rarely changes** (e.g., customer name)
4. **Join cost is high** (large tables, complex joins)

**Example: Shipment Tracking**

**Fully normalized:**

```sql
-- Query: Show shipments with customer name
SELECT s.shipment_id, c.customer_name, s.shipment_date
FROM shipments s
JOIN customers c ON s.customer_id = c.customer_id
WHERE s.shipment_date > '2024-01-01';
```

**With 10M shipments and frequent queries, this join is slow.**

**Denormalized:**

```sql
CREATE TABLE shipments (
    shipment_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    customer_name VARCHAR(255),  -- DENORMALIZED!
    shipment_date DATE
);

-- Query: No join needed
SELECT shipment_id, customer_name, shipment_date
FROM shipments
WHERE shipment_date > '2024-01-01';
```

**Trade-off:** When customer changes name, must update `customers` table **and** all their shipments. But if customer name rarely changes and queries are frequent, worth it.

**Best practice:** Denormalize with **triggers** or **application logic** to maintain consistency:

```sql
-- Trigger to keep denormalized data in sync
CREATE OR REPLACE FUNCTION sync_customer_name()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE shipments
    SET customer_name = NEW.customer_name
    WHERE customer_id = NEW.customer_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER customer_name_update
AFTER UPDATE OF customer_name ON customers
FOR EACH ROW
EXECUTE FUNCTION sync_customer_name();
```

---

## Physical Modeling: Performance Optimization

**Physical modeling** is optimizing for specific database technology and workload.

### Indexes

**Indexes** are data structures that speed up queries.

**How they work:** Like a book index—instead of scanning every page (full table scan), jump directly to the right page (index lookup).

#### When to Index

**Index columns that are:**
- ✅ Frequently in `WHERE` clauses
- ✅ Frequently in `JOIN` conditions
- ✅ Frequently in `ORDER BY` clauses
- ✅ High cardinality (many distinct values)

**Don't index columns that are:**
- ❌ Rarely queried
- ❌ Low cardinality (few distinct values, like boolean)
- ❌ Frequently updated (indexes slow down writes)
- ❌ Small tables (full scan is fast enough)

#### Types of Indexes

**1. B-Tree Index (Default)**

**Good for:**
- Equality (`WHERE customer_id = 'CUST-001'`)
- Range queries (`WHERE shipment_date > '2024-01-01'`)
- Sorting (`ORDER BY shipment_date`)

```sql
CREATE INDEX idx_shipments_customer ON shipments(customer_id);
CREATE INDEX idx_shipments_date ON shipments(shipment_date);
```

**2. Hash Index**

**Good for:**
- Equality only (`WHERE customer_id = 'CUST-001'`)
- Faster than B-Tree for equality
- **Not good for range queries**

```sql
CREATE INDEX idx_customers_email_hash ON customers USING HASH (email);
```

**3. Partial Index**

**Index only rows matching a condition:**

```sql
-- Index only active load orders (not completed/cancelled)
CREATE INDEX idx_load_orders_active
ON load_orders(scheduled_date)
WHERE status IN ('DRAFT', 'SCHEDULED');
```

**Benefit:** Smaller index, faster queries for common filter.

**4. Composite Index (Multi-column)**

**Index multiple columns together:**

```sql
CREATE INDEX idx_shipments_customer_date
ON shipments(customer_id, shipment_date);
```

**Query that uses it:**
```sql
SELECT * FROM shipments
WHERE customer_id = 'CUST-001'
AND shipment_date > '2024-01-01';
```

**Column order matters!** Index `(customer_id, shipment_date)` can be used for:
- ✅ `WHERE customer_id = ...`
- ✅ `WHERE customer_id = ... AND shipment_date = ...`
- ❌ `WHERE shipment_date = ...` (customer_id not in WHERE)

**5. Covering Index (Include Columns)**

**Index includes extra columns** so query doesn't need to access table:

```sql
CREATE INDEX idx_shipments_customer_covering
ON shipments(customer_id)
INCLUDE (shipment_date, destination);
```

**Query:**
```sql
-- Can answer entirely from index (no table access)
SELECT shipment_date, destination
FROM shipments
WHERE customer_id = 'CUST-001';
```

#### Petroleum Terminal Index Strategy

```sql
-- Indexes for load_orders table
CREATE INDEX idx_load_orders_customer ON load_orders(customer_id);
CREATE INDEX idx_load_orders_date ON load_orders(scheduled_date);
CREATE INDEX idx_load_orders_status ON load_orders(status)
    WHERE status != 'COMPLETED';  -- Partial index for active orders
CREATE INDEX idx_load_orders_product ON load_orders(product_code);

-- Composite index for common query pattern
CREATE INDEX idx_load_orders_customer_date
ON load_orders(customer_id, scheduled_date DESC);

-- Indexes for loading_operations table
CREATE INDEX idx_loading_operations_date ON loading_operations(start_time);
CREATE INDEX idx_loading_operations_bay ON loading_operations(bay_id);
CREATE INDEX idx_loading_operations_order ON loading_operations(load_order_id);

-- Covering index for dashboard query
CREATE INDEX idx_loading_operations_dashboard
ON loading_operations(start_time DESC)
INCLUDE (load_order_id, bay_id, status, quantity_loaded)
WHERE status = 'IN_PROGRESS';
```

### Partitioning

**Partitioning** splits large tables into smaller pieces.

**Benefits:**
- ✅ Query only relevant partition (faster)
- ✅ Archive old partitions easily
- ✅ Parallel query execution

**Types:**

**1. Range Partitioning (Most Common)**

**Partition by date range:**

```sql
-- Parent table
CREATE TABLE load_orders (
    load_order_id UUID,
    customer_id UUID,
    scheduled_date DATE,
    ...
) PARTITION BY RANGE (scheduled_date);

-- Partitions
CREATE TABLE load_orders_2024_01 PARTITION OF load_orders
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE load_orders_2024_02 PARTITION OF load_orders
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- Continue for each month...
```

**Query benefits:**
```sql
-- Only scans load_orders_2024_01 partition
SELECT * FROM load_orders
WHERE scheduled_date BETWEEN '2024-01-01' AND '2024-01-31';
```

**2. List Partitioning**

**Partition by discrete values:**

```sql
CREATE TABLE load_orders (
    ...
    status VARCHAR(50),
    ...
) PARTITION BY LIST (status);

CREATE TABLE load_orders_active PARTITION OF load_orders
    FOR VALUES IN ('DRAFT', 'SCHEDULED', 'IN_PROGRESS');

CREATE TABLE load_orders_completed PARTITION OF load_orders
    FOR VALUES IN ('COMPLETED', 'CANCELLED');
```

**3. Hash Partitioning**

**Distribute evenly across partitions:**

```sql
CREATE TABLE load_orders (
    ...
) PARTITION BY HASH (customer_id);

CREATE TABLE load_orders_p0 PARTITION OF load_orders
    FOR VALUES WITH (MODULUS 4, REMAINDER 0);

CREATE TABLE load_orders_p1 PARTITION OF load_orders
    FOR VALUES WITH (MODULUS 4, REMAINDER 1);

-- Continues for p2, p3...
```

---

## Complete DDL: Petroleum Terminal Database

**Full database schema with constraints, indexes, and partitioning:**

```sql
-- Customers
CREATE TABLE customers (
    customer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255),
    contact_email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    billing_address TEXT,
    payment_terms_days INTEGER DEFAULT 30,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_active ON customers(is_active) WHERE is_active = true;
CREATE INDEX idx_customers_email ON customers(contact_email);

-- Products
CREATE TABLE products (
    product_code VARCHAR(50) PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    density_kg_per_liter NUMERIC(6,4),
    hazmat_class VARCHAR(50),
    is_active BOOLEAN DEFAULT true
);

-- Bays
CREATE TABLE bays (
    bay_id VARCHAR(50) PRIMARY KEY,
    bay_number INTEGER NOT NULL UNIQUE,
    bay_type VARCHAR(50), -- 'TRUCK', 'RAIL', 'MARINE'
    capacity_barrels_per_hour NUMERIC(10,2),
    compatible_products VARCHAR(255)[], -- Array of product codes
    is_operational BOOLEAN DEFAULT true
);

-- Tanks
CREATE TABLE tanks (
    tank_id VARCHAR(50) PRIMARY KEY,
    tank_number INTEGER NOT NULL UNIQUE,
    capacity_barrels NUMERIC(10,2) NOT NULL CHECK (capacity_barrels > 0),
    current_level_barrels NUMERIC(10,2) DEFAULT 0
        CHECK (current_level_barrels >= 0 AND current_level_barrels <= capacity_barrels),
    current_product_code VARCHAR(50),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (current_product_code) REFERENCES products(product_code)
);

CREATE INDEX idx_tanks_product ON tanks(current_product_code);

-- Load Orders (Partitioned by scheduled_date)
CREATE TABLE load_orders (
    load_order_id UUID DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    requested_quantity_barrels NUMERIC(10,2) NOT NULL
        CHECK (requested_quantity_barrels > 0 AND requested_quantity_barrels <= 10000),
    status VARCHAR(50) DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    scheduled_date DATE NOT NULL CHECK (scheduled_date >= CURRENT_DATE),
    scheduled_time_start TIME,
    scheduled_time_end TIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (load_order_id, scheduled_date),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    FOREIGN KEY (product_code) REFERENCES products(product_code)
) PARTITION BY RANGE (scheduled_date);

-- Create partitions (example for 2024)
CREATE TABLE load_orders_2024_q1 PARTITION OF load_orders
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');
CREATE TABLE load_orders_2024_q2 PARTITION OF load_orders
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');
CREATE TABLE load_orders_2024_q3 PARTITION OF load_orders
    FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');
CREATE TABLE load_orders_2024_q4 PARTITION OF load_orders
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');

-- Indexes on load_orders (applied to all partitions)
CREATE INDEX idx_load_orders_customer ON load_orders(customer_id);
CREATE INDEX idx_load_orders_status ON load_orders(status)
    WHERE status IN ('DRAFT', 'SCHEDULED', 'IN_PROGRESS');
CREATE INDEX idx_load_orders_product ON load_orders(product_code);
CREATE INDEX idx_load_orders_customer_date
    ON load_orders(customer_id, scheduled_date DESC);

-- Loading Operations
CREATE TABLE loading_operations (
    loading_operation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    load_order_id UUID NOT NULL,
    bay_id VARCHAR(50) NOT NULL,
    driver_id UUID,
    operator_id UUID,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    quantity_loaded_barrels NUMERIC(10,2) CHECK (quantity_loaded_barrels >= 0),
    status VARCHAR(50) DEFAULT 'ASSIGNED'
        CHECK (status IN ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'ABORTED')),

    FOREIGN KEY (bay_id) REFERENCES bays(bay_id),
    CONSTRAINT end_after_start CHECK (end_time IS NULL OR end_time >= start_time)
);

CREATE INDEX idx_loading_operations_order ON loading_operations(load_order_id);
CREATE INDEX idx_loading_operations_date ON loading_operations(start_time);
CREATE INDEX idx_loading_operations_bay ON loading_operations(bay_id);
CREATE INDEX idx_loading_operations_status ON loading_operations(status)
    WHERE status IN ('ASSIGNED', 'IN_PROGRESS');

-- Carrier Tickets (1:1 with loading_operations)
CREATE TABLE carrier_tickets (
    ticket_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loading_operation_id UUID UNIQUE NOT NULL,  -- UNIQUE enforces 1:1
    bill_of_lading_number VARCHAR(50) UNIQUE NOT NULL,
    net_quantity_barrels NUMERIC(10,2) NOT NULL,
    gross_quantity_barrels NUMERIC(10,2),
    seal_number VARCHAR(50),
    issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ticket_pdf_path TEXT,

    FOREIGN KEY (loading_operation_id)
        REFERENCES loading_operations(loading_operation_id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_carrier_tickets_bol ON carrier_tickets(bill_of_lading_number);
CREATE INDEX idx_carrier_tickets_date ON carrier_tickets(issue_date);

-- Denormalized reporting table (for fast dashboard queries)
CREATE MATERIALIZED VIEW daily_loading_summary AS
SELECT
    DATE(lo.start_time) AS loading_date,
    c.customer_id,
    c.company_name,  -- Denormalized customer name
    p.product_code,
    p.product_name,  -- Denormalized product name
    COUNT(*) AS total_loads,
    SUM(lo.quantity_loaded_barrels) AS total_quantity_barrels,
    AVG(EXTRACT(EPOCH FROM (lo.end_time - lo.start_time))/3600) AS avg_duration_hours
FROM loading_operations lo
JOIN load_orders lor ON lo.load_order_id = lor.load_order_id
JOIN customers c ON lor.customer_id = c.customer_id
JOIN products p ON lor.product_code = p.product_code
WHERE lo.status = 'COMPLETED'
GROUP BY DATE(lo.start_time), c.customer_id, c.company_name,
         p.product_code, p.product_name;

-- Refresh materialized view daily
CREATE INDEX idx_daily_summary_date ON daily_loading_summary(loading_date DESC);
```

---

## Hands-On Exercise: Create Logical and Physical Models

**Use the same domain from Week 23.**

### Part 1: Normalize Your Schema (60 minutes)

Take your conceptual ERD from Week 23 and:

1. **Identify violations** of 1NF, 2NF, 3NF
2. **Normalize to 3NF**
3. **Document trade-offs** (if you choose to keep any denormalization)

**Deliverable:** Normalized table definitions with explanations

### Part 2: Write Complete DDL (60 minutes)

For your domain, write SQL DDL including:

1. **All tables** with appropriate data types
2. **All constraints:**
   - Primary keys
   - Foreign keys (with ON DELETE behavior)
   - NOT NULL
   - UNIQUE
   - CHECK
   - DEFAULT
3. **Comments** explaining business rules

**Deliverable:** Complete `schema.sql` file

### Part 3: Design Index Strategy (45 minutes)

Based on expected queries:

1. **List 5-10 common queries** in your domain
2. **Design indexes** to optimize those queries
3. **Write CREATE INDEX statements**
4. **Explain** why each index is needed

**Deliverable:** Index strategy document + SQL

### Part 4: Consider Partitioning (30 minutes)

Identify:
1. **Tables that could benefit from partitioning** (large, time-series data)
2. **Partitioning strategy** (range, list, hash)
3. **Partition definitions**

**Deliverable:** Partitioning plan + SQL

---

## Key Takeaways

✅ **Normalization eliminates redundancy** — 1NF (atomic values), 2NF (no partial dependencies), 3NF (no transitive dependencies), BCNF (every determinant is a key).

✅ **Denormalize strategically for performance** — When reads greatly outnumber writes and redundant data rarely changes.

✅ **Indexes speed up queries** — But slow down writes. Index high-cardinality columns in WHERE/JOIN/ORDER BY.

✅ **Index types matter** — B-Tree (default, range queries), Hash (equality only), Partial (filtered), Composite (multiple columns), Covering (includes extra columns).

✅ **Partitioning splits large tables** — Range (dates), List (status values), Hash (distribute evenly).

✅ **Physical modeling optimizes for workload** — Know your queries, know your database, measure performance.

✅ **Constraints enforce business rules** — NOT NULL, UNIQUE, CHECK, FOREIGN KEY prevent invalid data at database level.

✅ **Logical correctness + Physical optimization = Usable database** — Need both.

---

## Connection to Week 25

This week you learned **logical modeling** (normalization) and **physical modeling** (indexes, partitioning, optimization).

Next week you'll learn **Data Lifecycle & Temporal Modeling**—how to handle data that changes over time, including:
- Versioning and history tracking
- Slowly Changing Dimensions (SCD)
- Temporal tables
- Audit trails
- Soft deletes vs hard deletes
- Event sourcing for data

**Get ready to model the TIME dimension of your data.**

---

## Additional Resources

**Books:**
- *Database Design for Mere Mortals* by Michael Hernandez — Normalization explained clearly
- *High Performance MySQL* / *PostgreSQL Up and Running* — Database-specific optimization

**Tools:**
- **EXPLAIN ANALYZE** — PostgreSQL query planner (shows index usage)
- **pg_stat_statements** — Track query performance
- **pgAdmin, DBeaver** — Database management with ERD visualization

**For Your Context:**
- Analyze petroleum terminal query patterns (daily dashboard, monthly reports, real-time monitoring)
- Design indexes for operational queries (current bay status, scheduled loads, tank levels)
- Partition historical data (completed loads by month)

---

**End of Week 24 — Logical and Physical Modeling**

**Next:** Week 25 — Data Lifecycle & Temporal Modeling
