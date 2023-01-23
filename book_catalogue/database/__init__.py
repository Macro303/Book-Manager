from __future__ import annotations
__all__ = ["DATABASE_PATH"]

from enum import Enum

from book_catalogue import get_data_root
from book_catalogue.database.enum_converter import EnumConverter
from book_catalogue.database.tables import db

DATABASE_PATH = get_data_root() / "book-catalogue.sqlite"
db.bind(
    provider="sqlite",
    filename=str(DATABASE_PATH),
    create_db=True,
)
db.provider.converter_classes.append((Enum, EnumConverter))
db.generate_mapping(create_tables=True)


@db.on_connect(provider="sqlite")
def sqlite_case_sensitivity(database, connection) -> None:  # noqa: ANN001, ARG001
    cursor = connection.cursor()
    cursor.execute("PRAGMA case_sensitive_like = OFF")
