# MindBridge

A private, privacy-focused self-reflection and well-being app. It is a self-reflection tool,
not therapy, and it does not diagnose.

## Status
- Milestone 2: foundation (API, database, frontend shell, tests, CI).
- Milestone 3: authentication (register, login, logout, profile, preferences, account deletion).

## Run locally
1. `copy .env.example .env` and set a password.
2. `docker compose up -d db` (the database is exposed on host port 5433).
3. Backend: `cd backend`, `$env:DB_PASSWORD="<your password>"`, `mvn spring-boot:run`
4. Frontend: `cd frontend`, `npm install`, `npm run dev`, then open http://localhost:5173

## Tests
- Backend: `cd backend && mvn test` (Docker must be running)
- Frontend: `cd frontend && npm test`

## How authentication works
- Passwords are hashed with BCrypt (12 to 72 characters; BCrypt ignores anything past 72 bytes).
- Login creates a server-side session. The browser only holds an HttpOnly, SameSite=Lax cookie
  (`MBSESSION`); no token is readable by JavaScript. The session id changes on login (session fixation).
- CSRF protection: the server sets an `XSRF-TOKEN` cookie; the app echoes it in the `X-XSRF-TOKEN`
  header on every POST/PUT/DELETE. Requests without it get 403.
- Failed logins return one generic message, and 5 failures in 15 minutes for the same email and IP
  returns 429.
- The user id always comes from the session, never from the request.
- Deleting an account re-checks the password; the database cascades to the user's other rows.

## Known limitations
- Swagger UI is public in development and will be restricted before deployment.
- The login rate limiter is in memory (single instance). Behind a proxy it needs forwarded-IP
  configuration; both are tracked for the deployment milestone.
- Registering with an existing email returns 409, which reveals that the email has an account.
  Email verification will address this later.
- No password reset yet.
