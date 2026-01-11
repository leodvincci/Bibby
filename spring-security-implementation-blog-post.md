# Building Secure User Authentication in Spring Boot: A Deep Dive into Spring Security

## Why Authentication Security Can't Be an Afterthought

Picture this: you've built a beautiful library management system, complete with book catalogs, user profiles, and borrowing features. Everything works perfectly in your local environment. Then comes the question every developer dreads: "How do we handle user authentication and keep passwords secure?"

This is where I found myself with Bibby, my personal library management application. As a developer transitioning into software engineering, I knew that implementing authentication from scratch would be both a security nightmare and a reinvention of a very complex wheel. Enter Spring Security—a framework that's powerful but notoriously intimidating for newcomers.

In this post, I'll walk you through how I implemented secure user registration and authentication in Bibby using Spring Security, BCrypt password hashing, and Spring's authentication architecture. If you're learning Spring Security or building your portfolio, this real-world implementation will give you practical insights into how these pieces fit together.

## What We're Building

Before diving into code, let's clarify what we're implementing:

1. **User Registration Endpoint**: A public REST API where users can create accounts with email, password, first name, and last name
2. **Password Encryption**: Secure password storage using BCrypt with a configurable cost factor
3. **Custom Authentication**: Email-based authentication instead of the traditional username approach
4. **Security Configuration**: HTTP Basic authentication for protected endpoints while keeping registration public
5. **Test Infrastructure**: Unit tests that work correctly with Spring Security's filter chain

The goal? Create a production-ready authentication system that follows Spring Security best practices while keeping the code clean and maintainable.

## The Foundation: Adding Spring Security Dependencies

First things first—we need to add Spring Security to our project. In `pom.xml`, I added two key dependencies:

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

The moment you add `spring-boot-starter-security`, Spring Boot automatically secures all endpoints with HTTP Basic authentication and generates a default password at startup. This is great for quick protection but not suitable for production where we need custom user storage and authentication logic.

## Step 1: The Data Model—AppUserEntity

Every authentication system needs to store user information. Here's my JPA entity (`AppUserEntity.java:1`):

```java
@Entity
public class AppUserEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String password;
  private String email;
  private String firstName;
  private String lastName;

  // Constructors, getters, and setters...
}
```

A few design decisions worth noting:

- **Email as identifier**: I opted to remove the traditional `username` field and use email as the unique identifier. Most modern applications do this because users find it more intuitive.
- **Password storage**: This field will store the BCrypt-hashed password, never the plain text.
- **Simple structure**: For this phase, I'm keeping it minimal. Features like email verification, roles, and account status flags come later.

The corresponding JPA repository (`UserRegistrationJpaRepository.java:6`) includes a critical custom query method:

```java
public interface UserRegistrationJpaRepository extends JpaRepository<AppUserEntity, Long> {
  AppUserEntity findByEmail(String username);
}
```

## Step 2: Implementing UserDetails—Bridging Our Model to Spring Security

Spring Security doesn't work directly with our `AppUserEntity`. Instead, it expects objects that implement the `UserDetails` interface. This is where `AppUserImpl` comes in (`AppUserImpl.java:9`):

