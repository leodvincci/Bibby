# Week 23: Conceptual Data Modeling

**Course 6: Data Modeling in Real Domains — Week 23 of 26**

---

## The $15M Data Mistake

In 2016, a major retailer launched a new inventory management system. The development team had done everything right—or so they thought:

- ✅ Domain models designed (Course 4: DDD)
- ✅ Workflows mapped (Course 5: BPMN)
- ✅ Requirements documented (Course 3)

Then they handed it to the database team: "Build us a database."

The database team, working in isolation, created a schema based on... **their own interpretation** of what they thought the business needed.

**Three months later, the system launched.**

**Week 1:** Catastrophic data quality issues.

**The problems:**

1. **Products could exist without categories** — Business rule: "Every product must have a category." Database: Category was optional (nullable).

2. **Orders could reference deleted customers** — Business rule: "Can't delete a customer with active orders." Database: No foreign key constraint, orphaned records everywhere.

3. **Inventory quantities became negative** — Business rule: "Quantity can't go below zero." Database: No check constraint, inventory showed -500 units.

4. **Duplicate products** — Business rule: "Product SKU must be unique." Database: No unique constraint, same SKU entered multiple times with different spellings.

5. **Lost relationships** — Business rule: "An order contains multiple line items." Database: Modeled as denormalized JSON blob, couldn't query by product.

**The cost:**
- $15M in data cleanup and system fixes
- 6 months of operational chaos
- Damaged customer trust
- Loss of inventory accuracy

**The root cause:** **The database schema didn't reflect the domain model's business rules and relationships.**

**The lesson:** Data modeling isn't a separate activity done by a different team. It's a **translation** of your domain model (entities, aggregates, value objects, business rules) into a persistent structure. Get the translation wrong, and your data will violate business rules from day one.

Welcome to **Course 6: Data Modeling in Real Domains**.

---

## What Is Conceptual Data Modeling?

**Conceptual data modeling** is defining **what data** your system needs and **how it relates**, independent of any specific database technology.

### Three Levels of Data Modeling

**1. Conceptual Model (Week 23)**
- **Focus:** What entities exist and how they relate
- **Audience:** Business stakeholders, domain experts, developers
- **Questions:** "What are the things we care about?" "How do they relate?"
- **Notation:** Entity-Relationship Diagrams (ERD)
- **Technology-independent:** No tables, no columns, just concepts

**2. Logical Model (Week 24)**
- **Focus:** How to structure data for querying and integrity
- **Audience:** Developers, data architects
- **Questions:** "What are the tables?" "What are the keys?" "How do we enforce relationships?"
- **Notation:** Table definitions with keys and relationships
- **Technology-aware but not specific:** Works for any relational database

**3. Physical Model (Week 24)**
- **Focus:** How to optimize data for specific database technology
- **Audience:** Database administrators, performance engineers
- **Questions:** "What indexes?" "What partitioning?" "What storage?"
- **Notation:** DDL (Data Definition Language) for specific database
- **Technology-specific:** PostgreSQL, MySQL, Oracle, SQL Server, etc.

**This week (23): Conceptual modeling** — Understanding the business data at a high level.

---

## From Domain Models to Data Models

You've spent Course 4 (DDD) modeling the domain. Now you'll translate that to a data model.

### Domain Model Concepts → Data Model Concepts

| Domain Model (DDD) | Data Model (ERD) |
|-------------------|------------------|
| **Entity** (with identity) | **Entity** (table) |
| **Value Object** (no identity) | **Attributes** or **Embedded** |
| **Aggregate** (consistency boundary) | **Related tables** (within transaction) |
| **Association** (references between entities) | **Relationship** (1:1, 1:N, N:M) |
| **Business Rule** (invariant) | **Constraint** (PK, FK, CHECK, UNIQUE) |

**Example: Petroleum Terminal Domain**

**Domain Model (from Week 15):**

