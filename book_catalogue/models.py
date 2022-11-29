__all__ = ["User", "Book", "Author", "Series"]

from typing import Optional

from sqlalchemy import Column, ForeignKey, Integer, String, Table
from sqlalchemy.orm import relationship

from book_catalogue import schemas
from book_catalogue.database import Base

readers_table = Table(
    "Readers",
    Base.metadata,
    Column("user_id", ForeignKey("Users.user_id"), primary_key=True),
    Column("book_id", ForeignKey("Books.isbn"), primary_key=True),
)

book_authors_table = Table(
    "BookAuthors",
    Base.metadata,
    Column("book_id", ForeignKey("Books.isbn"), primary_key=True),
    Column("author_id", ForeignKey("Authors.author_id"), primary_key=True),
)

book_series_table = Table(
    "BookSeries",
    Base.metadata,
    Column("book_id", ForeignKey("Books.isbn"), primary_key=True),
    Column("series_id", ForeignKey("Series.series_id"), primary_key=True),
)


class User(Base):
    __tablename__ = "Users"

    user_id = Column(Integer, primary_key=True)
    username = Column(String, nullable=False)
    wished_books = relationship("Book", back_populates="wisher")
    read_books = relationship("Book", secondary=readers_table, back_populates="readers")

    def to_schema(self) -> schemas.User:
        return schemas.User(username=self.username)

    def __lt__(self, other):
        if not isinstance(other, User):
            raise NotImplementedError()
        return self.username < other.username

    def __eq__(self, other):
        if not isinstance(other, User):
            raise NotImplementedError()
        return self.username == other.username

    def __hash__(self):
        return hash((type(self), self.username))


class Book(Base):
    __tablename__ = "Books"

    isbn = Column(String, primary_key=True)
    title = Column(String, nullable=False)
    subtitle = Column(String)
    authors = relationship("Author", secondary=book_authors_table, back_populates="books_written")
    format = Column(String)
    series = relationship("Series", secondary=book_series_table, back_populates="books")
    publisher = Column(String)
    wisher_id = Column(Integer, ForeignKey("Users.user_id"))
    wisher = relationship("User", back_populates="wished_books")
    readers = relationship("User", secondary=readers_table, back_populates="read_books")
    open_library_id = Column(String, nullable=False)
    google_books_id = Column(String)
    goodreads_id = Column(String)
    library_thing_id = Column(String)

    def to_schema(self) -> schemas.Book:
        return schemas.Book(
            isbn=self.isbn,
            title=self.title,
            subtitle=self.subtitle,
            authors=sorted({x.name for x in self.authors}),
            format=self.format,
            series=sorted({x.name for x in self.series}),
            publisher=self.publisher,
            readers=sorted({x.username for x in self.readers}),
            wisher=self.wisher.username if self.wisher else None,
            identifiers=schemas.Identifiers(
                open_library_id=self.open_library_id,
                google_books_id=self.google_books_id,
                goodreads_id=self.goodreads_id,
                library_thing_id=self.library_thing_id,
            ),
            images=schemas.Images(
                small=f"https://covers.openlibrary.org/b/OLID/{self.open_library_id}-S.jpg"
                if self.open_library_id
                else f"http://books.google.com/books/content?id={self.google_books_id}&"
                "printsec=frontcover&img=1&zoom=1"
                if self.google_books_id
                else None,
                medium=f"https://covers.openlibrary.org/b/OLID/{self.open_library_id}-M.jpg"
                if self.open_library_id
                else f"http://books.google.com/books/content?id={self.google_books_id}&"
                "printsec=frontcover&img=1&zoom=5"
                if self.google_books_id
                else None,
                large=f"https://covers.openlibrary.org/b/OLID/{self.open_library_id}-L.jpg"
                if self.open_library_id
                else f"http://books.google.com/books/content?id={self.google_books_id}&"
                "printsec=frontcover&img=1&zoom=10"
                if self.google_books_id
                else None,
            ),
        )

    @property
    def first_series(self) -> Optional["Series"]:
        temp = sorted(self.series)
        return temp[0] if temp else None

    @property
    def first_author(self) -> Optional["Author"]:
        temp = sorted(self.authors)
        return temp[0] if temp else None

    def __lt__(self, other):
        if not isinstance(other, Book):
            raise NotImplementedError()
        if (self.first_series or "") != (other.first_series or ""):
            return (self.first_series or "") < (other.first_series or "")
        if self.title != other.title:
            return self.title < other.title
        if (self.subtitle or "") != (other.subtitle or ""):
            return self.subtitle < other.subtitle
        return (self.first_author or "") < (other.first_author or "")

    def __eq__(self, other):
        if not isinstance(other, Book):
            raise NotImplementedError()
        return (
            (self.first_series or ""),
            self.title,
            (self.subtitle or ""),
            (self.first_author or ""),
        ) == (
            (other.first_series or ""),
            other.title,
            (other.subtitle or ""),
            (other.first_author or ""),
        )

    def __hash__(self):
        return hash(
            (
                type(self),
                (self.first_series or ""),
                self.title,
                (self.subtitle or ""),
                (self.first_author or ""),
            )
        )


class Author(Base):
    __tablename__ = "Authors"

    author_id = Column(Integer, primary_key=True)
    name = Column(String, nullable=False)
    books_written = relationship("Book", secondary=book_authors_table, back_populates="authors")

    def __lt__(self, other):
        if not isinstance(other, Author):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other):
        if not isinstance(other, Author):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class Series(Base):
    __tablename__ = "Series"

    series_id = Column(Integer, primary_key=True)
    name = Column(String, nullable=False)
    books = relationship("Book", secondary=book_series_table, back_populates="series")

    def __lt__(self, other):
        if not isinstance(other, Series):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other):
        if not isinstance(other, Series):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))
