# Security

- Passwords are hashed with BCrypt; JWT tokens are signed with `PLMS_JWT_SECRET` (minimum 32 chars).
- External provider API keys are configured via environment variables (`GOOGLE_BOOKS_API_KEY`, `OPEN_LIBRARY_API_KEY`, `OMDB_API_KEY`).
- Sync credentials are configured via `PLMS_SYNC_API_KEY` and endpoint URL via `PLMS_SYNC_ENDPOINT`.
- Keys are never stored in the database.
- Only library metadata is stored; no sensitive personal data beyond loan contact names.
- Enable `PLMS_EXTERNAL_MOCK=true` for offline testing without external API calls.
- Authentication is required; deploy behind HTTPS if exposing publicly.
