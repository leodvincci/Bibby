# SECTION 11: STRUCTURAL PATTERNS - ADAPTER & FACADE

## Making Incompatible Systems Work Together

Welcome to Section 11, where we begin **Structural Patterns** - patterns that deal with object composition and relationships between entities.

Today's patterns: **Adapter** and **Facade**. Both help you work with external systems and complex subsystems, but in different ways. These are among the most practical patterns you'll use regularly.

---

## Adapter Pattern

### Intent

**Convert the interface of a class into another interface clients expect. Adapter lets classes work together that couldn't otherwise because of incompatible interfaces.**

### The Real-World Analogy

Think of a power adapter when traveling internationally. Your American laptop (client) expects American outlets (interface), but you're in Europe (incompatible interface). The adapter (pattern!) converts the European outlet to work with your laptop.

```
Your Laptop ──> Adapter ──> European Outlet
  (Client)    (Adapter)     (Adaptee)
```

### Classic Implementation

```java
// Target interface (what client expects)
public interface MediaPlayer {
    void play(String filename);
}

// Adaptee (incompatible interface)
public class Mp3Player {
    public void playMp3(String filename) {
        System.out.println("Playing MP3: " + filename);
    }
}

// Adapter (makes Adaptee compatible with Target)
public class Mp3Adapter implements MediaPlayer {
    private final Mp3Player mp3Player;

    public Mp3Adapter(Mp3Player mp3Player) {
        this.mp3Player = mp3Player;
    }

    @Override
    public void play(String filename) {
        // Adapt the interface
        mp3Player.playMp3(filename);
    }
}

// Client code
MediaPlayer player = new Mp3Adapter(new Mp3Player());
player.play("song.mp3");  // Works!
```

---

## Adapter in Bibby: External ISBN Lookup

### The Problem

You want to fetch book metadata from an external ISBN lookup API, but their interface doesn't match your domain:

```java
// External API (you don't control this)
public class OpenLibraryIsbnApi {
    public JsonResponse lookupIsbn(String isbn) {
        // Returns their JSON format
        return httpClient.get("https://openlibrary.org/isbn/" + isbn);
    }

    public static class JsonResponse {
        public String title;
        public String[] author_name;  // Snake case!
        public String publish_year;   // String, not int!
        public Map<String, Object> rawData;
    }
}

// Your domain model
public interface IsbnMetadataService {
    BookMetadata fetchMetadata(ISBN isbn);  // Value object, not String!
}

public class BookMetadata {
    private final Title title;
    private final Set<AuthorName> authors;
    private final PublicationYear year;
}
```

### The Solution: Adapter

```java
// infrastructure/external/isbn/OpenLibraryAdapter.java
@Service
public class OpenLibraryAdapter implements IsbnMetadataService {

    private final OpenLibraryIsbnApi externalApi;

    public OpenLibraryAdapter(OpenLibraryIsbnApi externalApi) {
        this.externalApi = externalApi;
    }

    @Override
    public BookMetadata fetchMetadata(ISBN isbn) {
        // Call external API with adapted parameter
        JsonResponse response = externalApi.lookupIsbn(isbn.getValue());

        // Translate to domain model
        return translateToDomain(response);
    }

    private BookMetadata translateToDomain(JsonResponse response) {
        // Convert title
        Title title = new Title(response.title);

        // Convert authors (array -> Set<AuthorName>)
        Set<AuthorName> authors = Arrays.stream(response.author_name)
            .map(this::parseAuthorName)
            .collect(Collectors.toSet());

        // Convert year (String -> PublicationYear)
        PublicationYear year = new PublicationYear(
            Integer.parseInt(response.publish_year)
        );

        return new BookMetadata(title, authors, year);
    }

    private AuthorName parseAuthorName(String fullName) {
        // Split "Eric Evans" -> AuthorName("Eric", "Evans")
        String[] parts = fullName.split(" ", 2);
        return new AuthorName(
            parts.length > 0 ? parts[0] : "",
            parts.length > 1 ? parts[1] : ""
        );
    }
}

// Usage in application service
@Service
public class AddBookFromIsbnService {

    private final IsbnMetadataService metadataService;  // Adapter injected!
    private final BookRepository bookRepository;

    public BookId addBookByIsbn(ISBN isbn) {
        // Fetch metadata (adapter handles translation)
        BookMetadata metadata = metadataService.fetchMetadata(isbn);

        // Create book from metadata
        Book book = Book.builder()
            .id(BookId.generate())
            .title(metadata.getTitle())
            .isbn(isbn)
            .authors(metadata.getAuthorIds())
            .publicationYear(metadata.getYear())
            .build();

        bookRepository.save(book);
        return book.getId();
    }
}
```

