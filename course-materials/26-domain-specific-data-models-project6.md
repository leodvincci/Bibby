# Week 26: Domain-Specific Data Models + PROJECT 6 (Final Capstone)

## The Story: "The $50M System That Nobody Could Extend"

The boardroom presentation had gone silent. The VP of Engineering had just delivered the news: their three-year-old inventory management system, which cost $50 million to build, couldn't handle the company's planned expansion into temperature-controlled warehousing.

"Why not?" the CEO asked, visibly frustrated. "We specified from day one that we'd eventually need cold storage."

The architect pulled up a database diagram. "The problem isn't what we built. It's *how* we built it. Look at this."

The schema appeared on screen: a monolithic `inventory_items` table with 87 columns. Generic fields like `attribute_1` through `attribute_20`. No clear domain boundaries. Product data mixed with warehouse data mixed with billing data. No temporal tracking. No event history. Foreign keys pointing everywhere and nowhere.

"This was designed by someone who knew databases," the architect continued, "but didn't understand *our domain*. They didn't talk to warehouse operators. They didn't model our business processes. They didn't consider how products differ—pharmaceuticals need lot tracking and expiration dates, electronics need serial numbers, perishables need temperature monitoring. They built a database, not a domain model."

The CTO pulled up a competitor's recent press release: "Our competitor just launched cold chain logistics in six months because they designed their system around domain concepts from the start. Aggregates. Bounded contexts. Domain events. They can add new product types by extending their model, not rewriting it."

The CEO asked the question everyone was dreading: "How long to fix ours?"

"Eighteen months. $15 million. And we'll have to run both systems in parallel during migration."

The silence returned.

Six months later, the company hired a new architecture team. Their first task: talk to warehouse operators, drivers, inventory managers, and customers. Understand the domain. Model the concepts. Design boundaries. Plan for change. Only *then* open the database design tool.

The new lead architect put it simply: "We're not building a database. We're building a *model of reality*. If the model is wrong, the database will be wrong, the code will be wrong, and the system will be wrong. Get the domain model right first."

---

## Introduction: The Culmination of 26 Weeks

You've reached the final week of your apprenticeship in domain-driven systems thinking. Over 25 weeks, you've built a comprehensive toolkit:

**Course 1 (Weeks 1-4): Systems Thinking & Complex Domains**
- Understanding systems, feedback loops, emergence, boundaries
- Recognizing complexity and avoiding oversimplification
- Seeing patterns and relationships, not just components

**Course 2 (Weeks 5-8): Product Discovery & Problem Identification**
- Identifying real user problems through observation and research
- Distinguishing symptoms from root causes
- Validating problems before building solutions

**Course 3 (Weeks 9-13): Requirements Engineering & Domain Understanding**
- Discovering and documenting functional and non-functional requirements
- Modeling domains with use cases, user stories, and domain analysis
- Validating and verifying requirements with stakeholders

**Course 4 (Weeks 14-18): Domain-Driven Design**
- Establishing ubiquitous language and bounded contexts
- Modeling with entities, value objects, and aggregates
- Using domain events and strategic design patterns

**Course 5 (Weeks 19-22): Business Process Modeling & Workflows**
- Creating process models with BPMN
- Designing human-software collaboration
- Integrating workflows with domain models through events

**Course 6 (Weeks 23-26): Data Modeling in Real Domains**
- Conceptual, logical, and physical data modeling
- Normalization, optimization, and performance tuning
- Temporal modeling and data lifecycle management

This week, you'll **integrate everything** into a complete, production-ready data architecture for a real-world domain.

### Why This Matters for You

Your career trajectory—from Navy petroleum operations to Regional Operations Coordinator to software engineer with an MBA—positions you uniquely for this capstone:

- **Operations experience:** You understand real-world complexity that systems must support
- **Technical foundation:** CS fundamentals and Java/Spring Boot give you implementation capability
- **Business perspective:** MBA coursework connects technology decisions to business outcomes
- **Domain expertise:** Petroleum terminal operations provide a rich, complex domain for modeling

Backend engineers who can design data architectures that *model the domain* rather than just *store data* are rare and valuable. This capstone demonstrates that capability.

---

## Core Concept: Domain-Driven Data Architecture

A **domain-driven data architecture** is not just a collection of tables and relationships. It's a *coherent model of business reality* that:

### 1. Reflects Domain Concepts Directly

**Anti-pattern:**
```sql
CREATE TABLE items (
    item_id UUID PRIMARY KEY,
    type VARCHAR(50),  -- 'product', 'customer', 'order', ???
    attribute_1 VARCHAR(255),
    attribute_2 VARCHAR(255),
    -- ...
    attribute_20 VARCHAR(255),
    data_blob JSONB
);
```

This is a "universal table" that can store anything—which means it models nothing. The domain is invisible.

**Domain-driven approach:**
```sql
-- Explicit domain concepts
CREATE TABLE products (
    product_id UUID PRIMARY KEY,
    product_code VARCHAR(50) UNIQUE NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_category VARCHAR(100) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL
);

CREATE TABLE customers (
    customer_id UUID PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    customer_type VARCHAR(50) NOT NULL CHECK (customer_type IN ('DIRECT', 'DISTRIBUTOR', 'GOVERNMENT')),
    credit_limit NUMERIC(12,2),
    payment_terms_days INT
);

CREATE TABLE load_orders (
    load_order_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(customer_id),
    product_id UUID NOT NULL REFERENCES products(product_id),
    requested_quantity NUMERIC(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL
);
```

