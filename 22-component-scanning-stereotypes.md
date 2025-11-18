# Section 22: Component Scanning & Stereotypes
## How Spring Boot Finds Your Beans

**Files:** `BibbyApplication.java`, package structure across 10 packages
**Concept:** Component scanning, stereotype annotations, @SpringBootApplication
**Time:** 45 min read

---

## What You'll Learn

By the end of this section, you'll understand:

- What component scanning is and how it works
- How `@SpringBootApplication` automatically enables component scanning
- The five stereotype annotations (`@Component`, `@Service`, `@Repository`, `@RestController`, `@Configuration`)
- **CRITICAL:** All stereotypes are the same annotation under the hood
- Bibby's package structure (10 packages, 41 Java files)
- How Spring scans `com.penrose.bibby.**` to find all beans
- What `@CommandScan` does for Spring Shell commands
- When beans won't be found (outside scan path)
- How to customize component scanning (include/exclude patterns)

Every concept is explained using **Bibby's actual package structure and annotations**.

---

## The Problem: How Does Spring Know What to Create?

In Section 21, you learned Spring creates beans automatically via dependency injection. But **how does Spring know which classes to create?**

```java
@Service
public class BookService { }  // How does Spring find this?

@RestController
public class BookController { }  // And this?

@Component
public class StartupRunner { }  // And this?
```

**Answer: Component Scanning**

Spring **scans your packages** at startup, looking for annotated classes.

---

## Component Scanning: The Automatic Bean Discovery

**Component scanning** is Spring's process of:

1. **Scanning** packages for classes with stereotype annotations
2. **Registering** those classes as bean definitions
3. **Creating** bean instances from those definitions
4. **Wiring** dependencies between beans

**It's automatic.** You don't configure anything explicitly. Just annotate your class, and Spring finds it.

---

## Bibby's Entry Point: @SpringBootApplication

**BibbyApplication.java:1-14**
```java
package com.penrose.bibby;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.command.annotation.CommandScan;

@CommandScan
@SpringBootApplication
public class BibbyApplication {
    public static void main(String[] args) {
        SpringApplication.run(BibbyApplication.class, args);
    }
}
```

**Key observations:**

1. **Package:** `com.penrose.bibby` (root package)
2. **@SpringBootApplication** (line 8)
3. **@CommandScan** (line 7) for Spring Shell commands

---

## @SpringBootApplication: The Three-in-One Annotation

`@SpringBootApplication` is a **meta-annotation** - it combines three annotations:

```java
@SpringBootApplication
// ↓ Expands to:
@SpringBootConfiguration   // Marks this as a configuration class
@EnableAutoConfiguration   // Enables Spring Boot's auto-configuration
@ComponentScan              // ← ENABLES COMPONENT SCANNING!
```

**The `@ComponentScan` part is what finds your beans.**

---

## Default Component Scan Behavior

**Rule:** `@ComponentScan` scans the **package where @SpringBootApplication is located** and **all sub-packages**.

**For Bibby:**

`@SpringBootApplication` is in `com.penrose.bibby`

**Scanned packages:**
```
com.penrose.bibby           ← Root package (scanned)
com.penrose.bibby.cli       ← Sub-package (scanned)
com.penrose.bibby.library   ← Sub-package (scanned)
com.penrose.bibby.library.book      ← Nested sub-package (scanned)
com.penrose.bibby.library.author    ← Nested sub-package (scanned)
com.penrose.bibby.library.bookcase  ← Nested sub-package (scanned)
com.penrose.bibby.library.shelf     ← Nested sub-package (scanned)
com.penrose.bibby.library.catalog   ← Nested sub-package (scanned)
com.penrose.bibby.library.genre     ← Nested sub-package (scanned)
com.penrose.bibby.library.user      ← Nested sub-package (scanned)
com.penrose.bibby.util              ← Sub-package (scanned)
```

**Total:** 10 packages scanned, 41 Java files examined

**NOT scanned:**
```
com.penrose.other       ← Different root package (NOT scanned)
com.example.bibby       ← Different organization (NOT scanned)
org.springframework     ← Third-party library (NOT scanned - has its own scanning)
```

---

## Stereotype Annotations: The Bean Markers

Spring recognizes **five stereotype annotations** when scanning:

