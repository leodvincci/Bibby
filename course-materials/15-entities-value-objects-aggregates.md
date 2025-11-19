# Week 15: Entities, Value Objects & Aggregates

**Course 4: Domain-Driven Design (DDD) — Week 15 of 26**

---

## The Identity Crisis That Cost $8M

In 2017, a financial services company launched a new loan processing system. Six months in, they discovered a catastrophic problem:

**The same loan existed multiple times in the database.**

How did this happen?

A customer applied for a loan. The application was saved to the database. The customer called back to update their income information. Instead of updating the existing loan, the system created a new one. Then the underwriter opened the loan for review—and created another copy.

Within weeks, the database had 50,000+ duplicate loans. Some were approved multiple times. Others were denied while their duplicates were funded. The accounting team couldn't reconcile which disbursements matched which loans.

**The root cause?** The developers didn't understand **identity**.

They modeled `Loan` as a simple data structure:

```java
public class Loan {
    private String applicantName;
    private BigDecimal amount;
    private LocalDate applicationDate;

    // No unique identifier!
    // No concept of "same loan"
}
```

Every time someone read a loan from the database, modified it, and saved it back, the system created a **new instance** instead of updating the existing one. The system couldn't tell if two `Loan` objects represented the **same real-world loan** or **different loans with identical data**.

**The fix cost $8M and took 18 months:** adding proper identifiers, deduplicating records, rebuilding trust with regulators.

**The lesson:** Some domain objects are defined by **who they are** (their identity). Others are defined by **what they contain** (their attributes). Getting this distinction wrong creates catastrophic bugs.

Welcome to **Week 15: Entities, Value Objects & Aggregates**—the tactical building blocks of domain models.

---

## The Three Building Blocks

Domain-Driven Design provides three fundamental patterns for modeling domain concepts in code:

1. **Entity** — An object defined by its **identity**, not its attributes
2. **Value Object** — An object defined by its **attributes**, not identity
3. **Aggregate** — A cluster of entities and value objects treated as a single unit with a consistency boundary

Let's explore each.

---

## Core Concept 1: Entities

An **Entity** is a domain object that:
- Has a **unique identity** that persists over time
- Can **change its attributes** while remaining the same entity
- Is **distinguishable** from other entities even if all attributes are identical

### Identity in the Real World

Think about your operational experience:

**A tank at a petroleum terminal** is an Entity:
- Tank #5 is always Tank #5, even when it's empty, full, or being cleaned
- Tank #5 today is the same Tank #5 from last year
- Two tanks can be identical (same capacity, same product, same location) but they're still **different tanks**

**A truck in a fleet** is an Entity:
- Truck #247 is uniquely identifiable by VIN or fleet number
- Even if you repaint it, replace the engine, and change the license plate, it's still the same truck
- Two identical trucks (same make, model, year) are still different trucks

**A customer** is an Entity:
- Jane Smith with customer ID #12345 is the same person even if she changes her address, phone number, or name
- Two people named "Jane Smith" are different customers

### Identity in Code

In Java, Entities must have an **identifier field**:

```java
public class Tank {
    private final TankId id;  // Immutable identifier
    private String tankNumber;
    private Capacity capacity;
    private Product currentProduct;
    private Volume currentLevel;

    public Tank(TankId id, String tankNumber, Capacity capacity) {
        this.id = Objects.requireNonNull(id, "Tank ID cannot be null");
        this.tankNumber = tankNumber;
        this.capacity = capacity;
    }

    public TankId getId() {
        return id;
    }

    // Attributes can change, identity cannot
    public void receive(Volume amount, Product product) {
        // Business logic to update level and product
        this.currentLevel = currentLevel.add(amount);
        this.currentProduct = product;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tank)) return false;
        Tank tank = (Tank) o;
        return id.equals(tank.id);  // Equality based on ID only!
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);  // Hash based on ID only!
    }
}
```

**Key characteristics:**