**Benefits**:
- Domain layer doesn't know about external API
- External API changes don't affect domain
- Easy to swap ISBN providers (Google Books, Amazon, etc.)
- Testable (mock the IsbnMetadataService)

---

## Adapter as Anti-Corruption Layer (DDD Connection)

Remember from Section 8: **Anti-Corruption Layer** is a DDD pattern. Adapter IS the implementation!

```
┌──────────────────────────────┐
│      Your Domain Model       │
│   (Clean, domain-centric)    │
└──────────┬───────────────────┘
           │
           │ depends on
           ▼
┌──────────────────────────────┐
│   IsbnMetadataService        │  ← Interface (Port)
│   (Domain language)          │
└──────────▲───────────────────┘
           │
           │ implements
           │
┌──────────────────────────────┐
│   OpenLibraryAdapter         │  ← Adapter (Anti-Corruption Layer)
│   (Translates)               │
└──────────┬───────────────────┘
           │
           │ uses
           ▼
┌──────────────────────────────┐
│   OpenLibraryIsbnApi         │  ← External system
│   (Their messy format)       │
└──────────────────────────────┘
```

The adapter *protects* (anti-corrupts) your domain from the external API's structure.

---

## Multiple Adapters for One Interface

You can have multiple adapters for different providers:

```java
// Domain interface (port)
public interface IsbnMetadataService {
    BookMetadata fetchMetadata(ISBN isbn);
}

// Adapter 1: Open Library
@Service
@ConditionalOnProperty(name = "isbn.provider", havingValue = "openlibrary")
public class OpenLibraryAdapter implements IsbnMetadataService {
    // Implementation using Open Library API
}

// Adapter 2: Google Books
@Service
@ConditionalOnProperty(name = "isbn.provider", havingValue = "googlebooks")
public class GoogleBooksAdapter implements IsbnMetadataService {
    // Implementation using Google Books API
}

// Adapter 3: Mock (for testing)
@Service
@Profile("test")
public class MockIsbnAdapter implements IsbnMetadataService {
    @Override
    public BookMetadata fetchMetadata(ISBN isbn) {
        return new BookMetadata(
            new Title("Test Book"),
            Set.of(new AuthorName("Test", "Author")),
            new PublicationYear(2024)
        );
    }
}
```

Switch providers via configuration:
```properties
# application.properties
isbn.provider=openlibrary
```

**This is the power of Adapter + Dependency Inversion!**

---

## Facade Pattern

### Intent

**Provide a unified interface to a set of interfaces in a subsystem. Facade defines a higher-level interface that makes the subsystem easier to use.**

### The Real-World Analogy

A restaurant facade: You (client) don't interact directly with the kitchen, dishwasher, inventory system, etc. You talk to the waiter (facade), who coordinates everything for you.

```
You ──> Waiter ──> Kitchen
              ├──> Dishwasher
              ├──> Inventory
              └──> Billing
```

### Classic Implementation

```java
// Complex subsystem
class CPU {
    void freeze() { }
    void jump(long position) { }
    void execute() { }
}

class Memory {
    void load(long position, byte[] data) { }
}

class HardDrive {
    byte[] read(long lba, int size) { }
}

// Facade - simplifies subsystem
class ComputerFacade {
    private final CPU cpu;
    private final Memory memory;
    private final HardDrive hardDrive;

    public ComputerFacade() {
        this.cpu = new CPU();
        this.memory = new Memory();
        this.hardDrive = new HardDrive();
    }

    // Simple interface hiding complexity
    public void start() {
        cpu.freeze();
        memory.load(BOOT_ADDRESS, hardDrive.read(BOOT_SECTOR, SECTOR_SIZE));
        cpu.jump(BOOT_ADDRESS);
        cpu.execute();
    }
}

// Client code - simple!
ComputerFacade computer = new ComputerFacade();
computer.start();  // All complexity hidden
```