| Annotation | Typical Use | Example from Bibby |
|------------|-------------|-------------------|
| `@Component` | Generic bean | `StartupRunner`, `CustomPromptProvider` |
| `@Service` | Business logic | `BookService`, `AuthorService` |
| `@Repository` | Data access | `BookRepository`, `AuthorRepository` |
| `@RestController` | REST endpoints | `BookController`, `BookCaseController` |
| `@Configuration` | Java config | (Bibby doesn't have custom configs) |

**CRITICAL INSIGHT:** These are all the **same annotation** under the hood!

---

## All Stereotypes Are @Component

Let's look at Spring's source code:

**@Service annotation:**
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component  // ← @Service IS a @Component!
public @interface Service {
    @AliasFor(annotation = Component.class)
    String value() default "";
}
```

**@Repository annotation:**
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component  // ← @Repository IS a @Component!
public @interface Repository {
    @AliasFor(annotation = Component.class)
    String value() default "";
}
```

**@RestController annotation:**
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller  // ← @Controller is ALSO a @Component!
@ResponseBody
public @interface RestController {
    @AliasFor(annotation = Controller.class)
    String value() default "";
}
```

**They're all `@Component` with different names!**

---

## Why Have Different Names If They're The Same?

**Semantic meaning:**

- `@Service` says "this is business logic"
- `@Repository` says "this talks to a database"
- `@RestController` says "this handles HTTP requests"
- `@Component` says "this is... something"

**Benefits:**

1. **Readability:** Tells developers the class's role
2. **AOP targeting:** Apply aspects to all `@Repository` classes
3. **Exception translation:** `@Repository` enables SQL exception translation
4. **Convention:** Follows industry patterns

**For component scanning, they're identical.** Spring scans for `@Component` and finds all stereotypes.

---

## Bibby's Bean Distribution Across Packages

Let's map where Bibby's beans are located:

### com.penrose.bibby (Root - 1 file)
```
BibbyApplication.java       @SpringBootApplication (also a @Component!)
```

### com.penrose.bibby.cli (2 files)
```
BookCommands.java           @Component
BookcaseCommands.java       @Component
CustomPromptProvider.java   @Component
LoadingBar.java             (no annotation - utility class, not a bean)
```

### com.penrose.bibby.library.book (3 beans)
```
BookService.java            @Service
BookRepository.java         @Repository (from JpaRepository)
BookController.java         @RestController
BookEntity.java             (no annotation - JPA entity, not a bean)
BookRequestDTO.java         (no annotation - record, not a bean)
BookDetailView.java         (no annotation - record, not a bean)
BookSummary.java            (no annotation - record, not a bean)
```

### com.penrose.bibby.library.author (2 beans)
```
AuthorService.java          @Service
AuthorRepository.java       @Repository
AuthorEntity.java           (no annotation - JPA entity)
```

### com.penrose.bibby.library.bookcase (3 beans)
```
BookcaseService.java        @Service
BookcaseRepository.java     @Repository
BookCaseController.java     @RestController
BookcaseEntity.java         (no annotation - JPA entity)
BookcaseDTO.java            (no annotation - record)
```

### com.penrose.bibby.library.shelf (3 beans)
```
ShelfService.java           @Service
ShelfRepository.java        @Repository
ShelfController.java        @RestController
ShelfEntity.java            (no annotation - JPA entity)
ShelfSummary.java           (no annotation - record)
```

### com.penrose.bibby.library.catalog (2 beans)
```
CatalogService.java         @Service
CatalogRepository.java      @Repository
CatalogEntity.java          (no annotation - JPA entity)
```

### com.penrose.bibby.library.genre (1 bean)
```
GenreRepository.java        @Repository
Genre.java                  (no annotation - enum)
```

### com.penrose.bibby.library.user (1 bean)
```
UserRepository.java         @Repository
UserEntity.java             (no annotation - JPA entity)
```

### com.penrose.bibby.util (1 bean)
```
StartupRunner.java          @Component
```

**Total Beans Found by Component Scanning: ~17**

**Not Beans (but scanned):**
- Entities (BookEntity, AuthorEntity, etc.) - JPA managed, not Spring beans
- Records (DTOs, views) - Value objects, not beans
- Enums (Genre) - Constants, not beans
- Utility classes without annotations (LoadingBar)

---

## How Component Scanning Works (Startup Process)

**When you run `java -jar Bibby.jar` (or `mvn spring-boot:run`):**

```
1. JVM starts
   ↓
2. main() method runs → SpringApplication.run(BibbyApplication.class)
   ↓
3. Spring Boot initialization begins
   ↓
4. Component Scanning Phase:
   ├── Scan com.penrose.bibby package
   ├── Find BibbyApplication.java (@SpringBootApplication)
   ├── Scan com.penrose.bibby.cli
   │   ├── Find BookCommands (@Component)
   │   ├── Find BookcaseCommands (@Component)
   │   └── Find CustomPromptProvider (@Component)
   ├── Scan com.penrose.bibby.library.book
   │   ├── Find BookService (@Service)
   │   ├── Find BookRepository (@Repository)
   │   └── Find BookController (@RestController)
   ├── Scan com.penrose.bibby.library.author
   │   ├── Find AuthorService (@Service)
   │   └── Find AuthorRepository (@Repository)
   ├── ... (repeat for all packages)
   └── Total: ~17 bean definitions registered
   ↓
5. Bean Creation Phase (from Section 21):
   ├── Create repositories (no dependencies)
   ├── Create services (depend on repositories)
   ├── Create controllers (depend on services)
   └── Create commands (depend on services + controllers)
   ↓
6. Application ready!
```

**If Spring doesn't find expected beans:**
```
***************************
APPLICATION FAILED TO START
***************************

Description:

Field bookService in com.penrose.bibby.cli.BookCommands required a bean of type 'BookService' that could not be found.

Action:

Consider defining a bean of type 'BookService' in your configuration.
```

---

## @CommandScan: Spring Shell's Component Scanning

**BibbyApplication.java:7**
```java
@CommandScan
@SpringBootApplication
public class BibbyApplication {
```

**What is @CommandScan?**

Spring Shell (Bibby's CLI framework) has its own component scanning for **command classes**.

**Without @CommandScan:** Spring Shell commands aren't discovered
**With @CommandScan:** Spring Shell scans for `@Command` annotations

**Bibby's commands:**
```java
@Component
@Command(command = "book", group = "Book Commands")
public class BookCommands { }

@Component
@Command(command = "bookcase", group = "Bookcase Commands")
public class BookcaseCommands { }
```

**Why both @Component AND @Command?**
- `@Component` → Spring creates it as a bean
- `@Command` → Spring Shell registers it as a CLI command

**If you remove @CommandScan:**
- Commands still created as beans (via @Component)
- But NOT registered as CLI commands
- Typing `book-create` in shell → "command not found"

---

## When Beans Won't Be Found

### Problem 1: Class Outside Scan Path

```java
package com.example.myapp;  // ← Different root package!

@Service
public class ExternalService { }
```

**Result:** NOT found. Spring only scans `com.penrose.bibby.**`

**Solution:** Move to `com.penrose.bibby.external.ExternalService`

---

### Problem 2: Missing Annotation

```java
package com.penrose.bibby.library.book;

public class BookHelper {  // ← NO @Component!
    public String formatTitle(String title) {
        return title.toUpperCase();
    }
}
```

**Result:** NOT found. No stereotype annotation.

**Solution:** Add `@Component`:
```java
@Component
public class BookHelper { }
```

**Or:** Don't make it a bean if it's stateless utility:
```java
public class BookHelper {
    private BookHelper() { }  // Prevent instantiation

    public static String formatTitle(String title) {  // Static method
        return title.toUpperCase();
    }
}
```

---

### Problem 3: Abstract Class

```java
@Service
public abstract class BaseService {  // ← Abstract!
    // ...
}
```

**Result:** Found, but Spring can't instantiate it (abstract classes can't be instantiated).

**Error:**
```
BeanCreationException: Error creating bean 'baseService': Instantiation of bean failed
```

**Solution:** Don't annotate abstract classes. Only concrete implementations.

---

## Customizing Component Scanning

### Scan Additional Packages

**If you have classes outside `com.penrose.bibby`:**

```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.penrose.bibby",        // Default
    "com.example.shared.utils"  // Additional package
})
public class BibbyApplication {
```

**Caution:** Specifying `basePackages` **REPLACES** the default. You must include `com.penrose.bibby` explicitly!

---

### Exclude Specific Classes

**Exclude a specific class from scanning:**

```java
@SpringBootApplication
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = LegacyService.class  // Don't scan this
    )
})
public class BibbyApplication {
```

**Use cases:**
- Exclude legacy code
- Disable specific beans for testing
- Exclude third-party beans

---

### Include Only Specific Annotations

**Scan only @Service classes:**

```java
@ComponentScan(
    useDefaultFilters = false,  // Disable default @Component scanning
    includeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ANNOTATION,
            classes = Service.class
        )
    }
)
```

**Result:** Only classes with `@Service` are scanned (no `@Component`, `@Repository`, etc.)

---

### Scan by Package Pattern

**Scan only specific sub-packages:**

```java
@ComponentScan(basePackages = "com.penrose.bibby.library.**")
```

**Result:** Scans library.* but NOT cli.* or util.*

---

## Bibby's Scanning Strategy: Perfect as-is

**BibbyApplication is in the root package** (`com.penrose.bibby`)

**Result:**
- ✅ All sub-packages scanned automatically
- ✅ No custom @ComponentScan needed
- ✅ No extra configuration
- ✅ Follows Spring Boot best practices

**This is the recommended approach:** Put `@SpringBootApplication` in the root package, and all sub-packages are scanned automatically.

---

## Meta-Annotations: Creating Custom Stereotypes

**You can create your own stereotype annotations:**

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service  // ← Inherits from @Service
public @interface LibraryService {
    String value() default "";
}
```

