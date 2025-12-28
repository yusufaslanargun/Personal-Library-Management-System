# Performance

## Indexes

- `media_item.search_vector` GIN index for full-text search
- BTree indexes on `type`, `status`, `year`, `deleted_at`
- `list_item(list_id, position)` for list ordering
- `loan(item_id)` and partial unique index for active loan

## Benchmark

Generate data:

```bash
DATABASE_URL=postgresql://plms:plms@localhost:5432/plms ./scripts/seed.sh
```

Run benchmark:

```bash
./scripts/benchmark.py --base http://localhost:8080 --runs 30 --query Seed --item-id 1
```

Sample result (fill with your run):

```
{
  "search": { "runs": 30, "p95_ms": 0, "avg_ms": 0 },
  "detail": { "runs": 30, "p95_ms": 0, "avg_ms": 0 }
}
```
