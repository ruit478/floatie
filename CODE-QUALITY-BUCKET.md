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

### 6. ~~Login catches `Exception` too broadly~~ **FIXED**
**File:** `AuthController.java:104-108`
- Any unexpected exception (DB failure, JWT error) is swallowed and returned as "Invalid username or password" (401), masking real failures.

### 7. ~~No guard against interacting with a dead pet~~ **FIXED**
**File:** `PetService.java:290-355`
- Feed, play, rest, clean, heal, sleep, and wake all proceed on a `DEAD` pet. Should return `Optional.empty()` early.

### 8. ~~Email is effectively optional at registration~~ **FIXED**
**File:** `AuthRequest.java:13-14`
- `@Email` passes for `null` — no `@NotNull`/`@NotBlank` on the field. Users can register without providing an email.

### 9. Password policy has no complexity requirements **WONT IMPLEMENT**
**File:** `AuthRequest.java:16-18`
- Only `@Size(min=6)` enforced. No digit, uppercase, or special-character requirement.

### 10. No rate limiting on `/auth/login` **WONT IMPLEMENT**
**Files:** `SecurityConfig.java`, `AuthController.java`
- The login endpoint has no brute-force protection.

### 11. ~~Race condition in `replacePetForUsername()`~~ **FIXED**
**File:** `PetService.java:549-553`
- Old pet is deleted then new one created. A concurrent `getPetInfo` arriving between delete and create gets a 404.

### 12. ~~Redundant second `save()` in pet creation~~ **FIXED**
**File:** `PetService.java:159-162`
- The entity is already managed after the first save; the second `save()` is unnecessary.

### 13. Auth endpoints lack `/api/v1/` prefix **WONT IMPLEMENT**
**File:** `AuthController.java:23`
- Uses `@RequestMapping("/auth")` while all other controllers use `/api/v1/...`. Clients must handle two base paths.

### 14. ~~Frontend — login/register loading spinner is dead code~~ **FIXED**
**File:** `login.component.ts:36-55`, `register.component.ts:35-51`
- `toSignal` + `switchMap` never emits `{loading: true}` because the inner observable emits only on completion. The spinner never activates.

### 15. ~~Frontend — `deleteAccount()` silently swallows errors~~ **FIXED**
**File:** `game.component.ts:141-143`
- `.subscribe()` without an error handler gives zero feedback on failure, leaving the user stranded in the modal.

### 16. Frontend — production API URL is a placeholder **WONT IMPLEMENT**
**File:** `environment.prod.ts:3`
- Points to `api.floatie.example.com`. Deploying without replacing silently breaks all API calls.

### 17. ~~Frontend — `doAction()` subscriptions never cleaned up~~ **FIXED**
**File:** `game.component.ts:98-112`
- HTTP action subscriptions lack `takeUntilDestroyed()`. If a response arrives after the component is destroyed, signals are set on a disposed context.

### 18. Frontend — `passwordMatchValidator` mutates child control instead of returning group error **WONT IMPLEMENT**
**File:** `register.component.ts:64-77`
- Side-effects `confirmPassword.setErrors()` rather than returning `{passwordMismatch: true}` on the group, violating Angular's validation contract.

### 19. Frontend — only one test file for ten source files **WONT IMPLEMENT**
**Files:** `src/app/` (only `app.component.spec.ts` exists)
- Zero coverage for auth.service, pet.service, toast.service, auth.guard, auth.interceptor, or any component beyond AppComponent.

### 20. ~~Frontend — no scroll lock on death modal~~ **FIXED**
**File:** `death-modal.component.css:2-9`
- The `position: fixed` overlay does not prevent background scrolling. Users can scroll content behind the modal.

---

## New scan findings (2026-05-12)

### 21. ~~Account deletion: filesystem delete before DB delete~~ **FIXED**
**File:** `AccountController.java:38-42`
- Sprite file was deleted from disk before `userRepository.delete(user)`. If the DB delete failed and rolled back, the sprite file was permanently lost. Fixed by saving pet ID, deleting DB record first, then deleting the file.

### 22. ~~Orphaned sprite files on transaction rollback~~ **FIXED**
**File:** `PetService.java:176-189`
- `generateAndSaveSprite()` wrote the PNG directly. If `ImageIO.write()` threw mid-write or the transaction rolled back, a corrupt/orphaned PNG remained on disk. Fixed by writing to a `.tmp` file first then renaming to `.png`.

### 23. ~~Frontend — open redirect via `returnUrl` query parameter~~ **FIXED**
**File:** `login.component.ts:64`
- `this.route.snapshot.queryParams['returnUrl']` was used directly in `router.navigate()` after login. An attacker could craft `/login?returnUrl=https://evil.com`. Fixed by validating the value starts with `/` and not `//`.

