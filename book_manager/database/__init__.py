__all__ = ["DATABASE_PATH"]

from book_manager import get_project_root
from book_manager.database.tables import db

DATABASE_PATH = get_project_root() / "book-manager.sqlite"

db.bind(
    provider="sqlite",
    filename=str(DATABASE_PATH),
    create_db=True,
)
db.generate_mapping(create_tables=True)


@db.on_connect(provider="sqlite")
def sqlite_case_sensitivity(database, connection):
    cursor = connection.cursor()
    cursor.execute("PRAGMA case_sensitive_like = OFF")
