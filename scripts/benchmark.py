#!/usr/bin/env python3
import json
import time
import urllib.request
import argparse


def measure(url, runs):
    times = []
    for _ in range(runs):
        start = time.time()
        with urllib.request.urlopen(url) as resp:
            resp.read()
        times.append((time.time() - start) * 1000)
    times.sort()
    p95_index = max(int(len(times) * 0.95) - 1, 0)
    return {
        "runs": runs,
        "p95_ms": round(times[p95_index], 2),
        "avg_ms": round(sum(times) / len(times), 2)
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://localhost:8080")
    parser.add_argument("--runs", type=int, default=30)
    parser.add_argument("--query", default="Seed")
    parser.add_argument("--item-id", type=int, default=1)
    args = parser.parse_args()

    search_url = f"{args.base}/items/search?query={args.query}&page=0&size=20"
    detail_url = f"{args.base}/items/{args.item_id}"

    results = {
        "search": measure(search_url, args.runs),
        "detail": measure(detail_url, args.runs)
    }

    print(json.dumps(results, indent=2))


if __name__ == "__main__":
    main()
