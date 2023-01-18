__all__ = ["Series"]

from pony.orm import Database, Optional, PrimaryKey, Required, Set

from book_catalogue import schemas

db = Database()


class User(db.Entity):
    _table_ = "Users"

    user_id: int = PrimaryKey(int, auto=True)
    username: str = Required(str)
    role: int = Optional(int, default=0)

    wished_books: list["Book"] = Set("Book", reverse="wisher")
    read_books: list["Book"] = Set("Book", table="Readers", reverse="readers")

    def to_schema(self) -> schemas.User:
        return schemas.User(user_id=self.user_id, username=self.username, role=self.role)


class Publisher(db.Entity):
    _table_ = "Publishers"

    publisher_id: int = PrimaryKey(int, auto=True)
    name: str = Required(str, unique=True)

    books: list["Book"] = Set("Book")

    def to_schema(self) -> schemas.Publisher:
        return schemas.Publisher(publisher_id=self.publisher_id, name=self.name)


class Book(db.Entity):
    _table_ = "Books"

    authors: list["BookAuthor"] = Set("BookAuthor")
    book_id: int = PrimaryKey(int, auto=True)
    description: str | None = Optional(str, nullable=True)
    format: str | None = Optional(str, nullable=True)
    image_url: str = Required(str)
    publisher: Publisher | None = Optional(Publisher, nullable=True)
    readers: list[User] = Set(User, table="Readers", reverse="read_books")
    series: list["BookSeries"] = Set("BookSeries")
    subtitle: str | None = Optional(str, nullable=True)
    title: str = Required(str)
    wisher: User = Optional(User, nullable=True, reverse="wished_books")

    goodreads_id: str | None = Optional(str, nullable=True)
    google_books_id: str | None = Optional(str, nullable=True)
    isbn_10: str | None = Optional(str, nullable=True, unique=True)
    isbn_13: str | None = Optional(str, nullable=True, unique=True)
    library_thing_id: str | None = Optional(str, nullable=True)
    open_library_id: str | None = Optional(str, nullable=True, unique=True)

    def to_schema(self) -> schemas.Book:
        return schemas.Book(
            authors=sorted(x.to_schema() for x in self.authors),
            description=self.description,
            format=self.format,
            identifiers=schemas.BookIdentifiers(
                book_id=self.book_id,
                goodreads_id=self.goodreads_id,
                google_books_id=self.google_books_id,
                isbn=self.isbn_13 or self.isbn_10,
                library_thing_id=self.library_thing_id,
                open_library_id=self.open_library_id,
            ),
            image_url=self.image_url,
            publisher=self.publisher.to_schema() if self.publisher else None,
            readers=sorted(x.to_schema() for x in self.readers),
            series=sorted(
                schemas.Series(
                    number=x.number,
                    series_id=x.series.series_id,
                    title=x.series.title,
                )
                for x in self.series
            ),
            subtitle=self.subtitle,
            title=self.title,
            wisher=self.wisher.to_schema() if self.wisher else None,
        )


class Role(db.Entity):
    _table_ = "Roles"

    name: str = Required(str, unique=True)
    role_id: int = PrimaryKey(int, auto=True)

    authors: list["BookAuthor"] = Set("BookAuthor", table="Books_Authors_Roles")


class BookAuthor(db.Entity):
    _table_ = "Books_Authors"

    author: "Author" = Required("Author")
    book: Book = Required(Book)
    roles: list[Role] = Set(Role, table="Books_Authors_Roles")

    PrimaryKey(book, author)

    def to_schema(self) -> schemas.Author:
        return schemas.Author(
            author_id=self.author.author_id,
            name=self.author.name,
            roles=sorted(
                schemas.AuthorRole(
                    name=x.name,
                    role_id=x.role_id,
                )
                for x in self.roles
            ),
        )


class Author(db.Entity):
    _table_ = "Authors"

    author_id: int = PrimaryKey(int, auto=True)
    name: str = Required(str)
    open_library_id: str | None = Optional(str, nullable=True, unique=True)

    books: list[BookAuthor] = Set(BookAuthor)

    def to_schema(self) -> schemas.Author:
        return schemas.Author(
            author_id=self.author_id,
            name=self.name,
            roles=[],
        )


class BookSeries(db.Entity):
    _table_ = "Books_Series"

    book: Book = Required(Book)
    number: int | None = Optional(int, nullable=True)
    series: "Series" = Required("Series")

    PrimaryKey(book, series)


class Series(db.Entity):
    series_id: int = PrimaryKey(int, auto=True)
    title: str = Required(str, unique=True)

    books: list[BookSeries] = Set(BookSeries)
