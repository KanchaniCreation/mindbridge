# MindBridge

A private, privacy-focused self-reflection and well-being app. It is a self-reflection tool,
not therapy, and it does not diagnose.

## Status
- Milestone 2: foundation (API, database, frontend shell, tests, CI).
- Milestone 3: authentication (register, login, logout, profile, preferences, account deletion).
- Milestone 4 (backend): private journal and mood tracker with search, filters, pagination and summaries.
  The React pages for these come next.

## Run locally
1. `copy .env.example .env` and set a password.
2. `docker compose up -d db` (the database is exposed on host port 5433).
3. Backend: `cd backend`, `$env:DB_PASSWORD="<your password>"`, `mvn spring-boot:run`
4. Frontend: `cd frontend`, `npm install`, `npm run dev`, then open http://localhost:5173

## Tests
- Backend: `cd backend && mvn test` (Docker must be running)
- Frontend: `cd frontend && npm test`

## API (all under /api/v1, all need a logged-in session and, for writes, the CSRF header)
| Method and path | Purpose |
|---|---|
| POST /journal | Create an entry (title and category optional, body required, max 10,000 characters) |
| GET /journal?q=&category=&page=&size= | List and search own entries (size capped at 100) |
| GET, PUT, DELETE /journal/{id} | Read, edit, delete one own entry (404 if not yours) |
| GET /journal/prompts | Guided journaling prompts |
| POST /moods | Mood check-in: moodValue 1 to 5, up to 5 emotions, up to 8 tags, optional note and recordedAt |
| GET /moods?from=&to=&tag=&page=&size= | List own check-ins |
| DELETE /moods/{id} | Delete one own check-in |
| GET /moods/summary?period=WEEK or MONTH&timezone= | Calculated summary of own check-ins |

## How the journal and mood features protect privacy
- Every query includes the user id from the session. Another user's entry id returns 404, the same as an id that does not exist.
- Lists return a short excerpt only; the full text is returned for a single entry.
- Journal text is never put in URLs except as an explicit search parameter sent over the user's own session, and it is not logged.
- Summaries are calculated from what the user recorded. With fewer than 5 check-ins, averages and direction are left empty instead of guessed. The summary says what was entered, not why.
- Deleting an account removes journal entries, mood entries, emotions and tags through database cascades (tested).

## How authentication works
- Passwords are hashed with BCrypt (12 to 72 characters; BCrypt ignores anything past 72 bytes).
- Login creates a server-side session. The browser only holds an HttpOnly, SameSite=Lax cookie
  (`MBSESSION`); no token is readable by JavaScript. The session id changes on login (session fixation).
- CSRF protection: the server sets an `XSRF-TOKEN` cookie; the app echoes it in the `X-XSRF-TOKEN`
  header on every POST/PUT/DELETE. Requests without it get 403.
- Failed logins return one generic message, and 5 failures in 15 minutes for the same email and IP
  returns 429.
- The user id always comes from the session, never from the request.

## Known limitations
- Swagger UI is public in development and will be restricted before deployment.
- The login rate limiter is in memory (single instance). Behind a proxy it needs forwarded-IP
  configuration.
- Registering with an existing email returns 409, which reveals that the email has an account.
- No password reset or email verification yet.
- Journal search uses simple case-insensitive matching (no full-text index yet); the characters % and _ are not escaped, which only affects the user's own search results.
- Journal text is readable by a database administrator (no end-to-end encryption).
- Repeated identical create requests are not yet deduplicated (idempotency keys are planned for the conversation milestone).
