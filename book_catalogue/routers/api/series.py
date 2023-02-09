__all__ = ["router"]

from fastapi import APIRouter, Body, HTTPException
from pony.orm import db_session, flush

from book_catalogue.controllers.book import BookController
from book_catalogue.controllers.series import SeriesController
from book_catalogue.database.tables import BookSeries
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas.series import SeriesRead, SeriesWrite

router = APIRouter(prefix="/series", tags=["Series"])


@router.get(path="")
def list_series() -> list[SeriesRead]:
    with db_session:
        return sorted({x.to_schema() for x in SeriesController.list_series()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_series(new_series: SeriesWrite) -> SeriesRead:
    with db_session:
        return SeriesController.create_series(new_series=new_series).to_schema()


@router.get(path="/{series_id}", responses={404: {"model": ErrorResponse}})
def get_series(series_id: int) -> SeriesRead:
    with db_session:
        return SeriesController.get_series(series_id=series_id).to_schema()


@router.patch(path="/{series_id}", responses={404: {"model": ErrorResponse}})
def update_series(series_id: int, updates: SeriesWrite) -> SeriesRead:
    with db_session:
        return SeriesController.update_series(series_id=series_id, updates=updates).to_schema()


@router.delete(path="/{series_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_series(series_id: int):
    with db_session:
        SeriesController.delete_series(series_id=series_id)


@router.post(path="/{series_id}/books", responses={404: {"model": ErrorResponse}})
def add_book_to_series(
    series_id: int, book_id: int = Body(embed=True), series_num: int = Body(default=0, embed=True)
) -> SeriesRead:
    with db_session:
        series = SeriesController.get_series(series_id=series_id)
        book = BookController.get_book(book_id=book_id)
        if book in [x.book for x in series.books]:
            raise HTTPException(status_code=400, detail="Book already exists in Series")
        BookSeries(book=book, series=series, number=series_num)
        flush()
        return series.to_schema()


@router.delete(path="/{series_id}/books/{book_id}", responses={404: {"model": ErrorResponse}})
def remove_book_from_series(series_id: int, book_id: int):
    with db_session:
        series = SeriesController.get_series(series_id=series_id)
        book = BookController.get_book(book_id=book_id)
        book_series = BookSeries.get(book=book, series=series)
        if not book_series:
            raise HTTPException(status_code=400, detail="Book doesn't exist in Series")
        book_series.delete()
        flush()
        return series.to_schema()
