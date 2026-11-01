# MindBridge

A private, privacy-focused self-reflection and well-being app. It is a self-reflection tool,
not therapy, and it does not diagnose.

## Status
- Milestone 2: foundation (API, database, frontend shell, tests, CI).
- Milestone 3: authentication (register, login, logout, profile, preferences, account deletion).
- Milestone 4: private journal and mood tracker (API, search, filters, pagination, summaries, React pages).
- Milestone 5 (backend): pseudonymous one-to-one chat with invite codes, live updates (Server-Sent Events),
  block and report, rate limits. Mutual-consent identity reveal is the next milestone. The chat React pages come next.

## Run locally
1. `copy .env.example .env` and set a password.
2. `docker compose up -d db` (the database is exposed on host port 5433).
3. Backend: `cd backend`, `$env:DB_PASSWORD="<your password>"`, `mvn spring-boot:run`
4. Frontend: `cd frontend`, `npm install`, `npm run dev`, then open http://localhost:5173

## Tests
- Backend: `cd backend && mvn test` (Docker must be running)
- Frontend: `cd frontend && npm test`

## Chat API (all under /api/v1, session cookie required, CSRF header on writes)
| Method and path | Purpose |
|---|---|
| POST /chat/adult-confirmation | Record the user's self-declared "I am 18 or older" (required for chat) |
| POST /chat/invites | Create a single-use invite code (valid 24 hours, at most 5 unused) |
| POST /chat/invites/redeem | Join a chat with someone's code; both people get a different pseudonym |
| GET /chats | List my chats (pseudonyms only) |
| GET /chats/{id}/messages?page=&size= | Read messages, newest first |
| POST /chats/{id}/messages | Send a message (client-generated clientMessageId makes retries safe) |
| GET /chats/{id}/stream | Live updates for a chat (Server-Sent Events) |
| POST /chats/{id}/close | Close (block) a chat |
| POST /chats/{id}/report | Report a chat (stores the reason only) and close it |

## How the chat protects people
- People see each other only as pseudonyms such as "Quiet Fox", new for every chat. API responses never contain an email, name or user id of the other person.
- Invite codes are random, single use, expire after 24 hours, and only a hash is stored. Wrong, used, expired and own codes all get the same answer.
- Only the two members can read, send, stream, close or report. Anyone else gets 404, the same as for a chat that does not exist.
- Retrying a send with the same clientMessageId never creates a duplicate message.
- Sending is limited to 30 messages a minute per user; code guessing is limited to 10 attempts per 15 minutes.
- Reports store the reason, not message text.
- Deleting an account removes that person's messages, closes the chat for the other person, and removes chats nobody belongs to.

## How the journal and mood features protect privacy
- Every query includes the user id from the session. Another user's entry id returns 404, the same as an id that does not exist.
- Lists return a short excerpt only; the full text is returned for a single entry.
- Summaries are calculated from what the user recorded. With fewer than 5 check-ins, averages and direction are left empty instead of guessed.

## How authentication works
- Passwords are hashed with BCrypt (12 to 72 characters).
- Login creates a server-side session in an HttpOnly, SameSite=Lax cookie. The session id changes on login.
- CSRF protection: the server sets an `XSRF-TOKEN` cookie; the app echoes it in the `X-XSRF-TOKEN` header on every POST/PUT/DELETE.
- Failed logins return one generic message, and 5 failures in 15 minutes for the same email and IP returns 429.
- The user id always comes from the session, never from the request.

## Known limitations
- Chat is pseudonymous, not anonymous. Administrators with database access, server logs (IP addresses, timing), screenshots, and details people write in messages can identify someone.
- The "I am 18 or older" confirmation is a self-declaration, not age verification.
- There is no automatic detection yet of messages that suggest immediate danger; the safety workflow is planned. MindBridge is not an emergency service.
- Messages are stored as text in the database (no end-to-end encryption).
- Live updates and rate limiters are in memory (single application instance).
- Swagger UI is public in development and will be restricted before deployment.
- Registering with an existing email returns 409, which reveals that the email has an account.
- No password reset or email verification yet.
