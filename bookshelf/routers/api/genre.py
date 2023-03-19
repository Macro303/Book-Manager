__all__ = ["router"]

from fastapi import APIRouter, Body, HTTPException
from pony.orm import db_session, flush

from bookshelf.controllers.book import BookController
from bookshelf.controllers.genre import GenreController
from bookshelf.models.genre import Genre, GenreIn
from bookshelf.responses import ErrorResponse

router = APIRouter(prefix="/genres", tags=["Genres"])


@router.get(path="")
def list_genres() -> list[Genre]:
    with db_session:
        return sorted({x.to_model() for x in GenreController.list_genres()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_genre(new_genre: GenreIn) -> Genre:
    with db_session:
        return GenreController.create_genre(new_genre=new_genre).to_model()


@router.get(path="/{genre_id}", responses={404: {"model": ErrorResponse}})
def get_genre(genre_id: int) -> Genre:
    with db_session:
        return GenreController.get_genre(genre_id=genre_id).to_model()


@router.patch(path="/{genre_id}", responses={404: {"model": ErrorResponse}})
def update_genre(genre_id: int, updates: GenreIn) -> Genre:
    with db_session:
        return GenreController.update_genre(genre_id=genre_id, updates=updates).to_model()


@router.delete(path="/{genre_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_genre(genre_id: int):
    with db_session:
        GenreController.delete_genre(genre_id=genre_id)


@router.post(
    path="/{genre_id}/books",
    responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}},
)
def add_book(*, genre_id: int, book_id: int = Body(embed=True)) -> Genre:
    with db_session:
        genre = GenreController.get_genre(genre_id=genre_id)
        book = BookController.get_book(book_id=book_id)
        if book in genre.books:
            raise HTTPException(status_code=409, detail="The Book is already linked to this Genre.")
        genre.books.add(book)
        flush()
        return genre.to_model()


@router.delete(
    path="/{genre_id}/books",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def remove_book(*, genre_id: int, book_id: int = Body(embed=True)) -> Genre:
    with db_session:
        genre = GenreController.get_genre(genre_id=genre_id)
        book = BookController.get_book(book_id=book_id)
        if book not in genre.books:
            raise HTTPException(status_code=400, detail="The Book isnt associated with this Genre.")
        genre.books.remove(book)
        flush()
        return genre.to_model()