Each table represents a **clear domain concept** with a name from the ubiquitous language.

### 2. Enforces Domain Invariants

Business rules aren't just application code—they're database constraints:

```sql
CREATE TABLE tanks (
    tank_id UUID PRIMARY KEY,
    tank_number VARCHAR(20) UNIQUE NOT NULL,
    capacity_barrels NUMERIC(10,2) NOT NULL CHECK (capacity_barrels > 0),
    current_volume_barrels NUMERIC(10,2) NOT NULL DEFAULT 0,
    product_id UUID REFERENCES products(product_id),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'DECOMMISSIONED')),

    -- Invariant: volume cannot exceed capacity
    CHECK (current_volume_barrels >= 0 AND current_volume_barrels <= capacity_barrels),

    -- Invariant: if tank has product, must have non-zero volume
    CHECK ((product_id IS NULL AND current_volume_barrels = 0) OR
           (product_id IS NOT NULL AND current_volume_barrels > 0))
);
```

These constraints **prevent invalid states** at the database level, regardless of which application writes the data.

### 3. Aligns with Bounded Contexts

Different bounded contexts have different models, even for the "same" entity:

**Scheduling Context:**
```sql
CREATE TABLE scheduling.load_orders (
    load_order_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    requested_date DATE NOT NULL,
    scheduled_time_slot VARCHAR(20),  -- '08:00-10:00'
    bay_assignment UUID
);
```

**Operations Context:**
```sql
CREATE TABLE operations.loading_operations (
    loading_operation_id UUID PRIMARY KEY,
    load_order_id UUID NOT NULL,  -- Reference to scheduling context
    bay_id UUID NOT NULL,
    operator_id UUID NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    actual_quantity NUMERIC(10,2)
);
```

**Inventory Context:**
```sql
CREATE TABLE inventory.tank_transactions (
    transaction_id UUID PRIMARY KEY,
    tank_id UUID NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,  -- 'WITHDRAWAL', 'RECEIPT'
    quantity_barrels NUMERIC(10,2) NOT NULL,
    loading_operation_id UUID,  -- May reference operations context
    occurred_at TIMESTAMP NOT NULL
);
```

Each context has its own schema with tables relevant to its responsibilities. They communicate through **domain events** or **anti-corruption layers**, not direct foreign keys across contexts.

### 4. Supports Business Processes

Tables store the **durable state** that workflows read and modify:

```sql
-- Process: Load Order Fulfillment

-- 1. Customer submits order → INSERT into load_orders
-- 2. Logistics coordinator schedules → UPDATE load_orders SET scheduled_date, bay_assignment
-- 3. Driver arrives → INSERT into loading_operations
-- 4. Loading begins → UPDATE loading_operations SET start_time, status = 'IN_PROGRESS'
-- 5. Loading completes → UPDATE loading_operations SET end_time, actual_quantity, status = 'COMPLETED'
-- 6. Inventory withdrawn → INSERT into tank_transactions
-- 7. Ticket generated → INSERT into carrier_tickets
```

The data model mirrors the process flow.

### 5. Preserves History and Audit Trail

Temporal modeling (Week 25) enables answering historical questions:

```sql
-- What was the tank volume on July 15 at 8 AM?
SELECT volume_barrels
FROM tank_inventory_measurements
WHERE tank_id = 'tank-001'
  AND measured_at <= '2024-07-15 08:00:00'
ORDER BY measured_at DESC
LIMIT 1;

-- Who changed the scheduled date for this load order?
SELECT user_name, occurred_at, old_values, new_values, change_reason
FROM audit_log
WHERE table_name = 'load_orders'
  AND record_id = 'load-order-123'
  AND 'scheduled_date' = ANY(changed_fields)
ORDER BY occurred_at;

-- Reconstruct load order lifecycle from events
SELECT event_type, event_data, occurred_at
FROM load_order_events
WHERE load_order_id = 'load-order-123'
ORDER BY sequence_number;
```

### 6. Optimized for Real Workloads

Physical design (Week 24) balances normalization with performance:

```sql
-- Normalized for consistency
CREATE TABLE load_order_line_items (
    line_item_id UUID PRIMARY KEY,
    load_order_id UUID NOT NULL REFERENCES load_orders(load_order_id),
    product_id UUID NOT NULL REFERENCES products(product_id),
    quantity_barrels NUMERIC(10,2) NOT NULL
);

-- Denormalized for dashboard performance
CREATE MATERIALIZED VIEW daily_loading_summary AS
SELECT
    DATE(lo.scheduled_date) as loading_date,
    p.product_category,
    COUNT(DISTINCT lo.load_order_id) as total_loads,
    SUM(lop.actual_quantity) as total_volume_loaded,
    AVG(EXTRACT(EPOCH FROM (lop.end_time - lop.start_time))/60) as avg_load_time_minutes
FROM load_orders lo
JOIN loading_operations lop ON lo.load_order_id = lop.load_order_id
JOIN products p ON lo.product_id = p.product_id
WHERE lop.status = 'COMPLETED'
GROUP BY DATE(lo.scheduled_date), p.product_category;

-- Refresh nightly
REFRESH MATERIALIZED VIEW CONCURRENTLY daily_loading_summary;
```

---

## The Complete Design Process

### Phase 1: Domain Understanding (Courses 1-3)

**Step 1: Identify the System Boundaries**
- What's inside the system? What's outside?
- What are the inputs and outputs?
- What external systems do we integrate with?

