from fastapi import APIRouter, Body
from fastapi.exceptions import HTTPException
from pony.orm import db_session

from book_manager import __version__
from book_manager.database.tables import BookTable, UserTable
from book_manager.models.book import Book
from book_manager.models.isbn import to_isbn
from book_manager.models.user import User
from book_manager.responses import ErrorResponse
from book_manager.services.open_library import lookup_book

router = APIRouter(
    prefix=f"/api/v{__version__.split('.')[0]}",
    tags=["API"],
    responses={
        422: {"description": "Validation error", "model": ErrorResponse},
    },
)


@router.post(
    path="/users", status_code=201, response_model=User, responses={404: {"model": ErrorResponse}}
)
def add_user(username: str = Body(embed=True)) -> User:
    with db_session:
        if UserTable.get(username=username):
            raise HTTPException(status_code=409, detail="User already exists")
        return UserTable(username=username).to_model()


@router.get(
    path="/users/{username}", response_model=User, responses={404: {"model": ErrorResponse}}
)
def get_user(username: str) -> User:
    with db_session:
        if not (user := UserTable.get(username=username)):
            raise HTTPException(status_code=404, detail="User doesn't exist")
        return user.to_model()


@router.post(
    path="/books", status_code=201, response_model=Book, responses={404: {"model": ErrorResponse}}
)
def add_book(isbn: str = Body(embed=True), wished: str | None = Body(embed=True)) -> Book:
    with db_session:
        isbn = to_isbn(isbn)
        if BookTable.get(isbn=isbn):
            raise HTTPException(status_code=409, detail="Book already exists")
        temp = lookup_book(isbn)
        if wished:
            temp.wished = wished
        else:
            temp.wished = None
        return BookTable.create(temp).to_model()


@router.get(path="/books", response_model=list[Book], responses={404: {"model": ErrorResponse}})
def list_books() -> list[Book]:
    with db_session:
        return sorted(x.to_model() for x in BookTable.select())


@router.get(path="/books/{isbn}", response_model=Book, responses={404: {"model": ErrorResponse}})
def get_book(isbn: str) -> Book:
    with db_session:
        isbn = to_isbn(isbn)
        if not (book := BookTable.get(isbn=isbn)):
            raise HTTPException(status_code=404, detail="Book not added")
        return book.to_model()


@router.put(path="/books/{isbn}", response_model=Book, responses={404: {"model": ErrorResponse}})
def update_book(
    isbn: str,
    wished: str | None = Body(embed=True),
    read: list[str] = Body(embed=True),
) -> Book:
    with db_session:
        isbn = to_isbn(isbn)
        if not (book := BookTable.get(isbn=isbn)):
            raise HTTPException(status_code=404, detail="Book not added")
        if wished:
            book.wished = UserTable.create_or_get(wished)
        else:
            book.wished = None
        if read:
            book.read = [UserTable.create_or_get(x) for x in read]
        else:
            book.read = []
        return book.to_model()


@router.delete(path="/books/{isbn}", status_code=204, responses={404: {"model": ErrorResponse}})
def remove_book(isbn: str):
    with db_session:
        isbn = to_isbn(isbn)
        if not (book := BookTable.get(isbn=isbn)):
            raise HTTPException(status_code=404, detail="Book not added")
        book.delete()
