from sqlalchemy import Column, ForeignKey, Integer, String
from sqlalchemy.orm import relationship

from book_manager import schemas
from book_manager.database import Base


class User(Base):
    __tablename__ = "Users"

    user_id = Column(Integer, primary_key=True)
    username = Column(String, nullable=False)
    wished_books = relationship(
        "Books", back_populates="wisher", cascade="all, delete, delete-orphan"
    )
    read_books = relationship(
        "Books", secondary="Readers", back_populates="Users", cascade="all, delete, delete-orphan"
    )

    def to_schema(self) -> schemas.User:
        return schemas.User(username=self.username)


class Book(Base):
    __tablename__ = "Books"

    isbn = Column(String, primary_key=True)
    title = Column(String, nullable=False)
    authors = relationship("Authors", secondary="BookAuthors", back_populates="Books")
    format = Column(String)
    series = relationship("Series", secondary="BookSeries", back_populates="Books")
    publisher = Column(String, nullable=False)
    readers = relationship("Users", secondary="Readers", back_populates="Books")
    wisher = Column(Integer, ForeignKey("Users.user_id"))
    open_library_id = Column(String, nullable=False)
    google_books_id = Column(String)
    goodreads_id = Column(String)
    library_thing_id = Column(String)

    def to_schema(self) -> schemas.Book:
        return schemas.Book(
            isbn=self.isbn,
            title=self.title,
            authors=sorted(x.name for x in self.authors),
            format=self.format,
            series=sorted(x.name for x in self.series),
            publisher=self.publisher,
            readers=sorted(x.name for x in self.readers),
            wisher=self.wisher.name if self.wisher else None,
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


class Reader(Base):
    __tablename__ = "Readers"

    row_id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("Users.user_id"))
    book_id = Column(String, ForeignKey("Books.isbn"))


class Author(Base):
    __tablename__ = "Authors"

    author_id = Column(Integer, primary_key=True)
    name = Column(String, nullable=False)
    written = relationship("Books", secondary="BookAuthors", back_populates="Authors")


class BookAuthor(Base):
    __tablename__ = "BookAuthors"

    row_id = Column(Integer, primary_key=True)
    author_id = Column(Integer, ForeignKey("Authors.author_id"))
    book_id = Column(String, ForeignKey("Books.isbn"))


class Series(Base):
    __tablename__ = "Series"

    series_id = Column(Integer, primary_key=True)
    name = Column(String, nullable=False)
    books = relationship("Books", secondary="BookSeries", back_populates="Series")


class BookSeries(Base):
    __tablename__ = "BookSeries"

    row_id = Column(Integer, primary_key=True)
    series_id = Column(Integer, ForeignKey("Series.series_id"))
    book_id = Column(String, ForeignKey("Books.isbn"))
