__all__ = ["SQLiteCache"]

import json
import sqlite3
from datetime import date, timedelta
from pathlib import Path
from typing import Any

from bookshelf import get_cache_root


class SQLiteCache:
    def __init__(
        self,
        path: Path = None,
        expiry: int | None = 14,
    ):
        self.expiry = expiry
        self.con = sqlite3.connect(path or get_cache_root() / "cache.sqlite")
        self.con.row_factory = sqlite3.Row

        self.con.execute("CREATE TABLE IF NOT EXISTS queries (query, response, query_date);")
        self.delete()

    def select(self, query: str) -> dict[str, Any]:
        if self.expiry:
            expiry = date.today() - timedelta(days=self.expiry)
            cursor = self.con.execute(
                "SELECT * FROM queries WHERE query = ? and query_date > ?;",
                (query, expiry.isoformat()),
            )
        else:
            cursor = self.con.execute("SELECT * FROM queries WHERE query = ?;", (query,))
        results = cursor.fetchone()
        if results:
            return json.loads(results["response"])
        return {}

    def insert(self, query: str, response: dict[str, Any]) -> None:
        self.con.execute(
            "INSERT INTO queries (query, response, query_date) VALUES (?, ?, ?);",
            (query, json.dumps(response), date.today().isoformat()),
        )
        self.con.commit()

    def delete(self) -> None:
        if not self.expiry:
            return
        expiry = date.today() - timedelta(days=self.expiry)
        self.con.execute("DELETE FROM queries WHERE query_date < ?;", (expiry.isoformat(),))
        self.con.commit()