**Step 2: Discover Domain Concepts**
- Interview domain experts (operators, coordinators, customers)
- Observe real operations
- Review existing documentation, forms, reports
- Create glossary of domain terms

**Step 3: Model Business Processes**
- What workflows exist?
- Who are the actors?
- What triggers each process?
- What are the decision points and exception paths?

**Step 4: Identify Domain Events**
- What significant things happen in the domain?
- What do people need to be notified about?
- What changes trigger other processes?

### Phase 2: Domain Modeling (Course 4)

**Step 5: Define Bounded Contexts**
- What are the major subsystems?
- Where are the natural boundaries?
- What teams own which areas?

**Step 6: Establish Ubiquitous Language**
- What terms do domain experts use?
- Resolve ambiguities and synonyms
- Document definitions

**Step 7: Identify Entities, Value Objects, and Aggregates**
- Which concepts have identity? (Entities)
- Which are immutable and defined by attributes? (Value Objects)
- What are the consistency boundaries? (Aggregates)

**Step 8: Model Domain Events**
- List all significant events
- Define event structure (what data they carry)
- Identify event publishers and subscribers

### Phase 3: Data Modeling (Course 6)

**Step 9: Create Conceptual Model (ERD)**
- Translate domain concepts to entities
- Define relationships and cardinality
- Identify natural keys

**Step 10: Develop Logical Schema**
- Normalize to 3NF
- Define primary and foreign keys
- Specify constraints (NOT NULL, CHECK, UNIQUE)
- Add temporal columns (valid_from, valid_to, sys_start, sys_end)

**Step 11: Design Physical Schema**
- Choose data types optimized for storage and performance
- Plan indexes based on query patterns
- Design partitioning strategy for large tables
- Identify denormalization opportunities for read-heavy queries
- Create materialized views for dashboards

**Step 12: Implement Audit and History**
- Add system-versioned temporal tables for entities that change
- Create audit_log table
- Implement soft delete where appropriate
- Design event store for event sourcing

### Phase 4: Integration (Course 5)

**Step 13: Map Processes to Data**
- For each BPMN process, identify what data it reads and writes
- Define transaction boundaries
- Plan for idempotency in event handlers

**Step 14: Design Event Flow**
- Which aggregate changes publish which events?
- Which processes subscribe to which events?
- How do events correlate to workflows?

### Phase 5: Validation and Optimization

**Step 15: Validate with Domain Experts**
- Review model with operators, coordinators, managers
- Verify it reflects reality
- Check for missing concepts or relationships

**Step 16: Performance Analysis**
- Identify critical queries
- Ensure proper indexes exist
- Test query performance with realistic data volumes
- Adjust denormalization strategy if needed

**Step 17: Document Trade-offs**
- Explain design decisions
- Document when and why you deviated from normal forms
- Justify index and partitioning strategies

---

## PROJECT 6: Complete Domain-Driven Data Architecture

### Overview

Design and document a **complete, production-ready data architecture** for one of the following domains:

1. **Petroleum Terminal Operations** (recommended if you want to leverage your domain expertise)
2. **Healthcare Patient Management System**
3. **E-commerce Platform**
4. **Fleet Management System**
5. **Property Management System**

Your deliverable will be a **30-40 page comprehensive design document** that integrates all six courses.

### Requirements

Your data architecture must include:

#### 1. Domain Understanding & Context (10-12 pages)

**A. System Overview**
- System purpose and scope
- Key stakeholders and their goals
- System boundaries (what's in, what's out)
- External system integrations

**B. Domain Glossary**
- 30-50 key domain terms with definitions
- Organized by bounded context
- Includes synonyms and clarifications

**C. Business Processes (BPMN)**
- 3-5 core end-to-end processes
- BPMN diagrams showing actors, activities, decision points
- Description of each process with happy path and exception paths
- Identification of process state that must be persisted

**D. Domain Events Catalog**
- List of 15-25 significant domain events
- Event definitions (what data they carry)
- Event publishers and subscribers
- Event flow diagram showing how events connect processes

**E. Bounded Contexts Map**
- Identify 3-5 bounded contexts
- Context responsibilities and boundaries
- Context relationships (Partnership, Customer-Supplier, ACL, etc.)
- Context map diagram

#### 2. Domain Model (8-10 pages)

**A. Core Domain Concepts**
- List and describe 20-30 key entities and value objects
- Aggregate boundaries with invariants
- Relationships between aggregates

**B. Ubiquitous Language Application**
- Show how domain terms map to code/database elements
- Examples of avoiding technical jargon
- Consistency across processes, events, and data

**C. Aggregate Design**
- Detail 5-7 key aggregates
- For each aggregate:
  - Root entity and child entities
  - Value objects within the aggregate
  - Business invariants enforced
  - Domain events published
  - Example lifecycle

#### 3. Data Architecture (12-15 pages)

**A. Conceptual Model**
- Complete ERD showing all entities and relationships
- Cardinality and relationship types
- Natural keys identified
- Domain concepts clearly represented

**B. Logical Schema**
- Normalized to 3NF (or BCNF where appropriate)
- All tables with columns, data types, constraints
- Primary keys, foreign keys, unique constraints
- CHECK constraints for business rules
- Temporal columns (valid_from, valid_to, etc.) for entities that change
- Soft delete columns where appropriate