```java
public class LoadOrder {  // Entity (Aggregate Root)
    private final LoadOrderId id;  // Identity
    private CustomerId customerId;  // Reference to Customer
    private Product product;  // Value Object
    private Volume requestedQuantity;  // Value Object
    private LoadOrderStatus status;  // Enum
    private List<LoadOrderLineItem> lineItems;  // Child entities
}

public class Product {  // Value Object
    private final ProductCode code;
    private final String name;
}

public class Volume {  // Value Object
    private final BigDecimal barrels;
}
```

**Data Model (ERD):**

```
┌─────────────────────────┐
│ LoadOrder (Entity)      │
├─────────────────────────┤
│ PK: load_order_id       │
│ FK: customer_id         │──┐
│     product_code        │  │
│     product_name        │  │
│     requested_quantity  │  │
│     status              │  │
│     created_at          │  │
└─────────────────────────┘  │
         │ 1                 │
         │ has               │
         │ N                 │
         ↓                   │
┌─────────────────────────┐  │
│ LoadOrderLineItem       │  │
├─────────────────────────┤  │
│ PK: line_item_id        │  │
│ FK: load_order_id       │  │
│     tank_id             │  │
│     quantity            │  │
└─────────────────────────┘  │
                             │
                             │ references
                             │
┌─────────────────────────┐  │
│ Customer (Entity)       │←─┘
├─────────────────────────┤
│ PK: customer_id         │
│     company_name        │
│     contact_email       │
└─────────────────────────┘
```

**Key translations:**
- `LoadOrder` entity → `LoadOrder` table
- `LoadOrderId` (identity) → `load_order_id` (primary key)
- `CustomerId` (reference) → `customer_id` (foreign key)
- `Product` (value object) → Embedded attributes (`product_code`, `product_name`)
- `Volume` (value object) → `requested_quantity` (single column)
- `List<LoadOrderLineItem>` (child entities) → Separate `LoadOrderLineItem` table with foreign key
- `LoadOrderStatus` (enum) → `status` (VARCHAR or ENUM column)

---

## Entity-Relationship Diagrams (ERD)

**ERD** is the standard notation for conceptual data models.

### ERD Components

**1. Entity (Rectangle)**
- Represents a **thing** we need to store data about
- Corresponds to domain entities (from Course 4)

```
┌─────────────┐
│  Customer   │
└─────────────┘
```

**2. Relationship (Diamond or Line)**
- Represents **association** between entities
- Named with a verb

```
┌─────────┐         ┌──────────┐
│Customer │─────────│LoadOrder │
└─────────┘ places  └──────────┘
```

**3. Attributes (Ovals or Listed)**
- Properties of an entity
- Correspond to domain entity fields and value objects

```
┌─────────────────────────┐
│  Customer               │
├─────────────────────────┤
│ customer_id (PK)        │
│ company_name            │
│ contact_email           │
│ phone_number            │
│ address                 │
└─────────────────────────┘
```

**4. Keys (Underlined)**
- **Primary Key (PK):** Uniquely identifies an entity instance
- **Foreign Key (FK):** References another entity's primary key

### Cardinality (How Many?)

**Cardinality** describes how many instances of one entity relate to instances of another.

**Notation:**

```
1:1  (One-to-One)
1:N  (One-to-Many)
N:M  (Many-to-Many)
```

#### One-to-One (1:1)

**One instance of A relates to exactly one instance of B.**

**Example:** One LoadingOperation has one CarrierTicket.

```
┌──────────────────┐ 1     1 ┌───────────────┐
│LoadingOperation  │─────────│CarrierTicket  │
└──────────────────┘ has     └───────────────┘
```

**In database:**
```sql
CREATE TABLE loading_operations (
    loading_operation_id UUID PRIMARY KEY,
    ...
);

CREATE TABLE carrier_tickets (
    ticket_id UUID PRIMARY KEY,
    loading_operation_id UUID UNIQUE NOT NULL,  -- UNIQUE enforces 1:1
    FOREIGN KEY (loading_operation_id) REFERENCES loading_operations(loading_operation_id)
);
```

#### One-to-Many (1:N)

**One instance of A relates to many instances of B.**

