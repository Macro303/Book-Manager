__all__ = ["SQLiteCache"]

import json
import sqlite3
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

from book_catalogue import get_cache_root


class SQLiteCache:
    def __init__(
        self,
        path: Path = get_cache_root() / "cache.sqlite",
        expiry: int | None = 14,
    ):
        self.expiry = expiry
        self.con = sqlite3.connect(path)
        self.con.row_factory = sqlite3.Row

        self.con.execute("CREATE TABLE IF NOT EXISTS queries (query, response, expiry);")
        self.delete()

    def select(self, query: str) -> dict[str, Any]:
        if self.expiry:
            cursor = self.con.execute(
                "SELECT * FROM queries WHERE query = ? and expiry > ?;",
                (query, datetime.now(timezone.utc).date().isoformat()),
            )
        else:
            cursor = self.con.execute("SELECT * FROM queries WHERE query = ?;", (query,))
        if results := cursor.fetchone():
            return json.loads(results["response"])
        return {}

    def insert(self, query: str, response: dict[str, Any]):
        if self.expiry:
            expiry = datetime.now(timezone.utc).date() + timedelta(days=self.expiry)
        else:
            expiry = datetime.now(timezone.utc).date()
        self.con.execute(
            "INSERT INTO queries (query, response, expiry) VALUES (?, ?, ?);",
            (query, json.dumps(response), expiry.isoformat()),
        )
        self.con.commit()

    def delete(self):
        if not self.expiry:
            return
        self.con.execute(
            "DELETE FROM queries WHERE expiry < ?;",
            (datetime.now(timezone.utc).date().isoformat(),),
        )
        self.con.commit()
