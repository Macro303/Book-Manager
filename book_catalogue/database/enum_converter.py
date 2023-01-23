from __future__ import annotations
__all__ = ["EnumConverter"]

from enum import Enum
from typing import Any

from pony.orm.dbapiprovider import StrConverter


class EnumConverter(StrConverter):
    def validate(self, val: Any, obj=None) -> Enum:  # noqa: ANN001, ARG002, ANN401
        if not isinstance(val, Enum):
            raise ValueError(f"Must be an Enum.  Got {type(val)}")
        return val

    def py2sql(self, val: Enum) -> str:
        return val.name

    def sql2py(self, value: str) -> Enum:
        return self.py_type[value]