1. **Identity is immutable** — Once created, a Tank's ID never changes
2. **Equality based on identity** — Two tanks are equal if they have the same ID, regardless of attributes
3. **Attributes can change** — Tank level, product, status can all change, but it's still the same tank
4. **Lifecycle matters** — Entities are created, modified, and eventually archived/deleted

### Entity Lifecycle

Entities move through states over time. From **Week 11 (State Machines)**, you know how to model these lifecycles:

**Loan Entity Lifecycle:**
```
Created → Under Review → Approved → Funded → Active → Paid Off
                ↓
              Denied
```

**Each state is still the same Loan** (same identity), just in different states.

```java
public class Loan {
    private final LoanId id;
    private LoanState state;
    private BigDecimal principal;
    private InterestRate rate;
    private LocalDate applicationDate;

    public void approve(Underwriter underwriter) {
        if (this.state != LoanState.UNDER_REVIEW) {
            throw new IllegalStateException(
                "Can only approve loans that are under review"
            );
        }
        this.state = LoanState.APPROVED;
        // Other approval logic...
    }

    public void fund(FundingSource source, LocalDate fundedDate) {
        if (this.state != LoanState.APPROVED) {
            throw new IllegalStateException(
                "Can only fund approved loans"
            );
        }
        this.state = LoanState.FUNDED;
        // Other funding logic...
    }
}
```

**Notice:** The loan's identity (`LoanId`) never changes. The state transitions, but it's always the same loan.

### When to Use Entities

Use an Entity when:
- ✅ The object needs to be **tracked over time** (you care about "the same X from yesterday")
- ✅ The object can **change** while remaining conceptually the same
- ✅ You need to **distinguish** between two objects even if their attributes are identical
- ✅ The object has a **lifecycle** with meaningful state transitions

**Examples from different domains:**

**Petroleum Terminal:**
- Tank, LoadOrder, LoadingOperation, Carrier, Driver

**Healthcare:**
- Patient, Appointment, MedicalRecord, Prescription

**E-commerce:**
- Customer, Order, ShoppingCart, Product (catalog item)

**Banking:**
- Account, Transaction, Loan, Customer

---

## Core Concept 2: Value Objects

A **Value Object** is a domain object that:
- Has **no identity** — it's defined entirely by its attributes
- Is **immutable** — once created, it never changes
- Is **replaceable** — if you want to "change" it, you create a new one
- Is **comparable by value** — two value objects with the same attributes are considered identical

### Values in the Real World

Think about concepts that are defined purely by their attributes:

**A mailing address**:
- "123 Main St, Houston, TX 77002" is the same address whether it's on your driver's license or a shipping label
- There's no such thing as "Address #5" that's distinct from "Address #6" if they both say "123 Main St"
- If you move, you don't "update the address"—you have a **new address**

**A monetary amount**:
- $50.00 is $50.00, regardless of where it appears
- There's no meaningful concept of "the same $50 from yesterday"
- Two amounts of $50.00 are indistinguishable

**A date range**:
- "January 1, 2024 to January 31, 2024" is defined by its start and end dates
- Two ranges with the same dates are identical
- You don't "update" a date range—you create a new one

### Value Objects in Code

```java
public class Address {
    private final String street;
    private final String city;
    private final String state;
    private final String zipCode;

    public Address(String street, String city, String state, String zipCode) {
        this.street = Objects.requireNonNull(street);
        this.city = Objects.requireNonNull(city);
        this.state = Objects.requireNonNull(state);
        this.zipCode = Objects.requireNonNull(zipCode);
    }

    // No setters! Immutable!

    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getZipCode() { return zipCode; }

    // To "change" an address, create a new one
    public Address withStreet(String newStreet) {
        return new Address(newStreet, this.city, this.state, this.zipCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;
        Address address = (Address) o;
        return street.equals(address.street) &&
               city.equals(address.city) &&
               state.equals(address.state) &&
               zipCode.equals(address.zipCode);  // Equality based on ALL attributes
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, state, zipCode);
    }

    @Override
    public String toString() {
        return street + ", " + city + ", " + state + " " + zipCode;
    }
}
```