**C. Physical Schema (Complete DDL)**
- Production-ready CREATE TABLE statements
- All constraints (NOT NULL, CHECK, UNIQUE, FK with ON DELETE behaviors)
- Sequences or UUID generation
- Partitioning for large tables (with partition definitions)
- Comments on tables and columns

**D. Index Strategy**
- List of all indexes with justification
- Index types (B-Tree, Hash, Partial, Composite, Covering)
- Query patterns each index supports
- Trade-off analysis (read vs write performance)

**E. Temporal Modeling**
- System-versioned temporal tables for key entities
- Bi-temporal model for at least one entity
- Audit trail implementation
- Event store design
- SCD type selection with rationale

**F. Denormalization Strategy**
- Materialized views for dashboards/reporting
- Triggers or refresh strategies
- Trade-off justification

#### 4. Process-Data Integration (4-5 pages)

**A. Workflow State Management**
- How BPMN processes read and write data
- Transaction boundaries
- Service task implementations (pseudocode or Java)

**B. Event-Driven Integration**
- How domain events trigger workflows
- How workflow completion publishes events
- Idempotency strategies

**C. Example Scenarios**
- Walk through 2-3 complete scenarios showing:
  - User action
  - Process triggered
  - Data read/written
  - Events published
  - Downstream effects

#### 5. Implementation Considerations (3-4 pages)

**A. Technology Stack**
- Database selection (PostgreSQL, MySQL, etc.) with rationale
- ORM considerations (JPA/Hibernate, jOOQ, etc.)
- Event bus (embedded, Kafka, RabbitMQ, etc.)
- Workflow engine (Camunda, Temporal, etc.)

**B. Performance Projections**
- Expected data volumes
- Query performance targets
- Scalability considerations
- Caching strategy

**C. Data Migration Strategy**
- If replacing existing system, how to migrate data
- Dual-write period considerations
- Data quality issues to address

**D. Operational Concerns**
- Backup and recovery strategy
- Data retention and archiving
- PII handling and GDPR compliance
- Monitoring and alerting

#### 6. Design Rationale & Trade-offs (2-3 pages)

**A. Key Design Decisions**
- Why these bounded contexts?
- Why these aggregate boundaries?
- Why this level of normalization?
- Why event sourcing for X but not Y?

**B. Trade-offs Made**
- Performance vs consistency
- Storage space vs query speed
- Flexibility vs simplicity
- Write complexity vs read complexity

**C. Alternative Approaches Considered**
- What other designs you evaluated
- Why you chose this approach
- Under what conditions you'd reconsider

### Domain-Specific Guidelines

#### Option 1: Petroleum Terminal Operations

If you choose this domain (recommended), focus on:

**Core Contexts:**
- Scheduling (load orders, time slots, bay assignments)
- Operations (loading operations, meters, operators)
- Inventory (tanks, receipts, withdrawals, reconciliation)
- Compliance (inspections, certifications, environmental reporting)
- Billing (tickets, invoicing, pricing)

**Key Processes:**
- Load Order Fulfillment (from request through completion)
- Tank Receipt and Blending
- Inventory Reconciliation
- Driver Check-in and Clearance
- Emergency Response (leak, overfill, etc.)

**Key Aggregates:**
- LoadOrder (with line items, scheduling)
- Tank (with inventory, product, capacity)
- LoadingOperation (with meter readings, ticket)
- Customer (with contracts, pricing)

**Temporal Needs:**
- Tank inventory measurements (bi-temporal: measured_at vs recorded_at)
- Product pricing (SCD Type 2: historical prices for invoicing)
- Load order status (event sourcing: complete lifecycle)
- Compliance records (audit trail: required by regulation)

**Technical Challenges:**
- Real-time meter readings (streaming data)
- Inventory reconciliation (eventual consistency)
- Multi-product tanks (complex business rules)
- Temperature/volume corrections (bi-temporal corrections)

#### Option 2: Healthcare Patient Management

If you choose this domain, focus on:

**Core Contexts:**
- Patient Registration
- Clinical Care (visits, diagnoses, treatments)
- Pharmacy (prescriptions, dispensing)
- Billing (claims, insurance, payments)
- Scheduling (appointments, providers, rooms)

**Key Processes:**
- Patient Check-in through Discharge
- Prescription Order to Dispensing
- Insurance Claim Processing
- Appointment Scheduling and Rescheduling

**Key Aggregates:**
- Patient (with demographics, insurance, history)
- Visit (with diagnoses, procedures, orders)
- Prescription (with medications, dosages, dispenses)
- Appointment (with provider, time, status)

**Temporal Needs:**
- Medical history (complete event log)
- Medication list (SCD Type 2: what was patient on at time of visit?)
- Insurance coverage (temporal: effective dates)
- Audit trail (HIPAA compliance requirement)

#### Option 3: E-commerce Platform

If you choose this domain, focus on:

**Core Contexts:**
- Catalog (products, categories, pricing)
- Shopping (cart, checkout)
- Order Fulfillment (picking, packing, shipping)
- Inventory (warehouses, stock levels)
- Customer Service (returns, refunds)

**Key Processes:**
- Browse to Purchase
- Order Fulfillment
- Return and Refund Processing
- Inventory Replenishment

**Key Aggregates:**
- Product (with variants, pricing, inventory)
- ShoppingCart (with items, totals)
- Order (with line items, shipping, payment)
- Shipment (with packages, tracking)

**Temporal Needs:**
- Product pricing (SCD Type 2: price at time of order)
- Inventory snapshots (for analytics)
- Order status history (event sourcing)
- Audit trail (for disputes)