**Usage:**
```java
@LibraryService
public class BookService {
    // Spring scans for @Service, finds @LibraryService (which IS a @Service)
}
```

**Benefits:**
- Domain-specific naming
- Apply custom AOP aspects to all `@LibraryService` beans
- Enforce conventions

**For Bibby:** Not needed. Standard stereotypes (`@Service`, `@Repository`) are sufficient.

---

## Bean Naming Conventions

**Spring auto-generates bean names:**

```java
@Service
public class BookService { }
// Bean name: "bookService" (class name with lowercase first letter)

@Service
public class AuthorService { }
// Bean name: "authorService"
```

**Explicit bean name:**

```java
@Service("customBookService")
public class BookService { }
// Bean name: "customBookService"
```

**Why name beans explicitly?**
- Multiple beans of same type (qualifier needed)
- Testing (reference by name)
- Debugging (clear names in logs)

**For Bibby:** Default names are fine. No naming conflicts.

---

## Debugging Component Scanning

**See which beans Spring found:**

Add to `application.properties`:
```properties
logging.level.org.springframework.boot.autoconfigure=DEBUG
logging.level.org.springframework.context.annotation=DEBUG
```

**Startup logs will show:**
```
Identified candidate component class: file [BookService.class]
Identified candidate component class: file [AuthorService.class]
Identified candidate component class: file [BookRepository.class]
...
```