**Key characteristics:**

1. **All fields are final** — Immutable by design
2. **No identity** — No `AddressId` field
3. **Equality based on all attributes** — Two addresses with identical fields are equal
4. **No setters** — To "change" an address, create a new instance
5. **"with" methods** — Convenient factory methods for creating modified copies

### Petroleum Terminal Example: Value Objects

```java
public class Volume {
    private final BigDecimal barrels;

    public Volume(BigDecimal barrels) {
        if (barrels.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Volume cannot be negative");
        }
        this.barrels = barrels;
    }

    public Volume add(Volume other) {
        return new Volume(this.barrels.add(other.barrels));
    }

    public Volume subtract(Volume other) {
        return new Volume(this.barrels.subtract(other.barrels));
    }

    public boolean isGreaterThan(Volume other) {
        return this.barrels.compareTo(other.barrels) > 0;
    }

    public BigDecimal toBarrels() {
        return barrels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Volume)) return false;
        Volume volume = (Volume) o;
        return barrels.equals(volume.barrels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barrels);
    }
}
```

**Why is Volume a Value Object?**
- 5,000 barrels is 5,000 barrels, regardless of context
- There's no concept of "Volume #123" that's different from "Volume #456" if both are 5,000 barrels
- If a tank level changes from 5,000 to 6,000 barrels, you don't "update the volume"—you have a **new volume**

### More Value Object Examples

**Money:**
```java
public class Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    // More operations...
}
```

**DateRange:**
```java
public class DateRange {
    private final LocalDate start;
    private final LocalDate end;

    public DateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        this.start = start;
        this.end = end;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    public boolean overlaps(DateRange other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }
}
```

**ProductCode:**
```java
public class ProductCode {
    private final String code;

    public ProductCode(String code) {
        if (!code.matches("[A-Z]{3}\\d{3}")) {
            throw new IllegalArgumentException(
                "Product code must be 3 letters followed by 3 digits"
            );
        }
        this.code = code;
    }

    public String getCode() { return code; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductCode)) return false;
        return code.equals(((ProductCode) o).code);
    }

    @Override
    public int hashCode() { return code.hashCode(); }
}
```

### Benefits of Value Objects

**1. Type Safety**

Instead of:
```java
public void scheduleLoad(String productCode, double quantity) {
    // Easy to accidentally swap parameters!
    scheduleLoad(quantity, productCode);  // Compiles but wrong!
}
```

Use:
```java
public void scheduleLoad(ProductCode productCode, Volume quantity) {
    // Type system prevents mistakes
    scheduleLoad(quantity, productCode);  // Compiler error!
}
```

**2. Encapsulated Validation**

```java
// Validation happens in the Value Object constructor
Volume volume = new Volume(new BigDecimal("-500"));  // Throws exception
DateRange range = new DateRange(laterDate, earlierDate);  // Throws exception
ProductCode code = new ProductCode("INVALID");  // Throws exception

// Once you have a valid Value Object, you know it's valid
public void processLoad(Volume quantity) {
    // No need to check if quantity is negative—it can't be!
}
```

**3. Domain Operations**

Value Objects can express domain operations:

```java
Volume initial = new Volume(new BigDecimal("5000"));
Volume received = new Volume(new BigDecimal("2000"));
Volume withdrawn = new Volume(new BigDecimal("1500"));

Volume final = initial.add(received).subtract(withdrawn);
// Result: 5,500 barrels

Money price = new Money(new BigDecimal("50.00"), Currency.USD);
Money discounted = price.multiply(0.9);  // 10% discount
```

**4. Testability**

Value Objects are easy to test because they're immutable and have no side effects:

