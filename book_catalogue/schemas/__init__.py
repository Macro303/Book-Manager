from __future__ import annotations

__all__ = [
    "Author",
    "AuthorRole",
    "AuthorIdentifiers",
    "Book",
    "BookIdentifiers",
    "Format",
    "Publisher",
    "Series",
    "User",
]

from book_catalogue.schemas._author import (
    Author,
    Identifiers as AuthorIdentifiers,
    Role as AuthorRole,
)
from book_catalogue.schemas._book import Book, Identifiers as BookIdentifiers
from book_catalogue.schemas._format import Format
from book_catalogue.schemas._publisher import Publisher
from book_catalogue.schemas._series import Series
from book_catalogue.schemas._user import User
