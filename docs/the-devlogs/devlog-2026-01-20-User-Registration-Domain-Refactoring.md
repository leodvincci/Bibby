# Devlog: User Registration Domain Refactoring

**Date:** 2026-01-20
**PRs:** #223, #224, #225, #226, #227
**Theme:** Domain-Driven Design / Command Pattern / DTO Refinement

## Context

The user registration module needed architectural improvements to better align with domain-driven design principles. The existing implementation directly used DTOs in the service layer, which coupled the domain logic to the web API contract. This refactoring introduces proper command and result objects to decouple the domain from the presentation layer, while also improving naming consistency across DTOs and mappers.

The work spanned 5 pull requests merged over approximately 4 hours, with a total of 13 functional commits focused on incremental improvements.

---

## Phase 1: Introducing Command Pattern

### Commit 1: `4c1cb4a` — Add UserRegistrationRequestCommand

**Added:**
- `UserRegistrationRequestCommand` record in domain layer
- New mapper methods in `UserRegistrationMapper`

**Modified:**
- `UserRegistrationService` to accept command instead of DTO
- `UserRegistrationController` to include mapper dependency

**Files changed:** 4 files, +25 lines, -6 lines

#### What Changed:

**Before:**
```java
public class UserRegistrationService {
    public UserEntity register(UserRegistrationRequestDTO dto) {
        // Service directly depends on web DTO
    }
}
```

**After:**
```java
public class UserRegistrationService {
    public UserEntity register(UserRegistrationRequestCommand command) {
        // Service works with domain command
    }
}
```

The controller now handles DTO-to-command conversion:
```java
@RestController
public class UserRegistrationController {
    private final UserRegistrationMapper mapper;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationRequestDTO dto) {
        UserRegistrationRequestCommand command = mapper.toCommand(dto);
        UserEntity result = service.register(command);
        return ResponseEntity.ok(mapper.toDTO(result));
    }
}
```

#### Rationale:
- **Decoupling:** Domain layer no longer depends on web contract
- **Testability:** Service can be tested without web DTOs
- **Flexibility:** Web DTOs can evolve without affecting domain logic

**PR #223 merged:** Tue Jan 20 18:52:17 2026

---

## Phase 2: DTO Naming Consistency

### Commit 2: `2363ded` — Rename Registration DTOs

**Renamed:**
- `UserRegistrationRequestDTO` → `RegisterUserRequestDTO`
- `UserRegistrationResponseDTO` → `RegisterUserResponseDTO`

**Modified:**
- `UserRegistrationMapper` (4 method signatures)
- `UserRegistrationController` (9 references)

**Files changed:** 4 files, +15 lines, -15 lines

#### Why This Matters:

The original naming was verbose and inconsistent with naming patterns elsewhere in the codebase. The new naming follows a verb-first pattern that better describes the action:

| Old Name | New Name | Pattern |
|----------|----------|---------|
| `UserRegistrationRequestDTO` | `RegisterUserRequestDTO` | `[Action][Resource][Type]` |
| `UserRegistrationResponseDTO` | `RegisterUserResponseDTO` | `[Action][Resource][Type]` |

This aligns with command naming (e.g., `CreateBookCommand`, `CheckoutBookCommand`) used in other modules.

**PR #224 merged:** Tue Jan 20 20:29:58 2026

---

## Phase 3: Command and Result Objects Refinement

### Commit 3: `dc9464c` — Replace Command and Add Result Object

**Added:**
- `RegisterUserCommand` (replaced `UserRegistrationRequestCommand`)
- `RegisterUserResult` record to encapsulate service response

**Renamed:**
- `UserRegistrationMapper` → `AppUserMapper`

**Modified:**
- `UserRegistrationService` now returns `RegisterUserResult`
- `RegisterUserResponseDTO` enhanced to include user ID

**Files changed:** 7 files, +31 lines, -22 lines