### 24. ~~Frontend — `doAction()` silently swallows errors~~ **FIXED**
**File:** `game.component.ts:108-109`
- The `.catch()` cleared the spinner but showed no user-facing feedback. The user had no way to know an action failed. Fixed by adding a `toastService.show()` call in the catch handler.

### 25. JWT filter catches `Exception` too broadly, loses stack trace
**File:** `JwtAuthenticationFilter.java:44-46`
- `catch (Exception e)` swallows the specific JWT failure reason (expired vs. malformed vs. bad signature). Line 45 logs only `e.getMessage()` without passing `e` to the logger, so the stack trace is discarded.

### 26. ~~Dead paths in `shouldNotFilter`~~ FIXED
**File:** `JwtAuthenticationFilter.java:68-69`
- `/api/hello` and `/api/status` are whitelisted from JWT auth but neither endpoint exists. If a future developer adds `/api/hello-world`, it would unintentionally bypass authentication due to `startsWith`.

### 27. ~~Authenticated endpoint that ignores user identity~~ **FIXED**
**File:** `SpriteController.java:26`, `SecurityConfig.java:43`
- `POST /api/v1/pet/sprite/generate` required a valid JWT but generated a stateless random image unrelated to the caller. Fixed by adding the endpoint to `permitAll()` in SecurityConfig.

### 28. ~~`@Transactional` on delegating method instead of the worker~~ **FIXED**
**File:** `PetService.java:260-283`
- `getPetInfo()` does the real work (reads entity, applies decay, saves) but had no `@Transactional`. `getPetStatus()` had `@Transactional` but merely delegates. Fixed by moving the annotation to `getPetInfo()`.

### 29. Swallowed IOException in sprite deletion **WONT IMPLEMENT**
**File:** `PetService.java:541`
- `deleteSpriteForPet()` catches `IOException`, logs at WARN without the exception object, and continues. Callers assume the file was deleted. Undeletable files accumulate silently.

### 30. Dead pet returns 404, indistinguishable from "no pet" **WONT IMPLEMENT**
**File:** `PetController.java:29-34`
- Both "you have no pet" and "your pet is dead" return 404. Clients can't tell the difference without a separate `/info` call. Consider 410 Gone or 400 with a descriptive body.

### 31. ~~`canRest` missing `isAsleep` guard~~ **FIXED**
**File:** `game.component.ts:73`
- `canFeed`, `canPlay`, and `canClean` all check `!p.isAsleep`. `canRest` did not, so the Rest button was clickable while the pet is sleeping. Fixed by adding `!p.isAsleep` to the `canRest` computed.

### 32. ~~`loadPet()` subscription not cleaned up on destroy~~ **FIXED**
**File:** `game.component.ts:79-86`
- Used `firstValueFrom` without guarding against destroyed component. Fixed by injecting `DestroyRef` and checking `destroyed` before setting signals.

### 33. ~~`app.component.html` is dead code with a stray `</div>`~~ **FIXED**
**File:** `app.component.html` (deleted)
- `app.component.ts` uses inline `template`, not `templateUrl`. The HTML file was never loaded. Deleted.

### 34. ~~Unused `CommonModule` imports in 3 components~~ **FIXED**
**Files:** `game.component.ts`, `login.component.ts`, `register.component.ts`
- All three use `@if`/`@for` control flow (not `*ngIf`/`*ngFor`), so `CommonModule` was unnecessary. Removed.

### 35. ~~Toast has no `aria-live` or `role`~~ **FIXED**
**File:** `toast.component.html:3`
- Added `role="alert"` and `aria-live="polite"` to toast elements so screen readers announce them.

### 36. ~~Death modal has no focus trap or Escape handler~~ **FIXED**
**File:** `death-modal.component.html`, `death-modal.component.ts`
- Added `role="dialog"`, `aria-modal="true"`, Escape key handler, focus trap (Tab cycling), and auto-focus on the first button.

### 37. ~~Action buttons lack `focus-visible` outlines~~ **FIXED**
**File:** `game.component.css`
- Added `.action-btn:focus-visible` rule with a visible outline for keyboard navigation.

### 38. ~~Death modal buttons missing `type="button"`~~ **FIXED**
**File:** `death-modal.component.html:8,11`
- Added `type="button"` to both "Generate new pet" and "Delete account" buttons.

### 39. Login username has no `maxLength` (register form has it) **WONT IMPLEMENT**
**File:** `login.component.ts:60`
- Username has `minLength(3)` but no `maxLength`. The register form enforces `maxLength(50)`. Inconsistent validation.

### 40. ~~`console.log` leaks error body~~ **FIXED**
**File:** `auth.service.ts:83`
- `console.log('Error body:', error.error)` logged the full backend response body. Removed.

### 41. ~~Bootstrap failure shows blank page~~ **FIXED**
**File:** `main.ts:6`
- `.catch((err) => console.error(err))` logged the error but showed a white page. Added a fallback HTML message in the catch handler.
