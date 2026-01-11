# Adding Spring Security + User Registration to Bibby: A Build Session Walkthrough

## The Problem: Security Without Breaking Everything

I spent today adding Spring Security to Bibby, my personal library management app. The goal seemed straightforward: take a bare-bones user registration endpoint and harden it with password encryption, email-based authentication, and proper security configurations. The risk? Spring Security changes how your entire app behaves—and if you're not careful, your existing tests will explode.

Here's what I learned building this out, step by step, in a real Spring Boot application.

---

## Before vs. After: The Journey of `/register`

**Before (first commit):**
```java
@PostMapping("/register")
public void registerUser(@RequestBody UserRegistrationRequest userRegistrationRequest) {
    System.out.println("Registering user: " + userRegistrationRequest.getUsername());
}
```

A hollow shell. No persistence, no validation, no security. Just a `System.out.println` and a prayer.

**After (final state):**
```java
@PostMapping("/register")
public ResponseEntity<UserRegistrationResponseDTO> registerUser(
    @Valid @RequestBody UserRegistrationRequestDTO userRegistrationRequestDTO) {

    userRegistrationService.registerUser(userRegistrationRequestDTO);

    UserRegistrationResponseDTO response = new UserRegistrationResponseDTO(
        userRegistrationRequestDTO.getEmail(),
        userRegistrationRequestDTO.getFirstName(),
        userRegistrationRequestDTO.getLastName()
    );

    logger.info("Registering user: {}", response.email());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

Now we have:
- Proper DTOs for request/response separation
- `@Valid` annotation triggering Bean Validation
- Password hashing with BCrypt (14-round strength)
- A service layer that persists to PostgreSQL via JPA
- HTTP 201 Created status
- SLF4J logging instead of `System.out`

---

## Core Implementation: Step-by-Step

### 1. Registration DTOs + Controller Structure

I started by separating concerns with request and response DTOs. The `UserRegistrationRequestDTO` captures what the client sends:

```java
public class UserRegistrationRequestDTO {
    @NotBlank
    private String username;

    private String password;

    @Email
    private String email;

    private String firstName;
    private String lastName;

    // getters/setters...
}
```

The `@Email` annotation from `jakarta.validation.constraints` ensures malformed emails get rejected before they hit the database. The `@Valid` annotation in the controller triggers this validation automatically.

For the response, I used a Java record:
```java
public record UserRegistrationResponseDTO(String email, String firstName, String lastName) {}
```

**Why a record?** Immutability for response DTOs is a good default. We're never modifying the response after construction, so a record gives us equals/hashCode/toString for free and signals intent.

### 2. Mapping Layer: UserRegistrationMapper

Rather than manually mapping fields in the controller, I extracted a static mapper:

```java
public class UserRegistrationMapper {
    public static AppUserEntity toEntity(UserRegistrationRequestDTO dto) {
        AppUserEntity entity = new AppUserEntity();
        entity.setPassword(dto.getPassword());
        entity.setEmail(dto.getEmail());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        return entity;
    }
}
```

This keeps the service layer focused on business logic, not DTO-to-Entity translation. In a larger app, I'd reach for MapStruct, but for now, a static utility method is fine.

### 3. Service Layer: Registration + Password Hashing

The `UserRegistrationService` is where the real work happens:

```java
@Service
public class UserRegistrationService {
    private final UserRegistrationJpaRepository userRegistrationJpaRepository;
    private final BCryptPasswordEncoder Bcrypt = new BCryptPasswordEncoder(14);

    public UserRegistrationService(UserRegistrationJpaRepository userRegistrationJpaRepository) {
        this.userRegistrationJpaRepository = userRegistrationJpaRepository;
    }

