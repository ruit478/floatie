# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

See `CODE-QUALITY-BUCKET.md` for a prioritized list of remaining issues — check it at the start of each session before writing new code.

## Project Overview

Floatie is a virtual pet application — users register, log in, and get a procedurally-generated pixel-art pet. The backend is Java 25 / Spring Boot 4.0; the frontend is Angular 21.2 with zoneless change detection and standalone components.

## Commands

### Backend (from `backend/`)

```bash
./gradlew bootRun                          # Start dev server (port 8080)
./gradlew test                             # Run all tests (unit + integration)
./gradlew test --tests "*ClassName"        # Run a single test class
./gradlew build -x test                    # Build without tests
```

The `bootRun` task accepts `-PjvmArgs=<args>` for JVM flags.

### Frontend (from `frontend/`)

```bash
npm start                  # ng serve (port 4200)
npm test                   # Karma unit tests (only app.component.spec.ts exists)
npm run test:pet           # Playwright E2E test (pet creation flow)
npm run build              # Production build
```

### Docker (from `backend/`)

```bash
docker compose up          # Postgres 16 + backend, both with health checks
```

## Architecture

### Backend (`backend/src/main/java/com/future/floatie/`)

**Auth flow:** Stateless JWT. `AuthController` handles `/auth/register`, `/auth/login` (public), and `DELETE /auth/account` (authenticated). All other endpoints require a `Bearer` token except for `/actuator/health` and `/api/v1/pet/sprite/generate`. `JwtAuthenticationFilter` extracts and validates the token on each request, skipping `/auth/register` and `/auth/login` only.

**Security:** `SecurityConfig` disables CSRF, sets session management to STATELESS, and configures CORS from `app.cors.allowed-origins`. Passwords are BCrypt (strength 12). Public paths: `/auth/register`, `/auth/login`, `/actuator/health`, `/api/v1/pet/sprite/generate`.

**Endpoints:**
- `AuthController` (`/auth`) — register, login (public); DELETE account (authenticated)
- `PetController` (`/api/v1/pet`) — `/info`, `/status` (GET); `/feed`, `/play`, `/rest`, `/clean`, `/heal`, `/sleep`, `/wake`, `/replace` (POST) — all authenticated
- `SpriteController` (`/api/v1/pet/sprite`) — `/generate` (POST, public) returns a random PNG sprite

**Entities:** `User` (UUID PK, username, email, passwordHash) has a 1:1 relationship with `Pet`. `Pet` holds subclass, colorHex, expression, lifeStage, 6 core stats (0-100), progression fields (xp, level, evolutionStage, evolutionPath), accessory fields (accessoryType, accessoryColorHex), bondLevel, isAsleep, and timestamp columns for decay calculations.

**Pet/sprite system:** `PetService.createPetForUser()` generates a random pet (species, color, name, expression, accessory) and saves a PNG sprite to disk via `PixelArtService`. The sprite is returned as base64 in `GET /api/v1/pet/info`. `PixelArtService` generates 128×128 pixel art from a 32×32 grid — each species has a template, colors are overlaid, expressions draw facial features (eyes/mouth), accessories render on top, and evolution paths apply visual modifications (glow, widen, or "neglected" patches).

**Enums:** `LifeStage` (EGG → BABY → CHILD → TEEN → ADULT → ELDER → DEAD), `EvolutionPath` (PERFECT, WELL_RAISED, NEGLECTED, OVERWEIGHT), `PetExpression` (HAPPY, SAD, MAD, SLEEPY, CONFUSED), `AccessoryType` (CROWN, COLLAR, BOW, GLASSES, NONE).

**Testing:** Integration tests extend `IntegrationTestBase` which provides a shared Testcontainers PostgreSQL container and registers all required Spring properties. Tests use `@SpringBootTest` + `@AutoConfigureMockMvc` and clean the database with `@BeforeEach`. Integration tests exist for auth, pet, and sprite controllers.

### Frontend (`frontend/src/app/`)

**Structure:** Components live in `components/` (game, login, register, death-modal, toast) with a `shared/field-error` reusable component. Services, guards, interceptors, and models are at the top level.

**Routing:** Three routes — `/login`, `/register` (both guarded by `GuestGuard` which redirects authenticated users to `/game`), and `/game` (guarded by `AuthGuard`). Default and wildcard redirect to `/login`.

**Auth:** `AuthService` stores JWT + username in `localStorage`, exposes a `BehaviorSubject<User|null>` as `currentUser$`, and provides `isAuthenticated()` which manually decodes the JWT payload to check expiry. The `authInterceptor` (functional) attaches `Authorization: Bearer <token>` to all requests except those to `/auth/`. On 401 responses it calls `logout()`. `deleteAccount()` calls `DELETE /auth/account` then clears local state.

**Game component:** Uses `inject()` with a discriminated union `PetState` (`loading` | `loaded` | `error`) stored in a signal. Pet data is fetched via `firstValueFrom` with `DestroyRef` guarding against updates after destroy. Actions (`feed`, `play`, `rest`, `clean`, `heal`, `toggleSleep`, `replacePet`) use `firstValueFrom` and check busy/dead/asleep guards before proceeding. `detectEvents()` compares old and new state to emit toast notifications for life stage, evolution, and level changes.

**Toast system:** `ToastService` manages a signal-based toast queue with auto-dismiss (4 seconds). Toast types: `info`, `success`, `warning`. Used for action feedback, lifecycle events, and error messaging.

**Component style:** All components are standalone, using functional guards (`AuthGuard`, `GuestGuard`) and a functional HTTP interceptor (`authInterceptor`). The app uses `provideZonelessChangeDetection()` and `provideBrowserGlobalErrorListeners()`.

**Environments:** `environment.ts` points `apiUrl` to `http://localhost:8080`; `environment.prod.ts` has a placeholder production URL.

**Testing:** One unit test file (`app.component.spec.ts`). E2E tests live in `tests/petCreation.spec.js` using Playwright.

### Docker / Infrastructure

`docker-compose.yml` (in `backend/`) defines two services: `postgres` (PostgreSQL 16-alpine with init SQL from `backend/src/main/resources/db/init.sql`) and `backend` (multi-stage Dockerfile using `eclipse-temurin:25-jdk-alpine` for build, `eclipse-temurin:25-jre-alpine` for runtime). The backend waits for the Postgres health check before starting.
