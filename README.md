spring-boot-auth-api



A production-ready authentication and authorization REST API built with Spring Boot, Spring Security, JWT, and PostgreSQL.



Features:

\- Registration \& login: with BCrypt password hashing (strength 12)

\- JWT access tokens: (short-lived, 15 min default) signed with HMAC-SHA256

\- Opaque, DB-backed refresh tokens: (7 days default), rotated on every use, revocable server-side

\- :Role-based authorization: (`ROLE\_USER`, `ROLE\_MODERATOR`, `ROLE\_ADMIN`) enforced both at the URL level (`SecurityConfig`) and method level (`@PreAuthorize`)

\- Account lockout: after configurable failed login attempts, with automatic unlock after a cooldown

\- Clean JSON error responses: for validation failures, auth failures, and access-denied cases (no Spring default error pages)

\- Flyway-managed schema:  Hibernate never auto-generates DDL, so the schema is explicit and versioned

\- Stateless sessions: no server-side session state; CORS configured explicitly

\- \*\*OpenAPI/Swagger UI\*\* for interactive API docs



Tech Stack"

Java 17+, Spring Boot 4.1.1, Spring Security, Spring Data JPA, PostgreSQL 16, Flyway, jjwt, Lombok



Project Structure:

src/main/java/com/example/authapi/

├── config/          SecurityConfig, ApplicationConfig (beans)

├── controller/       AuthController, UserController, AdminController

├── dto/              request/ and response/ payloads

├── entity/           User, Role, RoleName, RefreshToken

├── exception/        Custom exceptions + GlobalExceptionHandler

├── repository/       Spring Data JPA repositories

├── security/         JwtService, JwtAuthenticationFilter, UserDetails, entry point/handler

└── service/          AuthService, RefreshTokenService, UserService



src/main/resources/

├── application.properties          base config (env-var driven)

└── db/migration/              Flyway SQL migrations



Getting Started:

run locally on intellij



1\. Start Postgres and create db

2\. Export the required environment variables (see `.env.example`), at minimum:

&#x20;  bash:

&#x20;   JWT\_SECRET=$(openssl rand -base64 64)

&#x20;   DB\_URL=jdbc:postgresql://localhost:5432/authdb

&#x20;   DB\_USERNAME=postgres

&#x20;   DB\_PASSWORD=your\_password

&#x20;  

3\. Build and run:

&#x20;

&#x20; 

&#x20;Flyway will create the schema and seed the three roles automatically on first boot.



Tests run against an in-memory H2 database (see `src/test/resources/application.yml`) — no external Postgres needed.



Reference:



| Method | Endpoint                      | Auth required | Description                                  |

|--------|-------------------------------|----------------|-----------------------------------------------|

| POST   | `/api/auth/register`          | No             | Create a new account (defaults to `ROLE\_USER`) |

| POST   | `/api/auth/login`             | No             | Exchange credentials for an access + refresh token |

| POST   | `/api/auth/refresh-token`     | No             | Exchange a valid refresh token for a new pair |

| POST   | `/api/auth/logout`            | Yes            | Revoke the caller's refresh token             |

| GET    | `/api/users/me`               | Yes            | Get the current user's profile                |

| PUT    | `/api/users/me/password`      | Yes            | Change password (revokes existing sessions)   |

| GET    | `/api/admin/dashboard`        | Yes (`ROLE\_ADMIN`) | Example admin-only endpoint             |







&#x20;Promoting a user to admin



There is intentionally no seeded admin account - nothing ships with default admin credentials.

Register a normal account, then grant it `ROLE\_ADMIN` directly in the database:



sql

INSERT INTO user\_roles (user\_id, role\_id)

SELECT u.id, r.id FROM users u, roles r

WHERE u.username = 'your\_username' AND r.name = 'ROLE\_ADMIN';





Design notes / trade-offs



\- Refresh tokens are opaque UUIDs stored in Postgres, not JWTs. This lets the server revoke them

&#x20; instantly (logout, password change, detected compromise) - something you can't cleanly do with a

&#x20; self-contained JWT refresh token without an extra revocation-list lookup anyway.

\- One active refresh token per user. Logging in again (or refreshing) invalidates the previous

&#x20; refresh token. This keeps "logout everywhere" trivial. If you need concurrent multi-device sessions,

&#x20; remove the `deleteByUser` call in `RefreshTokenService.createRefreshToken` and key tokens by

&#x20; `(user, device)` instead of a `@OneToOne` on `user`.

\- Account lockout is in-database, not in-memory\*\* (`failed\_attempts`, `lock\_time` on `users`), so it

&#x20; survives restarts and works correctly across multiple app instances behind a load balancer.

\- Hibernate `ddl-auto: validate`, not `update`.\*\* Schema changes go through Flyway migrations only —

&#x20; this is what you want once there's real data you can't afford to have Hibernate guess about.

\- CORS and JWT secret are environment-driven, not hardcoded, so the same image can move from

&#x20; dev to staging to prod without a rebuild.