**Example:** One LoadOrder has many LoadOrderLineItems.

```
┌──────────┐ 1            N ┌─────────────────────┐
│LoadOrder │────────────────│LoadOrderLineItem    │
└──────────┘ contains       └─────────────────────┘
```

**In database:**
```sql
CREATE TABLE load_orders (
    load_order_id UUID PRIMARY KEY,
    ...
);

CREATE TABLE load_order_line_items (
    line_item_id UUID PRIMARY KEY,
    load_order_id UUID NOT NULL,  -- Many line items per order
    quantity NUMERIC,
    FOREIGN KEY (load_order_id) REFERENCES load_orders(load_order_id)
);
```

#### Many-to-Many (N:M)

**Many instances of A relate to many instances of B.**

**Example:** Many Products can be stored in many Tanks (over time). Many Tanks can store many Products (over time, not simultaneously).

```
┌─────────┐ N            M ┌─────────┐
│Product  │────────────────│Tank     │
└─────────┘ stored in      └─────────┘
```

**In database (requires junction table):**
```sql
CREATE TABLE products (
    product_code VARCHAR(50) PRIMARY KEY,
    product_name VARCHAR(255)
);

CREATE TABLE tanks (
    tank_id VARCHAR(50) PRIMARY KEY,
    capacity_barrels NUMERIC
);

-- Junction table
CREATE TABLE tank_product_history (
    history_id UUID PRIMARY KEY,
    tank_id VARCHAR(50) NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    stored_from TIMESTAMP,
    stored_to TIMESTAMP,
    FOREIGN KEY (tank_id) REFERENCES tanks(tank_id),
    FOREIGN KEY (product_code) REFERENCES products(product_code)
);
```

---

## Identifying Entities

**How do you know what should be an entity?**

### Entity Identification Checklist

An entity should:
- ✅ Represent a **distinct concept** in the domain
- ✅ Have **multiple instances** (if only one, might be a singleton config object)
- ✅ Have **identity** (you can distinguish one from another)
- ✅ Have a **lifecycle** (created, modified, maybe deleted)
- ✅ Have **attributes** (data to store about it)

**From your domain model (Course 4), entities are typically:**
1. **Aggregate Roots** → Always entities
2. **Entities within aggregates** → Usually entities (or embedded)
3. **Value Objects** → Usually **not** entities (embedded attributes instead)

### Petroleum Terminal Example

**Entities (from domain model):**

1. **Customer** ✅
   - Distinct concept: Yes (different customers)
   - Multiple instances: Yes (many customers)
   - Identity: Yes (customer ID)
   - Lifecycle: Yes (onboarded, active, churned)
   - Attributes: Yes (name, contact info, billing terms)

2. **LoadOrder** ✅
   - Distinct concept: Yes (scheduled load)
   - Multiple instances: Yes (many orders)
   - Identity: Yes (load order number)
   - Lifecycle: Yes (draft → scheduled → in-progress → completed)
   - Attributes: Yes (customer, product, quantity, date)

3. **Tank** ✅
   - Distinct concept: Yes (physical tank)
   - Multiple instances: Yes (multiple tanks at terminal)
   - Identity: Yes (tank number)
   - Lifecycle: Yes (installed, operational, decommissioned)
   - Attributes: Yes (capacity, current level, current product)

4. **LoadingOperation** ✅
   - Distinct concept: Yes (actual loading event)
   - Multiple instances: Yes (many loads per day)
   - Identity: Yes (operation ID)
   - Lifecycle: Yes (assigned → in-progress → completed)
   - Attributes: Yes (start time, end time, quantity loaded, bay)

**Not Entities (Value Objects → Embedded Attributes):**

1. **Volume** ❌
   - No identity (5,000 barrels is just a number)
   - Embedded as: `quantity_barrels NUMERIC`

2. **Product** ❌ (in this context)
   - Might be value object (just code + name)
   - Embedded as: `product_code VARCHAR, product_name VARCHAR`
   - **Alternative:** Could be entity if products have complex lifecycle, pricing, specifications

