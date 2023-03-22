__all__ = ["router"]

from datetime import date

from fastapi import APIRouter, Body, HTTPException
from pony.orm import db_session, flush

from bookshelf.controllers.book import BookController
from bookshelf.controllers.creator import CreatorController
from bookshelf.controllers.genre import GenreController
from bookshelf.controllers.role import RoleController
from bookshelf.controllers.series import SeriesController
from bookshelf.controllers.user import UserController
from bookshelf.database.tables import BookCreator, BookSeries, Reader
from bookshelf.models.book import (
    Book,
    BookCreatorIn,
    BookIn,
    BookSeriesIn,
    ImportBook,
)
from bookshelf.responses import ErrorResponse

router = APIRouter(prefix="/books", tags=["Books"])


@router.get(path="")
def list_books() -> list[Book]:
    with db_session:
        return sorted({x.to_model() for x in BookController.list_books()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_book(new_book: BookIn) -> Book:
    with db_session:
        return BookController.create_book(new_book=new_book).to_model()


@router.get(path="/{book_id}", responses={404: {"model": ErrorResponse}})
def get_book(book_id: int) -> Book:
    with db_session:
        return BookController.get_book(book_id=book_id).to_model()


@router.patch(
    path="/{book_id}",
    responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}},
)
def update_book(book_id: int, updates: BookIn) -> Book:
    with db_session:
        return BookController.update_book(book_id=book_id, updates=updates).to_model()


@router.delete(path="/{book_id}", responses={404: {"model": ErrorResponse}})
def delete_book(book_id: int):
    with db_session:
        BookController.delete_book(book_id=book_id)


@router.post(path="/import", status_code=201, responses={409: {"model": ErrorResponse}})
def import_book(new_book: ImportBook) -> Book:
    if not new_book.isbn and not new_book.edition_id:
        raise HTTPException(status_code=400, detail="Isbn or OpenLibrary Edition Id required")
    with db_session:
        book = BookController.import_book(
            isbn=new_book.isbn,
            edition_id=new_book.edition_id,
            wisher_id=new_book.wisher_id,
        )
        if new_book.collect:
            book.is_collected = True
            book.wishers = []
        flush()
        return book.to_model()


@router.put(path="", status_code=204)
def refresh_all_books(load_new_fields: bool = False):
    with db_session:
        for _book in BookController.list_books():
            if load_new_fields:
                BookController.load_new_field(book_id=_book.book_id)
            else:
                BookController.reset_book(book_id=_book.book_id)


@router.put(path="/{book_id}", responses={404: {"model": ErrorResponse}})
def refresh_book(book_id: int, load_new_fields: bool = False) -> Book:
    with db_session:
        if load_new_fields:
            return BookController.load_new_field(book_id=book_id).to_model()
        return BookController.reset_book(book_id=book_id).to_model()


