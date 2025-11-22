## Devlog Entry

**Date**: 2025-11-22

### Refactor: Introduce AuthorEntityFactory and Improve Field Encapsulation

**Context**

The `AuthorService.createNewAuthor()` method was manually constructing `AuthorEntity` objects by directly calling setters. This pattern was inconsistent with the factory pattern used elsewhere in the codebase (e.g., `BookFactory`). Additionally, service fields lacked proper encapsulation with access modifiers and immutability.

**Motivation**

Centralizing entity creation logic in a factory provides:

- Single source of truth for entity construction rules
- Easier testing of creation logic
- Consistency with existing architectural patterns
- Reduced duplication if multiple places need to create author entities

**Changes Made**

#### 1. Introduced AuthorEntityFactory Dependency

**Before:**

```java
AuthorRepository authorRepository;

public AuthorService(AuthorRepository authorRepository) {
    this.authorRepository = authorRepository;
}
```

**After:**

```java
private final AuthorRepository authorRepository;
private final AuthorEntityFactory authorEntityFactory;

public AuthorService(AuthorRepository authorRepository, AuthorEntityFactory authorEntityFactory) {
    this.authorRepository = authorRepository;
    this.authorEntityFactory = authorEntityFactory;
}
```

**Changes:**

- Added `AuthorEntityFactory` as a constructor dependency
- Made both fields `private final` for proper encapsulation and immutability
- Assigned factory instance in constructor

#### 2. Refactored Entity Creation to Use Factory

**Before:**

```java
public AuthorEntity createNewAuthor(String authorFirstName, String authorLastName){
    AuthorEntity authorEntity = new AuthorEntity();
    authorEntity.setFirstName(authorFirstName);
    authorEntity.setLastName(authorLastName);
    authorEntity.setFullName(authorFirstName + " " + authorLastName);
    return authorRepository.save(authorEntity);
}
```

**After:**

```java
public AuthorEntity createNewAuthor(String authorFirstName, String authorLastName) {
    return authorRepository.save(authorEntityFactory.createEntity(authorFirstName, authorLastName));
}
```

**Benefits:**

- Reduced method from 5 lines to 1 line
- Removed manual entity construction logic
- Delegated creation responsibility to specialized factory
- Eliminated direct knowledge of entity construction details

#### 3. Improved Code Style

- Added `private final` modifiers to fields for proper encapsulation
- Ensured constructor properly assigns all injected dependencies
- Added spacing after comma in method call for readability

**Architectural Improvements**

This change completes the factory pattern implementation across the domain layer:

- `BookFactory` handles book entity creation
- `AuthorEntityFactory` handles author entity creation
- Service layer focuses on orchestration, not construction

**Impact on Testing**

Factory pattern enables easier unit testing:

```java
// Can now mock the factory in tests
@Mock
private AuthorEntityFactory authorEntityFactory;

@Test
void testCreateNewAuthor() {
    when(authorEntityFactory.createEntity("John", "Doe"))
        .thenReturn(mockAuthorEntity);
    // ...
}
```

**Files Modified**

- `AuthorService.java` - introduced factory dependency, refactored creation method

**Lessons Learned**

During code review, identified a critical bug in the initial implementation:

- **Bug**: Factory field was declared but not assigned in constructor (would cause `NullPointerException`)
- **Fix**: Added proper constructor assignment
- **Prevention**: Always ensure injected dependencies are assigned to their fields

This reinforces the importance of:

- Thorough code review before committing
- Using `final` fields (compiler catches unassigned final fields)
- Following consistent naming conventions (camelCase for variables)

------

## Git Commit Message

```java
refactor: introduce AuthorEntityFactory for centralized entity creation

- Add AuthorEntityFactory dependency to AuthorService
- Refactor createNewAuthor() to delegate to factory pattern
- Add private final modifiers to service fields for encapsulation
- Ensure proper constructor assignment of all dependencies

This change aligns author entity creation with the factory pattern
used elsewhere in the codebase (BookFactory), centralizing creation
logic and improving testability.
```