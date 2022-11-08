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

from sqlalchemy.orm import Session

from book_manager.models import Author, Book, Series, User
from book_manager.services.open_library import retrieve_book


def create_user(db: Session, username: str) -> User:
    temp = User(username=username)
    db.add(temp)
    db.commit()
    db.refresh(temp)
    return temp


def get_user(db: Session, username: str) -> User | None:
    return db.query(User).filter(User.username == username).first()


def list_books(db: Session) -> list[Book]:
    return db.query(Book)


def add_book(db: Session, isbn: str, wisher: User | None) -> Book:
    temp = retrieve_book(db, isbn)
    temp.wisher = wisher
    db.add(temp)
    db.commit()
    db.refresh(temp)
    return temp


def get_book(db: Session, isbn: str) -> Book | None:
    return db.query(Book).filter(Book.isbn == isbn).first()


def update_book(db: Session, book: Book, wisher: User | None, readers: list[User]) -> Book:
    book.wisher = wisher
    book.readers = readers
    db.commit()
    db.refresh(book)
    return book


def refresh_book(db: Session, book: Book) -> Book:
    temp_isbn = book.isbn
    temp_wisher = book.wisher
    temp_readers = book.readers
    remove_book(db, book)
    book = add_book(db, temp_isbn, temp_wisher)
    return update_book(db, book, temp_wisher, temp_readers)


def remove_book(db: Session, book: Book):
    db.delete(book)
    db.commit()


def create_author(db: Session, name: str) -> Author:
    temp = Author(name=name)
    db.add(temp)
    db.commit()
    db.refresh(temp)
    return temp


def get_author(db: Session, name: str) -> Author | None:
    return db.query(Author).filter(Author.name == name).first()


def create_series(db: Session, name: str) -> Series:
    temp = Series(name=name)
    db.add(temp)
    db.commit()
    db.refresh(temp)
    return temp


def get_series(db: Session, name: str) -> Series | None:
    return db.query(Series).filter(Series.name == name).first()