#### Option 4: Fleet Management System

(See Week 25 hands-on exercise for detailed requirements)

#### Option 5: Property Management System

If you choose this domain, focus on:

**Core Contexts:**
- Properties (buildings, units, amenities)
- Leasing (tenants, leases, applications)
- Maintenance (work orders, vendors, inspections)
- Accounting (rent, deposits, payments, expenses)
- Communications (notices, complaints, messages)

**Key Processes:**
- Tenant Application to Lease Signing
- Rent Payment Processing
- Maintenance Request to Completion
- Lease Renewal or Termination

**Key Aggregates:**
- Property (with units, lease terms)
- Lease (with tenants, payments, terms)
- MaintenanceRequest (with work orders, vendor assignments)
- PaymentLedger (with charges, payments, balance)

**Temporal Needs:**
- Lease terms (SCD Type 2: renewals create new versions)
- Rent amounts (temporal: effective dates)
- Payment history (complete audit trail)
- Unit status changes (event sourcing: vacant → occupied → notice → vacant)

### Evaluation Criteria

Your project will be evaluated on:

**1. Domain Understanding (20%)**
- Depth of domain knowledge demonstrated
- Quality of glossary and definitions
- Accuracy of process models
- Identification of key events and concepts

**2. Domain Modeling (20%)**
- Appropriate bounded contexts
- Clear aggregate boundaries with valid invariants
- Correct entity vs value object classification
- Meaningful domain events

**3. Data Architecture Quality (30%)**
- Conceptual model clarity (ERD)
- Logical schema correctness (normalization, constraints)
- Physical schema completeness (DDL, indexes, partitioning)
- Temporal modeling appropriateness
- Query optimization strategy

**4. Integration & Cohesion (15%)**
- BPMN processes align with data model
- Domain events connect processes and data
- Transaction boundaries are clear
- Idempotency is addressed

**5. Documentation & Rationale (15%)**
- Clarity of explanations
- Justification of design decisions
- Trade-off analysis
- Alternative approaches discussed
- Production-readiness considerations

### Deliverable Format

Submit a single document (PDF or Markdown) with:

1. **Title Page**: Project title, your name, domain chosen, date
2. **Table of Contents**: With page numbers
3. **Executive Summary** (1 page): High-level overview of the architecture
4. **Main Sections** (as outlined above): 30-40 pages
5. **Appendices**:
   - Complete DDL (can be 10+ pages)
   - Sample queries demonstrating temporal capabilities
   - Event payload examples (JSON)
   - Java code snippets (optional, for key service tasks or event handlers)

**Formatting:**
- Use clear section headings
- Include diagrams for ERD, context map, BPMN processes, event flows
- Use code blocks for SQL and code
- Tables for catalogs (events, aggregates, indexes)

---

## Recommended Approach

### Week-by-Week (if you have 4 weeks for the capstone)

**Week 1: Domain Understanding**
- Choose your domain
- Interview domain experts or research extensively
- Create glossary
- Model 3-5 business processes in BPMN
- Identify domain events
- Define bounded contexts

**Week 2: Domain Modeling**
- List all entities and value objects
- Define aggregates and invariants
- Create conceptual ERD
- Document ubiquitous language application

**Week 3: Data Architecture**
- Develop logical schema (normalized)
- Write complete DDL
- Design index strategy
- Plan temporal modeling
- Create denormalized views
- Write sample queries

**Week 4: Integration & Documentation**
- Map processes to data operations
- Design event flows
- Document design rationale
- Analyze trade-offs
- Write implementation considerations
- Polish and finalize document

### Or Sprint-Style (if you're doing this intensively)

**Day 1-2: Domain Discovery**
- Research and understanding
- Process modeling
- Event identification

**Day 3-4: Conceptual & Logical Modeling**
- ERD creation
- Aggregate design
- Normalization

**Day 5-7: Physical Design**
- DDL writing
- Index design
- Temporal modeling
- Performance optimization

**Day 8-9: Integration**
- Process-data mapping
- Event flows
- Scenario walkthroughs

**Day 10: Documentation**
- Write all sections
- Create diagrams
- Finalize and polish

---

## Example: Petroleum Terminal - Partial Walkthrough

Let me demonstrate what a portion of this project looks like for the Petroleum Terminal domain:

### Bounded Context: Inventory

**Responsibilities:**
- Track current inventory levels in each tank
- Record receipts (incoming product)
- Record withdrawals (outgoing product via loading)
- Perform inventory reconciliation
- Generate inventory reports

**Key Entities:**
- Tank (aggregate root)
- TankInventoryMeasurement
- TankTransaction
- Product
- ReconciliationReport

**Aggregates:**

**Tank Aggregate:**
```
Tank (root)
  - tankId: TankId (identity)
  - tankNumber: String
  - capacity: Volume (value object)
  - currentVolume: Volume (calculated from measurements)
  - product: ProductCode (reference to Product)
  - status: TankStatus (ACTIVE, MAINTENANCE, DECOMMISSIONED)

Invariants:
  - currentVolume must not exceed capacity
  - Cannot change product if volume > 0 (must be empty first)
  - Cannot record withdrawal if insufficient volume

Domain Events Published:
  - TankReceivedProduct
  - ProductWithdrawnFromTank
  - TankVolumeReconciled
  - TankStatusChanged
```

**Logical Schema:**

