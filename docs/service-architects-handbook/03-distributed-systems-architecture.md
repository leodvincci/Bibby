# Chapter 3: Architecture of Distributed Systems

## Introduction: When Your Function Call Becomes a Network Call

Let me show you a piece of code from Bibby that looks innocent in a monolith:

```java
// BookCommands.java - searchByTitle()
BookEntity bookEntity = bookService.findBookByTitle(title);

if(bookEntity.getShelfId() != null){
    Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
    Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
    System.out.println("Book Was Found \nBookcase: " + bookcaseEntity.get().getBookcaseLabel() +
                       "\nShelf: " + shelfEntity.get().getShelfLabel());
}
```

**In a monolith, this takes about 5-10 milliseconds.** Three method calls, all in-process, probably one database query that joins the tables.

Now imagine we split Bibby into microservices:
- **Library Service** (owns books)
- **Shelf Service** (owns shelves)
- **Bookcase Service** (owns bookcases)

That same operation becomes:

```
User searches for "Sapiens"
  ↓
[1] HTTP GET → Library Service: /books/search?title=Sapiens  (10ms network + 5ms processing)
  ↓
[2] HTTP GET → Shelf Service: /shelves/42                    (10ms network + 5ms processing)
  ↓
[3] HTTP GET → Bookcase Service: /bookcases/7                (10ms network + 5ms processing)
  ↓
Total: ~45ms (9x slower than monolith)
```

**And that's if everything works perfectly.** What if:
- Shelf Service is down? (your search fails completely)
- Bookcase Service times out? (do you show partial results?)
- The book was moved to a different shelf between calls 1 and 2? (stale data)
- Network is congested? (latency spikes to 200ms)

**This is distributed systems in a nutshell**: You're trading the simplicity of in-process calls for the complexity of network boundaries, and you must design for failure as the default state.

In this chapter, I'll teach you the fundamental challenges of distributed systems using Bibby as our example. You'll learn why network boundaries change everything, how to reason about consistency and availability, and why "it works on my laptop" doesn't mean it works in production.

## Network Boundaries as First-Class Concerns

In Chapter 1, I mentioned that network calls are 100,000x slower than function calls. Let me break down what that actually means in practice.

### The Latency Hierarchy

```
L1 cache reference:              0.5 ns
Branch mispredict:               5   ns
L2 cache reference:              7   ns
Mutex lock/unlock:              25   ns
Main memory reference:          100  ns
Send 1K bytes over 1 Gbps:   10,000  ns  (10 µs)
Read 4K from SSD:           150,000  ns  (150 µs)
Round trip in same datacenter: 500,000 ns (500 µs)
Read 1 MB from SSD:       1,000,000  ns  (1 ms)
Disk seek:               10,000,000  ns  (10 ms)
Round trip CA to Netherlands: 150,000,000 ns (150 ms)
```

**The critical jump**: In-process method calls operate in nanoseconds. Network calls operate in milliseconds. That's **6 orders of magnitude difference**.

### Bibby's Search Flow: Monolith vs Microservices

Let's compare the same operation in both architectures.

**Monolith (current Bibby)**:

```java
// All in-process, single database query
BookEntity book = bookService.findBookByTitle("Sapiens");      // ~2ms (DB query)
ShelfEntity shelf = shelfService.findShelfById(book.getShelfId());    // ~1ms (in-memory, already loaded)
BookcaseEntity bookcase = bookcaseService.findBookCaseById(shelf.getBookcaseId());  // ~1ms
// Total: ~4ms
```

Actually, the current implementation uses a native query that joins everything:

```java
@Query(value = """
    SELECT b.book_id, b.title,
           STRING_AGG(CONCAT(a.first_name, ' ', a.last_name), ', ') as authors,
           bc.bookcase_label, s.shelf_label, b.book_status
    FROM books b
    JOIN book_authors ba ON b.book_id = ba.book_id
    JOIN authors a ON ba.author_id = a.author_id
    JOIN shelves s ON s.shelf_id = b.shelf_id
    JOIN bookcases bc ON bc.bookcase_id = s.bookcase_id
    WHERE b.book_id = :bookId
    GROUP BY b.book_id, b.title, bc.bookcase_label, s.shelf_label, b.book_status
""", nativeQuery = true)
BookDetailView getBookDetailView(Long bookId);
```