@router.post(
    path="/{book_id}/collect",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def collect_book(book_id: int) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if book.is_collected:
            raise HTTPException(status_code=400, detail="Book has already been collected.")
        book.is_collected = True
        book.wishers = []
        flush()
        return book.to_model()


@router.delete(
    path="/{book_id}/collect",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def discard_book(book_id: int) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if not book.is_collected:
            raise HTTPException(status_code=400, detail="Book has already been collected.")
        book.is_collected = False
        book.readers = []
        flush()
        return book.to_model()


@router.post(
    path="/{book_id}/wish",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def wish_book(book_id: int, wisher_id: int = Body(embed=True)) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if book.is_collected:
            raise HTTPException(status_code=400, detail="Book has not been collected.")
        user = UserController.get_user(user_id=wisher_id)
        if user in book.wishers:
            raise HTTPException(status_code=400, detail="Book has already been wished for by user.")
        book.wishers.add(user)
        flush()
        return book.to_model()


@router.delete(
    path="/{book_id}/wish",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def unwish_book(book_id: int, wisher_id: int = Body(embed=True)) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if book.is_collected:
            raise HTTPException(status_code=400, detail="Book has not been collected.")
        user = UserController.get_user(user_id=wisher_id)
        if user not in book.wishers:
            raise HTTPException(status_code=400, detail="Book has not been wished for by user yet.")
        book.wishers.remove(user)
        flush()
        return book.to_model()


@router.post(path="/{book_id}/read", responses={404: {"model": ErrorResponse}})
def read_book(
    book_id: int,
    reader_id: int = Body(embed=True),
    read_date: date | None = Body(default=None, embed=True),
) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        if not book.is_collected:
            raise HTTPException(status_code=400, detail="Book has not been collected.")
        user = UserController.get_user(user_id=reader_id)
        if Reader.get(book=book, user=user):
            raise HTTPException(status_code=400, detail="Book has already been read by user.")
        Reader(book=book, user=user, read_date=read_date)
        flush()
        return book.to_model()


@router.delete(path="/{book_id}/read", responses={404: {"model": ErrorResponse}})
def unread_book(book_id: int, reader_id: int = Body(embed=True)) -> Book:
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
        return book.to_model()


@router.post(
    path="/{book_id}/creators",
    responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}},
)
def add_creator(*, book_id: int, new_creator: BookCreatorIn) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        creator = CreatorController.get_creator(creator_id=new_creator.creator_id)
        if BookCreator.get(book=book, creator=creator):
            raise HTTPException(
                status_code=409,
                detail="The Creator is already linked to this Book.",
            )
        BookCreator(
            book=book,
            creator=creator,
            roles=[RoleController.get_role(role_id=x) for x in new_creator.role_ids],
        )
        flush()
        return book.to_model()


@router.delete(
    path="/{book_id}/creators",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def remove_creator(*, book_id: int, creator_id: int = Body(embed=True), role_id: int | None = Body(default=None, embed=True)) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        creator = CreatorController.get_creator(creator_id=creator_id)
        book_creator = BookCreator.get(book=book, creator=creator)
        if not book_creator:
            raise HTTPException(
                status_code=400,
                detail="The Creator isnt associated with this Book.",
            )
        if role_id:
            role = RoleController.get_role(role_id=role_id)
            if role in book_creator.roles:
                book_creator.roles.remove(role)
                flush()
                if not book_creator.roles:
                    book_creator.delete()
            else:
              raise HTTPException(
                  status_code=400,
                  detail="The Role isnt associated with this BookCreator."
              )
        else:
            book_creator.delete()
        flush()
        return book.to_model()


@router.post(
    path="/{book_id}/genres",
    responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}},
)
def add_genre(*, book_id: int, genre_id: int = Body(embed=True)) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        genre = GenreController.get_genre(genre_id=genre_id)
        if genre in book.genres:
            raise HTTPException(status_code=409, detail="The Genre is already linked to this Book.")
        book.genres.add(genre)
        flush()
        return book.to_model()


@router.delete(
    path="/{book_id}/genres",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def remove_genre(*, book_id: int, genre_id: int = Body(embed=True)) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        genre = GenreController.get_genre(genre_id=genre_id)
        if genre not in book.genres:
            raise HTTPException(status_code=400, detail="The Genre isnt associated with this Book.")
        book.genres.remove(genre)
        flush()
        return book.to_model()


@router.post(
    path="/{book_id}/series",
    responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}},
)
def add_series(*, book_id: int, new_series: BookSeriesIn) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        series = SeriesController.get_series(series_id=new_series.series_id)
        if BookSeries.get(book=book, series=series):
            raise HTTPException(
                status_code=409,
                detail="The Series is already linked to this Book.",
            )
        BookSeries(book=book, series=series, number=new_series.number)
        flush()
        return book.to_model()


@router.delete(
    path="/{book_id}/series",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def remove_series(*, book_id: int, series_id: int = Body(embed=True)) -> Book:
    with db_session:
        book = BookController.get_book(book_id=book_id)
        series = SeriesController.get_series(series_id=series_id)
        book_series = BookSeries.get(book=book, series=series)
        if not book_series:
            raise HTTPException(
                status_code=400,
                detail="The Series isnt associated with this Book.",
            )
        book_series.delete()
        flush()
        return book.to_model()