---

## Facade in Bibby: Library Management Facade

### The Problem

Your CLI needs to perform complex operations involving multiple services:

```java
// Complex workflow without facade
@ShellCommand
public class BookCommands {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final ShelfRepository shelfRepository;
    private final BookcaseRepository bookcaseRepository;
    private final IsbnMetadataService isbnService;
    private final ValidationService validationService;
    private final EventPublisher eventPublisher;

    public String addBookByIsbn(String isbn) {
        // Step 1: Validate ISBN
        if (!validationService.isValidIsbn(isbn)) {
            return "Invalid ISBN";
        }

        // Step 2: Check for duplicates
        if (bookRepository.existsByIsbn(ISBN.fromString(isbn))) {
            return "Book already exists";
        }

        // Step 3: Fetch metadata
        BookMetadata metadata = isbnService.fetchMetadata(ISBN.fromString(isbn));

        // Step 4: Find or create authors
        Set<AuthorId> authorIds = metadata.getAuthors().stream()
            .map(name -> {
                Optional<Author> existing = authorRepository.findByName(name);
                if (existing.isPresent()) {
                    return existing.get().getId();
                } else {
                    Author newAuthor = new Author(AuthorId.generate(), name);
                    authorRepository.save(newAuthor);
                    return newAuthor.getId();
                }
            })
            .collect(Collectors.toSet());

        // Step 5: Create book
        Book book = Book.builder()
            .id(BookId.generate())
            .title(metadata.getTitle())
            .isbn(ISBN.fromString(isbn))
            .authors(authorIds)
            .build();

        // Step 6: Save book
        bookRepository.save(book);

        // Step 7: Publish event
        eventPublisher.publish(new BookAddedEvent(book.getId()));

        return "Book added successfully";
    }
}
```

**This is too complex for a CLI command!**

### The Solution: Facade

```java
// application/facade/LibraryManagementFacade.java
@Service
public class LibraryManagementFacade {

    private final AddBookFromIsbnService addBookService;
    private final CheckOutBookService checkOutService;
    private final SearchBooksService searchService;
    private final OrganizeBooksService organizeService;

    // Simplified interface for complex operations
    public BookOperationResult addBookByIsbn(String isbn) {
        try {
            ISBN isbnValue = ISBN.fromString(isbn);
            BookId bookId = addBookService.execute(isbnValue);
            return BookOperationResult.success("Book added", bookId);
        } catch (InvalidIsbnException e) {
            return BookOperationResult.error("Invalid ISBN: " + e.getMessage());
        } catch (DuplicateBookException e) {
            return BookOperationResult.error("Book already exists");
        }
    }

    public BookOperationResult checkOutBook(String title) {
        try {
            checkOutService.execute(new Title(title));
            return BookOperationResult.success("Book checked out");
        } catch (BookNotFoundException e) {
            return BookOperationResult.error("Book not found: " + title);
        } catch (BookNotAvailableException e) {
            return BookOperationResult.error("Book not available: " + e.getReason());
        }
    }

    public List<BookSearchResult> searchBooks(String query) {
        return searchService.search(query);
    }

    public OrganizationResult organizeBooksByGenre() {
        return organizeService.organizeByGenre();
    }
}

// Now CLI is simple!
@ShellComponent
public class BookCommands {

    private final LibraryManagementFacade library;  // Just one dependency!

    @ShellMethod("Add book by ISBN")
    public String addBook(@ShellOption String isbn) {
        BookOperationResult result = library.addBookByIsbn(isbn);
        return result.getMessage();
    }

    @ShellMethod("Check out book")
    public String checkOut(@ShellOption String title) {
        BookOperationResult result = library.checkOutBook(title);
        return result.getMessage();
    }
}
```

**Benefits**:
- CLI code is simple and readable
- Complex logic hidden in facade
- Easy to test (mock facade)
- Clear separation of concerns

---

## Adapter vs. Facade: When to Use Each

### Use Adapter When:
- Integrating with external system
- Interface incompatibility
- You want to protect domain from external changes
- Need to swap implementations (Strategy pattern + Adapter)

### Use Facade When:
- Complex subsystem with many components
- Want to provide simplified interface
- Reduce coupling between client and subsystem
- Create convenient API for common operations

