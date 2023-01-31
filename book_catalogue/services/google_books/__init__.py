__all__ = ["google_books_lookup"]

from book_catalogue.schemas.book import BookWrite
from book_catalogue.services.google_books.service import GoogleBooks


def google_books_lookup(isbn: str, google_books_id: str | None = None) -> BookWrite:
    session = GoogleBooks()
    if google_books_id:
        result = session.get_book(book_id=google_books_id)
    else:
        result = session.get_book_by_isbn(isbn=isbn)
