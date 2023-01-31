__all__ = ["BookController"]

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.controllers.author import AuthorController
from book_catalogue.controllers.format import FormatController
from book_catalogue.controllers.publisher import PublisherController
from book_catalogue.controllers.series import SeriesController
from book_catalogue.controllers.user import UserController
from book_catalogue.database.tables import Book, BookAuthor, BookSeries
from book_catalogue.schemas.author import AuthorWrite, RoleWrite
from book_catalogue.schemas.book import BookAuthorWrite, BookSeriesWrite, BookWrite, Identifiers
from book_catalogue.schemas.format import FormatWrite
from book_catalogue.schemas.publisher import PublisherWrite
from book_catalogue.services.open_library import lookup_book_by_id, lookup_book_by_isbn
from book_catalogue.services.open_library.service import OpenLibrary
from book_catalogue.services.google_books import google_books_lookup
from book_catalogue.settings import Settings


class BookController:
    @classmethod
    def list_books(cls) -> list[Book]:
        return Book.select()

    @classmethod
    def create_book(cls, new_book: BookWrite) -> Book:
        if Book.get(isbn=new_book.identifiers.isbn):
            raise HTTPException(status_code=409, detail="Book already exists.")

        book = Book(
            description=new_book.description,
            format=FormatController.get_format(format_id=new_book.format_id)
            if new_book.format_id
            else None,
            image_url=new_book.image_url,
            publish_date=new_book.publish_date,
            publisher=PublisherController.get_publisher(publisher_id=new_book.publisher_id)
            if new_book.publisher_id
            else None,
            readers=[UserController.get_user(user_id=x) for x in new_book.reader_ids],
            subtitle=new_book.subtitle,
            title=new_book.title,
            wishers=[UserController.get_user(user_id=x) for x in new_book.wisher_ids],
            goodreads_id=new_book.identifiers.goodreads_id,
            google_books_id=new_book.identifiers.google_books_id,
            isbn=new_book.identifiers.isbn,
            library_thing_id=new_book.identifiers.library_thing_id,
            open_library_id=new_book.identifiers.open_library_id,
        )
        flush()
        for x in new_book.authors:
            author = AuthorController.get_author(author_id=x.author_id)
            roles = [AuthorController.get_role(role_id=y) for y in x.role_ids]
            BookAuthor(book=book, author=author, roles=roles)
        for x in new_book.series:
            series = SeriesController.get_series(series_id=x.series_id)
            BookSeries(book=book, series=series, number=x.number)
        flush()
        return book

    @classmethod
    def get_book(cls, book_id: int) -> Book:
        if book := Book.get(book_id=book_id):
            return book
        raise HTTPException(status_code=404, detail="Book not found.")

    @classmethod
    def update_book(cls, book_id: int, updates: BookWrite) -> Book:
        book = cls.get_book(book_id=book_id)
        book.authors = []
        for x in updates.authors:
            author = AuthorController.get_author(author_id=x.author_id)
            flush()
            temp = BookAuthor.get(book=book, author=author) or BookAuthor(book=book, author=author)
            temp.roles = [AuthorController.get_role(role_id=y) for y in x.role_ids]
        book.description = updates.description
        book.format = (
            FormatController.get_format(format_id=updates.format_id) if updates.format_id else None
        )
        book.image_url = updates.image_url
        book.publish_date = updates.publish_date
        book.publisher = (
            PublisherController.get_publisher(publisher_id=updates.publisher_id)
            if updates.publisher_id
            else None
        )
        book.readers = [UserController.get_user(user_id=x) for x in updates.reader_ids]
        book.series = []
        for x in updates.series:
            series = SeriesController.get_series(series_id=x.series_id)
            flush()
            temp = BookSeries.get(book=book, series=series) or BookSeries(book=book, series=series)
            temp.number = x.number
        book.subtitle = updates.subtitle
        book.title = updates.title
        book.wishers = [UserController.get_user(user_id=x) for x in updates.wisher_ids]

        book.goodreads_id = updates.identifiers.goodreads_id
        book.google_books_id = updates.identifiers.google_books_id
        book.isbn = updates.identifiers.isbn
        book.library_thing_id = updates.identifiers.library_thing_id
        book.open_library_id = updates.identifiers.open_library_id

        flush()
        return book

    @classmethod
    def delete_book(cls, book_id: int):
        book = cls.get_book(book_id=book_id)
        book.delete()

    @classmethod
    def _parse_open_library(cls, isbn: str | None, open_library_id: str | None) -> BookWrite:
        if not isbn and not open_library_id:
            raise NotImplementedError("Require Isbn or OpenLibrary Id")
        session = OpenLibrary(cache=None)
        if open_library_id:
            result = lookup_book_by_id(session=session, edition_id=open_library_id)
        else:
            result = lookup_book_by_isbn(session=session, isbn=isbn)

        authors = {}
        for entry in result["work"].authors:
            author = AuthorController.lookup_author(open_library_id=entry.author_id)
            if author not in authors:
                authors[author] = set()
            try:
                role = AuthorController.get_role_by_name(name="Writer")
            except HTTPException:
                role = AuthorController.create_role(new_role=RoleWrite(name="Writer"))
            authors[author].add(role)
        for entry in result["edition"].contributors:
            try:
                author = AuthorController.get_author_by_name(name=entry.name)
            except HTTPException:
                author = AuthorController.create_author(new_author=AuthorWrite(name=entry.name))
            if author not in authors:
                authors[author] = set()
            try:
                role = AuthorController.get_role_by_name(name=entry.role)
            except HTTPException:
                role = AuthorController.create_role(new_role=RoleWrite(name=entry.role))
            authors[author].add(role)
        authors = [
            BookAuthorWrite(author_id=key.author_id, role_ids=[x.role_id for x in value])
            for key, value in authors.items()
        ]

        publisher_list = []
        for x in result["edition"].publishers:
            for y in x.split(";"):
                try:
                    publisher = PublisherController.get_publisher_by_name(name=y.strip())
                except HTTPException:
                    publisher = PublisherController.create_publisher(
                        new_publisher=PublisherWrite(name=y.strip())
                    )
                publisher_list.append(publisher)
        publisher = next(iter(sorted(publisher_list, key=lambda x: x.name)), None)

        format = None
        if result["edition"].physical_format:
            try:
                format = FormatController.get_format_by_name(name=result["edition"].physical_format)
            except HTTPException:
                format = FormatController.create_format(
                    new_format=FormatWrite(name=result["edition"].physical_format)
                )

        return BookWrite(
            authors=authors,
            description=result["edition"].get_description() or result["work"].get_description(),
            format_id=format.format_id if format else None,
            identifiers=Identifiers(
                goodreads_id=next(iter(result["edition"].identifiers.goodreads), None),
                google_books_id=next(iter(result["edition"].identifiers.google), None),
                isbn=isbn,
                library_thing_id=next(iter(result["edition"].identifiers.librarything), None),
                open_library_id=result["edition"].edition_id,
            ),
            image_url=f"https://covers.openlibrary.org/b/OLID/{result['edition'].edition_id}-L.jpg",
            publish_date=result["edition"].get_publish_date(),
            publisher_id=publisher.publisher_id if publisher else None,
            series=[],
            subtitle=result["edition"].subtitle,
            title=result["edition"].title,
        )

    @classmethod
    def lookup_book(cls, isbn: str, wisher_id: int | None) -> Book:
        if book := Book.get(isbn=isbn):
            if wisher_id and (wisher := UserController.get_user(user_id=wisher_id)):
                if wisher in book.wishers:
                    raise HTTPException(status_code=409, detail="Book already wished for.")
                book.wishers.add(wisher)
                return book
            raise HTTPException(status_code=409, detail="Book already exists.")

        settings = Settings.load()
        if settings.source.open_library:
            new_book = cls._parse_open_library(isbn=isbn, open_library_id=None)
        elif settings.source.google_books:
            new_book = google_books_lookup(isbn=isbn, google_books_id=None)
        else:
            raise HTTPException(status_code=500, detail="Incorrect config setup, review source settings.")
        if wisher_id:
            new_book.wisher_ids = [wisher_id]
        return cls.create_book(new_book=new_book)

    @classmethod
    def reset_book(cls, book_id: int) -> Book:
        if not (book := cls.get_book(book_id=book_id)):
            raise HTTPException(status_code=404, detail="Book not found.")

        settings = Settings.load()
        if settings.source.open_library:
            updates = cls._parse_open_library(isbn=book.isbn, open_library_id=book.open_library_id)
        elif settings.source.google_books:
            updates = google_books_lookup(isbn=book.isbn, google_books_id=book.google_books_id)
        else:
            raise HTTPException(status_code=500, detail="Incorrect config setup, review source settings.")
        updates.reader_ids = [x.user_id for x in book.readers]
        updates.series = [
            BookSeriesWrite(series_id=x.series.series_id, number=x.number) for x in book.series
        ]
        updates.wisher_ids = [x.user_id for x in book.wishers]
        return cls.update_book(book_id=book_id, updates=updates)

    @classmethod
    def load_new_field(cls, book_id: int) -> Book:
        if not (book := cls.get_book(book_id=book_id)):
            raise HTTPException(status_code=404, detail="Book not found.")

        settings = Settings.load()
        if settings.source.open_library:
            updates = cls._parse_open_library(isbn=book.isbn, open_library_id=book.open_library_id)
        elif settings.source.google_books:
            updates = google_books_lookup(isbn=book.isbn, google_books_id=book.google_books_id)
        else:
            raise HTTPException(status_code=500, detail="Incorrect config setup, review source settings.")
        book.publish_date = updates.publish_date
        flush()
        return book
