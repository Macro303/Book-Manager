__all__ = ["router"]

from datetime import date

from fastapi import APIRouter, Body, HTTPException
from pony.orm import db_session, flush

from book_catalogue.controllers.book import BookController
from book_catalogue.controllers.creator import CreatorController
from book_catalogue.controllers.role import RoleController
from book_catalogue.controllers.user import UserController
from book_catalogue.database.tables import BookCreator, Reader
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas.book import (
    BookCreatorWrite,
    BookRead,
    BookWrite,
    LookupBook,
)

router = APIRouter(prefix="/books", tags=["Books"])


@router.get(path="")
def list_books() -> list[BookRead]:
    with db_session:
        return sorted({x.to_schema() for x in BookController.list_books()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_book(new_book: BookWrite) -> BookRead:
    with db_session:
        return BookController.create_book(new_book=new_book).to_schema()


@router.get(path="/{book_id}", responses={404: {"model": ErrorResponse}})
def get_book(book_id: int) -> BookRead:
    with db_session:
        return BookController.get_book(book_id=book_id).to_schema()


@router.patch(
    path="/{book_id}", responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}}
)
def update_book(book_id: int, updates: BookWrite) -> BookRead:
    with db_session:
        return BookController.update_book(book_id=book_id, updates=updates).to_schema()


@router.delete(path="/{book_id}", responses={404: {"model": ErrorResponse}})
def delete_book(book_id: int):
    with db_session:
        BookController.delete_book(book_id=book_id)


@router.post(path="/lookup", status_code=201, responses={409: {"model": ErrorResponse}})
def lookup_book(new_book: LookupBook) -> BookRead:
    with db_session:
        book = BookController.lookup_book(isbn=new_book.isbn, wisher_id=new_book.wisher_id)
        if new_book.collect:
            book.is_collected = True
            book.wishers = []
        flush()
        return book.to_schema()


@router.put(path="", status_code=204)
def reset_all_books(load_new_fields: bool = False):
    with db_session:
        for _book in BookController.list_books():
            if load_new_fields:
                BookController.load_new_field(book_id=_book.book_id)
            else:
                BookController.reset_book(book_id=_book.book_id)


@router.put(path="/{book_id}", responses={404: {"model": ErrorResponse}})
def reset_book(book_id: int, load_new_fields: bool = False) -> BookRead:
    with db_session:
        if load_new_fields:
            return BookController.load_new_field(book_id=book_id).to_schema()
        return BookController.reset_book(book_id=book_id).to_schema()


@router.post(
    path="/{book_id}/collect",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def collect_book(book_id: int) -> BookRead:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if book.is_collected:
            raise HTTPException(status_code=400, detail="Book has already been collected.")
        book.is_collected = True
        book.wishers = []
        flush()
        return book.to_schema()


@router.delete(
    path="/{book_id}/collect",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def discard_book(book_id: int) -> BookRead:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if not book.is_collected:
            raise HTTPException(status_code=400, detail="Book has already been collected.")
        book.is_collected = False
        book.readers = []
        flush()
        return book.to_schema()


@router.put(
    path="/{book_id}/wish", responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}}
)
def wish_book(book_id: int, wisher_id: int = Body(embed=True)) -> BookRead:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if book.is_collected:
            raise HTTPException(status_code=400, detail="Book has not been collected.")
        user = UserController.get_user(user_id=wisher_id)
        if user in book.wishers:
            book.wishers.remove(user)
        else:
            book.wishers.add(user)
        flush()
        return book.to_schema()


@router.post(path="/{book_id}/read", responses={404: {"model": ErrorResponse}})
def read_book(
    book_id: int,
    reader_id: int = Body(embed=True),
    read_date: date | None = Body(default=None, embed=True),
) -> BookRead:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if not book.is_collected:
            raise HTTPException(status_code=400, detail="Book has not been collected.")
        user = UserController.get_user(user_id=reader_id)
        if _ := Reader.get(book=book, user=user):
            raise HTTPException(status_code=400, detail="Book has already been read by user.")
        Reader(book=book, user=user, read_date=read_date)
        flush()
        return book.to_schema()


@router.delete(path="/{book_id}/read", responses={404: {"model": ErrorResponse}})
def unread_book(book_id: int, reader_id: int = Body(embed=True)) -> BookRead:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if not book.is_collected:
            raise HTTPException(status_code=400, detail="Book has not been collected.")
        user = UserController.get_user(user_id=reader_id)
        reader = Reader.get(book=book, user=user)
        if not reader:
            raise HTTPException(status_code=400, detail="Book has not been read by user yet.")
        reader.delete()
        flush()
        return book.to_schema()


@router.patch(
    path="/{book_id}/creators",
    responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}},
)
def set_creators(book_id: int, creators: list[BookCreatorWrite] = Body(embed=True)) -> BookRead:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        for _creator in book.creators:
            _creator.delete()
        flush()
        for _creator in creators:
            BookCreator(
                book=book,
                creator=CreatorController.get_creator(creator_id=_creator.creator_id),
                roles=[RoleController.get_role(role_id=x) for x in _creator.role_ids],
            )
        flush()
        return book.to_schema()
