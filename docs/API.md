# API

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

All endpoints (except `/auth/*` and `/sync/remote`) require `Authorization: Bearer <token>`.

## Core Endpoints

Auth
- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`

Items
- `GET /items`
- `POST /items`
- `GET /items/{id}`
- `PUT /items/{id}`
- `DELETE /items/{id}` (soft delete)
- `POST /items/{id}/restore`
- `GET /items/trash`

Search
- `GET /items/search?query=&type=&status=&tags=&author=&cast=&year=&condition=&location=&page=&size=`

External
- `GET /external/books/lookup?isbn=`
- `POST /external/books/confirm`
- `POST /items/{id}/external-link`
- `GET /items/{id}/external-refresh`
- `POST /items/{id}/external-apply`

Lists
- `GET /lists`
- `POST /lists`
- `GET /lists/{id}`
- `PUT /lists/{id}`
- `DELETE /lists/{id}`
- `POST /lists/{id}/items`
- `DELETE /lists/{id}/items/{itemId}`
- `POST /lists/{id}/items/reorder`

Progress
- `POST /items/{id}/progress`
- `GET /items/{id}/progress`

Loans
- `POST /items/{id}/loan`
- `GET /items/{id}/loan` (active)
- `POST /loans/{loanId}/return`
- `GET /loans/overdue`

Import/Export
- `GET /export?format=csv|json`
- `POST /import?format=csv|json` (multipart file)

Sync
- `POST /sync/enable`
- `POST /sync/run`
- `GET /sync/status`
- `POST /sync/remote` (cloud endpoint, API key)

Health
- `GET /actuator/health`
