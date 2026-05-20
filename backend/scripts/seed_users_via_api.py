#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
import sys
import time
import urllib.error
import urllib.request


def post_json(url: str, payload: dict, timeout_s: float = 10.0) -> tuple[int, str]:
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
    )

    try:
        with urllib.request.urlopen(req, timeout=timeout_s) as resp:
            body = resp.read().decode("utf-8")
            return resp.status, body
    except urllib.error.HTTPError as e:
        # HTTPError is also a file-like object
        body = e.read().decode("utf-8") if e.fp is not None else ""
        return e.code, body


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Seed users via YarnSpace API")
    parser.add_argument(
        "--base-url",
        type=str,
        default="http://localhost:8000",
        help="API base url, e.g. http://localhost:8000 (default)",
    )
    parser.add_argument("--count", type=int, default=10, help="How many users to create (default: 10)")
    parser.add_argument("--prefix", type=str, default="user", help="Username prefix (default: user)")
    parser.add_argument("--start", type=int, default=1, help="Starting index (default: 1)")
    parser.add_argument(
        "--password",
        type=str,
        default="password123",
        help="Password for all created users (default: password123)",
    )
    parser.add_argument(
        "--sleep-ms",
        type=int,
        default=0,
        help="Optional delay between requests (default: 0)",
    )
    args = parser.parse_args(argv)

    if args.count <= 0:
        print("--count must be > 0", file=sys.stderr)
        return 2

    base = args.base_url.rstrip("/")
    register_url = f"{base}/auth/register"

    created = 0
    attempted = 0

    for i in range(args.start, args.start + args.count * 3):
        if created >= args.count:
            break

        username = f"{args.prefix}{i}"
        payload = {
            "username": username,
            "email": f"{username}@example.com",
            "displayName": username,
            "password": args.password,
            # "accentColor": "sage",
        }

        attempted += 1
        status, body = post_json(register_url, payload)

        if status in (200, 201):
            created += 1
            print(f"[{created}/{args.count}] Created {username}")
        elif status == 409:
            # username/email already exists
            print(f"[skip] {username} already exists (409)")
        else:
            print(f"[error] Failed to create {username} (HTTP {status})")
            if body:
                print(body)

        if args.sleep_ms > 0:
            time.sleep(args.sleep_ms / 1000.0)

    if created < args.count:
        print(f"Only created {created}/{args.count} users (attempted {attempted}).")

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