**This is ONE database round trip, probably 3-5ms total.**

**Microservices (if we split Bibby)**:

```
Client calls Catalog Service
  ↓
Catalog Service → Library Service: GET /books/search?title=Sapiens
  • Network latency: 10ms
  • Library Service processes: 5ms
  • Library Service DB query: 3ms
  • Response back: 10ms
  = 28ms

Catalog Service → Shelf Service: GET /shelves/42
  • Network latency: 10ms
  • Shelf Service processes: 2ms
  • Shelf Service DB query: 2ms
  • Response back: 10ms
  = 24ms

Catalog Service → Bookcase Service: GET /bookcases/7
  • Network latency: 10ms
  • Bookcase Service processes: 2ms
  • Response back: 10ms
  = 22ms

Total: 74ms (15-20x slower!)
```

**And this assumes**:
- All services are healthy
- Network is fast (same datacenter)
- No timeouts, retries, or failures
- Services respond immediately (no queuing)

### Why This Matters

**Latency compounds in distributed systems.** If your checkout flow requires 5 service calls, and each adds 20ms, that's 100ms of pure network overhead before any real work happens.

**Real-world impact**:
- Amazon found that 100ms of latency costs 1% of sales
- Google found that 500ms slower = 20% drop in traffic
- Users perceive >100ms as "slow"

**The lesson**: Every network boundary you add multiplies your latency. Design accordingly.

## Distributed Failure as the Default State

Here's the mental shift you need to make: **In distributed systems, failure is not an exception — it's the default state you design for.**

### The Bibby Checkout Flow

Let's look at the current checkout implementation:

```java
// BookCommands.java
public void checkOutBook(){
    String bookTitle = /* get from user input */;
    BookEntity book = bookService.findBookByTitle(bookTitle);

    if (book.getBookStatus().equals("CHECKED_OUT")){
        System.out.println("This one's already off the shelf.");
    } else {
        bookService.checkOutBook(book);  // Updates status to CHECKED_OUT
        System.out.println("All set — " + bookTitle + " is checked out.");
    }
}
```

**In a monolith, this either works or it doesn't.** If the database is down, the whole app is down. If the method succeeds, the book is checked out. Atomicity is guaranteed by the transaction.

Now imagine Bibby as microservices:

```
┌─────────────────┐       ┌──────────────────┐      ┌─────────────────┐
│ CLI / Frontend  │       │ Circulation      │      │    Library      │
│                 │       │    Service       │      │    Service      │
└────────┬────────┘       └────────┬─────────┘      └────────┬────────┘
         │                         │                         │
         │  1. Check out "Sapiens" │                         │
         │────────────────────────>│                         │
         │                         │                         │
         │                         │  2. Get book details    │
         │                         │────────────────────────>│
         │                         │                         │
         │                         │  3. Book data           │
         │                         │<────────────────────────│
         │                         │                         │
         │                         │  4. Update book status  │
         │                         │────────────────────────>│
         │                         │        ❌ TIMEOUT       │
         │                         │                         │
         │  ⁉️ What happens now?   │                         │
         │<────────────────────────│                         │
```

**Failure scenarios**:

1. **Network timeout between Circulation and Library**
   - Did the book status update? You don't know.
   - Should you retry? (might double-update)
   - Should you tell the user it failed? (maybe it actually succeeded)

2. **Library Service crashed after receiving request**
   - The request might have been processed
   - Or it might have died mid-transaction
   - You have no idea which

3. **Library Service is slow (5 seconds to respond)**
   - Do you wait? (user sees "loading..." forever)
   - Do you time out? (maybe it completes after you give up)
   - Do you show partial success? ("Checkout in progress...")

