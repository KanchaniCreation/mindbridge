# MindBridge

A private, privacy-focused self-reflection and well-being app. It is a self-reflection tool,
not therapy, and it does not diagnose.

## Status
Milestone 2 (foundation): API, database, frontend shell, tests. No user features yet.

## Run locally
1. `copy .env.example .env` and set a password.
2. `docker compose up -d db` (database is exposed on host port 5433)
3. Backend: `cd backend`, `$env:DB_PASSWORD="<your password>"`, `mvn spring-boot:run`
4. Frontend: `cd frontend`, `npm install`, `npm run dev`

## Tests
- Backend: `cd backend && mvn test` (Docker must be running)
- Frontend: `cd frontend && npm test`

## Known limitations
Swagger UI is public in development and will be restricted before deployment.
