__all__ = ["Book"]

from datetime import date

from pydantic import BaseModel, Extra, Field


def to_camel_case(value: str) -> str:
    temp = value.replace("_", " ").title().replace(" ", "")
    return temp[0].lower() + temp[1:]


class CamelModel(BaseModel):
    class Config:
        alias_generator = to_camel_case
        allow_population_by_field_name = True
        anystr_strip_whitespace = True
        extra = Extra.forbid


class Book(CamelModel):
    title: str
    authors: list[str]
    publisher: str
    published_date: date
