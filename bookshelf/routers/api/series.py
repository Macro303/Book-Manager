__all__ = ["router"]

from fastapi import APIRouter, Body, HTTPException
from pony.orm import db_session, flush

from bookshelf.controllers.book import BookController
from bookshelf.controllers.series import SeriesController
from bookshelf.database.tables import BookSeries
from bookshelf.models.series import Series, SeriesBookIn, SeriesIn
from bookshelf.responses import ErrorResponse

router = APIRouter(prefix="/series", tags=["Series"])


@router.get(path="")
def list_series() -> list[Series]:
    with db_session:
        return sorted({x.to_model() for x in SeriesController.list_series()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_series(new_series: SeriesIn) -> Series:
    with db_session:
        return SeriesController.create_series(new_series=new_series).to_model()


@router.get(path="/{series_id}", responses={404: {"model": ErrorResponse}})
def get_series(series_id: int) -> Series:
    with db_session:
        return SeriesController.get_series(series_id=series_id).to_model()


@router.patch(path="/{series_id}", responses={404: {"model": ErrorResponse}})
def update_series(series_id: int, updates: SeriesIn) -> Series:
    with db_session:
        return SeriesController.update_series(series_id=series_id, updates=updates).to_model()


@router.delete(path="/{series_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_series(series_id: int):
    with db_session:
        SeriesController.delete_series(series_id=series_id)


@router.post(
    path="/{series_id}/books",
    responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}},
)
def add_book(
    series_id: int,
    new_book: SeriesBookIn,
) -> Series:
    with db_session:
        series = SeriesController.get_series(series_id=series_id)
        book = BookController.get_book(book_id=new_book.book_id)
        if BookSeries.get(book=book, series=series):
            raise HTTPException(
                status_code=409,
                detail="The Book is already linked to this Series.",
            )
        BookSeries(book=book, series=series, number=new_book.number)
        flush()
        return series.to_model()


@router.delete(
    path="/{series_id}/books",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def remove_book(series_id: int, book_id: int = Body(embed=True)) -> Series:
    with db_session:
        series = SeriesController.get_series(series_id=series_id)
        book = BookController.get_book(book_id=book_id)
        book_series = BookSeries.get(book=book, series=series)
        if not book_series:
            raise HTTPException(
                status_code=400,
                detail="The Book isnt associated with this Series.",
            )
        book_series.delete()
        flush()
        return series.to_model()