```sql
-- Tank Aggregate Root
CREATE TABLE inventory.tanks (
    tank_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tank_number VARCHAR(20) UNIQUE NOT NULL,
    capacity_barrels NUMERIC(10,2) NOT NULL CHECK (capacity_barrels > 0),
    product_code VARCHAR(50) REFERENCES inventory.products(product_code),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'DECOMMISSIONED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CHECK ((product_code IS NULL) OR (product_code IS NOT NULL))
);

-- Tank Inventory Measurements (Bi-temporal)
CREATE TABLE inventory.tank_inventory_measurements (
    measurement_id BIGSERIAL PRIMARY KEY,
    tank_id UUID NOT NULL REFERENCES inventory.tanks(tank_id),

    -- Valid time (when measurement was taken)
    measured_at TIMESTAMP NOT NULL,

    -- Transaction time (when recorded in system)
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    corrected_at TIMESTAMP,

    -- Measurements
    volume_barrels NUMERIC(10,2) NOT NULL CHECK (volume_barrels >= 0),
    temperature_fahrenheit NUMERIC(5,2),
    water_content_percentage NUMERIC(5,4) CHECK (water_content_percentage BETWEEN 0 AND 100),

    -- Audit
    measured_by UUID NOT NULL,
    measurement_method VARCHAR(50) NOT NULL
        CHECK (measurement_method IN ('AUTOMATIC_GAUGE', 'MANUAL_STICK', 'TANK_STRAPPING')),

    is_current_measurement BOOLEAN DEFAULT true,
    superseded_by BIGINT REFERENCES inventory.tank_inventory_measurements(measurement_id),

    UNIQUE (tank_id, measured_at, is_current_measurement)
);

CREATE INDEX idx_tank_measurements_valid_time
    ON inventory.tank_inventory_measurements(tank_id, measured_at DESC)
    WHERE is_current_measurement = true;

CREATE INDEX idx_tank_measurements_transaction_time
    ON inventory.tank_inventory_measurements(tank_id, recorded_at DESC);

-- Tank Transactions (Event Sourcing)
CREATE TABLE inventory.tank_transaction_events (
    event_id BIGSERIAL PRIMARY KEY,
    tank_id UUID NOT NULL REFERENCES inventory.tanks(tank_id),
    event_type VARCHAR(100) NOT NULL
        CHECK (event_type IN ('TANK_RECEIVED_PRODUCT', 'PRODUCT_WITHDRAWN',
                               'INVENTORY_RECONCILED', 'MEASUREMENT_CORRECTED')),
    event_data JSONB NOT NULL,
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id UUID NOT NULL,
    sequence_number INT NOT NULL,

    UNIQUE (tank_id, sequence_number)
);

CREATE INDEX idx_tank_events_order
    ON inventory.tank_transaction_events(tank_id, sequence_number);

-- Current State Projection (Materialized View)
CREATE MATERIALIZED VIEW inventory.tanks_current_state AS
SELECT
    t.tank_id,
    t.tank_number,
    t.capacity_barrels,
    t.product_code,
    p.product_name,
    t.status,
    COALESCE(latest.volume_barrels, 0) as current_volume_barrels,
    latest.measured_at as last_measurement_at,
    t.capacity_barrels - COALESCE(latest.volume_barrels, 0) as available_capacity_barrels,
    CASE
        WHEN COALESCE(latest.volume_barrels, 0) = 0 THEN 'EMPTY'
        WHEN latest.volume_barrels >= t.capacity_barrels * 0.95 THEN 'NEAR_FULL'
        WHEN latest.volume_barrels <= t.capacity_barrels * 0.10 THEN 'LOW'
        ELSE 'NORMAL'
    END as inventory_status
FROM inventory.tanks t
LEFT JOIN inventory.products p ON t.product_code = p.product_code
LEFT JOIN LATERAL (
    SELECT volume_barrels, measured_at
    FROM inventory.tank_inventory_measurements
    WHERE tank_id = t.tank_id
      AND is_current_measurement = true
    ORDER BY measured_at DESC
    LIMIT 1
) latest ON true
WHERE t.status = 'ACTIVE';

CREATE UNIQUE INDEX idx_tanks_current_state_tank
    ON inventory.tanks_current_state(tank_id);
```

**Domain Event:**

```java
public class ProductWithdrawnFromTank extends DomainEvent {
    private final UUID tankId;
    private final String tankNumber;
    private final String productCode;
    private final BigDecimal quantityBarrels;
    private final UUID loadingOperationId;  // Reference to operations context
    private final Instant occurredAt;
    private final UUID withdrawnBy;

    // Constructor, getters
}
```

**Service Task (BPMN Integration):**

```java
@Component("recordTankWithdrawalDelegate")
public class RecordTankWithdrawalDelegate implements JavaDelegate {

    @Autowired
    private TankRepository tankRepository;

    @Autowired
    private EventPublisher eventPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        UUID loadingOperationId = UUID.fromString(
            (String) execution.getVariable("loadingOperationId")
        );
        BigDecimal quantityLoaded = (BigDecimal) execution.getVariable("actualQuantityLoaded");
        UUID tankId = UUID.fromString((String) execution.getVariable("tankId"));

        // Load aggregate
        Tank tank = tankRepository.findById(tankId)
            .orElseThrow(() -> new IllegalArgumentException("Tank not found"));

        // Execute domain logic (enforces invariants)
        tank.recordWithdrawal(quantityLoaded, loadingOperationId, getCurrentUserId());

        // Persist
        tankRepository.save(tank);

        // Publish events
        tank.getDomainEvents().forEach(event -> {
            eventPublisher.publish(event);

            // Also record in event store
            if (event instanceof ProductWithdrawnFromTank) {
                tankEventStore.append(tankId, event);
            }
        });
    }
}
```

