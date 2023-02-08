__all__ = ["BookController"]

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.controllers.creator import CreatorController
from book_catalogue.controllers.format import FormatController
from book_catalogue.controllers.genre import GenreController
from book_catalogue.controllers.publisher import PublisherController
from book_catalogue.controllers.role import RoleController
from book_catalogue.controllers.series import SeriesController
from book_catalogue.controllers.user import UserController
from book_catalogue.database.tables import Book, BookCreator, BookSeries
from book_catalogue.schemas.book import BookSeriesWrite, BookWrite
from book_catalogue.services import google_books, open_library
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
            genres=[GenreController.get_genre(genre_id=x) for x in new_book.genre_ids],
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
        for x in new_book.creators:
            creator = CreatorController.get_creator(creator_id=x.creator_id)
            roles = [RoleController.get_role(role_id=y) for y in x.role_ids]
            BookCreator(book=book, creator=creator, roles=roles)
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
        book.creators = []
        for x in updates.creators:
            creator = CreatorController.get_creator(creator_id=x.creator_id)
            flush()
            temp = BookCreator.get(book=book, creator=creator) or BookCreator(
                book=book, creator=creator
            )
            temp.roles = [RoleController.get_role(role_id=y) for y in x.role_ids]
        book.description = updates.description
        book.format = (
            FormatController.get_format(format_id=updates.format_id) if updates.format_id else None
        )
        book.genres = [GenreController.get_genre(genre_id=x) for x in updates.genre_ids]
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
            new_book = open_library.lookup_book(isbn=isbn, open_library_id=None)
        elif settings.source.google_books:
            new_book = google_books.lookup_book(isbn=isbn, google_books_id=None)
        else:
            raise HTTPException(
                status_code=500, detail="Incorrect config setup, review source settings."
            )
        if wisher_id:
            new_book.wisher_ids = [wisher_id]
        return cls.create_book(new_book=new_book)

    @classmethod
    def reset_book(cls, book_id: int) -> Book:
        if not (book := cls.get_book(book_id=book_id)):
            raise HTTPException(status_code=404, detail="Book not found.")

        settings = Settings.load()
        if settings.source.open_library:
            updates = open_library.lookup_book(
                isbn=book.isbn,
                open_library_id=book.open_library_id,
                google_books_id=book.google_books_id,
            )
            updates.series = [
                BookSeriesWrite(series_id=x.series.series_id, number=x.number) for x in book.series
            ]
        elif settings.source.google_books:
            updates = google_books.lookup_book(isbn=book.isbn, google_books_id=book.google_books_id)
            updates.format_id = book.format.format_id if book.format else None
            updates.identifiers.goodreads_id = book.goodreads_id
            updates.identifiers.library_thing_id = book.library_thing_id
            updates.identifiers.open_library_id = book.open_library_id
            updates.series = [
                BookSeriesWrite(series_id=x.series.series_id, number=x.number) for x in book.series
            ]
            updates.subtitle = book.subtitle
        else:
            raise HTTPException(
                status_code=500, detail="Incorrect config setup, review source settings."
            )
        updates.is_collected = book.is_collected
        updates.reader_ids = [x.user_id for x in book.readers]
        updates.wisher_ids = [x.user_id for x in book.wishers]
        return cls.update_book(book_id=book_id, updates=updates)

    @classmethod
    def load_new_field(cls, book_id: int) -> Book:
        if not (book := cls.get_book(book_id=book_id)):
            raise HTTPException(status_code=404, detail="Book not found.")

        settings = Settings.load()
        if settings.source.open_library:
            updates = open_library.lookup_book(isbn=book.isbn, open_library_id=book.open_library_id)
            updates.series = [
                BookSeriesWrite(series_id=x.series.series_id, number=x.number) for x in book.series
            ]
        elif settings.source.google_books:
            updates = google_books.lookup_book(isbn=book.isbn, google_books_id=book.google_books_id)
            updates.format_id = book.format.format_id if book.format else None
            updates.identifiers.goodreads_id = book.goodreads_id
            updates.identifiers.library_thing_id = book.library_thing_id
            updates.identifiers.open_library_id = book.open_library_id
            updates.series = [
                BookSeriesWrite(series_id=x.series.series_id, number=x.number) for x in book.series
            ]
            updates.subtitle = book.subtitle
        else:
            raise HTTPException(
                status_code=500, detail="Incorrect config setup, review source settings."
            )
        book.genres = [GenreController.get_genre(genre_id=x) for x in updates.genre_ids]
        flush()
        return book