4. **Response lost on the way back**
   - Library Service successfully updated the book
   - Network dropped the HTTP 200 response
   - Circulation Service thinks it failed
   - User tries again → "This book is already checked out"

**In a distributed system, you must handle ALL of these.** There's no atomic transaction across services. Network failures are frequent and unpredictable.

### Partial Failures: The Worst Kind

Here's the nightmare scenario. User searches for a book:

```
User → Catalog Service → Library Service (✅ responds: book found)
                      → Shelf Service (✅ responds: shelf "A1")
                      → Bookcase Service (❌ timeout: no response)
```

**What do you show the user?**

Option 1: Fail the entire request
```
"Sorry, search is unavailable. Try again later."
```
- ❌ User is frustrated
- ❌ You HAD the book data!

Option 2: Show partial results
```
"Sapiens - Found on Shelf A1 (bookcase information unavailable)"
```
- ✅ User gets some value
- ❌ Incomplete data might confuse
- ❌ What if bookcase label is critical?

Option 3: Return cached/stale data
```
"Sapiens - Last known location: Bookcase 'Main', Shelf A1"
```
- ✅ User gets full information
- ❌ Might be wrong (book could have moved)

**There's no right answer.** The best choice depends on your domain. For search? Partial results are probably fine. For checkout? You need certainty.

### The Eight Fallacies of Distributed Computing

Peter Deutsch and James Gosling (Sun Microsystems) identified assumptions programmers incorrectly make:

1. **The network is reliable** → It's not. Packets get lost, routers fail.
2. **Latency is zero** → It's measured in milliseconds, not nanoseconds.
3. **Bandwidth is infinite** → You can't send gigabytes between services every second.
4. **The network is secure** → Services can be compromised, traffic intercepted.
5. **Topology doesn't change** → Services move, IPs change, load balancers get added.
6. **There is one administrator** → You don't control the whole network.
7. **Transport cost is zero** → Serialization, network transmission, deserialization all cost CPU and time.
8. **The network is homogeneous** → You'll have different protocols, encodings, latencies.

**Let's apply these to Bibby's microservices**:

If we split Bibby, the search flow violates **all eight fallacies**:

1. Library Service might not respond (network unreliable)
2. Each service call adds 10-50ms latency
3. Returning full book objects with authors, metadata → large payloads
4. Book data might be sensitive (need authentication/authorization)
5. Shelf Service might move to a different server
6. You don't control the datacenter network
7. Serializing BookEntity → JSON → bytes costs CPU
8. Catalog might use HTTP/REST, Library might use gRPC

## CAP Theorem and Practical Implications

CAP theorem states: **In a distributed system, you can only have 2 of 3: Consistency, Availability, Partition tolerance.**

Let me translate that using Bibby.

### The Three Properties

**Consistency (C)**: Every read receives the most recent write.
- If you update a book's status to "CHECKED_OUT", every service sees that immediately.

**Availability (A)**: Every request receives a response (success or failure).
- Even if some nodes are down, the system responds.

**Partition Tolerance (P)**: The system continues operating despite network partitions.
- If Library Service can't talk to Catalog Service, both keep working.

### CAP in Bibby: The Checkout Scenario

Imagine Bibby split into:
- **Library Service** (East Coast datacenter)
- **Catalog Service** (West Coast datacenter)

A network partition occurs — they can't communicate.

**Scenario**: User checks out "Sapiens" via Library Service.

```
         Network Partition
              ╳╳╳╳╳
┌──────────────┐   ╳╳╳   ┌──────────────┐
│   Library    │   ╳╳╳   │   Catalog    │
│   Service    │   ╳╳╳   │   Service    │
│              │   ╳╳╳   │              │
│ "Sapiens":   │   ╳╳╳   │ "Sapiens":   │
│ CHECKED_OUT  │   ╳╳╳   │ AVAILABLE    │
└──────────────┘   ╳╳╳   └──────────────┘
```

**CP (Consistency + Partition tolerance)**: Sacrifice Availability

