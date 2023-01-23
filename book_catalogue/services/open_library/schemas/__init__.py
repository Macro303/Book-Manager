from __future__ import annotations
__all__ = ["BaseModel", "DatetimeResource", "TextResource", "Resource"]

from datetime import datetime

from pydantic import BaseModel as PyModel, Extra


class BaseModel(PyModel):
    class Config:
        allow_population_by_field_name = True
        anystr_strip_whitespace = True
        extra = Extra.forbid


class DatetimeResource(BaseModel):
    type: str
    value: datetime


class Resource(BaseModel):
    key: str


class TextResource(BaseModel):
    type: str
    value: str