```java
public class AppUserImpl implements UserDetails {
  private final AppUserEntity appUserEntity;

  public AppUserImpl(AppUserEntity appUserEntity) {
    this.appUserEntity = appUserEntity;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton(new SimpleGrantedAuthority("USER"));
  }

  @Override
  public String getPassword() {
    return appUserEntity.getPassword();
  }

  @Override
  public String getUsername() {
    return appUserEntity.getEmail();  // Email is our username
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

This adapter pattern is elegant—it wraps our entity and translates it into Spring Security's language:

- **getUsername()**: Returns the email instead of a traditional username
- **getAuthorities()**: Currently returns a simple "USER" role. In a real application, you'd fetch roles from the database
- **Account status methods**: All return `true` for now, but these hooks let you implement features like account expiration or locking

## Step 3: Loading Users—UserDetailsService Implementation

Spring Security needs a way to load user details during authentication. That's the job of `UserDetailsService`. Here's my implementation (`AppUserDetailsServiceImpl.java:12`):

```java
@Component
public class AppUserDetailsServiceImpl implements UserDetailsService {
  private final UserRegistrationJpaRepository userRegistrationJpaRepository;

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

When a user tries to authenticate, Spring Security calls `loadUserByUsername()` with the provided identifier (in our case, an email). We query the database, and if found, wrap the entity in our `AppUserImpl` adapter. If not found, we throw `UsernameNotFoundException`, which Spring Security handles gracefully by denying access.

## Step 4: The Security Configuration—Putting It All Together

The heart of our security setup lives in `WebSecurityConfigs` (`WebSecurityConfigs.java:16`):

```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfigs {
  private final UserDetailsService userDetailsService;

  public WebSecurityConfigs(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(authorize ->
            authorize
                .requestMatchers("/api/v1/user/registration/register").permitAll()
                .anyRequest().authenticated())
        .csrf(csrf -> csrf.disable())
        .formLogin(Customizer.withDefaults())
        .logout(Customizer.withDefaults())
        .httpBasic(Customizer.withDefaults());
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(14);
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider =
        new DaoAuthenticationProvider(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }
}
```

Let's break down each component:

### SecurityFilterChain

This configures Spring Security's filter chain—the series of filters that intercept and process every HTTP request:

- **Request authorization**: The registration endpoint (`/api/v1/user/registration/register`) is public (`permitAll()`), while all other endpoints require authentication
- **CSRF disabled**: For a REST API consumed by separate frontends, CSRF protection is typically disabled. If you're using session-based auth with server-rendered pages, keep CSRF enabled
- **Multiple auth mechanisms**: I've enabled form login (for potential web UI), logout handling, and HTTP Basic (useful for API testing with tools like Postman or curl)

### PasswordEncoder Bean

```java
@Bean
public PasswordEncoder passwordEncoder() {
  return new BCryptPasswordEncoder(14);
}
```

This is crucial for security. BCrypt is a password-hashing algorithm specifically designed to be slow, making brute-force attacks computationally expensive. The parameter `14` is the cost factor—higher numbers mean more security but slower hashing. The default is 10, but I chose 14 for better security. Each increment doubles the computation time.

### DaoAuthenticationProvider

```java
@Bean
public DaoAuthenticationProvider authenticationProvider() {
  DaoAuthenticationProvider authProvider =
      new DaoAuthenticationProvider(userDetailsService);
  authProvider.setPasswordEncoder(passwordEncoder());
  return authProvider;
}
```

This bean wires together our `UserDetailsService` and `PasswordEncoder`. When a user attempts to authenticate:

1. Spring Security extracts credentials from the request
2. Calls our `AppUserDetailsServiceImpl.loadUserByUsername()`
3. Uses the `PasswordEncoder` to verify the provided password against the stored hash
4. If successful, authentication proceeds; if not, access is denied

## Step 5: User Registration Flow

Now that authentication infrastructure is in place, let's implement registration. The flow starts with the controller (`UserRegistrationController.java:26`):

```java
@RestController
@RequestMapping("/api/v1/user/registration")
public class UserRegistrationController {
  private final UserRegistrationService userRegistrationService;
  private final Logger logger = LoggerFactory.getLogger(UserRegistrationController.class);

  @PostMapping("/register")
  public ResponseEntity<UserRegistrationResponseDTO> registerUser(
      @Valid @RequestBody UserRegistrationRequestDTO userRegistrationRequestDTO) {

    userRegistrationService.registerUser(userRegistrationRequestDTO);

    UserRegistrationResponseDTO response = new UserRegistrationResponseDTO(
        userRegistrationRequestDTO.getEmail(),
        userRegistrationRequestDTO.getFirstName(),
        userRegistrationRequestDTO.getLastName());

    logger.info("Registering user: {}", response.email());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
```

Key points:

- **@Valid annotation**: Triggers Jakarta Bean Validation on the incoming DTO
- **HTTP 201 Created**: Proper REST semantics for resource creation
- **Response DTO**: Never return passwords in responses, even hashed ones

The request DTO (`UserRegistrationRequestDTO.java:6`) includes validation:

```java
public class UserRegistrationRequestDTO {
  @NotBlank
  private String username;

  private String password;

  @Email
  private String email;

  private String firstName;
  private String lastName;

  // Getters and setters...
}
```

The `@Email` annotation ensures the email format is valid, while `@NotBlank` prevents empty usernames.

### The Service Layer—Where Password Hashing Happens

The `UserRegistrationService` (`UserRegistrationService.java:8`) handles the business logic:

```java
@Service
public class UserRegistrationService {
  private final UserRegistrationJpaRepository userRegistrationJpaRepository;
  private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(14);

  @Transactional
  public void registerUser(UserRegistrationRequestDTO userRegistrationRequestDTO) {
    AppUserEntity appUserEntity = UserRegistrationMapper.toEntity(userRegistrationRequestDTO);
    appUserEntity.setPassword(bcrypt.encode(userRegistrationRequestDTO.getPassword()));
    userRegistrationJpaRepository.save(appUserEntity);
  }
}
```

The critical line is password hashing:

```java
appUserEntity.setPassword(bcrypt.encode(userRegistrationRequestDTO.getPassword()));
```

This takes the plain text password from the request and transforms it into a BCrypt hash before persisting to the database. BCrypt automatically handles salt generation—you don't need to manage salts separately.

The `UserRegistrationMapper` is a simple utility class that maps DTO to entity:

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

## Testing with Spring Security

Testing controllers with Spring Security requires special attention. Here's my test (`UserRegistrationControllerTest.java:24`):

```java
@WebMvcTest(
    value = UserRegistrationController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = EnableWebSecurity.class))
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
          "lastName": "Penrose"
        }
        """;

    mockMvc.perform(
        post("/api/v1/user/registration/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("ldpenrose@gmail.com"))
        .andExpect(jsonPath("$.firstName").value("Leo"))
        .andExpect(jsonPath("$.lastName").value("Penrose"));
  }
}
```

The annotations `excludeFilters` and `@AutoConfigureMockMvc(addFilters = false)` are crucial—they disable Spring Security filters during testing. For unit tests focused on controller logic, you don't want the security layer interfering. For integration tests that verify security rules, you'd remove these annotations and use `@WithMockUser` or similar annotations.

## What I Learned and Gotchas

### 1. BCrypt Instances Matter

Initially, I created separate `BCryptPasswordEncoder` instances in `WebSecurityConfigs` and `UserRegistrationService`. This works, but it's cleaner to inject the bean to ensure consistency:

```java
// Better approach (future refactor)
@Service
public class UserRegistrationService {
  private final PasswordEncoder passwordEncoder;  // Injected, not instantiated

  public UserRegistrationService(PasswordEncoder passwordEncoder, ...) {
    this.passwordEncoder = passwordEncoder;
  }
}
```

### 2. Email vs. Username

The Spring Security interface method is still named `loadUserByUsername()`, even though I'm using email. The "username" here is conceptual—it's just the user's unique identifier. Don't let the naming confuse you.

### 3. CSRF and REST APIs

Disabling CSRF for REST APIs is standard practice when using stateless authentication (like JWT or API keys). However, if you're using session-based auth, **keep CSRF protection enabled** and handle CSRF tokens properly in your frontend.

### 4. Testing Complexity

Spring Security adds layers to your application, which means testing requires more setup. Understanding when to include or exclude security filters in tests is essential for maintainable test suites.

## Architectural Trade-offs and Future Improvements

### Current Limitations

1. **No duplicate email checking**: The registration service doesn't verify if an email already exists. In production, this should return a 409 Conflict.

2. **No email verification**: Users can register with any email. A real application needs email confirmation tokens.

3. **Simple role model**: Everyone gets the "USER" role. Most applications need hierarchical roles (USER, ADMIN, MODERATOR) stored in the database.

4. **No password validation**: Beyond basic presence, there's no enforcement of password complexity rules.

5. **Error handling**: The controller doesn't handle validation errors gracefully. A `@ControllerAdvice` would improve the API's error responses.

### Next Steps

My roadmap for improving this authentication system includes:

1. **JWT authentication**: Replacing HTTP Basic with JWT tokens for stateless API authentication
2. **Refresh token mechanism**: Long-lived refresh tokens with short-lived access tokens
3. **Email verification**: Sending confirmation emails with expiring tokens
4. **Password reset flow**: Secure password recovery via email
5. **Role-based authorization**: Implementing `@PreAuthorize` annotations for fine-grained access control
6. **Account management**: Endpoints for updating profile info and changing passwords
7. **Rate limiting**: Protecting against brute-force attacks on the login endpoint

## Conclusion

Implementing Spring Security properly requires understanding several interconnected concepts: the `UserDetails` contract, `UserDetailsService` for loading users, `PasswordEncoder` for secure hashing, and `SecurityFilterChain` for request authorization. While it feels like a lot of boilerplate initially, this architecture provides a robust, extensible foundation for authentication.

The key takeaway? Don't roll your own authentication system. Spring Security has been battle-tested across thousands of applications and provides security guarantees that would take years to implement correctly from scratch. Yes, the learning curve is steep, but the investment pays off in security and maintainability.

If you're building a portfolio or learning Spring Boot, implementing authentication properly demonstrates to potential employers that you understand both framework conventions and security fundamentals—skills that are crucial for production applications.

The complete implementation is available in my [Bibby repository](https://github.com/leodvincci/Bibby) under the commits from January 11, 2026. Feel free to explore the code, and if you have questions or suggestions, I'm always learning and would love to hear your feedback.

---

*Leo Penrose is a developer transitioning into software engineering, documenting the journey of building real-world applications. Follow along at [github.com/leodvincci](https://github.com/leodvincci).*
