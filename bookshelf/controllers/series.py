__all__ = ["SeriesController"]

from fastapi import HTTPException
from pony.orm import flush

from bookshelf.database.tables import Series
from bookshelf.models.series import SeriesIn


class SeriesController:
    @classmethod
    def list_series(cls) -> list[Series]:
        return Series.select()

    @classmethod
    def create_series(cls, new_series: SeriesIn) -> Series:
        if Series.get(name=new_series.name):
            raise HTTPException(status_code=409, detail="Series already exists.")
        series = Series(name=new_series.name, is_reading_order=new_series.is_reading_order)
        flush()
        return series

    @classmethod
    def get_series(cls, series_id: int) -> Series:
        if series := Series.get(series_id=series_id):
            return series
        raise HTTPException(status_code=404, detail="Series not found.")

    @classmethod
    def update_series(cls, series_id: int, updates: SeriesIn) -> Series:
        series = cls.get_series(series_id=series_id)
        series.name = updates.name
        series.is_reading_order = updates.is_reading_order
        flush()
        return series

    @classmethod
    def delete_series(cls, series_id: int):
        series = cls.get_series(series_id=series_id)
        series.delete()

    @classmethod
    def get_series_by_name(cls, name: str) -> Series:
        if series := Series.get(name=name):
            return series
        raise HTTPException(status_code=404, detail="Series not found.")
