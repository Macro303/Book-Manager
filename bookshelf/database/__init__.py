__all__ = ["sqlite_filepath"]

from enum import Enum

from bookshelf import get_data_root
from bookshelf.database.enum_converter import EnumConverter
from bookshelf.database.tables import db
from bookshelf.settings import Settings

sqlite_filepath = get_data_root() / Settings().database.name
db.bind(
    provider="sqlite",
    filename=str(sqlite_filepath),
    create_db=True,
)
db.provider.converter_classes.append((Enum, EnumConverter))
db.generate_mapping(create_tables=True)


@db.on_connect(provider="sqlite")
def sqlite_case_sensitivity(database, connection) -> None:  # noqa: ANN001, ARG001
    cursor = connection.cursor()
    cursor.execute("PRAGMA case_sensitive_like = OFF")
