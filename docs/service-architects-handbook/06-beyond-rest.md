# Chapter 6: Beyond REST: GraphQL, gRPC, Webhooks

## Introduction: When REST Isn't the Right Tool

Bibby's current search flow looks like this from the CLI:

```java
// BookCommands.java - searchByTitle()
BookEntity bookEntity = bookService.findBookByTitle(title);

if(bookEntity.getShelfId() != null){
    Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
    Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
    // Display all the information
}
```

This works in a monolith. But imagine if Bibby becomes microservices, and you're building a mobile app that needs to display book search results. With REST, you'd need:

```
Mobile App → API Gateway:
  1. GET /books?title=Sapiens → {bookId: 42, title, shelfId: 7, ...}
  2. GET /shelves/7 → {shelfLabel: "A1", bookcaseId: 3}
  3. GET /bookcases/3 → {bookcaseLabel: "Main"}

Total: 3 round trips from mobile (slow on 3G/4G)
```

And what if the mobile app only wants the book title and shelf location, not all the other metadata? With REST, you get everything or nothing.

**This is where alternative protocols shine.** In this chapter, I'll teach you when and why to use GraphQL, gRPC, Server-Sent Events, and Webhooks by solving real problems in Bibby's domain.

## When GraphQL Solves Real Problems

**GraphQL** is a query language for APIs. Clients specify exactly what data they want, and the server returns only that data.

### The Problem GraphQL Solves

REST has two problems:
1. **Over-fetching**: You get fields you don't need
2. **Under-fetching**: You need multiple requests to get all the data

**Example from Bibby**: Mobile app showing book search results

**With REST**:
```json
GET /books/42

{
  "bookId": 42,
  "title": "Clean Code",
  "isbn": "978-0132350884",  // ← Don't need this
  "publisher": "Prentice Hall",  // ← Don't need this
  "publicationYear": 2008,  // ← Don't need this
  "genre": "Programming",  // ← Don't need this
  "edition": 1,  // ← Don't need this
  "description": "...",  // ← Don't need this (500 chars!)
  "checkoutCount": 5,  // ← Don't need this
  "bookStatus": "AVAILABLE",  // ← Need this
  "shelfId": 7  // ← Need to make another request to get shelf label
}

GET /shelves/7
{
  "shelfId": 7,
  "shelfLabel": "A1",  // ← Need this
  "bookcaseId": 3,  // ← Need to make another request
  "shelfPosition": 2  // ← Don't need this
}

GET /bookcases/3
{
  "bookcaseId": 3,
  "bookcaseLabel": "Main",  // ← Need this
  "shelfCapacity": 10  // ← Don't need this
}
```

**Result**: 3 HTTP requests, transferred ~800 bytes, used only ~100 bytes.

**With GraphQL**:
```graphql
query {
  book(id: 42) {
    title
    bookStatus
    shelf {
      shelfLabel
      bookcase {
        bookcaseLabel
      }
    }
  }
}
```

**Response**:
```json
{
  "data": {
    "book": {
      "title": "Clean Code",
      "bookStatus": "AVAILABLE",
      "shelf": {
        "shelfLabel": "A1",
        "bookcase": {
          "bookcaseLabel": "Main"
        }
      }
    }
  }
}
```

**Result**: 1 HTTP request, transferred ~120 bytes, got exactly what you needed.

### Implementing GraphQL for Bibby

**1. Define GraphQL Schema**:

```graphql
type Book {
  bookId: ID!
  title: String!
  isbn: String
  authors: [Author!]!
  shelf: Shelf
  bookStatus: BookStatus!
  checkoutCount: Int
  publicationYear: Int
}

type Author {
  authorId: ID!
  firstName: String!
  lastName: String!
  fullName: String!
  books: [Book!]!
}

type Shelf {
  shelfId: ID!
  shelfLabel: String!
  shelfPosition: Int
  bookcase: Bookcase!
  books: [Book!]!
}

type Bookcase {
  bookcaseId: ID!
  bookcaseLabel: String!
  shelfCapacity: Int
  shelves: [Shelf!]!
}

enum BookStatus {
  AVAILABLE
  CHECKED_OUT
  RESERVED
  LOST
  ARCHIVED
}

type Query {
  book(id: ID!): Book
  books(title: String, status: BookStatus, shelfId: ID): [Book!]!
  shelf(id: ID!): Shelf
  shelves(bookcaseId: ID): [Shelf!]!
  bookcase(id: ID!): Bookcase
  bookcases: [Bookcase!]!
}

type Mutation {
  createBook(input: CreateBookInput!): Book!
  checkoutBook(bookId: ID!): Checkout!
}

input CreateBookInput {
  title: String!
  isbn: String
  authors: [CreateAuthorInput!]!
}

input CreateAuthorInput {
  firstName: String!
  lastName: String!
}
```