**Temporal Query Examples:**

```sql
-- 1. What is the current volume in Tank T-001?
SELECT current_volume_barrels
FROM inventory.tanks_current_state
WHERE tank_number = 'T-001';

-- 2. What was the volume at 8:00 AM on July 15, 2024?
SELECT volume_barrels
FROM inventory.tank_inventory_measurements
WHERE tank_id = (SELECT tank_id FROM inventory.tanks WHERE tank_number = 'T-001')
  AND measured_at <= '2024-07-15 08:00:00'
  AND is_current_measurement = true
ORDER BY measured_at DESC
LIMIT 1;

-- 3. What did we think the volume was at 8 AM when we ran the 9 AM report?
SELECT volume_barrels
FROM inventory.tank_inventory_measurements
WHERE tank_id = (SELECT tank_id FROM inventory.tanks WHERE tank_number = 'T-001')
  AND measured_at <= '2024-07-15 08:00:00'
  AND recorded_at <= '2024-07-15 09:00:00'
  AND is_current_measurement = true
ORDER BY measured_at DESC
LIMIT 1;

-- 4. Show all measurement corrections for Tank T-001 in July 2024
SELECT
    measurement_id,
    measured_at,
    volume_barrels,
    recorded_at,
    corrected_at,
    superseded_by,
    measurement_method
FROM inventory.tank_inventory_measurements
WHERE tank_id = (SELECT tank_id FROM inventory.tanks WHERE tank_number = 'T-001')
  AND measured_at BETWEEN '2024-07-01' AND '2024-07-31'
ORDER BY measured_at, recorded_at;

-- 5. Reconstruct Tank T-001 complete event history
SELECT
    event_id,
    event_type,
    event_data,
    occurred_at,
    sequence_number
FROM inventory.tank_transaction_events
WHERE tank_id = (SELECT tank_id FROM inventory.tanks WHERE tank_number = 'T-001')
ORDER BY sequence_number;

-- 6. Calculate total withdrawals from Tank T-001 in June 2024
SELECT
    COUNT(*) as withdrawal_count,
    SUM((event_data->>'quantityBarrels')::NUMERIC) as total_withdrawn_barrels
FROM inventory.tank_transaction_events
WHERE tank_id = (SELECT tank_id FROM inventory.tanks WHERE tank_number = 'T-001')
  AND event_type = 'PRODUCT_WITHDRAWN'
  AND occurred_at >= '2024-06-01'
  AND occurred_at < '2024-07-01';
```

**Index Justification:**

1. **idx_tank_measurements_valid_time**: Supports "as-of" queries filtering by measured_at (most common query pattern for historical lookups)

2. **idx_tank_measurements_transaction_time**: Supports bi-temporal queries asking "what did we know when?" (audit and compliance)

3. **idx_tank_events_order**: Supports event replay for state reconstruction (critical for event sourcing pattern)

4. **idx_tanks_current_state_tank**: Unique index on materialized view for fast current-state lookups (dashboard queries)

**Trade-offs:**

- **Event sourcing for transactions vs direct state updates**: Chose event sourcing because:
  - Regulatory requirement for complete audit trail
  - Need to answer "why is volume different from expected?" requires event sequence
  - Inventory reconciliation benefits from replay capability
  - Trade-off: More complex code, more storage (but critical for compliance)

- **Bi-temporal measurements vs simple timestamps**: Chose bi-temporal because:
  - Manual stick readings often recorded minutes after actual measurement
  - Corrections are common when gauge readings are found to be inaccurate
  - Invoicing disputes require proving "what did we record at billing time?"
  - Trade-off: Query complexity, larger table (but avoids litigation costs)

- **Materialized view for current state vs always calculate**: Chose materialized view because:
  - Dashboard queries need sub-second response for 100+ tanks
  - Current state queries vastly outnumber historical queries (90/10 split)
  - Refresh nightly is acceptable (real-time operators use direct measurement table)
  - Trade-off: Data staleness, refresh overhead (but acceptable for use case)

---

This example demonstrates **one bounded context** in a complete project. Your full PROJECT 6 would include 3-5 contexts with similar depth.

---

## Reflection Questions

1. **Integration Thinking:** How does this capstone pull together everything you've learned from Week 1 through Week 25? Where do you see the connections?

2. **Career Application:** How will completing this project help you in job interviews? What specific skills does it demonstrate?

3. **Domain Expertise:** If you choose the Petroleum Terminal domain, how does your operational experience change the data model? What would someone without that experience miss?

4. **MBA Perspective:** How do the data architecture decisions connect to business outcomes? Where are the cost/benefit trade-offs?

5. **Learning Journey:** What was the most surprising thing you learned in these 26 weeks? What concept clicked later that didn't make sense at first?

---

## Key Takeaways

1. **Data architecture is domain modeling** - Tables and columns should represent domain concepts, not generic "data storage"

2. **Constraints enforce invariants** - Business rules belong in the database, not just application code

3. **Bounded contexts need schema boundaries** - Don't force one universal model; embrace multiple perspectives

4. **Temporal modeling is non-negotiable** - In real systems, "what was this value on date X?" is a business requirement

