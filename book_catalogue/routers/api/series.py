from __future__ import annotations

__all__ = ["router"]

from fastapi import APIRouter
from pony.orm import db_session

from book_catalogue.controllers.series import SeriesController
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