**2. Implement Resolvers (Spring Boot + GraphQL Java)**:

```java
@Controller
public class BookGraphQLController {

    @Autowired
    private BookService bookService;

    @Autowired
    private ShelfService shelfService;

    @QueryMapping
    public BookEntity book(@Argument Long id) {
        return bookService.findBookById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    @QueryMapping
    public List<BookEntity> books(
        @Argument String title,
        @Argument BookStatus status,
        @Argument Long shelfId
    ) {
        // Filter logic
        if (title != null) {
            return bookService.findByTitle(title);
        }
        if (shelfId != null) {
            return bookService.findByShelfId(shelfId);
        }
        return bookService.findAll();
    }

    // Field resolver for nested shelf
    @SchemaMapping(typeName = "Book", field = "shelf")
    public ShelfEntity shelf(BookEntity book) {
        if (book.getShelfId() == null) {
            return null;
        }
        return shelfService.findShelfById(book.getShelfId()).orElse(null);
    }

    @MutationMapping
    public BookEntity createBook(@Argument CreateBookInput input) {
        return bookService.createBook(input);
    }
}
```

**3. Client Usage**:

```javascript
// Mobile app can request exactly what it needs
const SEARCH_BOOKS = gql`
  query SearchBooks($title: String!) {
    books(title: $title) {
      bookId
      title
      bookStatus
      shelf {
        shelfLabel
        bookcase {
          bookcaseLabel
        }
      }
    }
  }
`;

// Different screen, different data needs
const BOOK_DETAIL = gql`
  query BookDetail($id: ID!) {
    book(id: $id) {
      bookId
      title
      isbn
      publicationYear
      description
      authors {
        fullName
      }
      checkoutCount
    }
  }
`;
```

### When GraphQL Is the Right Choice

**✅ Use GraphQL when**:
- Mobile/web clients with different data needs
- Complex, nested data structures (Book → Authors → Books)
- Clients need flexible querying (show only available books on shelf A1)
- Rapid frontend iteration (add fields without backend changes)
- You want to minimize round trips

**❌ Don't use GraphQL when**:
- Simple CRUD operations (REST is simpler)
- Service-to-service communication (use gRPC instead)
- Real-time streaming (use WebSockets/SSE)
- File uploads (GraphQL is clunky for this)
- Team doesn't understand it (training cost > benefits)

### GraphQL Gotchas

**1. The N+1 Problem**

```graphql
query {
  books {  # Returns 100 books
    title
    authors {  # For each book, query authors
      fullName  # → 100 database queries!
    }
  }
}
```

**Solution**: DataLoader (batching + caching)

```java
@Bean
public DataLoader<Long, List<AuthorEntity>> authorLoader() {
    return DataLoader.newMappedDataLoader((Set<Long> bookIds) -> {
        // Batch load authors for all books in one query
        return authorRepository.findByBookIds(bookIds);
    });
}
```

**2. Query Complexity Attacks**

Malicious client:
```graphql
query {
  books {
    shelf {
      bookcase {
        shelves {
          books {
            shelf {
              bookcase {
                # ... infinitely nested!
              }
            }
          }
        }
      }
    }
  }
}
```

**Solution**: Query depth limiting, complexity analysis

```java
@Configuration
public class GraphQLConfig {
    @Bean
    public GraphQLServlet graphQLServlet() {
        return GraphQLServlet.builder()
            .maximumQueryDepth(10)  // ← Prevent deep nesting
            .maximumQueryComplexity(500)  // ← Complexity budget
            .build();
    }
}
```

## gRPC and Protocol Buffers

**gRPC** is a high-performance RPC framework using Protocol Buffers (protobuf) for serialization.

### When to Use gRPC

**gRPC is for service-to-service communication**, not client-facing APIs.

**Scenario**: If Bibby splits into microservices, internal calls should use gRPC, not REST.

