__all__ = ["SeriesController"]

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import Series
from book_catalogue.schemas.series import SeriesWrite


class SeriesController:
    @classmethod
    def list_series(cls) -> list[Series]:
        return Series.select()

    @classmethod
    def create_series(cls, new_series: SeriesWrite) -> Series:
        if Series.get(title=new_series.title):
            raise HTTPException(status_code=409, detail="Series already exists.")
        series = Series(title=new_series.title)
        flush()
        return series

    @classmethod
    def get_series(cls, series_id: int) -> Series:
        if series := Series.get(series_id=series_id):
            return series
        raise HTTPException(status_code=404, detail="Series not found.")

    @classmethod
    def update_series(cls, series_id: int, updates: SeriesWrite) -> Series:
        series = cls.get_series(series_id=series_id)
        series.title = updates.title
        flush()
        return series

    @classmethod
    def delete_series(cls, series_id: int):
        series = cls.get_series(series_id=series_id)
        series.delete()
