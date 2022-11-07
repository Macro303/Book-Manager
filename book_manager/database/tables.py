__all__ = ["db", "BookTable", "UserTable"]

from pony.orm import Database, Optional, PrimaryKey, Required, Set

from book_manager.models.book import Book, Identifiers, Images
from book_manager.models.isbn import to_isbn
from book_manager.models.user import User

db = Database()


class UserTable(db.Entity):
    _table_ = "Users"

    username = PrimaryKey(str)

    wished_books = Set("BookTable", reverse="wished")
    read_books = Set("BookTable", reverse="read", table="User_Books")

    @staticmethod
    def create_or_get(username: str) -> "UserTable":
        return UserTable.get(username=username) or UserTable(username=username)

    def to_model(self) -> User:
        return User(username=self.username)


class BookTable(db.Entity):
    _table_ = "Books"

    isbn = PrimaryKey(str)
    publisher = Required(str)
    title = Required(str)
    page_count = Required(int)
    format = Optional(str, nullable=True)
    publish_date = Required(str)
    series = Set("SeriesTable", table="Book_Series")
    description = Optional(str, nullable=True)
    subjects = Set("SubjectTable", table="Book_Subjects")
    authors = Set("AuthorTable", table="Book_Authors")
    open_library_id = Required(str, unique=True)
    google_books_id = Optional(str, nullable=True)
    goodreads_id = Optional(str, nullable=True)
    library_thing_id = Optional(str, nullable=True)

    wished = Optional("UserTable", nullable=True)
    read = Set("UserTable", table="User_Books")

    @staticmethod
    def create(book: Book) -> "BookTable":
        output = BookTable(
            isbn=book.isbn,
            publisher=book.publisher,
            title=book.title,
            page_count=book.page_count,
            format=book.format,
            publish_date=book.publish_date,
            description=book.description,
            open_library_id=book.identifiers.open_library_id,
            google_books_id=book.identifiers.google_books_id,
            goodreads_id=book.identifiers.goodreads_id,
            library_thing_id=book.identifiers.library_thing_id,
            wished=UserTable.create_or_get(book.wished) if book.wished else None,
        )
        series_list = []
        for series in book.series:
            series_list.append(SeriesTable.create_or_get(series))
        output.series = series_list
        subject_list = []
        for subject in book.subjects:
            subject_list.append(SubjectTable.create_or_get(subject))
        output.subjects = subject_list
        author_list = []
        for author in book.authors:
            author_list.append(AuthorTable.create_or_get(author))
        output.authors = author_list
        return output

    def to_model(self) -> Book:
        return Book(
            isbn=to_isbn(self.isbn),
            publisher=self.publisher,
            title=self.title,
            page_count=self.page_count,
            format=self.format,
            publish_date=self.publish_date,
            series=sorted(x.value for x in self.series),
            description=self.description,
            subjects=sorted(x.value for x in self.subjects),
            authors=sorted(x.value for x in self.authors),
            identifiers=Identifiers(
                open_library_id=self.open_library_id,
                google_books_id=self.google_books_id,
                goodreads_id=self.goodreads_id,
                library_thing_id=self.library_thing_id,
            ),
            images=Images(
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
            wished=self.wished.username if self.wished else None,
            read=sorted(x.username for x in self.read),
        )


class SeriesTable(db.Entity):
    _table_ = "Series"

    series_id = PrimaryKey(int, auto=True)
    value = Required(str, unique=True)
    books = Set("BookTable", table="Book_Series")

    @staticmethod
    def create_or_get(value: str) -> "SeriesTable":
        return SeriesTable.get(value=value) or SeriesTable(value=value)


class SubjectTable(db.Entity):
    _table_ = "Subjects"

    subject_id = PrimaryKey(int, auto=True)
    value = Required(str)
    books = Set("BookTable", table="Book_Subjects")

    @staticmethod
    def create_or_get(value: str) -> "SubjectTable":
        return SubjectTable.get(value=value) or SubjectTable(value=value)


class AuthorTable(db.Entity):
    _table_ = "Authors"

    author_id = PrimaryKey(int, auto=True)
    value = Required(str)
    books = Set("BookTable", table="Book_Authors")

    @staticmethod
    def create_or_get(value: str) -> "AuthorTable":
        return AuthorTable.get(value=value) or AuthorTable(value=value)