**Count beans at runtime:**

```java
@Component
public class BeanCounter implements CommandLineRunner {
    @Autowired
    private ApplicationContext context;

    @Override
    public void run(String... args) {
        String[] beanNames = context.getBeanDefinitionNames();
        System.out.println("Total beans: " + beanNames.length);
        // Output: Total beans: 150+ (17 custom + 130+ from Spring Boot)
    }
}
```

---

## Your Action Items

**Priority 1: Understand Bibby's Package Structure**

1. Draw Bibby's package tree on paper:
   ```
   com.penrose.bibby
   ├── BibbyApplication (@SpringBootApplication)
   ├── cli (commands)
   ├── library
   │   ├── book (service, repo, controller, entity)
   │   ├── author
   │   ├── bookcase
   │   ├── shelf
   │   ├── catalog
   │   ├── genre
   │   └── user
   └── util
   ```

2. For each package, list:
   - Which classes are beans (annotated)
   - Which classes are NOT beans (entities, records, utilities)

**Priority 2: Verify Component Scanning**

3. Add debug logging to `application.properties`:
   ```properties
   logging.level.org.springframework.context.annotation=DEBUG
   ```

4. Run `mvn spring-boot:run` and watch startup logs

5. Verify all 17 custom beans are found:
   - 5 services
   - 5 repositories
   - 3 controllers
   - 2 commands
   - 2 utilities