3. **Address** ❌
   - No identity (just structured data)
   - Embedded as: `street VARCHAR, city VARCHAR, state VARCHAR, zip VARCHAR`

---

## Attributes and Data Types

**Attributes** are the properties/fields of entities.

### Choosing Attributes

**For each entity, ask:**
1. What do we need to **know** about it?
2. What do we need to **display** to users?
3. What do we need to **search/filter** by?
4. What do we need to **calculate** with?
5. What **audit information** do we need? (created_at, updated_at, created_by)

**Example: Customer Entity**

```
┌─────────────────────────────────┐
│ Customer                        │
├─────────────────────────────────┤
│ PK: customer_id (UUID)          │
│     company_name (VARCHAR 255)  │
│     contact_name (VARCHAR 255)  │
│     contact_email (VARCHAR 255) │
│     phone_number (VARCHAR 20)   │
│     billing_address (TEXT)      │
│     payment_terms_days (INT)    │
│     is_active (BOOLEAN)         │
│     created_at (TIMESTAMP)      │
│     updated_at (TIMESTAMP)      │
└─────────────────────────────────┘
```

### Data Types

| Concept | Common Data Type | Notes |
|---------|------------------|-------|
| Identity (ID) | UUID, BIGINT, VARCHAR | UUID preferred for distributed systems |
| Name, Text | VARCHAR(n) | Specify max length |
| Long text | TEXT | Unlimited length |
| Whole number | INTEGER, BIGINT | BIGINT for large numbers |
| Decimal | NUMERIC(p,s), DECIMAL | Exact precision (e.g., NUMERIC(10,2) = $12345678.90) |
| Date | DATE | Just date, no time |
| Time | TIME | Just time, no date |
| Date + Time | TIMESTAMP | Full date and time |
| True/False | BOOLEAN | Yes/no flags |
| Enum/Status | VARCHAR, ENUM | "DRAFT", "SCHEDULED", "COMPLETED" |
| Binary data | BYTEA, BLOB | Files, images |
| JSON | JSON, JSONB | Semi-structured data |

**Petroleum Terminal Data Types:**

```sql
CREATE TABLE load_orders (
    load_order_id UUID PRIMARY KEY,                    -- Identity
    customer_id UUID NOT NULL,                         -- Foreign key
    product_code VARCHAR(50),                          -- Product code
    product_name VARCHAR(255),                         -- Product name
    requested_quantity_barrels NUMERIC(10,2),          -- Volume (exact decimal)
    actual_quantity_barrels NUMERIC(10,2),
    status VARCHAR(50),                                -- DRAFT, SCHEDULED, COMPLETED
    scheduled_date DATE,                               -- Just the date
    scheduled_time TIME,                               -- Time window start
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,    -- Audit
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Keys: Primary, Foreign, Natural, Surrogate

### Primary Key (PK)

**Uniquely identifies each row** in a table.

**Properties:**
- ✅ Must be **unique** (no duplicates)
- ✅ Must be **not null** (every row must have a value)
- ✅ Should be **immutable** (doesn't change)
- ✅ Should be **simple** (one column preferred)

**Types:**

**1. Surrogate Key** (System-generated)
- UUID, auto-increment integer
- **Pros:** Guaranteed unique, never changes, no business meaning
- **Cons:** Meaningless to humans

```sql
customer_id UUID PRIMARY KEY DEFAULT gen_random_uuid()
```

**2. Natural Key** (Business-meaningful)
- Social security number, email, product SKU
- **Pros:** Meaningful, no extra column needed
- **Cons:** Might change (email), might not be truly unique, privacy concerns (SSN)

```sql
product_code VARCHAR(50) PRIMARY KEY  -- SKU is natural key
```

**Recommendation:** **Use surrogate keys** (UUID or BIGINT) for most entities. Natural keys can be **unique constraints** but not primary keys.

```sql
CREATE TABLE customers (
    customer_id UUID PRIMARY KEY,           -- Surrogate key
    email VARCHAR(255) UNIQUE NOT NULL,     -- Natural key as unique constraint
    company_name VARCHAR(255)
);
```

### Foreign Key (FK)

**References another table's primary key**, establishing a relationship.

**Properties:**
- ✅ Enforces **referential integrity** (can't reference non-existent row)
- ✅ Prevents **orphaned records** (can't delete parent if child exists, unless CASCADE)
- ✅ Enables **joins** (query related data)

```sql
CREATE TABLE load_orders (
    load_order_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,                    -- Foreign key
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);
```

**Cascading:**

```sql
-- If customer is deleted, delete all their orders too
FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE

