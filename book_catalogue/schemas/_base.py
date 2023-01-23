from __future__ import annotations
__all__ = ["BaseModel"]

from pydantic import BaseModel as PyModel, Extra


class BaseModel(PyModel):
    class Config:
        allow_population_by_field_name = True
        anystr_strip_whitespace = True
        extra = Extra.forbid