```
┌─────────────────┐      gRPC       ┌──────────────────┐
│  Catalog        │ ──────────────▶ │  Library         │
│  Service        │  (fast, typed)  │  Service         │
└─────────────────┘                  └──────────────────┘
       │                                      │
       │ REST/GraphQL                         │
       ▼                                      ▼
  (Mobile App)                           (Admin UI)
```

### Defining Bibby Services with Protobuf

**1. Define service in `.proto` file**:

```protobuf
syntax = "proto3";

package bibby.library;

service LibraryService {
  rpc GetBook (GetBookRequest) returns (BookResponse);
  rpc SearchBooks (SearchBooksRequest) returns (SearchBooksResponse);
  rpc CreateBook (CreateBookRequest) returns (BookResponse);
  rpc CheckoutBook (CheckoutRequest) returns (CheckoutResponse);
}

message GetBookRequest {
  int64 book_id = 1;
}

message BookResponse {
  int64 book_id = 1;
  string title = 2;
  string isbn = 3;
  repeated Author authors = 4;
  BookStatus status = 5;
  int64 shelf_id = 6;
}

message Author {
  int64 author_id = 1;
  string first_name = 2;
  string last_name = 3;
  string full_name = 4;
}

enum BookStatus {
  BOOK_STATUS_UNSPECIFIED = 0;
  AVAILABLE = 1;
  CHECKED_OUT = 2;
  RESERVED = 3;
  LOST = 4;
  ARCHIVED = 5;
}

message SearchBooksRequest {
  optional string title = 1;
  optional BookStatus status = 2;
  optional int64 shelf_id = 3;
}

message SearchBooksResponse {
  repeated BookResponse books = 1;
}

message CreateBookRequest {
  string title = 1;
  string isbn = 2;
  repeated CreateAuthorRequest authors = 3;
}

message CreateAuthorRequest {
  string first_name = 1;
  string last_name = 2;
}

message CheckoutRequest {
  int64 book_id = 1;
  int64 user_id = 2;
}

message CheckoutResponse {
  int64 checkout_id = 1;
  int64 book_id = 2;
  string due_date = 3;
}
```

**2. Generate Java code** (happens automatically with Maven/Gradle plugin):

```xml
<plugin>
  <groupId>org.xolstice.maven.plugins</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <configuration>
    <protocArtifact>com.google.protobuf:protoc:3.21.7:exe:${os.detected.classifier}</protocArtifact>
    <pluginId>grpc-java</pluginId>
    <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.50.2:exe:${os.detected.classifier}</pluginArtifact>
  </configuration>
</plugin>
```

**3. Implement server**:

```java
@GrpcService
public class LibraryGrpcService extends LibraryServiceGrpc.LibraryServiceImplBase {

    @Autowired
    private BookService bookService;

    @Override
    public void getBook(GetBookRequest request, StreamObserver<BookResponse> responseObserver) {
        Long bookId = request.getBookId();

        BookEntity book = bookService.findBookById(bookId)
            .orElseThrow(() -> Status.NOT_FOUND
                .withDescription("Book with ID " + bookId + " not found")
                .asRuntimeException());

        BookResponse response = mapToProto(book);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void searchBooks(SearchBooksRequest request, StreamObserver<SearchBooksResponse> responseObserver) {
        List<BookEntity> books = bookService.findBooks(
            request.hasTitle() ? request.getTitle() : null,
            request.hasStatus() ? mapFromProto(request.getStatus()) : null,
            request.hasShelfId() ? request.getShelfId() : null
        );

        SearchBooksResponse response = SearchBooksResponse.newBuilder()
            .addAllBooks(books.stream().map(this::mapToProto).collect(Collectors.toList()))
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private BookResponse mapToProto(BookEntity entity) {
        BookResponse.Builder builder = BookResponse.newBuilder()
            .setBookId(entity.getBookId())
            .setTitle(entity.getTitle());

        if (entity.getIsbn() != null) {
            builder.setIsbn(entity.getIsbn());
        }

        if (entity.getShelfId() != null) {
            builder.setShelfId(entity.getShelfId());
        }

        // Map authors
        entity.getAuthors().forEach(author ->
            builder.addAuthors(Author.newBuilder()
                .setAuthorId(author.getAuthorId())
                .setFirstName(author.getFirstName())
                .setLastName(author.getLastName())
                .setFullName(author.getFullName())
                .build())
        );

        return builder.build();
    }
}
```