### Key Difference

**Adapter**: Makes *one* incompatible interface compatible
**Facade**: Simplifies *many* interfaces into one convenient interface

---

## Combining Patterns

You can use both together:

```java
// Facade that uses Adapters internally
@Service
public class BookMetadataFacade {

    private final IsbnMetadataService isbnAdapter;  // Adapter
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;

    // Simplified method
    public EnrichedBookMetadata getCompleteMetadata(ISBN isbn) {
        // Use adapter to fetch from external API
        BookMetadata metadata = isbnAdapter.fetchMetadata(isbn);

        // Enrich with local data
        Set<Author> authors = resolveAuthors(metadata.getAuthors());
        Publisher publisher = resolvePublisher(metadata.getPublisher());

        // Return enriched result
        return new EnrichedBookMetadata(metadata, authors, publisher);
    }

    private Set<Author> resolveAuthors(Set<AuthorName> names) {
        // Complex logic to find/create authors
    }

    private Publisher resolvePublisher(String publisherName) {
        // Complex logic to find/create publisher
    }
}
```

---

## Action Items

### 1. Create ISBN Lookup Adapter (4-5 hours)

Implement adapter for external ISBN API:
- Define `IsbnMetadataService` interface in domain
- Create `OpenLibraryAdapter` in infrastructure
- Handle translation from external format to domain
- Add error handling
- Test with mock API

### 2. Build Library Management Facade (3-4 hours)

Create facade for CLI:
- Identify complex operations in current CLI commands
- Extract to application services
- Create facade that coordinates services
- Simplify CLI commands to use facade
- Compare before/after readability

### 3. Identify Existing Adapters (1-2 hours)

Review your code:
- Where do you integrate with Spring Data JPA?
- Where do you call external APIs?
- Are these properly adapted to domain?

Document improvements needed.

### 4. Create Mock Adapters for Testing (2-3 hours)

Build test adapters:
- `MockIsbnAdapter` that returns test data
- Use `@Profile("test")` to activate in tests
- Write tests using mock adapters
- Compare to testing with real API

### 5. Refactor One Complex Method (2-3 hours)

Find a complex method with many dependencies:
- Create facade to simplify it
- Move complexity into facade
- Reduce dependencies in original method
- Measure improvement (lines of code, complexity)

---

## Key Takeaways

### 1. Adapter = Interface Translation
- Makes incompatible interfaces work together
- Critical for external integrations
- Implements Anti-Corruption Layer from DDD

### 2. Facade = Simplification
- Hides complex subsystems
- Provides convenient, high-level interface
- Reduces coupling between client and subsystem

### 3. Both Promote Loose Coupling
- Clients depend on interfaces, not implementations
- Easy to swap implementations
- Testable with mocks

### 4. Adapter Protects Domain
- External changes don't affect domain
- Domain stays clean and focused
- Translation happens at boundary

### 5. Facade Improves Usability
- Complex systems become simple to use
- Common operations are easy
- Advanced operations still possible (bypass facade)

---

## Further Study

**Books**:
- "Design Patterns" by Gang of Four - Adapter (139), Facade (185)
- "Patterns of Enterprise Application Architecture" by Martin Fowler
- "Clean Architecture" by Robert Martin - Chapter on boundaries

**Real-World Examples**:
- Spring's `JdbcTemplate` (facade over JDBC)
- Spring's `RestTemplate` (facade + adapter)
- Your JPA repositories (adapter from Spring Data to domain)

---

## Mentor's Note

Adapter and Facade are patterns you'll use constantly. Unlike Singleton (which you should avoid), these are bread-and-butter patterns for professional development.

**Real-world usage**:
- Every external API integration → Adapter
- Every complex subsystem → Facade
- Every anti-corruption layer → Adapter
- Every convenience API → Facade

The patterns are so natural you might already be using them without knowing the names. That's fine! The value is in recognizing the pattern and applying it consciously.

**Pro tip**: When you see yourself writing a class with 10+ dependencies, that's a signal you might need a Facade. When you're translating data formats, that's a signal you need an Adapter.

In Section 12, we'll cover Decorator and Proxy - patterns for adding behavior dynamically and controlling access.

---

**Section 11 Complete** | Next: Section 12 - Structural Patterns: Decorator & Proxy