#### The Full Flow:

```
Request DTO → Command → Service → Result → Response DTO
     ↓           ↓                    ↓          ↓
Web Layer   Domain Layer         Domain Layer  Web Layer
```

**Before:**
```java
// Service returned entity directly
UserEntity result = service.register(command);
RegisterUserResponseDTO response = mapper.toResponseDTO(result);
```

**After:**
```java
// Service returns domain result object
RegisterUserResult result = service.register(command);
RegisterUserResponseDTO response = mapper.toResponseDTO(result);
```

#### Why Result Objects?

| Concern | Without Result | With Result |
|---------|---------------|-------------|
| Return value | Returns entity | Returns focused result |
| Coupling | Exposes full entity | Hides entity internals |
| Evolution | Entity changes break API | Result buffers changes |
| Clarity | Purpose unclear | Intent explicit |

The `RegisterUserResult` record contains only the data needed for the response:
```java
public record RegisterUserResult(
    Long userId,
    String username,
    String email
) {}
```

#### Mapper Renaming:

`UserRegistrationMapper` → `AppUserMapper` because:
1. It maps between multiple representations (DTO, Command, Result, Entity)
2. "AppUser" is the core domain concept
3. "Registration" was too specific for a mapper that might handle other user operations

**PR Not yet merged at this point**

---

### Commit 4: `f030d9c` — Remove Unused Mapper Method

**Removed:**
- `toDTO(RegisterUserCommand)` method from `AppUserMapper`

**Modified:**
- Code formatting in `UserRegistrationController`
- Formatting in `RegisterUserResult`

**Files changed:** 3 files, +7 lines, -10 lines

#### Dead Code Removal:

The `toDTO(RegisterUserCommand)` method was added in earlier commits but never used. Commands flow toward the domain, not back toward the web layer. Mapping commands to DTOs would violate the intended direction of data flow:

```
✅ Correct flow:  DTO → Command → Service
❌ Wrong flow:    Command → DTO (never happens)
```

Removing unused methods keeps the mapper focused and prevents confusion.

---

### Commit 5: `209e53d` — Formatting and Test Enhancement

**Modified:**
- `UserRegistrationController` formatting (4 lines)
- `UserRegistrationControllerTest` enhanced with additional assertions

**Files changed:** 2 files, +10 lines, -3 lines

#### Test Improvements:

Enhanced the registration test to verify more aspects of the response:
```java
@Test
void testRegisterUser() {
    // ... existing setup ...

    // Enhanced assertions
    assertNotNull(response.getUserId());
    assertEquals("testuser", response.getUsername());
    assertEquals("test@example.com", response.getEmail());
}
```

**PR #225 merged:** Tue Jan 20 22:03:50 2026

---

### Commit 6: `b2c7edee` — Controller Formatting Improvements

**Modified:**
- Line formatting in `UserRegistrationController` (4 lines)
- Test formatting cleanup (9 lines)

**Files changed:** 2 files, +6 lines, -7 lines

Minor formatting adjustments for consistency and readability.

---

## Phase 4: Mapper Method Addition

### Commit 7: `e0a007b` — Add toResponseDTO Method

**Added:**
- `toResponseDTO(RegisterUserResult)` method in `AppUserMapper`

**Modified:**
- `UserRegistrationController` to use new mapper method
- Enhanced logging in registration flow

**Files changed:** 2 files, +8 lines, -3 lines

#### Completing the Mapping Chain:

This commit completes the full mapping chain by adding the final transformation:

```java
// Now the full flow is mapped:
RegisterUserRequestDTO requestDTO = /* from HTTP request */;
RegisterUserCommand command = mapper.toCommand(requestDTO);
RegisterUserResult result = service.register(command);
RegisterUserResponseDTO responseDTO = mapper.toResponseDTO(result); // ← new method
```

Before this, the controller was manually constructing the response DTO. Now all mapping logic lives in `AppUserMapper`.