```java
// Library Service
public void checkOutBook(Long bookId) {
    // Try to notify Catalog Service
    try {
        catalogService.updateBookStatus(bookId, "CHECKED_OUT");
    } catch (NetworkException e) {
        // Partition detected! Refuse to proceed.
        throw new ServiceUnavailableException(
            "Cannot guarantee consistency with Catalog. Checkout blocked."
        );
    }
    // Only proceed if Catalog confirmed update
    bookRepository.updateStatus(bookId, "CHECKED_OUT");
}
```

**Result**: Checkout fails. User can't check out the book until the partition heals.
- ✅ Consistency maintained (both services agree, or neither updates)
- ❌ Availability sacrificed (system rejects requests during partition)

**AP (Availability + Partition tolerance)**: Sacrifice Consistency

```java
// Library Service
public void checkOutBook(Long bookId) {
    // Update locally regardless of Catalog Service
    bookRepository.updateStatus(bookId, "CHECKED_OUT");

    // Try to notify Catalog, but don't block
    try {
        catalogService.updateBookStatus(bookId, "CHECKED_OUT");
    } catch (NetworkException e) {
        // Log for later reconciliation
        logger.warn("Failed to notify Catalog. Will sync later.");
        eventQueue.publish(new BookCheckedOutEvent(bookId));
    }

    return new CheckoutResponse("Success");
}
```

**Result**: Checkout succeeds. Catalog shows stale data until partition heals.
- ✅ Availability maintained (checkout works despite partition)
- ❌ Consistency sacrificed (services disagree temporarily)

### Practical CAP Decisions

**For Bibby, which should you choose?**

**Catalog/Search → AP** (Eventual Consistency)
- It's okay if search results are 1 second stale
- Users don't care if a book shows "AVAILABLE" for a moment after checkout
- Availability matters more (searches must always work)

**Checkout → CP** (Strong Consistency)
- Cannot check out a book that's already out
- Better to fail the request than create inconsistency
- Consistency matters more (correctness over availability)

**The principle**: **Choose consistency vs availability per use case, not globally.**

## Consistency Models: Strong, Eventual, and Everything In Between

When you split Bibby into services, you need to decide: How consistent does data need to be?

### Strong Consistency

**Definition**: All services see the same data at the same time. Writes are immediately visible everywhere.

**Example in Bibby**: Checking out a book

```java
// This must be strongly consistent
@Transactional
public void checkOutBook(Long bookId) {
    BookEntity book = bookRepository.findById(bookId);

    if (book.getStatus() == BookStatus.CHECKED_OUT) {
        throw new AlreadyCheckedOutException();
    }

    book.setStatus(BookStatus.CHECKED_OUT);
    book.incrementCheckoutCount();
    bookRepository.save(book);
}
```

In a monolith, the database transaction guarantees strong consistency. In microservices, you need distributed transactions (2-phase commit, Sagas) — which we'll cover in Chapter 9.

