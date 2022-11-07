from fastapi import APIRouter, Body, Depends
from fastapi.exceptions import HTTPException
from sqlalchemy.orm import Session

from book_manager import __version__, controller
from book_manager.database import SessionLocal
from book_manager.isbn import to_isbn
from book_manager.responses import ErrorResponse
from book_manager.schemas import Book, User

router = APIRouter(
    prefix=f"/api/v{__version__.split('.')[0]}",
    tags=["API"],
    responses={
        422: {"description": "Validation error", "model": ErrorResponse},
    },
)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@router.post(
    path="/users", status_code=201, response_model=User, responses={409: {"model": ErrorResponse}}
)
def create_user(username: str = Body(embed=True), db: Session = Depends(get_db)) -> User:
    db_user = controller.get_user(db, username)
    if db_user:
        raise HTTPException(status_code=409, detail="User already exists.")
    return controller.create_user(db, username).to_schema()


@router.get(
    path="/users/{username}", response_model=User, responses={404: {"model": ErrorResponse}}
)
def get_user(username: str, db: Session = Depends(get_db)) -> User:
    db_user = controller.get_user(db, username)
    if not db_user:
        raise HTTPException(status_code=404, detail="User doesn't exist.")
    return db_user.to_schema()


@router.post(
    path="/books",
    status_code=201,
    response_model=Book,
    responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}},
)
def add_book(
    isbn: str = Body(embed=True),
    wisher: str | None = Body(embed=True),
    db: Session = Depends(get_db),
) -> Book:
    isbn = to_isbn(isbn)
    db_wisher = controller.get_user(db, wisher)
    if not db_wisher:
        raise HTTPException(status_code=404, detail="User doesn't exist.")
    db_book = controller.get_book(db, isbn)
    if db_book:
        raise HTTPException(status_code=409, detail="Book already added.")
    return controller.add_book(db, isbn, db_wisher).to_schema()


@router.get(path="/books", response_model=list[Book], responses={404: {"model": ErrorResponse}})
def list_books(db: Session = Depends(get_db)) -> list[Book]:
    return sorted(x.to_schema() for x in controller.list_books(db))


@router.get(path="/books/{isbn}", response_model=Book, responses={404: {"model": ErrorResponse}})
def get_book(isbn: str, db: Session = Depends(get_db)) -> Book:
    isbn = to_isbn(isbn)
    db_book = controller.get_book(db, isbn)
    if not db_book:
        raise HTTPException(status_code=404, detail="Book not added.")
    return db_book.to_schema()


@router.post(path="/books/{isbn}", response_model=Book, responses={404: {"model": ErrorResponse}})
def refresh_book(isbn: str, db: Session = Depends(get_db)) -> Book:
    isbn = to_isbn(isbn)
    db_book = controller.get_book(db, isbn)
    if not db_book:
        raise HTTPException(status_code=404, detail="Book not added.")
    return controller.refresh_book(db, db_book).to_schema()


@router.put(path="/books/{isbn}", response_model=Book, responses={404: {"model": ErrorResponse}})
def update_book(
    isbn: str,
    wisher: str | None = Body(embed=True),
    readers: list[str] = Body(embed=True),
    db: Session = Depends(get_db),
) -> Book:
    isbn = to_isbn(isbn)
    db_book = controller.get_book(db, isbn)
    if not db_book:
        raise HTTPException(status_code=404, detail="Book not added.")
    db_wisher = None
    if wisher:
        db_wisher = controller.get_user(db, wisher)
        if not db_wisher:
            raise HTTPException(status_code=404, detail="User doesn't exist.")
    db_readers = []
    for reader in readers:
        db_reader = controller.get_user(db, reader)
        if not db_reader:
            raise HTTPException(status_code=404, detail="User doesn't exist.")
        db_readers.append(db_reader)
    return controller.update_book(db, db_book, db_wisher, db_readers).to_schema()


@router.delete(path="/books/{isbn}", status_code=204, responses={404: {"model": ErrorResponse}})
def remove_book(isbn: str, db: Session = Depends(get_db)):
    isbn = to_isbn(isbn)
    db_book = controller.get_book(db, isbn)
    if not db_book:
        raise HTTPException(status_code=404, detail="Book not added.")
    controller.remove_book(db, db_book)