**4. Implement client** (Catalog Service calling Library Service):

```java
@Service
public class CatalogService {

    private final LibraryServiceBlockingStub libraryClient;

    public CatalogService(ManagedChannel channel) {
        this.libraryClient = LibraryServiceGrpc.newBlockingStub(channel);
    }

    public BookInfo getBookInfo(Long bookId) {
        GetBookRequest request = GetBookRequest.newBuilder()
            .setBookId(bookId)
            .build();

        BookResponse response = libraryClient.getBook(request);

        return new BookInfo(response.getTitle(), response.getStatus());
    }
}
```

### gRPC vs REST: The Tradeoffs

| **gRPC** | **REST** |
|----------|----------|
| Binary (protobuf) — 3-10x smaller | Text (JSON) — human-readable |
| HTTP/2 — multiplexing, streaming | HTTP/1.1 — simple |
| Strongly typed contracts | Loosely typed (JSON Schema optional) |
| Code generation (clients auto-generated) | Manual client code or OpenAPI gen |
| Excellent for service-to-service | Excellent for public APIs |
| Poor browser support | Universal browser support |
| Harder to debug (binary) | Easy to debug (curl, browser) |
| Faster (2-5x) | Slower but "fast enough" |

**Rule of thumb**:
- **Internal microservices**: gRPC
- **Public/partner APIs**: REST or GraphQL
- **Mobile apps**: REST or GraphQL (gRPC-Web exists but niche)

## Streaming APIs: Server-Sent Events & WebSockets

Bibby's CLI is currently pull-based. What if we wanted real-time updates?

### Use Case: Real-Time Book Availability

**Scenario**: Library dashboard shows real-time book checkout status.

**With REST** (polling):
```javascript
// Client polls every 5 seconds
setInterval(() => {
  fetch('/books/42')
    .then(r => r.json())
    .then(book => updateUI(book.status));
}, 5000);

// Problems:
// - Wastes bandwidth (99% of the time nothing changed)
// - 5 second delay before seeing updates
// - Scales poorly (1000 clients = 200 req/sec)
```

**With Server-Sent Events** (push):
```java
// Server pushes updates when book status changes
@GetMapping(value = "/books/{id}/status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<BookStatusEvent> streamBookStatus(@PathVariable Long id) {
    return bookStatusPublisher
        .filter(event -> event.getBookId().equals(id))
        .map(event -> new BookStatusEvent(id, event.getStatus()));
}
```

**Client**:
```javascript
const eventSource = new EventSource('/books/42/status-stream');

eventSource.onmessage = (event) => {
  const data = JSON.parse(event.data);
  updateUI(data.status);  // Updates instantly when status changes
};
```

### Server-Sent Events (SSE) vs WebSockets

| **Server-Sent Events** | **WebSockets** |
|------------------------|----------------|
| Unidirectional (server → client) | Bidirectional (server ↔ client) |
| HTTP/1.1 compatible | Requires upgrade to WS protocol |
| Auto-reconnect built-in | Manual reconnect logic |
| Simple (EventSource API) | More complex |
| Text-based | Binary or text |
| Good for: dashboards, notifications | Good for: chat, gaming, collaboration |

**For Bibby's use cases, SSE is usually enough.**

### Implementing SSE in Bibby

**1. Event publisher**:

```java
@Service
public class BookEventPublisher {

    private final Sinks.Many<BookStatusEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void publishStatusChange(Long bookId, BookStatus newStatus) {
        BookStatusEvent event = new BookStatusEvent(bookId, newStatus, LocalDateTime.now());
        sink.tryEmitNext(event);
    }

    public Flux<BookStatusEvent> subscribe() {
        return sink.asFlux();
    }
}
```

**2. Service layer publishes events**:

```java
@Service
public class BookService {

    @Autowired
    private BookEventPublisher eventPublisher;

    public void checkOutBook(Long bookId) {
        BookEntity book = findBookById(bookId).orElseThrow();
        book.setBookStatus(BookStatus.CHECKED_OUT.toString());
        bookRepository.save(book);

        // Publish event for real-time subscribers
        eventPublisher.publishStatusChange(bookId, BookStatus.CHECKED_OUT);
    }
}
```

**3. SSE endpoint**:

```java
@RestController
@RequestMapping("/api/v2/events")
public class BookEventController {

    @Autowired
    private BookEventPublisher eventPublisher;

    @GetMapping(value = "/books/status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<BookStatusEvent>> streamBookEvents() {
        return eventPublisher.subscribe()
            .map(event -> ServerSentEvent.<BookStatusEvent>builder()
                .id(String.valueOf(event.getBookId()))
                .event("book-status-changed")
                .data(event)
                .build());
    }

    // Stream events for specific book
    @GetMapping(value = "/books/{id}/status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<BookStatusEvent>> streamBookStatus(@PathVariable Long id) {
        return eventPublisher.subscribe()
            .filter(event -> event.getBookId().equals(id))
            .map(event -> ServerSentEvent.<BookStatusEvent>builder()
                .event("status-changed")
                .data(event)
                .build());
    }
}
```

**4. Client usage**:

```javascript
// Listen to all book status changes
const allEvents = new EventSource('/api/v2/events/books/status');
allEvents.addEventListener('book-status-changed', (e) => {
  const {bookId, status} = JSON.parse(e.data);
  console.log(`Book ${bookId} is now ${status}`);
});

// Listen to specific book
const bookEvents = new EventSource('/api/v2/events/books/42/status');
bookEvents.addEventListener('status-changed', (e) => {
  const {status} = JSON.parse(e.data);
  updateDashboard(status);
});
```

## Webhooks: Event-Driven Callbacks

**Webhooks** are "reverse APIs" — your service calls the client's API when something happens.

### Use Case: External Library System Integration

**Scenario**: Bibby integrates with a public library system. When a book is checked out from Bibby, notify the public library.

**Without webhooks** (polling):
```
Public Library System polls Bibby every minute:
GET /api/checkouts?since=2025-11-18T10:00:00

// Wasteful, slow, scales poorly
```

**With webhooks**:
```
Bibby calls Public Library when checkout happens:
POST https://public-library.com/webhooks/checkout
{
  "bookId": 42,
  "isbn": "978-0132350884",
  "checkedOutAt": "2025-11-18T10:15:00",
  "dueDate": "2025-12-02"
}
```

### Implementing Webhooks in Bibby

**1. Store webhook subscriptions**:

```java
@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscription {
    @Id
    @GeneratedValue
    private Long id;

    private String url;  // Callback URL
    private String event;  // "book.checked_out", "book.checked_in"
    private String secret;  // For HMAC signature verification
    private boolean active;
}
```

**2. Webhook delivery service**:

```java
@Service
public class WebhookService {

    @Autowired
    private WebhookSubscriptionRepository subscriptionRepo;

    @Autowired
    private RestTemplate restTemplate;

    public void triggerWebhook(String event, Object payload) {
        List<WebhookSubscription> subscriptions = subscriptionRepo
            .findByEventAndActive(event, true);

        for (WebhookSubscription sub : subscriptions) {
            deliverWebhook(sub, payload);
        }
    }

    private void deliverWebhook(WebhookSubscription subscription, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            String signature = generateSignature(json, subscription.getSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Bibby-Event", subscription.getEvent());
            headers.set("X-Bibby-Signature", signature);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(json, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                subscription.getUrl(),
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Webhook delivered to {}", subscription.getUrl());
            }
        } catch (Exception e) {
            logger.error("Failed to deliver webhook", e);
            // Retry logic, dead letter queue, etc.
        }
    }

    private String generateSignature(String payload, String secret) {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }
}
```

**3. Trigger webhooks from service layer**:

```java
@Service
public class CheckoutService {

    @Autowired
    private WebhookService webhookService;

    public Checkout checkoutBook(Long bookId, Long userId) {
        // Create checkout
        Checkout checkout = new Checkout(bookId, userId);
        checkoutRepository.save(checkout);

        // Trigger webhook
        webhookService.triggerWebhook("book.checked_out", new CheckoutWebhookPayload(
            checkout.getCheckoutId(),
            bookId,
            userId,
            checkout.getDueDate()
        ));

        return checkout;
    }
}
```

**4. Recipient server (Public Library)**:

```java
@PostMapping("/webhooks/checkout")
public ResponseEntity<Void> handleBibbyCheckout(
    @RequestBody CheckoutWebhookPayload payload,
    @RequestHeader("X-Bibby-Signature") String signature
) {
    // Verify signature
    if (!verifySignature(payload, signature)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Process checkout
    publicLibraryService.recordExternalCheckout(payload.getIsbn());

    return ResponseEntity.ok().build();
}
```

