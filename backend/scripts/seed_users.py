#!/usr/bin/env python3

from __future__ import annotations

import argparse
import random
import sys
from typing import Iterable

import bcrypt
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError

import models
from db import Base, SessionLocal, engine


ACCENT_COLORS: list[str] = [
    "sage",
    "lavender",
    "peach",
    "sky",
    "mint",
    "rose",
]


def hash_password(password: str) -> str:
    pwd_bytes = password.encode("utf-8")
    # bcrypt only uses first 72 bytes
    if len(pwd_bytes) > 72:
        pwd_bytes = pwd_bytes[:72]
    return bcrypt.hashpw(pwd_bytes, bcrypt.gensalt()).decode("utf-8")


def username_exists(db, username: str) -> bool:
    from models import User

    stmt = select(User.id).where(User.username == username).limit(1)
    return db.execute(stmt).scalar_one_or_none() is not None


def iter_usernames(prefix: str, start: int) -> Iterable[tuple[int, str]]:
    i = start
    while True:
        yield i, f"{prefix}{i}"
        i += 1


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Seed fake users into the YarnSpace database")
    parser.add_argument("--count", type=int, default=10, help="How many users to create (default: 10)")
    parser.add_argument("--prefix", type=str, default="user", help="Username prefix (default: user)")
    parser.add_argument(
        "--start",
        type=int,
        default=1,
        help="Starting index for usernames (default: 1 => user1, user2, ...)",
    )
    parser.add_argument(
        "--password",
        type=str,
        default="password123",
        help="Plaintext password to set for all seeded users (default: password123)",
    )
    args = parser.parse_args(argv)

    if args.count <= 0:
        print("--count must be > 0", file=sys.stderr)
        return 2

    # Ensure tables exist
    Base.metadata.create_all(bind=engine)

    created = 0
    pwd_hash = hash_password(args.password)

    from models import User

    with SessionLocal() as db:
        for _, username in iter_usernames(args.prefix, args.start):
            if created >= args.count:
                break

            if username_exists(db, username):
                continue

            user = User(
                username=username,
                email=f"{username}@example.com",
                display_name=username,
                accent_color=random.choice(ACCENT_COLORS),
                avatar_url=None,
                password_hash=pwd_hash,
            )
            db.add(user)

            try:
                db.commit()
                db.refresh(user)
                created += 1
                print(f"Created user id={user.id} username={user.username} displayName={user.display_name}")
            except IntegrityError:
                db.rollback()
                continue

    if created < args.count:
        print(f"Only created {created}/{args.count} users (some usernames/emails already existed).")

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

