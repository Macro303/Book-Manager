from sqlalchemy.orm import Session

from book_manager.models import Book, User
from book_manager.services.open_library import retrieve_book


def create_user(db: Session, username: str) -> User | None:
    temp = User(username=username)
    db.add(temp)
    db.commit()
    db.refresh(temp)
    return temp


def get_user(db: Session, username: str) -> User | None:
    return db.query(User).filter(User.username == username).first()


def list_books(db: Session) -> list[Book]:
    return db.query(Book)


def add_book(db: Session, isbn: str, wisher: User | None) -> Book | None:
    temp = retrieve_book(isbn)
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
    delete_book(db, book)
    book = add_book(db, temp_isbn)
    return update_book(db, book, temp_wisher, temp_readers)


def delete_book(db: Session, book: Book):
    db.delete(book)
    db.commit()