### Webhook Best Practices

**1. Retry logic**:
```java
@Retry(name = "webhookRetry", fallbackMethod = "webhookFallback")
public void deliverWebhook(WebhookSubscription sub, Object payload) {
    // Try up to 3 times with exponential backoff
}
```

**2. Dead letter queue**:
```java
public void webhookFallback(WebhookSubscription sub, Object payload, Exception e) {
    // Move to dead letter queue for manual review
    deadLetterQueue.add(new FailedWebhook(sub, payload, e));
}
```

**3. Idempotency**:
```java
headers.set("X-Bibby-Delivery-Id", UUID.randomUUID().toString());
// Recipient can deduplicate using this ID
```

## Protocol Decision Matrix

Here's when to use each protocol in Bibby:

| **Use Case** | **Protocol** | **Why** |
|--------------|--------------|---------|
| Mobile app API | REST or GraphQL | Universal support, flexible |
| Admin dashboard | GraphQL | Complex nested queries |
| Service-to-service | gRPC | Fast, typed, efficient |
| Real-time availability | SSE | Server → client push |
| Chat/collaboration | WebSockets | Bidirectional |
| External integrations | Webhooks | Event-driven, no polling |
| File uploads | REST (multipart) | Standard, well-supported |
| Bulk data export | REST (streaming) | Simple, works everywhere |

## Action Items

Before moving to Chapter 7, implement one alternative protocol:

### 1. Add GraphQL to Bibby

Set up Spring GraphQL and create a query for books:

```graphql
query {
  books(title: "Clean") {
    bookId
    title
    authors {
      fullName
    }
    shelf {
      shelfLabel
    }
  }
}
```

### 2. Design gRPC Service

Write a `.proto` file for Bibby's Library Service with GetBook, SearchBooks, and CheckoutBook operations.

### 3. Implement SSE for Book Events

Create an endpoint that streams book status changes in real-time. Test with EventSource in a browser.

### 4. Build Webhook System

Implement webhook subscriptions for "book.checked_out" events. Create a test endpoint to receive webhooks.

### 5. Compare Performance

Measure:
- REST: GET /books/42 + GET /shelves/7 + GET /bookcases/3
- GraphQL: Single query with nested data
- gRPC: GetBook call with embedded shelf info

Which is faster? Which uses less bandwidth?

## Key Takeaways

1. **REST isn't always the answer** — Different protocols solve different problems

2. **GraphQL for flexible client queries** — Mobile apps with varied data needs

3. **gRPC for service-to-service** — Fast, typed, efficient internal communication

4. **SSE for server-push updates** — Dashboards, notifications, real-time status

5. **WebSockets for bidirectional** — Chat, gaming, collaboration tools

6. **Webhooks for event-driven integration** — No polling, instant notifications

7. **Protocol choice affects architecture** — Don't use gRPC for public APIs

8. **Combine protocols strategically** — Public REST API + internal gRPC + SSE for updates

9. **GraphQL has complexity costs** — N+1 queries, query complexity, caching challenges

10. **Measure before deciding** — "GraphQL is faster" depends on your use case

## Further Reading

### GraphQL
- **GraphQL Specification** — graphql.org/learn
- **"Production Ready GraphQL"** by Marc-André Giroux
- **Apollo GraphQL Documentation** — apollographql.com/docs

### gRPC
- **gRPC Documentation** — grpc.io
- **"Practical gRPC"** by Joshua B. Humphries
- **Protocol Buffers Guide** — developers.google.com/protocol-buffers

### Real-Time
- **Server-Sent Events Specification** — W3C standard
- **WebSocket RFC 6455**
- **"Real-Time Web Application Development"** by Rami Sayar

---

## What's Next?

In **Chapter 7: API Security & Authentication**, we'll secure Bibby's APIs:
- OAuth2 flows and when to use each
- OpenID Connect (OIDC) for identity
- JWTs and their footguns
- API keys, basic auth, and their place
- Rate limiting and throttling strategies
- Zero-trust architecture patterns

**Remember**: You've learned how to communicate between services. Next, you'll learn how to secure those communications.

Protocols are tools. Security is mandatory. Let's learn how to protect Bibby's data.