5. **Events connect everything** - Domain events link processes, contexts, and data changes into a coherent system

6. **Optimization requires understanding workloads** - Don't optimize blindly; measure query patterns first

7. **Trade-offs are everywhere** - Every design decision has costs and benefits; make them explicitly and document why

8. **Documentation is part of the design** - If you can't explain your decisions, you don't understand them well enough

9. **Domain experts are your co-designers** - The best data models come from collaboration, not isolation

10. **This is a craft** - Getting good at data architecture takes practice, iteration, and learning from mistakes

---

## Connection to Your Career

### The Portfolio Piece

This PROJECT 6 deliverable is the **centerpiece of your technical portfolio**. When interviewing for backend engineering roles, you can:

- **Show systems thinking**: "I designed this by understanding the domain first, not jumping to tables"
- **Demonstrate DDD knowledge**: "Here are my bounded contexts and aggregates with clear invariants"
- **Prove data modeling skills**: "I normalized to 3NF here, but denormalized here for performance—let me explain why"
- **Show temporal modeling expertise**: "I implemented bi-temporal modeling for audit compliance"
- **Illustrate integration skills**: "Here's how BPMN workflows interact with the domain model through events"

### The Interview Conversation

Interviewer: "Tell me about a complex system you've designed."

You: "I designed a complete data architecture for a petroleum terminal operations system. Let me walk you through my bounded contexts..." [Pull up your PROJECT 6 document]

**You're not just showing code—you're showing thinking.**

### The Career Progression

This 26-week course has prepared you for roles like:

- **Backend Software Engineer** (junior-to-mid level)
- **Domain Modeling Specialist**
- **Data Architect** (with more experience)
- **Solutions Architect**
- **Technical Lead** (with team leadership skills)

Your unique combination—operations background + CS fundamentals + MBA + domain-driven systems thinking—positions you for **systems integration** roles in industrial automation, energy, logistics, or manufacturing.

---

## Closing Thoughts: The Journey from Week 1 to Week 26

Twenty-six weeks ago, you started with systems thinking—learning to see wholes, not just parts. To recognize feedback loops. To understand emergence and boundaries.

Then you learned to discover problems, not assume solutions. To observe users. To validate needs.

You mastered requirements engineering—turning fuzzy ideas into clear, testable specifications.

You dove into Domain-Driven Design—ubiquitous language, bounded contexts, aggregates, events. You learned that code should speak the language of the business.

You modeled business processes with BPMN—capturing the choreography of people and systems working together.

And finally, you learned to design data architectures that preserve truth, enforce consistency, optimize performance, and maintain history.

But most importantly, you learned that **all of these are connected**. Systems thinking informs domain modeling. Requirements drive data structures. Processes depend on persistent state. Events connect everything.

You've built a **unified mental model** of how to design software systems that actually work in the messy, complex, ever-changing real world.

**Now go build something amazing.**

---

## Additional Resources

### Books (Comprehensive References)

- **"Domain-Driven Design" by Eric Evans** - The original DDD book; read it now with full context
- **"Implementing Domain-Driven Design" by Vaughn Vernon** - Practical implementation patterns
- **"The Data Warehouse Toolkit" by Ralph Kimball** - Dimensional modeling and SCD patterns
- **"Designing Data-Intensive Applications" by Martin Kleppmann** - Modern data architecture patterns
- **"Database Internals" by Alex Petrov** - Deep dive into how databases actually work

### Online Resources

- **Martin Fowler's Blog**: https://martinfowler.com/ (patterns, architecture, refactoring)
- **DDD Community**: https://www.dddcommunity.org/ (forums, examples, conferences)
- **PostgreSQL Documentation**: https://www.postgresql.org/docs/ (authoritative reference)
- **Event Storming**: https://www.eventstorming.com/ (collaborative modeling technique)

### Communities

- **r/softwarearchitecture** (Reddit)
- **DDD/CQRS/ES Discord** (search for invite links)
- **Local meetups**: Domain-Driven Design, Software Architecture, Database groups

### Conferences

- **Domain-Driven Design Europe** (DDD Europe)
- **KanDDDinsky** (DDD conference)
- **QCon** (Software architecture)
- **GOTO Conferences** (Software development)

### Next Learning Steps

1. **Implement your PROJECT 6** - Build a working prototype with Spring Boot + PostgreSQL
2. **Study CQRS (Command Query Responsibility Segregation)** - Advanced pattern for read/write separation
3. **Learn Kubernetes and cloud deployment** - Take your designs to production
4. **Explore event-driven architectures at scale** - Kafka, event streaming, distributed systems
5. **Study distributed data patterns** - Sagas, eventual consistency, distributed transactions

---

## Final Assignment: PROJECT 6

**Due:** 4 weeks from starting (or sooner if you're working intensively)

**Deliverable:** 30-40 page comprehensive data architecture document following the requirements outlined above

**Submission:** Save as `PROJECT-6-[YourDomain]-Data-Architecture.pdf` or `.md`

**Self-Evaluation:** After completing, evaluate yourself against the criteria on page 20

**Optional:** Build a working prototype implementing key parts of your architecture

---

**Congratulations on completing Week 26 and the entire 26-week apprenticeship!**

You are now equipped to design domain-driven systems that model reality, enforce consistency, optimize performance, and adapt to change. Go create systems that matter.

**- End of Week 26 -**
**- End of Course: Domain-Driven Systems Thinking for Software Engineers -**
