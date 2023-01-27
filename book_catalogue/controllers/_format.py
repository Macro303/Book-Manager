from __future__ import annotations

__all__ = ["FormatController"]

import logging

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import Format
from book_catalogue.schemas._format import NewFormat

LOGGER = logging.getLogger(__name__)


class FormatController:
    @classmethod
    def list_formats(cls) -> list[Format]:
        return Format.select()

    @classmethod
    def create_format(cls, new_format: NewFormat) -> Format:
        if Format.get(name=new_format.name):
            raise HTTPException(status_code=409, detail="Format already exists.")
        format = Format(name=new_format.name)
        flush()
        return format

    @classmethod
    def get_format(cls, format_id: int) -> Format:
        if format := Format.get(format_id=format_id):
            return format
        raise HTTPException(status_code=404, detail="Format not found.")

    @classmethod
    def update_format(cls, format_id: int, updates: NewFormat):
        format = cls.get_format(format_id=format_id)
        format.name = updates.name
        flush()
        return format

    @classmethod
    def delete_format(cls, format_id: int):
        format = cls.get_format(format_id=format_id)
        format.delete()

    @classmethod
    def get_format_by_name(cls, name: str) -> Format:
        if format := Format.get(name=name):
            return format
        raise HTTPException(status_code=404, detail="Format not found.")
