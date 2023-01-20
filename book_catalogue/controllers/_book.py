__all__ = ["BookController"]

import logging

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import Book, Publisher, BookAuthor, BookSeries
from book_catalogue.schemas._book import BookUpdate, BookUpdateAuthor, BookUpdateSeries
from book_catalogue.services.open_library.service import OpenLibrary
from book_catalogue.controllers import AuthorController, PublisherController, UserController, SeriesController
from book_catalogue.isbn import to_isbn_13
from book_catalogue.schemas import User
from book_catalogue.services.open_library import lookup_book

LOGGER = logging.getLogger(__name__)


class BookController:
    @staticmethod
    def list_books() -> list[Book]:
        return Book.select()

    @staticmethod
    def create_book(
        description: str | None,
        format: str | None,
        image_url: str,
        publisher: Publisher | None,
        subtitle: str | None,
        title: str,
        wisher: User,
        goodreads_id: str | None,
        isbn_13: str,
        library_thing_id: str | None,
        open_library_id: str
    ) -> Book:
        if Book.get(isbn_13=isbn_13):
            raise HTTPException(status_code=409, detail="Book already exists.")
        book = Book(
            description=description,
            format=format,
            image_url=image_url,
            publisher=publisher,
            subtitle=subtitle,
            title=title,
            wisher=wisher,
            goodreads_id=goodreads_id,
            isbn_13=isbn_13,
            library_thing_id=library_thing_id,
            open_library_id=open_library_id
        )
        flush()
        return book

    @staticmethod
    def update_book(book_id: int, book_update: BookUpdate) -> Book:
        if not (book := BookController.get_book(book_id=book_id)):
            raise HTTPException(status_code=404, detail="Book not found.")
        book.authors = []
        for x in book_update.authors:
            temp = BookAuthor.get(
                book=book,
                author=AuthorController.get_author(author_id=x.author_id)
            ) or BookAuthor(
                book=book,
                author=AuthorController.get_author(author_id=x.author_id)
            )
            temp.roles = [AuthorController.get_role(role_id=y) for y in x.role_ids]
        book.description = book_update.description
        book.format = book_update.format
        book.image_url = book_update.image_url
        book.publisher = PublisherController.get_publisher(publisher_id=book_update.publisher_id)
        book.readers = [UserController.get_user(user_id=x) for x in book_update.reader_ids]
        book.series = [
            BookSeries(
                series=SeriesController.get_series(series_id=x.series_id),
                number=x.number
            ) for x in book_update.series
        ]
        book.subtitle = book_update.subtitle
        book.title = book_update.title
        book.wisher = UserController.get_user(user_id=book_update.wisher)

        book.goodreads_id = book_update.goodreads_id
        book.google_books_id = book_update.google_books_id
        book.isbn_13 = book_update.isbn_13
        book.library_thing_id = book_update.library_thing_id
        book.open_library_id = book_update.open_library_id

        flush()
        return book

    @staticmethod
    def get_book(book_id: int) -> Book:
        if book := Book.get(book_id=book_id):
            return book
        raise HTTPException(status_code=404, detail="Book not found.")

    @staticmethod
    def delete_book(book_id: int):
        if book := Book.get(book_id=book_id):
            return book.delete()
        raise HTTPException(status_code=404, detail="Book not found.")

    @staticmethod
    def lookup_book_by_isbn(isbn: str, wisher: User) -> Book:
        isbn_13 = to_isbn_13(value=isbn)
        if book := Book.get(isbn_13=isbn_13):
            raise HTTPException(status_code=409, detail="Book already exists.")
        session = OpenLibrary(cache=None)
        result = lookup_book(session=session, isbn=isbn_13)

        publisher_list = []
        for x in result["edition"].publishers:
            for y in x.split(";"):
                publisher_list.append(PublisherController.get_publisher_by_name(name=y.strip()) or PublisherController.create_publisher(name=y.strip()))

        book = BookController.create_book(
            description=result["edition"].get_description() or result["work"].get_description(),
            format=result["edition"].physical_format,
            image_url=f"https://covers.openlibrary.org/b/OLID/{result['edition'].edition_id}-L.jpg",
            publisher=next(iter(sorted(publisher_list, key=lambda x: x.name)), None),
            subtitle=result["edition"].subtitle,
            title=result["edition"].title,
            wisher=wisher,
            goodreads_id=next(iter(result["edition"].identifiers.goodreads), None),
            isbn_13=isbn_13,
            library_thing_id=next(iter(result["edition"].identifiers.librarything), None),
            open_library_id=result["edition"].edition_id,
        )

        authors = {}
        for entry in result["work"].authors:
            author = AuthorController.lookup_author(open_library_id=entry.author_id)
            if author not in authors:
                authors[author] = set()
            authors[author].add(AuthorController.get_role_by_name(name="Writer") or AuthorController.create_role(name="Writer"))
        for entry in result["edition"].contributors:
            author = AuthorController.get_author_by_name(name=entry.name) or AuthorController.create_author(name=entry.name)
            if author not in authors:
                authors[author] = set()
            authors[author].add(AuthorController.get_role_by_name(name=entry.role) or AuthorController.create_role(name=entry.role))
        for author, roles in authors.items():
            BookAuthor(book=book, author=author, roles=roles)
        flush()
        return book

    @staticmethod
    def reset_book(book_id: int) -> Book:
        if not (book := BookController.get_book(book_id=book_id)):
            raise HTTPException(status_code=409, detail="Book not found.")
        session = OpenLibrary(cache=None)
        result = lookup_book(session=session, isbn=book.isbn_13)

        authors = {}
        for entry in result["work"].authors:
            author = AuthorController.lookup_author(open_library_id=entry.author_id)
            if author not in authors:
                authors[author] = set()
            authors[author].add(
                AuthorController.get_role_by_name(name="Writer") or AuthorController.create_role(name="Writer"))
        for entry in result["edition"].contributors:
            author = AuthorController.get_author_by_name(name=entry.name) or AuthorController.create_author(name=entry.name)
            if author not in authors:
                authors[author] = set()
            authors[author].add(
                AuthorController.get_role_by_name(name=entry.role) or AuthorController.create_role(name=entry.role))

        publisher_list = []
        for x in result["edition"].publishers:
            for y in x.split(";"):
                publisher_list.append(PublisherController.get_publisher_by_name(name=y.strip()) or PublisherController.create_publisher(name=y.strip()))
        publisher = next(iter(sorted(publisher_list, key=lambda x: x.name)), None)

        book_update = BookUpdate(
            authors=[
                BookUpdateAuthor(author_id=key.author_id, role_ids=[x.role_id for x in value])
                for key, value in authors.items()
            ],
            description=result["edition"].get_description() or result["work"].get_description(),
            format=result["edition"].physical_format,
            image_url=f"https://covers.openlibrary.org/b/OLID/{result['edition'].edition_id}-L.jpg",
            publisher_id=publisher.publisher_id if publisher else book.publisher.publisher_id,
            reader_ids=[x.user_id for x in book.readers],
            series=[
                BookUpdateSeries(series_id=x.series.series_id, number=x.number)
                for x in book.series
            ],
            subtitle=result["edition"].subtitle,
            title=result["edition"].title,
            wisher_id=book.wisher.user_id,
            goodreads_id=next(iter(result["edition"].identifiers.goodreads), book.goodreads_id),
            isbn_13=book.isbn_13,
            library_thing_id=next(iter(result["edition"].identifiers.librarything), book.library_thing_id),
            open_library_id=result["edition"].edition_id,
        )
        flush()
        return BookController.update_book(book_id=book.book_id, book_update=book_update)