-- If customer is deleted, prevent if they have orders
FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT

-- If customer is deleted, set orders' customer_id to NULL
FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL
```

---

## Business Rules as Constraints

**Constraints** enforce business rules at the database level.

### Types of Constraints

**1. NOT NULL** — Required field

```sql
company_name VARCHAR(255) NOT NULL  -- Every customer must have a name
```

**2. UNIQUE** — No duplicates

```sql
email VARCHAR(255) UNIQUE  -- No two customers can have the same email
```

**3. CHECK** — Custom validation

```sql
requested_quantity_barrels NUMERIC(10,2) CHECK (requested_quantity_barrels > 0)
-- Quantity must be positive
```

```sql
scheduled_date DATE CHECK (scheduled_date >= CURRENT_DATE)
-- Can't schedule loads in the past
```

**4. PRIMARY KEY** — Unique identifier

```sql
customer_id UUID PRIMARY KEY
```

**5. FOREIGN KEY** — Relationship integrity

```sql
FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
```

**6. DEFAULT** — Auto-populate value

```sql
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
status VARCHAR(50) DEFAULT 'DRAFT'
```

### Petroleum Terminal Constraints Example

```sql
CREATE TABLE load_orders (
    load_order_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    requested_quantity_barrels NUMERIC(10,2) NOT NULL
        CHECK (requested_quantity_barrels > 0 AND requested_quantity_barrels <= 10000),
    status VARCHAR(50) DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    scheduled_date DATE CHECK (scheduled_date >= CURRENT_DATE),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT
);
```

**Business rules enforced:**
- Load order must have a customer (NOT NULL)
- Quantity must be positive and ≤ 10,000 barrels (CHECK)
- Status must be one of the valid values (CHECK)
- Can't schedule in the past (CHECK)
- Can't delete a customer who has load orders (ON DELETE RESTRICT)
- Every order has creation timestamp (DEFAULT, NOT NULL)

---

## Normalization (Introduction)

**Normalization** is organizing data to **reduce redundancy** and **improve integrity**.

### The Problem: Redundant Data

**Un-normalized:**

```
LoadOrders Table:
┌──────────┬──────────────┬─────────────┬───────────────┬────────────┐
│order_id  │customer_name │customer_email│customer_phone│product_name│
├──────────┼──────────────┼─────────────┼───────────────┼────────────┤
│LO-001    │Acme Corp     │acme@ex.com  │555-1234      │Diesel      │
│LO-002    │Acme Corp     │acme@ex.com  │555-1234      │Gasoline    │
│LO-003    │Beta Inc      │beta@ex.com  │555-5678      │Diesel      │
└──────────┴──────────────┴─────────────┴───────────────┴────────────┘
```

**Problems:**
- ❌ **Redundancy:** "Acme Corp" data repeated for every order
- ❌ **Update anomaly:** If Acme changes email, must update multiple rows
- ❌ **Insertion anomaly:** Can't store customer without an order
- ❌ **Deletion anomaly:** Deleting last order deletes customer data

### The Solution: Separate Tables

**Normalized:**

```
Customers Table:
┌─────────────┬──────────────┬─────────────┬───────────────┐
│customer_id  │customer_name │customer_email│customer_phone│
├─────────────┼──────────────┼─────────────┼───────────────┤
│CUST-001     │Acme Corp     │acme@ex.com  │555-1234      │
│CUST-002     │Beta Inc      │beta@ex.com  │555-5678      │
└─────────────┴──────────────┴─────────────┴───────────────┘

