# Architecture

## Layers

- Presentation (Web UI): React + TypeScript (Vite)
- Application/Controllers: Spring MVC REST controllers
- Subsystem Services:
  - Inventory: item CRUD, tags, soft delete/restore
  - External Integration: ISBN lookup, metadata refresh, diff/apply
  - Lists: list CRUD + reorder
  - Progress: progress logs + percent update
  - Loans: loan lifecycle + overdue
  - Data I/O & Sync: import/export, sync state
- Persistence: Spring Data JPA repositories + Flyway migrations
- External Provider Clients: Open Library (ISBN, active), Google Books (future), OMDb (optional)

## Data Model

- MediaItem (BOOK/DVD) with BookInfo or DvdInfo details
- Tag + MediaItemTag (many-to-many)
- ExternalLink for provider mappings
- List + ListItem (position + priority)
- ProgressLog for progress history
- Loan for lending history
- SyncState for cloud sync status

## Sync (Feature Flag)

Sync is controlled by `PLMS_SYNC_ENABLED`. The service runs a full sync on first run and sends incremental updates afterward. Conflicts are resolved with last-write-wins (LWW) and conflict counts are stored in `sync_state`.
Offline queueing is not implemented; sync runs only on demand.

## Key Invariants

- One active loan per item: enforced by partial unique index + service check
- dueDate >= startDate: enforced by DB check + service validation
- Progress percent derived from current/total and clamped to 0-100
- Soft delete: `deleted_at` hides items from default views

## Search

PostgreSQL full-text search uses `search_vector` with GIN index. Filters apply AND semantics (including tags, author, and cast) and stable pagination is enforced with deterministic ordering.