#### Logging Enhancements:

Added structured logging at key points:
```java
log.info("Starting user registration for username: {}", command.username());
RegisterUserResult result = service.register(command);
log.info("User registered successfully with ID: {}", result.userId());
```

**PR #226 merged:** Tue Jan 20 22:21:27 2026

---

## Phase 5: Final Polish

### Commit 8: `6ca363e` — Controller Line Formatting

**Modified:**
- Line formatting in `UserRegistrationController` for readability

**Files changed:** 1 file, +2 lines, -1 line

Minor readability improvement to method chain.

---

### Commit 9: `e86fab4` — BCryptPasswordEncoder Variable Rename

**Modified:**
- Renamed `BCryptPasswordEncoder` variable in `UserRegistrationService`

**Files changed:** 1 file, +2 lines, -2 lines

#### Consistency Fix:

**Before:**
```java
private final BCryptPasswordEncoder passwordEncoder; // generic name
```

**After:**
```java
private final BCryptPasswordEncoder bCryptPasswordEncoder; // matches type name
```

Variable now matches the type name convention used elsewhere in the codebase.

**PR #227 merged:** Tue Jan 20 22:36:24 2026

---

## Architecture Evolution

### Before Refactoring:

```
┌─────────────────────────────────────┐
│   UserRegistrationController        │
│   - accepts DTO                     │
│   - passes DTO directly to service  │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   UserRegistrationService           │
│   - operates on DTO (web concern!)  │
│   - returns Entity                  │
└──────────────┬──────────────────────┘
               │
               ▼
        [UserEntity]
```

**Problems:**
- Domain layer coupled to web DTOs
- No clear separation of concerns
- Entity exposed directly to web layer
- Hard to change API without affecting domain

---

### After Refactoring:

```
┌─────────────────────────────────────┐
│   UserRegistrationController        │
│   - accepts RegisterUserRequestDTO  │
│   - maps to RegisterUserCommand     │
│   - maps result to ResponseDTO      │
└──────────────┬──────────────────────┘
               │
               ▼
       [AppUserMapper]
               │
               ▼
┌─────────────────────────────────────┐
│   UserRegistrationService           │
│   - accepts RegisterUserCommand     │
│   - returns RegisterUserResult      │
└──────────────┬──────────────────────┘
               │
               ▼
    [RegisterUserResult]
```

**Benefits:**
- ✅ Domain layer independent of web concerns
- ✅ Clear boundaries between layers
- ✅ Entity hidden from web layer
- ✅ Result object provides focused data
- ✅ API can evolve independently

---

## Files Modified Summary

| File | Changes | Description |
|------|---------|-------------|
| `UserRegistrationRequestCommand.java` | Deleted (replaced) | Initial command object |
| `RegisterUserCommand.java` | Created | Final command object |
| `RegisterUserResult.java` | Created | Domain result object |
| `UserRegistrationMapper.java` | Renamed | Became `AppUserMapper` |
| `AppUserMapper.java` | Modified (+22 lines) | Added mapping methods, removed unused |
| `RegisterUserRequestDTO.java` | Renamed | Was `UserRegistrationRequestDTO` |
| `RegisterUserResponseDTO.java` | Renamed/Enhanced | Was `UserRegistrationResponseDTO`, added userId |
| `UserRegistrationService.java` | Modified | Now uses command/result objects |
| `UserRegistrationController.java` | Modified | Uses mapper for all conversions |
| `UserRegistrationControllerTest.java` | Enhanced | Added assertions |

**Total:** 10 files modified, ~80 net lines added

---

## Pull Request Timeline

| PR # | Title | Commits | Merged Time |
|------|-------|---------|-------------|
| #223 | Initial Command Introduction | 1 | 18:52:17 |
| #224 | DTO Naming Consistency | 1 | 20:29:58 |
| #225 | Formatting & Test Enhancement | 1 | 22:03:50 |
| #226 | Mapper Method Addition | 1 | 22:21:27 |
| #227 | Final Variable Rename | 1 | 22:36:24 |