LoadOrders Table:
┌──────────┬─────────────┬────────────┐
│order_id  │customer_id  │product_name│
├──────────┼─────────────┼────────────┤
│LO-001    │CUST-001     │Diesel      │
│LO-002    │CUST-001     │Gasoline    │
│LO-003    │CUST-002     │Diesel      │
└──────────┴─────────────┴────────────┘
```

**Benefits:**
- ✅ Customer data stored once
- ✅ Update customer email in one place
- ✅ Can store customers without orders
- ✅ Deleting order doesn't delete customer

**Normal forms** (1NF, 2NF, 3NF, BCNF) provide formal rules for normalization. We'll cover them in Week 24.

---

## Complete Example: Petroleum Terminal ERD

**Entities:**
- Customer
- LoadOrder
- LoadingOperation
- Tank
- Product (as entity for this example)
- Bay
- Driver
- CarrierTicket

**Conceptual ERD:**

```
┌──────────┐ 1        N ┌──────────┐ 1        1 ┌─────────────────┐
│Customer  │────────────│LoadOrder │────────────│LoadingOperation │
└──────────┘ places     └──────────┘ executes   └─────────────────┘
                              │ N                         │ 1
                              │ contains                  │ generates
                              │ 1                         │ 1
                        ┌─────────────────┐         ┌──────────────┐
                        │LoadOrderLineItem│         │CarrierTicket │
                        └─────────────────┘         └──────────────┘
                              │ N
                              │ uses
                              │ 1
                           ┌──────┐
                           │Tank  │
                           └──────┘
                              │ N
                              │ located at
                              │ 1
                           ┌─────┐
                           │Bay  │
                           └─────┘

┌─────────────────┐ N        1 ┌─────────────────┐
│LoadingOperation │────────────│Driver           │
└─────────────────┘ performed  └─────────────────┘
       by

┌─────────────────┐ N        1 ┌─────────┐
│LoadingOperation │────────────│Product  │
└─────────────────┘ loads      └─────────┘
```

**With attributes:**

```
┌─────────────────────────────┐
│ Customer                    │
├─────────────────────────────┤
│ PK: customer_id             │
│     company_name            │
│     contact_name            │
│     contact_email           │
│     phone_number            │
│     payment_terms_days      │
└─────────────────────────────┘
       │ 1
       │ places
       │ N
       ↓
┌─────────────────────────────┐
│ LoadOrder                   │
├─────────────────────────────┤
│ PK: load_order_id           │
│ FK: customer_id             │
│ FK: product_code            │
│     requested_quantity      │
│     status                  │
│     scheduled_date          │
│     scheduled_time          │
└─────────────────────────────┘
       │ 1
       │ executes
       │ 1
       ↓
┌─────────────────────────────┐
│ LoadingOperation            │
├─────────────────────────────┤
│ PK: loading_operation_id    │
│ FK: load_order_id           │
│ FK: bay_id                  │
│ FK: driver_id               │
│ FK: product_code            │
│     start_time              │
│     end_time                │
│     quantity_loaded         │
│     status                  │
└─────────────────────────────┘
       │ 1
       │ generates
       │ 1
       ↓
