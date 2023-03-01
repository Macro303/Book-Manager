__all__ = ["FormatController"]

from fastapi import HTTPException
from pony.orm import flush

from bookshelf.database.tables import Format
from bookshelf.models.format import FormatIn


class FormatController:
    @classmethod
    def list_formats(cls) -> list[Format]:
        return Format.select()

    @classmethod
    def create_format(cls, new_format: FormatIn) -> Format:
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
    def update_format(cls, format_id: int, updates: FormatIn):
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