**Time span:** ~3 hours 44 minutes (18:52 → 22:36)

---

## Pattern Recognition

This refactoring demonstrates several key patterns:

### 1. Command Pattern
Encapsulates request data as an object, decoupling the requester from the executor.

### 2. Result Object Pattern
Returns focused data from services instead of exposing full entities.

### 3. Anti-Corruption Layer
The mapper acts as an anti-corruption layer between web and domain concerns.

### 4. Incremental Refactoring
Changes were made in small, safe steps with continuous integration:
- Add command objects → Rename DTOs → Add result objects → Remove unused code

### 5. Naming Consistency
Moving toward verb-first naming (`RegisterUser` instead of `UserRegistration`) aligns with command-oriented design.

---

## Design Principles Applied

| Principle | How Applied |
|-----------|-------------|
| **Separation of Concerns** | Web DTOs vs Domain Commands/Results |
| **Single Responsibility** | Mapper handles only transformations |
| **Dependency Inversion** | Domain doesn't depend on web layer |
| **Open/Closed** | Result objects allow extension without modification |
| **Interface Segregation** | Commands contain only needed data |

---

## Technical Debt Addressed

### Before:
❌ Service layer coupled to web DTOs
❌ Entities exposed directly to web layer
❌ Inconsistent DTO naming
❌ Manual mapping in controllers
❌ Dead mapper methods

### After:
✅ Clean domain/web separation
✅ Result objects hide entity internals
✅ Consistent verb-first DTO naming
✅ Centralized mapping logic
✅ Unused code removed

---

## Testing Impact

### Test Changes:
- Enhanced controller test with result object assertions
- Improved readability of test code
- Tests now verify the complete mapping chain

### Future Test Improvements:
- [ ] Add unit tests for `AppUserMapper`
- [ ] Test command validation
- [ ] Test result object construction
- [ ] Integration tests for full registration flow

---

## Reflection

This refactoring series demonstrates the value of incremental improvement. Rather than a single massive change, the work was broken into logical phases:

1. **Introduce** command objects
2. **Standardize** naming
3. **Add** result objects
4. **Remove** unused code
5. **Polish** formatting and logging

Each phase was independently reviewable and mergeable, reducing risk and making the architectural evolution clear.

The key insight: **DTOs should stay at boundaries.** The domain should speak its own language (Commands, Results, Entities), not the language of the web layer. The mapper is the translator.

### What We Learned:

**Small records with focused data are powerful.** Both `RegisterUserCommand` and `RegisterUserResult` are simple records, but they create clear contracts and decouple layers.

**Naming matters.** The shift from `UserRegistrationRequestDTO` to `RegisterUserRequestDTO` isn't just cosmetic—it signals a verb-first, action-oriented design that aligns with domain-driven principles.

**The mapper is an anti-corruption layer.** `AppUserMapper` isn't just converting formats; it's protecting the domain from web concerns and vice versa.

---

## Next Steps

### Immediate:
- [ ] Apply same pattern to other registration-related operations
- [ ] Add validation annotations to command objects
- [ ] Implement error result objects for failure cases

### Future Enhancements:
- [ ] Extract user registration to separate bounded context
- [ ] Add domain events for registration completion
- [ ] Implement registration audit logging
- [ ] Add registration confirmation workflow

---

## Metrics

- **Total Commits:** 13 (9 functional + 4 merges)
- **Files Modified:** 10 unique files
- **Net Lines Changed:** ~80 lines
- **Time Span:** ~4 hours
- **Pull Requests:** 5 (all merged)
- **Merge Conflicts:** 0

---

**Author:** leodvincci
**Date Range:** Jan 20, 2026 (18:46 - 22:36)
**Branch:** refactor/user-registration-domain → main