# Code Quality Bucket List — Floatie

Remaining items for a future session (all fixes from the 2026-05-11 sweep were applied; these were deferred).

---

## Before release

### 1. ~~`application-local.properties` committed to Git with secrets~~ **WONT IMPLEMENT**
**File:** `backend/src/main/resources/application-local.properties`
- Contains `jwt.secret` and `spring.datasource.password`
- Not listed in `.gitignore`
- Rotate secrets and add `**/application-local.properties` to `.gitignore` before any public push

### 2. ~~Empty JWT secret default breaks startup when env var is unset~~ **WONT IMPLEMENT**
**Files:** `application.properties:11`, `JwtUtil.java:23-24`
- `jwt.secret=${JWT_SECRET:}` defaults to empty string → `Keys.hmacShaKeyFor()` throws at startup
- Add a non-empty dev default or throw a clear error on blank secret

---

## When ready

### 3. ~~No unique constraint on `User.email`~~ **WONT IMPLEMENT**
**File:** `User.java:25-27`
- `@Column` has no `unique = true`; no `existsByEmail()` check in registration
- Duplicate emails silently stored

### 4. ~~No `type="button"` on action buttons~~ **WONT IMPLEMENT**
**File:** `game.component.html` (6 action buttons + logout button)
- Missing `type="button"` — if wrapped in a `<form>` they act as submit buttons

### 5. ~~E2E test hardcodes `localhost:4200`~~ **WONT IMPLEMENT**
**File:** `tests/petCreation.spec.js:21,59`
- Should use Playwright's `baseURL` config for portability across environments

---

## New scan findings (2026-05-11)

### 6. Login catches `Exception` too broadly
**File:** `AuthController.java:104-108`
- Any unexpected exception (DB failure, JWT error) is swallowed and returned as "Invalid username or password" (401), masking real failures.

### 7. No guard against interacting with a dead pet
**File:** `PetService.java:290-355`
- Feed, play, rest, clean, heal, sleep, and wake all proceed on a `DEAD` pet. Should return `Optional.empty()` early.

### 8. Email is effectively optional at registration
**File:** `AuthRequest.java:13-14`
- `@Email` passes for `null` — no `@NotNull`/`@NotBlank` on the field. Users can register without providing an email.

### 9. Password policy has no complexity requirements
**File:** `AuthRequest.java:16-18`
- Only `@Size(min=6)` enforced. No digit, uppercase, or special-character requirement.

### 10. No rate limiting on `/auth/login`
**Files:** `SecurityConfig.java`, `AuthController.java`
- The login endpoint has no brute-force protection.

### 11. Race condition in `replacePetForUsername()`
**File:** `PetService.java:549-553`
- Old pet is deleted then new one created. A concurrent `getPetInfo` arriving between delete and create gets a 404.

### 12. Redundant second `save()` in pet creation
**File:** `PetService.java:159-162`
- The entity is already managed after the first save; the second `save()` is unnecessary.

### 13. Auth endpoints lack `/api/v1/` prefix
**File:** `AuthController.java:23`
- Uses `@RequestMapping("/auth")` while all other controllers use `/api/v1/...`. Clients must handle two base paths.

### 14. Frontend — login/register loading spinner is dead code
**File:** `login.component.ts:36-55`, `register.component.ts:35-51`
- `toSignal` + `switchMap` never emits `{loading: true}` because the inner observable emits only on completion. The spinner never activates.

### 15. Frontend — `deleteAccount()` silently swallows errors
**File:** `game.component.ts:141-143`
- `.subscribe()` without an error handler gives zero feedback on failure, leaving the user stranded in the modal.

### 16. Frontend — production API URL is a placeholder
**File:** `environment.prod.ts:3`
- Points to `api.floatie.example.com`. Deploying without replacing silently breaks all API calls.

### 17. Frontend — `doAction()` subscriptions never cleaned up
**File:** `game.component.ts:98-112`
- HTTP action subscriptions lack `takeUntilDestroyed()`. If a response arrives after the component is destroyed, signals are set on a disposed context.

### 18. Frontend — `passwordMatchValidator` mutates child control instead of returning group error
**File:** `register.component.ts:64-77`
- Side-effects `confirmPassword.setErrors()` rather than returning `{passwordMismatch: true}` on the group, violating Angular's validation contract.

### 19. Frontend — only one test file for ten source files
**Files:** `src/app/` (only `app.component.spec.ts` exists)
- Zero coverage for auth.service, pet.service, toast.service, auth.guard, auth.interceptor, or any component beyond AppComponent.

### 20. Frontend — no scroll lock on death modal
**File:** `death-modal.component.css:2-9`
- The `position: fixed` overlay does not prevent background scrolling. Users can scroll content behind the modal.
