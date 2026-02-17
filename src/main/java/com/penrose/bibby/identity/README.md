# Identity Bounded Context

## Purpose

The **Identity** bounded context manages **user identity and authentication** in Bibby. It owns user accounts, credentials, registration, and authentication concerns. This context answers questions like "Who is this user?", "Can they log in?", and "Is this email already registered?"

------

## Modules

### User Registration (`api/`, `core/`, `infrastructure/`)

Manages user account creation and credential storage with encrypted passwords.

**Domain model:**
- User identity represented via `AppUserEntity` with `id`, `email`, and encrypted `password`

**Public API (commands and results):**
- `RegisterUserCommand` (record) — `email`, `password` (plaintext input)
- `RegisterUserResult` (record) — `userId`, `email` (confirmation output)
- `RegisterUserRequestDTO` (record) — HTTP layer DTO for registration requests
- `RegisterUserResponseDTO` (record) — HTTP layer DTO for registration responses

**Application services:**
- `UserRegistrationService` — handles user registration flow, encrypts passwords with BCrypt (strength 14), persists users
  - `registerUser(RegisterUserCommand)` → `RegisterUserResult`
- `AppUserDetailsServiceImpl` — Spring Security integration (UserDetailsService implementation)

**Infrastructure:**
- `AppUserEntity` — JPA entity for user persistence with `@Id`, `email`, and `password` fields
- `UserRegistrationJpaRepository` — Spring Data JPA repository
- `AppUserMapper` — maps between command/entity layers
- `AppUserImpl` — UserDetails adapter for Spring Security

------

## Key Rules / Invariants

- Passwords are encrypted using BCrypt with a work factor of 14 before storage
- User emails must be unique (enforced at persistence layer)
- All password inputs are plaintext in commands; encryption happens in the service layer before persistence
- User registration is a stateless operation returning an immutable result

------

## Ubiquitous Language

- **User** — an authenticated account holder in Bibby
- **Email** — the primary identifier for user accounts
- **Password** — user credential, always encrypted before storage
- **Registration** — the process of creating a new user account
- **BCrypt** — the cryptographic hash function used for password encryption

------

## Out of Scope

- Authorization and role-based access control (future enhancement)
- Password reset and email verification flows
- OAuth/social login integration
- User profile management beyond email

------

## Package Layout

```
identity/
├── api/
│   ├── commands/              # RegisterUserCommand
│   ├── dtos/                  # RegisterUserRequestDTO, RegisterUserResponseDTO
│   └── results/               # RegisterUserResult
├── core/
│   ├── application/           # UserRegistrationService
│   └── AppUserDetailsServiceImpl.java
└── infrastructure/
    ├── entity/                # AppUserEntity
    ├── mapping/               # AppUserMapper
    ├── repository/            # UserRegistrationJpaRepository
    └── AppUserImpl.java       # Spring Security UserDetails adapter
```

------

## Example Flows

- **Register a new user**
    1. Client submits `RegisterUserRequestDTO` with email and plaintext password
    2. Web layer converts to `RegisterUserCommand`
    3. `UserRegistrationService.registerUser(command)` encrypts password with BCrypt
    4. Saves `AppUserEntity` via `UserRegistrationJpaRepository`
    5. Returns `RegisterUserResult` with user ID and email
- **Authenticate a user (Spring Security integration)**
    1. `AppUserDetailsServiceImpl.loadUserByUsername(email)` queries repository
    2. Returns `AppUserImpl` (UserDetails) with encrypted password for Spring Security validation