**Cost**: Slower (must wait for all nodes to agree), less available (can't update during partitions).

### Eventual Consistency

**Definition**: Given enough time with no new updates, all services will eventually see the same data. Temporary inconsistency is acceptable.

**Example in Bibby**: Search index

```java
// Library Service
public void updateBook(BookEntity book) {
    bookRepository.save(book);
    eventBus.publish(new BookUpdatedEvent(book));  // Fire and forget
}

// Catalog Service (separate process)
@EventListener
public void onBookUpdated(BookUpdatedEvent event) {
    searchIndex.updateBook(event.getBookId(), event.getTitle());
    // Might process this 100ms later — that's fine for search
}
```

User updates a book title:
- **T=0ms**: Library Service saves "Clean Code"
- **T=50ms**: Event reaches Catalog Service
- **T=100ms**: Search index updated

**For 100ms, search shows the old title.** That's eventual consistency.

**Cost**: More complex reasoning ("is this data fresh?"), potential user confusion.

### Causal Consistency

**Definition**: Operations that are causally related are seen in the same order by all nodes.

**Example in Bibby**: Book shelving

```
Event 1: Book created
Event 2: Book assigned to Shelf A1 (depends on Event 1)
Event 3: Shelf A1 renamed to A2

All services must see these in order: 1 → 2 → 3
```

If Catalog Service sees Event 3 before Event 2, it will show the book on a shelf that doesn't exist yet!

**Solution**: Version vectors, logical clocks (Lamport timestamps).

### Read-Your-Own-Writes Consistency

**Definition**: After you write data, your subsequent reads always see that write.

**Example in Bibby**: User adds a book

```
User → Library Service: POST /books {title: "Sapiens"}
       ✅ Response: 201 Created

User → Catalog Service: GET /search?title=Sapiens
       ❌ Response: [] (search index not updated yet!)
```

**User experience**: "I just added 'Sapiens' but search says it doesn't exist!"

**Solution**:
1. Read from Library Service immediately after write
2. Pass a version token: "show me data at least as fresh as version X"
3. Sticky sessions (route user to same replica that has their write)

### Session Consistency

**Definition**: Within a single user session, consistency guarantees hold (e.g., read-your-own-writes, monotonic reads).

**Example in Bibby**: Browse flow

```java
// User browses: Bookcase → Shelf → Books
// Session 1: User sees Shelf A1 has 10 books
// Session 2: (30 seconds later) User selects Shelf A1, should see ≥10 books
```

If book count goes **backwards** within a session, that's confusing!

**Solution**: Pin user to same service replica for the session.

## Network Partitions in Practice

Let's walk through a real partition scenario using Bibby's checkout flow.

### The Scenario

```
User is in California, using Bibby CLI.

Bibby's architecture:
- Library Service: us-east-1 (Virginia)
- Circulation Service: us-west-1 (California)
- Network link between regions is severed (construction accident cuts fiber)

User tries to check out "Sapiens"
```

**What happens?**

```
California
  ↓
┌─────────────────────┐
│ Circulation Service │  ← Can reach this (same region)
│   (us-west-1)       │
└──────────┬──────────┘
           │
           │ GET /books/sapiens
           ↓
        ╔══════╗ Network Partition
        ║ ╳╳╳╳ ║
        ╚══════╝
           ↓
┌──────────────────────┐
│  Library Service     │  ← Cannot reach this!
│   (us-east-1)        │
└──────────────────────┘
```

**Option 1: Fail Fast** (CP approach)

```java
// Circulation Service
public CheckoutResponse checkout(String userId, String bookTitle) {
    try {
        BookDto book = libraryServiceClient.getBook(bookTitle, timeout=2000ms);
    } catch (TimeoutException e) {
        throw new ServiceUnavailableException(
            "Library Service unavailable. Cannot complete checkout."
        );
    }

    // If we get here, Library responded
    // ... rest of checkout logic
}
```

**User sees**: "Checkout unavailable. Please try again later."

**Pros**: No data corruption, users know there's a problem
**Cons**: Feature is completely down during partition

**Option 2: Degrade Gracefully** (AP approach)

```java
// Circulation Service with local cache
public CheckoutResponse checkout(String userId, String bookTitle) {
    BookDto book;

    try {
        book = libraryServiceClient.getBook(bookTitle, timeout=2000ms);
    } catch (TimeoutException e) {
        // Fall back to cached data
        book = cacheService.getCachedBook(bookTitle);

        if (book == null) {
            throw new ServiceUnavailableException("Book not found in cache");
        }

        logger.warn("Using cached book data due to partition. Age: " + cacheAge(book));
    }

    // Proceed with checkout using cached data
    // Queue event for reconciliation when partition heals
    checkoutEventQueue.enqueue(new PendingCheckout(userId, book.getId()));

    return new CheckoutResponse("Success (pending confirmation)");
}
```

**User sees**: "Book checked out. Confirmation pending."

**Pros**: Feature remains available during partition
**Cons**: Might check out a book that's actually unavailable (cached data is stale)

### Split Brain Scenario

**Worse case**: Both sides of the partition accept writes.

```
Virginia (Library Service)          California (Circulation Service)
User A checks out "Sapiens"         User B checks out "Sapiens"
  ↓                                   ↓
BookStatus = CHECKED_OUT            BookStatus = CHECKED_OUT
(in Virginia DB)                    (in California DB)

           ╳╳╳╳╳ Partition ╳╳╳╳╳

Both think they succeeded!
```

**When partition heals**: Conflict! Two users checked out the same book.

**Resolution strategies**:

1. **Last-Write-Wins** (simple, wrong)
   ```java
   // Whoever wrote last overwrites the other
   if (timestampA > timestampB) {
       finalStatus = statusFromA;
   } else {
       finalStatus = statusFromB;
   }
   ```
   **Problem**: One user's checkout gets silently lost.

2. **Application-Level Merge** (complex, correct)
   ```java
   // Detect conflict, apply business rules
   if (conflict detected) {
       // In Bibby's domain: first checkout wins, second gets rejected
       winner = earlierCheckout;
       loser = laterCheckout;

       notifyUser(loser.userId, "Book was already checked out. Reverting your transaction.");
       compensatingTransaction(loser);
   }
   ```

3. **Vector Clocks** (track causality)
   ```java
   // Each write includes a version vector
   Virginia: checkout(book, version=[VA:5, CA:3])
   California: checkout(book, version=[VA:3, CA:6])

   // On merge: versions diverged (5≠3, 3≠6) → conflict detected
   ```

## Latency as a Cost: The Bibby Browse Flow

Let's analyze Bibby's current browse flow and show how latency compounds in distributed systems.

### Current Monolith Flow

```java
// BookcaseCommands.java - browse flow
1. User selects Bookcase → bookcaseService.getAllBookcases()      // ~2ms
2. User selects Shelf → shelfService.getShelfSummariesForBookcase() // ~3ms
3. User selects Book → bookService.getBooksForShelf()              // ~3ms

Total: ~8ms (perceived as instant)
```

### Microservices Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│   CLI    │────▶│ Catalog  │────▶│ Bookcase │────▶│  Shelf   │
│          │     │ Service  │     │ Service  │     │ Service  │
└──────────┘     └──────────┘     └──────────┘     └──────────┘

Step 1: Get bookcases
  CLI → Catalog Service: 10ms network
  Catalog → Bookcase Service: 10ms network + 5ms processing
  Response back: 10ms network
  Total: 35ms

Step 2: Get shelves for selected bookcase
  CLI → Catalog Service: 10ms
  Catalog → Shelf Service: 10ms + 5ms processing
  Response back: 10ms
  Total: 35ms

Step 3: Get books for selected shelf
  CLI → Catalog Service: 10ms
  Catalog → Book Service: 10ms + 5ms processing
  Response back: 10ms
  Total: 35ms

Grand Total: 105ms (13x slower)
```

**And this assumes sequential calls.** The user is waiting 105ms for what used to take 8ms.

### Optimization: Parallel Fetching

**Instead of**:
```
1. Get bookcase (35ms)
2. Wait for response...
3. Get shelf (35ms)
4. Wait for response...
5. Get book (35ms)
```

**Do this** (when possible):
```
1. Get bookcase (35ms) ──┐
2. Get shelf (35ms)     ─┤─── All at once
3. Get book (35ms)      ─┘

Total: 35ms (still 4x slower, but better than 13x)
```

**Problem**: You can't parallelize sequential dependencies. You need the bookcase ID to fetch shelves!

### Caching Strategy

```java
// Catalog Service caches bookcase/shelf structure
@Cacheable(value = "bookcases", ttl = 300000) // 5 minutes
public List<BookcaseDto> getAllBookcases() {
    return bookcaseServiceClient.getAll();
}

// User's first browse: 35ms (cache miss)
// User's second browse: 1ms (cache hit)
```

**Trade-off**: Stale data vs speed. If a bookcase is added, users won't see it for 5 minutes.

## Action Items

Before moving to Chapter 4, complete these exercises:

### 1. Measure Bibby's Current Performance

Add timing to three operations:
- `bookService.findBookByTitle()`
- `shelfService.findShelfById()`
- The full `searchByTitle()` flow

Record:
- Best case (warm cache, fast DB)
- Average case (typical load)
- Worst case (cold cache, slow DB)

This is your **baseline**. If you went to microservices, expect 5-10x slowdown.

### 2. Design for Partition Tolerance

For Bibby's checkout operation, design **both** strategies:

**Strategy A (CP)**: Fail if Library Service is unreachable
```java
// Your implementation
```

**Strategy B (AP)**: Use cached data and queue for reconciliation
```java
// Your implementation
```

Which would you choose for Bibby? Why?

### 3. Identify Consistency Requirements

For each Bibby operation, classify consistency needs:

| Operation | Consistency Level | Justification |
|-----------|------------------|---------------|
| Search books | Eventual | Stale results by 1s is fine |
| Check out book | Strong | Can't double-checkout |
| Browse shelves | ??? | ??? |
| Assign shelf | ??? | ??? |
| Update book metadata | ??? | ??? |

### 4. Calculate Latency Budget

If Bibby were microservices, and you have a 100ms total latency budget for search:
- How many service calls can you afford?
- Which services would you combine to stay under budget?
- Where would you add caching?

### 5. Design a Retry Strategy

For the call `Library Service → Shelf Service`, design a retry policy:
```java
public ShelfDto getShelf(Long shelfId) {
    // Your retry logic here
    // Consider:
    // - How many retries?
    // - How long to wait between retries?
    // - Exponential backoff?
    // - When to give up?
}
```

## Key Takeaways

1. **Network calls are 100,000x slower than function calls** — Every service boundary adds 10-50ms latency

2. **Distributed failure is the default state** — Design for timeouts, retries, and partial failures first

3. **CAP theorem forces trade-offs** — Choose consistency OR availability during partitions (you can't have both)

4. **Consistency models are a spectrum** — Strong (slow, correct) → Eventual (fast, eventually correct)

5. **Partial failures are the hardest problem** — When some services respond and others don't, there's no obvious "right" answer

6. **Latency compounds across service calls** — 3 services × 20ms each = 60ms of pure network overhead

7. **Partitions will happen** — Cables get cut, datacenters fail, networks congest

8. **Fallacies of distributed computing are real** — The network is unreliable, has latency, and will change

9. **Monoliths are FAST** — Bibby's search is 8ms. Microservices version would be 50-100ms.

10. **Measure first, distribute second** — Know your current performance before adding network boundaries

## Further Reading

### Essential Papers
- **"Fallacies of Distributed Computing"** by L. Peter Deutsch (1994)
- **"CAP Twelve Years Later: How the 'Rules' Have Changed"** by Eric Brewer (2012)
- **"Harvest, Yield, and Scalable Tolerant Systems"** by Fox & Brewer (1999)

### Books
- **"Designing Data-Intensive Applications"** by Martin Kleppmann — Best book on distributed systems concepts
- **"Database Internals"** by Alex Petrov — Understanding consistency and replication
- **"Distributed Systems"** by Maarten van Steen & Andrew Tanenbaum — Comprehensive textbook

### Real-World Case Studies
- **AWS S3 Outage (2017)** — Lessons in availability vs consistency
- **GitHub MySQL Failover (2018)** — When consistency checks fail
- **Google Chubby** — Building consistent distributed locks

---

## What's Next?

In **Chapter 4: REST Fundamentals**, we'll design the actual HTTP APIs that would connect Bibby's services. You'll learn:
- How to model resources (Book, Shelf, Bookcase as REST entities)
- HTTP semantics that matter (idempotency, safety, status codes)
- Pagination, filtering, sorting for large collections
- How to design APIs that minimize round trips
- Common REST anti-patterns to avoid

**Remember**: You've learned WHY distributed systems are hard. Next, you'll learn HOW to communicate across them correctly using REST APIs.

Now you understand the cost of network boundaries. Let's learn to use them wisely.
