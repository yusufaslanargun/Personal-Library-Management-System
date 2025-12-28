# FR/NFR Checklist

## Functional Requirements

| ID | Status | Evidence |
| --- | --- | --- |
| FR-01 | DONE | UI: Add (ISBN) + `GET /external/books/lookup`, `POST /external/books/confirm` |
| FR-02 | DONE | UI: Add (Manual) + `POST /items` |
| FR-03 | DONE | `DELETE /items/{id}`, `POST /items/{id}/restore`, UI: Trash |
| FR-04 | DONE | Tags/condition/location persisted via `PUT /items/{id}` |
| FR-05 | DONE | `GET /items/search` with AND filters (tags, author, cast) + stable pagination |
| FR-06 | DONE | Open Library client with timeout/rate-limit and error handling (Google Books kept for future) |
| FR-07 | DONE | `ExternalLink` persisted and returned on item detail |
| FR-08 | DONE | `GET /items/{id}/external-refresh` + `POST /items/{id}/external-apply` |
| FR-09 | DONE | Error banner in UI and retry by re-running lookup |
| FR-10 | DONE | `POST /lists`, `PUT /lists/{id}`, `DELETE /lists/{id}` |
| FR-11 | DONE | `POST /lists/{id}/items` + reorder endpoint, duplicate constraint |
| FR-12 | DONE | `list_item.position` persisted, UI reorder |
| FR-13 | DONE | Progress log computes percent from total + clamps |
| FR-14 | DONE | DVD progress uses runtime total |
| FR-15 | DONE | `POST /items/{id}/progress` + history list |
| FR-16 | DONE | UI updates progress without reload |
| FR-17 | DONE | Loan create validates dates + overdue dashboard |
| FR-18 | DONE | `POST /loans/{loanId}/return` keeps history |
| FR-19 | DONE | `GET /export`, `POST /import` JSON/CSV with validation + summary |
| FR-20 | DONE | `/sync/*` endpoints, LWW resolution + status |

## Non-Functional Requirements

| ID | Status | Evidence |
| --- | --- | --- |
| U1 | DONE | Add via ISBN flow in UI (Lookup -> Pick -> Confirm) |
| U2 | DONE | Progress entry form + immediate update |
| U3 | DONE | Empty states + validation errors in UI |
| U4 | DONE | Keyboard accessible inputs + focus styles |
| R1 | DONE | Soft delete + explicit restore; transactional import |
| R2 | DONE | External failures return 502 without blocking local ops |
| R3 | DONE | Partial unique index `uniq_active_loan_per_item` |
| R4 | DONE | Import validation + errors summary |
| P1 | DONE | Full-text search + GIN index + benchmark script |
| P2 | DONE | Item detail endpoint + benchmark script |
| P3 | DONE | UI minimal reflows; check in PERF.md |
| Spt1 | DONE | Subsystem services in `com.example.plms.service` |
| Spt2 | DONE | Actuator health + structured logging |
| Spt3 | DONE | `.env` driven config for provider endpoints/keys |
