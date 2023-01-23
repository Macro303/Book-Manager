from __future__ import annotations

__all__ = ["lookup_book"]

from book_catalogue.services.open_library.schemas.edition import Edition
from book_catalogue.services.open_library.schemas.work import Work
from book_catalogue.services.open_library.service import OpenLibrary


def lookup_book(session: OpenLibrary, isbn: str) -> dict[str, Edition | Work]:
    edition = session.get_edition_by_isbn(isbn=isbn)
    work = session.get_work(work_id=edition.works[0].key.split("/")[-1])

    return {"edition": edition, "work": work}
