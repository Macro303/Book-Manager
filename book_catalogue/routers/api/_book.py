from __future__ import annotations

__all__ = ["router"]

from fastapi import APIRouter, Body, HTTPException
from pony.orm import db_session, flush

from book_catalogue.controllers import BookController, UserController
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas import Book
from book_catalogue.schemas._book import LookupBook, NewBook

router = APIRouter(prefix="/books", tags=["Books"])


@router.get(path="")
def list_books() -> list[Book]:
    with db_session:
        return sorted({x.to_schema() for x in BookController.list_books()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_book(new_book: NewBook) -> Book:
    with db_session:
        return BookController.create_book(new_book=new_book)


@router.get(path="/{book_id}", responses={404: {"model": ErrorResponse}})
def get_book(book_id: int) -> Book:
    with db_session:
        return BookController.get_book(book_id=book_id).to_schema()


@router.patch(
    path="/{book_id}", responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}}
)
def update_book(book_id: int, updates: NewBook) -> Book:
    with db_session:
        return BookController.update_book(book_id=book_id, updates=updates).to_schema()


@router.delete(path="/{book_id}", responses={404: {"model": ErrorResponse}})
def delete_book(book_id: int):
    with db_session:
        BookController.delete_book(book_id=book_id)


@router.post(path="/lookup", status_code=201, responses={409: {"model": ErrorResponse}})
def lookup_book(new_book: LookupBook) -> Book:
    with db_session:
        return BookController.lookup_book(
            isbn=new_book.isbn, wisher_id=new_book.wisher_id
        ).to_schema()


@router.put(path="", status_code=204)
def reset_all_books():
    with db_session:
        for _book in BookController.list_books():
            BookController.reset_book(book_id=_book.book_id)


@router.put(path="/{book_id}", responses={404: {"model": ErrorResponse}})
def reset_book(book_id: int) -> Book:
    with db_session:
        return BookController.reset_book(book_id=book_id).to_schema()


@router.post(
    path="/{book_id}/wish", responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}}
)
def wish_book(book_id: int, wisher_id: int) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if not book.wishers:
            raise HTTPException(status_code=400, detail="Book has already been collected.")
        user = UserController.get_user(user_id=wisher_id)
        if user in book.wishers:
            book.wishers.remove(user)
        else:
            book.wishers.add(user)
        flush()
        return book.to_schema()


@router.post(
    path="/{book_id}/collect",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def collect_book(book_id: int) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if not book.wishers:
            raise HTTPException(status_code=400, detail="Book has already been collected.")
        book.wishers = []
        flush()
        return book.to_schema()


@router.post(path="/{book_id}/read", responses={404: {"model": ErrorResponse}})
def read_book(
    book_id: int,
    user_id: int = Body(embed=True),  # noqa: B008
) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if book.wishers:
            raise HTTPException(status_code=400, detail="Book has not been collected.")
        user = UserController.get_user(user_id=user_id)
        if user in book.readers:
            book.readers.remove(user)
        else:
            book.readers.add(user)
        flush()
        return book.to_schema()


@router.post(path="/{book_id}/format", responses={409: {"model": ErrorResponse}})
def create_format(book_id: int, name: str = Body(embed=True)) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        book.format = name
        flush()
        return book.to_schema()