**Priority 3: Experiment with Scanning**

6. Create a test class OUTSIDE `com.penrose.bibby`:
   ```java
   package com.example.test;

   @Service
   public class TestService { }
   ```

7. Run application - Spring won't find it (expected)

8. Add custom `@ComponentScan` to include `com.example.test`

9. Verify `TestService` is now found

**Priority 4: Understand Stereotype Equivalence**

10. Replace `@Service` with `@Component` on `BookService`

11. Run application - works identically (they're the same!)

12. Change back to `@Service` (better semantics)

---

## Key Takeaways

**1. Component Scanning is Automatic:**
- Spring scans packages at startup
- Finds classes with stereotype annotations
- Registers them as bean definitions

**2. @SpringBootApplication Enables Scanning:**
- Combines @ComponentScan + @EnableAutoConfiguration + @Configuration
- Scans package where it's located + all sub-packages
- Bibby scans `com.penrose.bibby.**`

**3. All Stereotypes Are @Component:**
- @Service IS @Component
- @Repository IS @Component
- @RestController IS @Controller IS @Component
- Different names for semantic meaning, not functionality

**4. Default Scan Path = Best Practice:**
- Put @SpringBootApplication in root package
- All sub-packages scanned automatically
- No custom @ComponentScan needed
- Bibby follows this perfectly

**5. Beans vs Non-Beans:**
- Beans: Annotated classes (services, repos, controllers, commands)
- Not Beans: Entities, records, enums, utility classes
- Total: 17 custom beans + 130+ from Spring Boot

**6. @CommandScan for Spring Shell:**
- Separate scanning for @Command annotations
- Bibby uses both @Component and @Command
- Required for CLI command registration

**7. Fail-Fast on Missing Beans:**
- If bean not found, application crashes at startup
- Better than runtime NullPointerException
- Check package structure and annotations

---

## Bibby's Component Scanning Grade: A+

**What you're doing perfectly:**

✅ @SpringBootApplication in root package (`com.penrose.bibby`)
✅ Logical package structure (cli, library, util)
✅ All beans in sub-packages (automatically scanned)
✅ Appropriate stereotype usage (@Service, @Repository, @RestController)
✅ @CommandScan for Spring Shell commands
✅ No custom @ComponentScan needed (follows best practices)

**No improvements needed!**

---

## What's Next

**Section 23: Bean Lifecycle & Scopes**

Now that you understand how Spring **finds** beans (component scanning) and **wires** them (dependency injection), we'll explore the **bean lifecycle** - what happens after a bean is created.

You'll learn:
- Bean creation order (repositories → services → controllers)
- Initialization callbacks (@PostConstruct)
- Destruction callbacks (@PreDestroy)
- Bean scopes (singleton, prototype, request, session)
- Lazy vs eager initialization
- When to use lifecycle hooks
- How to customize bean initialization

We'll analyze when Bibby's beans are created and understand the complete lifecycle from scanning to destruction.

**Ready when you are!**

---

**Mentor's Note:**

Component scanning is Spring's **discovery mechanism**. It's the invisible process that happens every time you start Bibby, finding all your annotated classes and registering them as beans.

Understanding component scanning answers fundamental questions:
- "How does Spring know BookService exists?"
- "Why do I need @Service?"
- "What if I put a class in the wrong package?"

You've discovered Bibby follows Spring Boot best practices perfectly:
- @SpringBootApplication in the root package
- Logical package organization
- No custom scanning configuration needed

Most developers never think about component scanning - it "just works." But when it doesn't (class in wrong package, missing annotation), you now know exactly why and how to fix it.

This knowledge transforms "magic" into "understood."

---

**Files Referenced:**
- `BibbyApplication.java:1-14` (@SpringBootApplication in root package)
- Package structure across 10 packages (41 Java files total)
- 17 custom beans discovered via component scanning

**Total Packages Scanned:** 10
**Total Beans Found:** ~17 custom + 130+ from Spring Boot

**Estimated Reading Time:** 45 minutes
**Estimated Action Items Time:** 30 minutes
**Total Section Time:** 75 minutes

---

*Section 22 Complete - Section 23: Bean Lifecycle & Scopes Next*
