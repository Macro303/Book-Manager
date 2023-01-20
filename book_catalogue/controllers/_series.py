__all__ = ["SeriesController"]

import logging

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import Series

LOGGER = logging.getLogger(__name__)


class SeriesController:
    @staticmethod
    def list_series() -> list[Series]:
        return Series.select()

    @staticmethod
    def create_series(title: str) -> Series:
        if Series.get(title=title):
            raise HTTPException(status_code=409, detail="Series already exists.")
        series = Series(title=title)
        flush()
        return series

    @staticmethod
    def get_series(series_id: int) -> Series:
        if series := Series.get(series_id=series_id):
            return series
        raise HTTPException(status_code=404, detail="Series not found.")

    @staticmethod
    def delete_series(series_id: int):
        if series := Series.get(series_id=series_id):
            return series.delete()
        raise HTTPException(status_code=404, detail="Series not found.")