```java
@Test
public void volumeAddition() {
    Volume v1 = new Volume(new BigDecimal("1000"));
    Volume v2 = new Volume(new BigDecimal("500"));
    Volume result = v1.add(v2);

    assertEquals(new BigDecimal("1500"), result.toBarrels());
    // v1 and v2 are unchanged (immutable)
}
```

### When to Use Value Objects

Use a Value Object when:
- ✅ Identity doesn't matter—only the **attributes** define the concept
- ✅ The object should be **immutable**
- ✅ Two objects with the same attributes should be considered **identical**
- ✅ You want **type safety** for primitive values
- ✅ You want to **encapsulate validation rules**
- ✅ You want to **express domain operations**

**Examples:**
- Addresses, phone numbers, email addresses
- Monetary amounts, quantities, measurements
- Date ranges, time periods
- Codes, identifiers (as values, not entity IDs)
- Coordinates, locations
- Percentages, rates, ratios

---

## Entity vs Value Object: The Decision

**Ask these questions:**

| Question | Entity | Value Object |
|----------|--------|--------------|
| Do I care about tracking **the same thing over time**? | ✅ Yes | ❌ No |
| Can it **change** while remaining the same thing? | ✅ Yes | ❌ No |
| Do two objects with identical attributes represent **different things**? | ✅ Yes | ❌ No |
| Does it have a **lifecycle** with state transitions? | ✅ Yes | ❌ No |
| Should it be **immutable**? | ❌ No | ✅ Yes |
| Is it defined purely by its **attributes**? | ❌ No | ✅ Yes |

**Examples:**