┌─────────────────────────────┐
│ CarrierTicket               │
├─────────────────────────────┤
│ PK: ticket_id               │
│ FK: loading_operation_id    │
│     bill_of_lading_number   │
│     net_quantity            │
│     seal_number             │
│     issue_date              │
└─────────────────────────────┘
```

---

## Hands-On Exercise: Create Conceptual Data Model

**Choose the same domain from previous projects** (petroleum terminal, hospital, e-commerce, property management).

### Part 1: Identify Entities (30 minutes)

From your domain model (Course 4), list:
1. **10-15 entities** that need persistent storage
2. For each, specify:
   - Why it's an entity (passes the entity checklist)
   - What aggregate it belongs to (if applicable)
   - Primary key strategy (UUID, BIGINT, natural key)

**Deliverable:** Entity list with justifications

### Part 2: Define Relationships (45 minutes)

For each pair of related entities:
1. **What is the relationship?** (Customer "places" LoadOrder)
2. **What is the cardinality?** (1:1, 1:N, N:M)
3. **Is it mandatory or optional?** (Can a Customer exist without LoadOrders? Can a LoadOrder exist without a Customer?)

**Deliverable:** Relationship matrix or list

### Part 3: Draw ERD (60 minutes)

Create a conceptual ERD showing:
- All entities (rectangles)
- All relationships (lines with cardinality)
- Key attributes for each entity
- Primary keys (PK) marked
- Foreign keys (FK) marked

**Tools:**
- **Lucidchart** (web-based, ERD template)
- **draw.io** (free, ERD shapes)
- **dbdiagram.io** (text-to-ERD)
- Hand-drawn (totally acceptable)

**Deliverable:** ERD diagram

### Part 4: Define Attributes and Constraints (45 minutes)

For 5-7 key entities, list:
1. **All attributes** with data types
2. **Constraints:**
   - NOT NULL (required fields)
   - UNIQUE (no duplicates)
   - CHECK (validation rules)
   - DEFAULT (default values)
3. **Business rules** each constraint enforces

**Deliverable:** Attribute and constraint specifications

---

## Reflection Questions

1. **Value Objects vs Entities in data models:** When should a value object (like Address) be embedded attributes vs a separate table?

2. **Natural vs Surrogate keys:** What are the trade-offs? When would you use a natural key as the primary key?

3. **Normalization:** What's the cost of normalization (more tables, more joins)? When might denormalization be acceptable?

4. **From your operational experience:** Think of data you tracked (spreadsheets, paper forms, legacy systems). What entities would you extract? What relationships?

5. **Constraints vs application code:** Should business rules be enforced in the database (constraints) or in application code? Or both?

---

## Key Takeaways

✅ **Conceptual data modeling is technology-independent** — Focus on what entities exist and how they relate, not tables and columns yet.

✅ **Translate domain models to data models** — Entities → tables, Value Objects → embedded attributes, Aggregates → related tables.

✅ **ERD shows entities and relationships** — Visual representation of your data structure.

✅ **Cardinality matters** — 1:1, 1:N, N:M determine how you implement relationships (foreign keys, junction tables).

✅ **Keys establish identity and relationships** — Primary keys identify, foreign keys relate.

✅ **Constraints enforce business rules** — NOT NULL, UNIQUE, CHECK, FOREIGN KEY prevent invalid data at database level.

✅ **Normalization reduces redundancy** — Separate tables for separate concerns, reducing update anomalies.

✅ **Data modeling flows from domain modeling** — Course 4 (DDD) informs Course 6 (data modeling).

---

## Connection to Week 24

This week you learned **conceptual data modeling**—identifying entities, relationships, and attributes at a high level.

Next week you'll learn **Logical and Physical Modeling**—translating your conceptual model into actual table definitions with:
- Normalization (1NF, 2NF, 3NF, BCNF)
- Indexes for performance
- Denormalization trade-offs
- Database-specific optimization
- Complete DDL (CREATE TABLE statements)

**Get ready to turn your ERD into an actual database schema.**

---

## Additional Resources

**Books:**
- *Database Design for Mere Mortals* by Michael Hernandez — Excellent introduction to data modeling
- *SQL and Relational Theory* by C.J. Date — Deep dive into relational model

**Tools:**
- **Lucidchart** — Web-based ERD modeling
- **dbdiagram.io** — Text-to-ERD, great for version control
- **ER/Studio, MySQL Workbench** — Professional data modeling tools

**Standards:**
- **Chen notation** (diamonds for relationships)
- **Crow's Foot notation** (lines with symbols for cardinality)
- **UML class diagrams** (can also represent data models)

**For Your Context:**
- Model petroleum terminal operations data (inventory, scheduling, operations, compliance)
- Compare your ERD to existing database schemas (if you have access)
- Identify where current schemas violate business rules

---

**End of Week 23 — Conceptual Data Modeling**

**Next:** Week 24 — Logical and Physical Modeling