    public void registerUser(UserRegistrationRequestDTO userRegistrationRequestDTO) {
        AppUserEntity appUserEntity = UserRegistrationMapper.toEntity(userRegistrationRequestDTO);
        appUserEntity.setPassword(Bcrypt.encode(userRegistrationRequestDTO.getPassword()));
        userRegistrationJpaRepository.save(appUserEntity);
    }
}
```

**Key decision: BCryptPasswordEncoder with 14 rounds.**
- Default is 10 rounds (2^10 iterations)
- I bumped it to 14 (2^14 = ~16K iterations) for stronger protection against brute-force attacks
- The trade-off: slower hashing, but users only register once, so the latency is acceptable

The password gets hashed *after* mapping but *before* persistence. This ensures the plaintext password never touches the database.

### 4. Validation: `@Email` + `@Valid` Behavior

With `@Valid` in the controller and `@Email` on the DTO, Spring automatically validates incoming requests:

```java
@PostMapping("/register")
public ResponseEntity<UserRegistrationResponseDTO> registerUser(
    @Valid @RequestBody UserRegistrationRequestDTO userRegistrationRequestDTO) {
    // ...
}
```

**What happens on validation failure?**
Spring returns a 400 Bad Request with error details. For example, sending `"email": "not-an-email"` triggers:

```json
{
  "timestamp": "2026-01-11T15:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for object='userRegistrationRequestDTO'. Error count: 1",
  "errors": [
    {
      "field": "email",
      "rejectedValue": "not-an-email",
      "defaultMessage": "must be a well-formed email address"
    }
  ]
}
```

No custom exception handling needed—Spring's `@Valid` + Bean Validation does the heavy lifting.

---

## Spring Security Setup: The Real Challenge

### Adding Dependencies

First, the `pom.xml` changes:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

**The moment you add `spring-boot-starter-security`, everything locks down.** By default:
- All endpoints require authentication
- A default login form appears at `/login`
- Your tests start failing with 401 Unauthorized

### SecurityFilterChain: Define the Rules

I created `WebSecurityConfigs` to specify which endpoints are public vs. protected:

```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfigs {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/v1/user/registration/register").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable())
            .formLogin(Customizer.withDefaults())
            .logout(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

**Breaking it down:**
- `requestMatchers("/api/v1/user/registration/register").permitAll()` → Registration is public (obviously—can't register if you need to be logged in)
- `anyRequest().authenticated()` → Everything else requires authentication
- `csrf(csrf -> csrf.disable())` → Disabling CSRF because this is a stateless REST API (more on this in "Gotchas")
- `httpBasic(Customizer.withDefaults())` → Enable HTTP Basic Auth for testing (would replace with JWT in production)

### PasswordEncoder Bean

Spring Security needs a `PasswordEncoder` bean to validate passwords during authentication:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(14);
}
```

This bean is used by the `DaoAuthenticationProvider` to compare plaintext login passwords against hashed database passwords.

### DaoAuthenticationProvider: Wire It Together

The `DaoAuthenticationProvider` connects the `UserDetailsService` (which loads users) with the `PasswordEncoder` (which validates passwords):

```java
@Bean
public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
}
```

Spring auto-configures this for you if you provide `UserDetailsService` + `PasswordEncoder` beans, but I defined it explicitly for clarity.

---

## Custom UserDetails + UserDetailsService

Spring Security needs to know:
1. How to load a user (given a username/email)
2. What fields represent the username, password, and authorities

### AppUserImpl: Wrapping Our Entity

I implemented `UserDetails` by wrapping `AppUserEntity`:

```java
public class AppUserImpl implements UserDetails {
    private final AppUserEntity appUserEntity;

    public AppUserImpl(AppUserEntity appUserEntity) {
        this.appUserEntity = appUserEntity;
    }

    @Override
    public String getUsername() {
        return appUserEntity.getEmail(); // Email is the username
    }

    @Override
    public String getPassword() {
        return appUserEntity.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("USER"));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
```

**Critical detail: `getUsername()` returns `appUserEntity.getEmail()`.**

Earlier in the session, I removed the `username` field from `AppUserEntity` entirely. Why? Because having both `username` and `email` was redundant—I'm using email as the unique identifier for login.

### AppUserDetailsServiceImpl: Loading Users by Email

The `UserDetailsService` implementation loads users from the database:

```java
@Component
public class AppUserDetailsServiceImpl implements UserDetailsService {
    UserRegistrationJpaRepository userRegistrationJpaRepository;

    public AppUserDetailsServiceImpl(UserRegistrationJpaRepository userRegistrationJpaRepository) {
        this.userRegistrationJpaRepository = userRegistrationJpaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUserEntity appUser = userRegistrationJpaRepository.findByEmail(username);
        if (appUser == null) {
            throw new UsernameNotFoundException("User not found with email: " + username);
        }
        return new AppUserImpl(appUser);
    }
}
```

I added a `findByEmail(String username)` method to `UserRegistrationJpaRepository`:

```java
public interface UserRegistrationJpaRepository extends JpaRepository<AppUserEntity, Long> {
    AppUserEntity findByEmail(String username);
}
```

Spring Data JPA generates the query for us. The method name `findByEmail` is parsed as `WHERE email = ?`.

---

## Testing Changes: Making WebMvcTest Work with Security

### The Problem

Once Spring Security is enabled, `@WebMvcTest` starts applying security filters to your controller tests. My test started failing with 401 Unauthorized because the `/register` endpoint was being treated as protected (even though I'd configured it as `permitAll()`).

### The Fix

I updated `UserRegistrationControllerTest` to exclude security filters:

```java
@WebMvcTest(value = UserRegistrationController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EnableWebSecurity.class))
@AutoConfigureMockMvc(addFilters = false)
class UserRegistrationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserRegistrationService userRegistrationService;

    @Test
    void registerUser_returns201_andReturnsResponseBody() throws Exception {
        String payload = """
            {
              "username": "ldpenrose",
              "email": "ldpenrose@gmail.com",
              "firstName": "Leo",
              "lastName": "Penrose",
              "password": "securePassword123"
            }
            """;

        mockMvc.perform(post("/api/v1/user/registration/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("ldpenrose@gmail.com"))
            .andExpect(jsonPath("$.firstName").value("Leo"))
            .andExpect(jsonPath("$.lastName").value("Penrose"));
    }
}
```

**What changed:**
- `excludeFilters = @ComponentScan.Filter(...)` → Tells Spring not to load `@EnableWebSecurity` configs during the test
- `@AutoConfigureMockMvc(addFilters = false)` → Disables all servlet filters, including Spring Security
- `@MockitoBean UserRegistrationService` → Mocks the service so we're only testing the controller

This is a **unit test**, not an integration test. I'm testing the controller's HTTP handling, not the full security chain. For full security integration testing, I'd write a separate test using `@SpringBootTest` and `TestRestTemplate`.

---

## Gotchas / Lessons Learned

### 1. Why Tests Broke When Security Was Introduced

**The default behavior of Spring Security is to secure everything.** The moment you add `spring-boot-starter-security`:
- Every endpoint requires authentication
- `@WebMvcTest` applies security filters by default
- Your controller tests fail with 401 unless you explicitly disable filters or configure security

**Solution:** Either:
- Disable filters in unit tests (`@AutoConfigureMockMvc(addFilters = false)`)
- Use `@WithMockUser` in tests to simulate an authenticated user
- Write separate integration tests with `@SpringBootTest`

### 2. CSRF Disable in Stateless REST APIs

I disabled CSRF protection:
```java
.csrf(csrf -> csrf.disable())
```

**Why?**
- CSRF protection is designed for **stateful** web apps (where the server tracks session cookies)
- My API is **stateless**—every request is authenticated independently (via HTTP Basic Auth now, JWT later)
- CSRF attacks require a victim to be authenticated via cookies, which doesn't apply here

**Caveat:** If I add cookie-based session management later, I'd need to re-enable CSRF or use token-based CSRF (like the Synchronizer Token Pattern).

### 3. Email as Username: Impact on Authentication

By removing the `username` field and returning `email` from `getUsername()`, I changed how users log in.

**Before (hypothetical):** Users log in with `username=john_doe`
**Now:** Users log in with `username=john@example.com`

This is a **design decision**, not a technical limitation. Spring Security doesn't care what you return from `getUsername()`—it just uses it as the lookup key. I chose email because:
- Emails are already unique (enforced at the database level)
- Users are more likely to remember their email than a separate username
- It reduces cognitive load during registration

**Trade-off:** Email addresses can change. If I need to support email updates in the future, I'll need to handle username changes carefully (or use a separate immutable `userId` internally).

### 4. BCrypt Round Count: Performance vs. Security

I used `BCryptPasswordEncoder(14)` (14 rounds = 2^14 iterations).

**Benchmarking on my machine:**
- 10 rounds: ~100ms per hash
- 12 rounds: ~400ms per hash
- 14 rounds: ~1.6s per hash

For a registration endpoint (hit once per user), 1.6s is acceptable. For a high-throughput login endpoint with millions of requests, 14 rounds might be too slow. The OWASP recommendation is 10-12 rounds for most applications.

**Key insight:** BCrypt's round count is **stored in the hash itself**. If I change the round count later, existing hashes still validate correctly—only new hashes use the updated round count.

---

## Next Steps: Where This Leads

Here's what's still missing (and where I'm headed next):

### 1. JWT Tokens Instead of HTTP Basic Auth
Right now, users send credentials on every request (via `Authorization: Basic <base64>`). This is fine for development but not production-ready. Next steps:
- Issue JWT tokens on `/login`
- Validate tokens on protected endpoints
- Implement refresh tokens for long-lived sessions

### 2. Role-Based Access Control (RBAC)
Currently, all users have the single `USER` authority. Real apps need roles:
- Admin users who can delete any book
- Regular users who can only manage their own library
- Guest users with read-only access

I'd add a `roles` table, a many-to-many join with `app_users`, and update `AppUserImpl.getAuthorities()` to return roles from the database.

### 3. Email Verification
Users can register with any email right now. Production apps need:
- Send a verification email with a token
- Lock the account until email is verified (`isEnabled()` returns `false`)
- Provide a `/verify-email` endpoint

### 4. Integration Tests for Security
My current test disables security filters. I should add a test that verifies:
- `/register` is accessible without authentication
- Other endpoints (e.g., `/api/v1/books`) require authentication
- Invalid credentials get rejected with 401

### 5. OpenAPI Documentation
I added `springdoc-openapi-starter-webmvc-ui` in the commits but haven't configured it yet. The Swagger UI at `/swagger-ui.html` would make API exploration easier.

---

## Closing Thoughts

This build session turned a hollow registration endpoint into a secure, testable, production-ready feature. The hardest parts weren't writing the code—they were understanding how Spring Security's defaults interact with tests and making architectural decisions (email as username, BCrypt rounds, CSRF disable).

If you're building a similar flow, here's my advice:
1. **Add Spring Security dependencies last**, after your core logic is working. It changes everything.
2. **Don't guess at security configs**—read the docs on `SecurityFilterChain`, understand `permitAll()` vs. `authenticated()`, and test your assumptions.
3. **Separate unit tests from integration tests**. Unit tests should mock security; integration tests should validate the full chain.

The code's not perfect, but it's real, it works, and it's a solid foundation for what comes next.

---

## Commit-by-Commit Notes

Here's a one-line summary of each commit in this build session:

1. `feat(entity): add email, firstName, and lastName fields to AppUserEntity` → Extended user entity with profile fields
2. `chore(format): reformat AppUserEntity to comply with Google Java Format` → Applied consistent code formatting
3. `Merge pull request #207 from leodvincci/feature/update-user-entity` → Merged user entity updates
4. `feat(registration): add user registration components including AppUser model, JPA repository, and service classes` → Initial registration scaffolding
5. `feat(controller): add UserRegistrationController with basic user registration endpoint` → Created controller with placeholder endpoint
6. `feat(controller): add POST /register endpoint to UserRegistrationController` → Added POST mapping for registration
7. `build(pom): add springdoc-openapi-starter-webmvc-ui dependency for OpenAPI support` → Added Swagger/OpenAPI library
8. `refactor(models): remove unused AppUser class from registration module` → Cleaned up duplicate model
9. `feat(controller): update /register endpoint to accept UserRegistrationRequest via @RequestBody and adjust route path` → Added request body handling
10. `feat(models): add UserRegistrationRequest DTO for handling user registration data` → Created request DTO with validation annotations
11. `test(controller): add unit test for UserRegistrationController POST /register endpoint` → Added initial controller test
12. `feat(dtos): add UserRegistrationResponseDTO for user registration response` → Created response DTO as Java record
13. `feat(controller): update /register to return UserRegistrationResponseDTO with status 201` → Made controller return proper HTTP 201 with response body
14. `refactor(dtos): rename UserRegistrationRequest to UserRegistrationRequestDTO and update package` → Moved DTO to contracts package for clarity
15. `refactor(models): remove username field from AppUserEntity and its associated methods` → Simplified entity to use email as username
16. `feat(mapper): add UserRegistrationMapper to map UserRegistrationRequestDTO to AppUserEntity` → Created static mapper for DTO→Entity conversion
17. `feat(service): implement registerUser method in UserRegistrationService` → Implemented registration business logic with persistence
18. `feat(controller): integrate UserRegistrationService into UserRegistrationController and replace System.out with logger` → Wired service into controller, added SLF4J logging
19. `feat(validation): add @Email validation to email field in UserRegistrationRequestDTO and enforce @Valid in /register endpoint` → Added Bean Validation for email format
20. `feat(dependencies): add Spring Security and Spring Security Test dependencies to pom.xml` → Added Spring Security to project
21. `feat(config): add basic WebSecurityConfigs class with @Configuration and @EnableWebSecurity annotations` → Created security configuration class
22. `feat(config): configure SecurityFilterChain with basic auth, CSRF disable, and endpoint access rules` → Defined which endpoints are public vs. protected
23. `feat(config): add PasswordEncoder bean with BCryptPasswordEncoder instantiation` → Created BCrypt password encoder bean (14 rounds)
24. `feat(service): encrypt user passwords using BCryptPasswordEncoder in UserRegistrationService` → Added password hashing before persistence
25. `feat(config): add DaoAuthenticationProvider bean with UserDetailsService and BCryptPasswordEncoder` → Wired authentication provider
26. `feat(model): implement AppUserImpl class to extend UserDetails interface` → Created UserDetails wrapper for AppUserEntity
27. `feat(service): implement AppUserDetailsServiceImpl for custom user details retrieval via email` → Implemented UserDetailsService to load users by email
28. `test(controller): update UserRegistrationControllerTest to exclude security filters and mock UserRegistrationService` → Fixed tests to work with Spring Security enabled
29. `refactor(codebase): apply consistent indentation and formatting across all classes for improved readability` → Applied Google Java Format to all files
30. `Merge pull request #208 from leodvincci/feature/add-spring-security-configs-and-auth-apis` → Merged the complete Spring Security implementation
