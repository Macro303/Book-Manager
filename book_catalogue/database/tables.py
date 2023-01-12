__all__ = ["Series"]

from pony.orm import Database, Optional, PrimaryKey, Required, Set

from book_catalogue import schemas

db = Database()


class User(db.Entity):
    _table_ = "Users"

    user_id: int = PrimaryKey(int, auto=True)
    username: str = Required(str)

    wished_books: list["Book"] = Set("Book", reverse="wisher")
    read_books: list["Book"] = Set("Book", table="Readers", reverse="readers")

    def to_schema(self) -> schemas.User:
        return schemas.User(user_id=self.user_id, username=self.username)


class Book(db.Entity):
    _table_ = "Books"

    book_id: int = PrimaryKey(int, auto=True)
    title: str = Required(str)
    subtitle: str | None = Optional(str, nullable=True)
    authors: list["Author"] = Set("Author", table="BookAuthors")
    format: str | None = Optional(str, nullable=True)
    series: list["Series"] = Set("Series", table="BookSeries")
    publishers: list["Publisher"] = Set("Publisher", table="BookPublishers")
    wisher: User = Optional(User, nullable=True, reverse="wished_books")
    readers: list[User] = Set(User, table="Readers", reverse="read_books")

    isbn_10: str | None = Optional(str, nullable=True, unique=True)
    isbn_13: str | None = Optional(str, nullable=True, unique=True)
    open_library_id: str | None = Optional(str, nullable=True, unique=True)
    google_books_id: str | None = Optional(str, nullable=True, unique=True)
    goodreads_id: str | None = Optional(str, nullable=True, unique=True)
    library_thing_id: str | None = Optional(str, nullable=True, unique=True)
    image_url: str | None = Optional(str, nullable=True)

    def to_schema(self) -> schemas.Book:
        return schemas.Book(
            title=self.title,
            subtitle=self.subtitle,
            authors=sorted(x.to_schema() for x in self.authors),
            format=self.format,
            series=[x.to_schema() for x in self.series],
            publishers=sorted(x.to_schema() for x in self.publishers),
            wisher=self.wisher.to_schema() if self.wisher else None,
            readers=sorted(x.to_schema() for x in self.readers),
            identifiers=schemas.BookIdentifiers(
                book_id=self.book_id,
                isbn_10=self.isbn_10,
                isbn_13=self.isbn_13,
                open_library_id=self.open_library_id,
                google_books_id=self.google_books_id,
                goodreads_id=self.goodreads_id,
                library_thing_id=self.library_thing_id,
            ),
            image_url=self.image_url,
        )


class Author(db.Entity):
    _table_ = "Authors"

    author_id: int = PrimaryKey(int, auto=True)
    name: str = Required(str)

    open_library_id: str | None = Optional(str, nullable=True, unique=True)

    books: list[Book] = Set(Book, table="BookAuthors")

    def to_schema(self) -> schemas.Author:
        return schemas.Author(
            name=self.name,
            identifiers=schemas.AuthorIdentifiers(
                author_id=self.author_id, open_library_id=self.open_library_id
            ),
        )


class Publisher(db.Entity):
    _table_ = "Publishers"

    publisher_id: int = PrimaryKey(int, auto=True)
    name: str = Required(str, unique=True)

    books: list[Book] = Set(Book, table="BookPublishers")

    def to_schema(self) -> schemas.Publisher:
        return schemas.Publisher(publisher_id=self.publisher_id, name=self.name)


class Series(db.Entity):
    series_id: int = PrimaryKey(int, auto=True)
    title: str = Required(str)

    books: list[Book] = Set(Book, table="BookSeries")

    def to_schema(self) -> schemas.Series:
        return schemas.Series(series_id=self.series_id, title=self.title)
