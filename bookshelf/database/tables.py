__all__ = [
    "Creator",
    "Book",
    "BookCreator",
    "BookSeries",
    "Format",
    "Genre",
    "Publisher",
    "Role",
    "Series",
    "User",
]

from datetime import date
from typing import Optional as Opt

from pony.orm import Database, Optional, PrimaryKey, Required, Set

from bookshelf.models.book import Book as ModelBook, Identifiers as BookIdentifiers
from bookshelf.models.creator import Creator as ModelCreator, Identifiers as CreatorIdentifiers
from bookshelf.models.format import Format as ModelFormat
from bookshelf.models.genre import Genre as ModelGenre
from bookshelf.models.publisher import Publisher as ModelPublisher
from bookshelf.models.role import Role as ModelRole
from bookshelf.models.series import Series as ModelSeries
from bookshelf.models.user import User as ModelUser

db = Database()


class Book(db.Entity):
    _table_ = "books"

    book_id: int = PrimaryKey(int, auto=True)
    creators: list["BookCreator"] = Set("BookCreator")
    description: str | None = Optional(str, nullable=True)
    format: Opt["Format"] = Optional("Format", nullable=True)
    genres: list["Genre"] = Set("Genre", table="books_genres")
    image_url: str = Required(str)
    is_collected: bool = Optional(bool, default=False)
    publish_date: date | None = Optional(date, nullable=True)
    publisher: Opt["Publisher"] = Optional("Publisher", nullable=True)
    readers: list["Reader"] = Set("Reader")
    series: list["BookSeries"] = Set("BookSeries")
    subtitle: str | None = Optional(str, nullable=True)
    title: str = Required(str)
    wishers: list["User"] = Set("User", table="wishers", reverse="wished_books")

    goodreads_id: str | None = Optional(str, nullable=True)
    google_books_id: str | None = Optional(str, nullable=True)
    isbn: str | None = Optional(str, unique=True, nullable=True)
    library_thing_id: str | None = Optional(str, nullable=True)
    open_library_id: str | None = Optional(str, nullable=True, unique=True)

    def to_model(self) -> ModelBook:
        return ModelBook(
            creators=sorted({x.to_model() for x in self.creators}),
            book_id=self.book_id,
            description=self.description,
            format=self.format.to_model() if self.format else None,
            genres=sorted({x.to_model() for x in self.genres}),
            identifiers=BookIdentifiers(
                goodreads_id=self.goodreads_id,
                google_books_id=self.google_books_id,
                isbn=self.isbn,
                library_thing_id=self.library_thing_id,
                open_library_id=self.open_library_id,
            ),
            image_url=self.image_url,
            is_collected=self.is_collected,
            publish_date=self.publish_date,
            publisher=self.publisher.to_model() if self.publisher else None,
            readers=sorted({x.user.to_model() for x in self.readers}),
            series=sorted({x.to_model() for x in self.series}),
            subtitle=self.subtitle,
            title=self.title,
            wishers=sorted({x.to_model() for x in self.wishers}),
        )


class BookCreator(db.Entity):
    _table_ = "books_creators"

    book: Book = Required(Book)
    creator: "Creator" = Required("Creator")
    roles: list["Role"] = Set("Role", table="books_creators_roles")

    PrimaryKey(book, creator)

    def to_model(self) -> ModelCreator:
        temp = self.creator.to_model()
        temp.roles = sorted({x.to_model() for x in self.roles})
        return temp


class BookSeries(db.Entity):
    _table_ = "books_series"

    book: Book = Required(Book)
    number: int = Optional(int, default=0)
    series: "Series" = Required("Series")

    PrimaryKey(book, series)

    def to_model(self) -> ModelSeries:
        temp = self.series.to_model()
        temp.number = self.number
        return temp


class Creator(db.Entity):
    _table_ = "creators"

    bio: str | None = Optional(str, nullable=True)
    creator_id: int = PrimaryKey(int, auto=True)
    image_url: str | None = Optional(str, nullable=True)
    name: str = Required(str, unique=True)

    goodreads_id: str | None = Optional(str, nullable=True)
    library_thing_id: str | None = Optional(str, nullable=True)
    open_library_id: str | None = Optional(str, nullable=True, unique=True)

    books: list[BookCreator] = Set(BookCreator)

    def to_model(self) -> ModelCreator:
        return ModelCreator(
            creator_id=self.creator_id,
            bio=self.bio,
            identifiers=CreatorIdentifiers(
                goodreads_id=self.goodreads_id,
                library_thing_id=self.library_thing_id,
                open_library_id=self.open_library_id,
            ),
            image_url=self.image_url,
            name=self.name,
            roles=[],
        )


class Format(db.Entity):
    _table_ = "formats"

    format_id: int = PrimaryKey(int, auto=True)
    name: str = Required(str, unique=True)

    books: list[Book] = Set(Book)

    def to_model(self) -> ModelFormat:
        return ModelFormat(format_id=self.format_id, name=self.name)


class Genre(db.Entity):
    _table_ = "genres"

    genre_id: int = PrimaryKey(int, auto=True)
    name: str = Required(str, unique=True)

    books: list[Book] = Set(Book, table="books_genres")

    def to_model(self) -> ModelGenre:
        return ModelGenre(genre_id=self.genre_id, name=self.name)


class Publisher(db.Entity):
    _table_ = "publishers"

    publisher_id: int = PrimaryKey(int, auto=True)
    name: str = Required(str, unique=True)

    books: list[Book] = Set(Book)

    def to_model(self) -> ModelPublisher:
        return ModelPublisher(publisher_id=self.publisher_id, name=self.name)


class Reader(db.Entity):
    _table_ = "readers"

    book: Book = Required(Book)
    user: "User" = Required("User")
    read_date: date | None = Optional(date, nullable=True)

    PrimaryKey(book, user)


class Role(db.Entity):
    _table_ = "roles"

    name: str = Required(str, unique=True)
    role_id: int = PrimaryKey(int, auto=True)

    creators: list[BookCreator] = Set(BookCreator, table="books_creators_roles")

    def to_model(self) -> ModelRole:
        return ModelRole(name=self.name, role_id=self.role_id)


class Series(db.Entity):
    _table_ = "series"

    series_id: int = PrimaryKey(int, auto=True)
    name: str = Required(str, unique=True)
    is_reading_order: bool = Optional(bool, default=False, sql_default=False)

    books: list[BookSeries] = Set(BookSeries)

    def to_model(self) -> ModelSeries:
        return ModelSeries(
            series_id=self.series_id,
            name=self.name,
            is_reading_order=self.is_reading_order,
        )


class User(db.Entity):
    _table_ = "users"

    image_url: str | None = Optional(str, nullable=True)
    role: int = Optional(int, default=0)
    user_id: int = PrimaryKey(int, auto=True)
    username: str = Required(str)

    read_books: list[Reader] = Set(Reader)
    wished_books: list[Book] = Set(Book, table="wishers", reverse="wishers")

    def to_model(self) -> ModelUser:
        return ModelUser(
            user_id=self.user_id,
            username=self.username,
            role=self.role,
            image_url=self.image_url,
        )
