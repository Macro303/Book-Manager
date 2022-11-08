__all__ = [
    "create_user",
    "get_user",
    "list_books",
    "add_book",
    "get_book",
    "update_book",
    "refresh_book",
    "remove_book",
    "create_author",
    "get_author",
    "create_series",
    "get_series",
]
import logging

from sqlalchemy.orm import Session

from book_catalogue.models import Author, Book, Series, User
from book_catalogue.services.open_library import retrieve_book

LOGGER = logging.getLogger(__name__)


def create_user(db: Session, username: str) -> User:
    LOGGER.info(f"Creating user: {username}")
    temp = User(username=username)
    db.add(temp)
    db.commit()
    db.refresh(temp)
    return temp


def get_user(db: Session, username: str) -> User | None:
    LOGGER.info(f"Getting user: {username}")
    return db.query(User).filter(User.username == username).first()


def list_books(db: Session) -> list[Book]:
    LOGGER.info("Listing books")
    return db.query(Book)


def add_book(db: Session, isbn: str, wisher: User | None) -> Book:
    LOGGER.info(f"Adding book: {isbn}")
    temp = retrieve_book(db, isbn)
    temp.wisher = wisher
    db.add(temp)
    db.commit()
    db.refresh(temp)
    return temp


def get_book(db: Session, isbn: str) -> Book | None:
    LOGGER.info(f"Getting book: {isbn}")
    return db.query(Book).filter(Book.isbn == isbn).first()


def update_book(db: Session, book: Book, wisher: User | None, readers: list[User]) -> Book:
    LOGGER.info(f"Updating book: {book.isbn}")
    book.wisher = wisher
    book.readers = readers
    db.commit()
    db.refresh(book)
    return book


def refresh_book(db: Session, book: Book) -> Book:
    LOGGER.info(f"Refreshing book: {book.isbn}")
    temp_isbn = book.isbn
    temp_wisher = book.wisher
    temp_readers = book.readers
    remove_book(db, book)
    book = add_book(db, temp_isbn, temp_wisher)
    return update_book(db, book, temp_wisher, temp_readers)


def remove_book(db: Session, book: Book):
    LOGGER.info(f"Removing book: {book.isbn}")
    db.delete(book)
    db.commit()


def create_author(db: Session, name: str) -> Author:
    LOGGER.info(f"Creating author: {name}")
    temp = Author(name=name)
    db.add(temp)
    db.commit()
    db.refresh(temp)
    return temp


def get_author(db: Session, name: str) -> Author | None:
    LOGGER.info(f"Getting author: {name}")
    return db.query(Author).filter(Author.name == name).first()


def create_series(db: Session, name: str) -> Series:
    LOGGER.info(f"Creating series: {name}")
    temp = Series(name=name)
    db.add(temp)
    db.commit()
    db.refresh(temp)
    return temp


def get_series(db: Session, name: str) -> Series | None:
    LOGGER.info(f"Getting series: {name}")
    return db.query(Series).filter(Series.name == name).first()