| Concept | Entity or Value? | Why? |
|---------|------------------|------|
| **Customer** | Entity | Identity matters. Jane Smith (#12345) is distinct from Jane Smith (#67890). Changes over time. |
| **Customer's Address** | Value Object | Just data. "123 Main St" is "123 Main St" regardless of who it belongs to. Immutable. |
| **Bank Account** | Entity | Identity matters. Account #555 is distinct from Account #666 even if they have the same balance. |
| **Account Balance** | Value Object | Just a number. $1000 is $1000. No identity. |
| **Product (catalog item)** | Entity | Identity matters. Product #SKU-123 is tracked over time. Price changes, but it's still the same product. |
| **Product Price** | Value Object | Just an amount. $19.99 is $19.99. |
| **Tank** | Entity | Identity matters. Tank #5 is distinct from Tank #6. Level changes, but it's still Tank #5. |
| **Tank Capacity** | Value Object | Just a volume. 10,000 barrels is 10,000 barrels. |

**Tricky Case: "Product" in Different Contexts**

In an **inventory context**, "Product" might be a Value Object:
```java
// Product as a value (just a type/category)
public class Product {
    private final ProductCode code;
    private final String name;
    // Immutable, no identity
}
```

In a **catalog context**, "Product" might be an Entity:
```java
// Product as an entity (catalog item with lifecycle)
public class Product {
    private final ProductId id;  // Identity
    private ProductCode code;
    private String name;
    private Money price;  // Can change
    private ProductStatus status;  // ACTIVE, DISCONTINUED, etc.
}
```

**This is OK!** Different **Bounded Contexts** (Week 14) can model the same concept differently.

---

## Core Concept 3: Aggregates

An **Aggregate** is a cluster of domain objects (Entities and Value Objects) that are treated as a single unit for data changes.

### The Consistency Problem

Imagine a **LoadOrder** for a petroleum terminal:

```java
public class LoadOrder {
    private LoadOrderId id;
    private Customer customer;
    private Product product;
    private Volume requestedQuantity;
    private List<LoadOrderLineItem> lineItems;
    private LoadOrderStatus status;
}

public class LoadOrderLineItem {
    private LoadOrderLineItemId id;
    private Product product;
    private Volume quantity;
    private Tank sourceTank;
}
```

**Business rule:** "The sum of all line item quantities must equal the requested quantity."

**Problem:** If `LoadOrder` and `LoadOrderLineItem` are separate, independently modifiable entities, how do you enforce this rule?

```java
// Someone could do this:
LoadOrder order = repository.find(orderId);
order.setRequestedQuantity(new Volume(new BigDecimal("5000")));
repository.save(order);

// Then later:
LoadOrderLineItem item = itemRepository.find(itemId);
item.setQuantity(new Volume(new BigDecimal("999")));  // Violates the rule!
itemRepository.save(item);
```

**The invariant is broken** because there's no single place to enforce it.

### The Aggregate Solution

**Make `LoadOrder` an Aggregate** with `LoadOrderLineItem` as internal parts:

```java
public class LoadOrder {  // Aggregate Root
    private final LoadOrderId id;
    private Customer customer;
    private Product product;
    private Volume requestedQuantity;
    private List<LoadOrderLineItem> lineItems = new ArrayList<>();

    // External code cannot directly modify lineItems
    // Must go through the aggregate root

    public void addLineItem(Product product, Volume quantity, Tank sourceTank) {
        Volume totalAfterAdding = calculateTotal().add(quantity);

        if (totalAfterAdding.isGreaterThan(requestedQuantity)) {
            throw new IllegalStateException(
                "Total line item quantity cannot exceed requested quantity"
            );
        }

        lineItems.add(new LoadOrderLineItem(product, quantity, sourceTank));
    }

    public void removeLineItem(LoadOrderLineItem item) {
        lineItems.remove(item);
    }

    private Volume calculateTotal() {
        return lineItems.stream()
            .map(LoadOrderLineItem::getQuantity)
            .reduce(Volume.ZERO, Volume::add);
    }

    // No public setters for lineItems!
    // All modifications go through controlled methods
}
```

**Key principles:**

1. **Aggregate Root** — `LoadOrder` is the root. External code can only reference the root, not internal parts.
2. **Consistency Boundary** — All invariants are enforced within the aggregate.
3. **Transactional Boundary** — The entire aggregate is saved/loaded as one unit.
4. **Single Entry Point** — Modifications to internal parts must go through the root.

### Aggregate Rules

**1. Reference Only the Aggregate Root**

External code holds references to the root, not internal parts:

```java
// ✅ Good
LoadOrder order = loadOrderRepository.find(orderId);
order.addLineItem(product, quantity, tank);
loadOrderRepository.save(order);  // Saves entire aggregate

// ❌ Bad
LoadOrderLineItem item = lineItemRepository.find(itemId);  // No direct access!
item.setQuantity(newQuantity);  // Bypasses the aggregate root!
```

**2. Keep Aggregates Small**

Include only what needs to be **consistent within a single transaction**:

```java
// ✅ Good: LoadOrder aggregate includes line items
// (they must be consistent with the order total)

public class LoadOrder {
    private List<LoadOrderLineItem> lineItems;  // Inside aggregate
}

// ✅ Good: Customer is a separate aggregate
// (order changes don't require customer changes)

public class LoadOrder {
    private CustomerId customerId;  // Reference by ID, not by object
}
```

**❌ Bad: Including too much**

```java
public class LoadOrder {
    private Customer customer;  // Full object - too much!
    private List<Tank> tanks;  // Full objects - way too much!
    private Terminal terminal;  // Full object - unnecessary!
}
```

**If you include `Customer` as a full object**, every time you modify an order, you'd load the entire customer object, all their orders, payment history, etc. **Aggregate bloat** kills performance.

**3. Reference Other Aggregates by ID**

```java
public class LoadOrder {
    private final LoadOrderId id;
    private final CustomerId customerId;  // ID reference, not object
    private final TankId sourceTankId;    // ID reference, not object

    // To get the actual customer:
    Customer customer = customerRepository.find(this.customerId);
}
```

**4. One Aggregate Per Transaction**

Ideally, a single transaction should modify only one aggregate:

```java
// ✅ Good
@Transactional
public void approveLoadOrder(LoadOrderId orderId) {
    LoadOrder order = repository.find(orderId);
    order.approve();
    repository.save(order);
}

// ⚠️ Careful: Modifying two aggregates in one transaction
@Transactional
public void transferFunds(AccountId from, AccountId to, Money amount) {
    Account fromAccount = accountRepository.find(from);
    Account toAccount = accountRepository.find(to);

    fromAccount.withdraw(amount);
    toAccount.deposit(amount);

    accountRepository.save(fromAccount);
    accountRepository.save(toAccount);
}
```

If modifying multiple aggregates is necessary, consider:
- **Domain Events** (Week 16) for eventual consistency
- **Sagas** for complex multi-aggregate workflows
- Accepting short-term inconsistency

### Designing Aggregates: Petroleum Terminal Example

**Aggregate 1: Tank**
```java
public class Tank {  // Aggregate Root
    private final TankId id;
    private TankNumber number;
    private Capacity capacity;
    private Volume currentLevel;
    private Product currentProduct;

    // Invariant: currentLevel cannot exceed capacity
    public void receive(Volume amount) {
        Volume newLevel = this.currentLevel.add(amount);
        if (newLevel.isGreaterThan(this.capacity.toVolume())) {
            throw new IllegalStateException("Cannot exceed tank capacity");
        }
        this.currentLevel = newLevel;
    }
}
```

**Aggregate 2: LoadingOperation**
```java
public class LoadingOperation {  // Aggregate Root
    private final LoadingOperationId id;
    private final LoadOrderId loadOrderId;  // Reference by ID
    private final BayId bayId;              // Reference by ID
    private final DriverId driverId;        // Reference by ID
    private List<MeterReading> readings = new ArrayList<>();
    private LoadingOperationStatus status;

    // Invariant: Can only add readings when status is IN_PROGRESS
    public void recordMeterReading(Volume reading, Instant timestamp) {
        if (this.status != LoadingOperationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only record readings during loading");
        }
        readings.add(new MeterReading(reading, timestamp));
    }

    // Invariant: Can only complete if readings are present
    public CarrierTicket complete() {
        if (readings.isEmpty()) {
            throw new IllegalStateException("Cannot complete loading without readings");
        }
        this.status = LoadingOperationStatus.COMPLETED;
        return generateCarrierTicket();
    }

    private CarrierTicket generateCarrierTicket() {
        Volume finalVolume = readings.get(readings.size() - 1).getVolume();
        return new CarrierTicket(id, finalVolume, Instant.now());
    }
}
```

**Notice:**
- `MeterReading` is a Value Object **inside** the `LoadingOperation` aggregate
- `LoadOrder`, `Bay`, and `Driver` are **separate aggregates** referenced by ID
- Business rules are enforced by the aggregate root

---

## Putting It All Together: Complete Example

**Healthcare Appointment Scheduling Domain:**

**Entities:**
```java
public class Patient {  // Aggregate Root
    private final PatientId id;
    private PersonName name;
    private ContactInfo contactInfo;
    private List<Appointment> upcomingAppointments;  // Inside aggregate

    public void scheduleAppointment(Doctor doctor, TimeSlot slot) {
        if (hasConflictingAppointment(slot)) {
            throw new IllegalStateException("Patient already has appointment in that time slot");
        }
        upcomingAppointments.add(new Appointment(doctor.getId(), slot));
    }
}

public class Appointment {  // Entity inside Patient aggregate
    private final AppointmentId id;
    private final DoctorId doctorId;  // Reference by ID
    private TimeSlot timeSlot;  // Value Object
    private AppointmentStatus status;
}
```

**Value Objects:**
```java
public class PersonName {
    private final String firstName;
    private final String lastName;

    public PersonName(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName = Objects.requireNonNull(lastName);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}

public class TimeSlot {
    private final LocalDateTime start;
    private final LocalDateTime end;

    public TimeSlot(LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End must be after start");
        }
        this.start = start;
        this.end = end;
    }

    public boolean overlaps(TimeSlot other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }
}
```

---

## Connection to Course 3: From Requirements to Domain Model

In **Week 9-13**, you gathered requirements. Now you translate them into domain models:

**Functional Requirement (from Week 9):**
> "The system shall prevent scheduling a load order if the requested product is not available in sufficient quantity."

**Domain Model Translation:**

```java
public class LoadOrder {  // Aggregate Root
    private CustomerId customerId;
    private Product product;
    private Volume requestedQuantity;

    public static LoadOrder schedule(
        CustomerId customerId,
        Product product,
        Volume requestedQuantity,
        InventoryService inventoryService  // Domain service
    ) {
        Volume available = inventoryService.getAvailableQuantity(product);

        if (requestedQuantity.isGreaterThan(available)) {
            throw new InsufficientInventoryException(
                "Requested " + requestedQuantity + " but only " + available + " available"
            );
        }

        return new LoadOrder(customerId, product, requestedQuantity);
    }
}
```

**Business Rule (from Week 9):**
> "A tank can only accept a product if it is empty or contains the same compatible product."

**Domain Model Translation:**

```java
public class Tank {
    private Volume currentLevel;
    private Product currentProduct;

    public void receive(Volume amount, Product product) {
        if (!canAccept(product)) {
            throw new IncompatibleProductException(
                "Tank contains " + currentProduct + ", cannot accept " + product
            );
        }
        this.currentLevel = currentLevel.add(amount);
        this.currentProduct = product;
    }

    private boolean canAccept(Product product) {
        return this.isEmpty() || this.currentProduct.isCompatibleWith(product);
    }

    private boolean isEmpty() {
        return this.currentLevel.equals(Volume.ZERO);
    }
}
```

**State Machine (from Week 11):**
> LoadOrder: Draft → Submitted → Confirmed → Scheduled → In Progress → Completed

**Domain Model Translation:**

```java
public class LoadOrder {
    private LoadOrderStatus status;

    public void submit() {
        if (status != LoadOrderStatus.DRAFT) {
            throw new IllegalStateException("Can only submit draft orders");
        }
        this.status = LoadOrderStatus.SUBMITTED;
    }

    public void confirm() {
        if (status != LoadOrderStatus.SUBMITTED) {
            throw new IllegalStateException("Can only confirm submitted orders");
        }
        this.status = LoadOrderStatus.CONFIRMED;
    }
}
```

---

## Hands-On Exercise: Building a Domain Model

**Choose the same domain you used in Week 14** (petroleum terminal, hospital, e-commerce, property management).

**Part 1: Identify Entities and Value Objects (60 minutes)**

1. From your Week 14 Ubiquitous Language glossary, classify each term as:
   - **Entity** (has identity, tracks over time, can change)
   - **Value Object** (no identity, immutable, defined by attributes)
   - **Enum** (fixed set of values)
   - **Neither** (might be a service, event, or something else)

2. For **3-5 key Entities**, design the class structure:
   - What is the identity field?
   - What Value Objects does it contain?
   - What other Entities does it reference (by ID)?
   - What is its lifecycle (states)?

3. For **5-7 Value Objects**, design the class structure:
   - What are the attributes?
   - What validation rules apply?
   - What domain operations does it support?

**Deliverable format:**

```java
// Entity Example
public class [EntityName] {
    private final [EntityId] id;  // Identity
    private [ValueObject] attribute;
    private [OtherEntityId] otherEntityId;  // Reference by ID

    // Key methods that enforce invariants
}

// Value Object Example
public class [ValueObjectName] {
    private final [Type] attribute;

    // Constructor with validation
    // Domain operations
    // equals/hashCode based on attributes
}
```

**Part 2: Design Aggregates (45 minutes)**

1. Identify **3-4 Aggregates** in your domain:
   - Which Entity is the Aggregate Root?
   - What Entities/Value Objects are **inside** the aggregate?
   - What Entities are **outside** (referenced by ID)?
   - What invariants must the aggregate enforce?

2. For each aggregate, write:
   - The aggregate boundary (what's in, what's out)
   - 2-3 invariants that must be maintained
   - Method signatures for operations that enforce those invariants

**Deliverable format:**

```markdown
## Aggregate: [AggregateName]

**Aggregate Root:** [EntityName]

**Inside the Aggregate:**
- [EntityOrValueObject]: [Why it's included]
- [EntityOrValueObject]: [Why it's included]

**Outside the Aggregate (referenced by ID):**
- [Entity]: [Why it's external]

**Invariants:**
1. [Invariant description]
2. [Invariant description]

**Key Operations:**
```java
public void operationName() {
    // Enforces invariant #1
}
```

**Part 3: Implement One Complete Aggregate (90 minutes)**

Choose your most important aggregate and implement it fully in Java:

1. Define all Value Objects it uses
2. Define the Aggregate Root Entity
3. Implement methods that enforce invariants
4. Write unit tests for the invariants

**Deliverable:** Working Java code with tests.

---

## Reflection Questions

1. **Look at code you've written in the past.** Did you use primitives (`String`, `BigDecimal`) where Value Objects would have been better? What bugs or complexity would Value Objects have prevented?

2. **Think about entities without identity.** Have you ever seen a system where entities that should have had identity didn't? What problems did that cause?

3. **Aggregate size is a trade-off.** Too small → can't enforce invariants. Too large → performance problems. How do you decide where to draw the boundary?

4. **In your operational experience, what invariants were critical?** (Example: "A tank can't be overfilled," "A load can't start without a driver signature"). How would you enforce those as aggregates?

5. **Value Objects make code more expressive.** Compare `Volume` vs `BigDecimal`, `Address` vs four separate `String` fields. Which is easier to understand and maintain?

---

## Key Takeaways

✅ **Entities have identity** — Defined by who they are, not what they contain. Can change over time while remaining the same entity.

✅ **Value Objects have no identity** — Defined entirely by attributes. Immutable and replaceable. Two with same attributes are identical.

✅ **Aggregates enforce invariants** — Cluster of entities/value objects with a consistency boundary. External code references only the root.

✅ **Keep aggregates small** — Include only what must be consistent in a single transaction. Reference other aggregates by ID.

✅ **Value Objects provide type safety** — Prevent bugs by making invalid states unrepresentable (`Volume` can't be negative if constructor validates).

✅ **Domain models encode business rules** — Requirements from Course 3 become methods on entities and aggregates that enforce rules.

✅ **Use the Ubiquitous Language** — Class names, method names, and field names come directly from your Week 14 glossary.

---

## Connection to Week 16

This week you learned the **building blocks** of domain models: Entities, Value Objects, and Aggregates.

Next week you'll learn how these building blocks **communicate changes**: **Domain Events & Commands**.

You'll learn:
- How aggregates emit events when important things happen
- How to design event-driven architectures
- How to integrate bounded contexts using events
- How to maintain consistency across aggregates

**Get ready to make your domain models reactive and decoupled.**

---

## Additional Resources

**Books:**
- *Domain-Driven Design* by Eric Evans — Chapters on Entities, Value Objects, and Aggregates
- *Implementing Domain-Driven Design* by Vaughn Vernon — Especially Chapter 5 (Entities), 6 (Value Objects), 10 (Aggregates)

**Articles:**
- Martin Fowler: "Value Object" (martinfowler.com/bliki/ValueObject.html)
- Vaughn Vernon: "Effective Aggregate Design" (three-part series)

**For Java/Spring:**
- Spring Data JPA supports aggregates well (use `@Embedded` for Value Objects, `@OneToMany(cascade = ALL)` for aggregate parts)
- Consider using `@Embeddable` for Value Objects in JPA entities

---

**End of Week 15 — Entities, Value Objects & Aggregates**

**Next:** Week 16 — Domain Events & Commands
