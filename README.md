# NeetCode Tracker

A backend service that schedules NeetCode 150 pattern-recall reviews using the
**FSRS-4.5** (Free Spaced Repetition Scheduler) algorithm — the same model used
by modern Anki. Built with Spring Boot, Postgres, and JWT auth.

This exists because NeetCode's own spaced-repetition reminders are a Pro-tier
feature behind a paywall, and the underlying algorithm isn't published. This
implementation uses FSRS's actual published parameters, so the scheduling math
is fully transparent and free.

## Stack

- Java 21 / Spring Boot 3.3
- Spring Data JPA + PostgreSQL
- Spring Security + JWT (stateless)
- JUnit 5 for the FSRS algorithm's unit tests
- Docker for deployment

## Architecture

```
Controller  → AuthController, ProblemController, ReviewController
Service     → FsrsService (pure scheduling math), ReviewService (persistence + FSRS), AppUserDetailsService
Repository  → UserRepository, ProblemRepository, ReviewCardRepository
Entity      → User, Problem, ReviewCard
```

`FsrsService` has zero Spring or persistence dependencies — it's plain math
(difficulty, stability, retrievability → next interval), which is what makes
it independently unit-testable in `FsrsServiceTest`.

## Running locally

1. Start Postgres (or use Docker):
   ```bash
   docker run --name neetcode-db -e POSTGRES_PASSWORD=postgres \
     -e POSTGRES_DB=neetcode_tracker -p 5432:5432 -d postgres:16
   ```

2. Set a real JWT secret (32+ random bytes) as an env var, or it'll fall back
   to a dev default:
   ```bash
   export JWT_SECRET="$(openssl rand -base64 48)"
   ```

3. Run the app:
   ```bash
   mvn spring-boot:run
   ```

The NeetCode 150 catalog seeds itself automatically on first boot via
`src/main/resources/data.sql`.

## Running tests

```bash
mvn test
```

`FsrsServiceTest` checks the algorithm's actual behavior — that repeated
"Good" ratings grow the interval, "Again" shrinks it and increments lapses,
"Easy" schedules further out than "Good", and difficulty stays within FSRS's
published 1–10 bounds.

## API reference

All endpoints are under `/api`. Auth endpoints and the plain problem list are
public; everything else requires `Authorization: Bearer <token>`.

| Method | Path                          | Description                                   |
|--------|-------------------------------|------------------------------------------------|
| POST   | `/api/auth/register`          | Create an account, returns a JWT               |
| POST   | `/api/auth/login`             | Log in, returns a JWT                          |
| GET    | `/api/problems`                | List all 150 problems (merges your progress if authenticated) |
| GET    | `/api/reviews/due`             | Cards due for review right now                 |
| POST   | `/api/reviews/{problemId}/solve` | Mark a problem solved, save your pattern note |
| PATCH  | `/api/reviews/{problemId}/note`  | Edit a saved note without affecting scheduling |
| POST   | `/api/reviews/{problemId}/rate`  | Submit a recall rating (1=Again, 2=Hard, 3=Good, 4=Easy) — reschedules via FSRS |

Example:
```bash
curl -X POST localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"joseph","password":"a-strong-password"}'

curl -X POST localhost:8080/api/reviews/3/rate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"rating": 3}'
```


