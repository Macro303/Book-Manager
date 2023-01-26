from __future__ import annotations

__all__ = ["BookController"]

import logging

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.controllers._author import AuthorController
from book_catalogue.controllers._publisher import PublisherController
from book_catalogue.controllers._series import SeriesController
from book_catalogue.controllers._user import UserController
from book_catalogue.database.tables import Book, BookAuthor, BookSeries
from book_catalogue.schemas._author import NewAuthor, NewRole
from book_catalogue.schemas._book import Identifiers, NewBook, NewBookAuthor, NewBookSeries
from book_catalogue.schemas._publisher import NewPublisher
from book_catalogue.services.open_library import lookup_book_by_id, lookup_book_by_isbn
from book_catalogue.services.open_library.service import OpenLibrary

LOGGER = logging.getLogger(__name__)


class BookController:
    @classmethod
    def list_books(cls) -> list[Book]:
        return Book.select()

    @classmethod
    def create_book(cls, new_book: NewBook) -> Book:
        if Book.get(isbn_13=new_book.identifiers.isbn):
            raise HTTPException(status_code=409, detail="Book already exists.")

        book = Book(
            description=new_book.description,
            format=new_book.format,
            image_url=new_book.image_url,
            publisher=PublisherController.get_publisher(publisher_id=new_book.publisher_id)
            if new_book.publisher_id
            else None,
            readers=[UserController.get_user(user_id=x) for x in new_book.reader_ids],
            subtitle=new_book.subtitle,
            title=new_book.title,
            wishers=[UserController.get_user(user_id=x) for x in new_book.wisher_ids],
            goodreads_id=new_book.identifiers.goodreads_id,
            google_books_id=new_book.identifiers.google_books_id,
            isbn_13=new_book.identifiers.isbn,
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
    def update_book(cls, book_id: int, updates: NewBook) -> Book:
        book = cls.get_book(book_id=book_id)
        book.authors = []
        for x in updates.authors:
            author = AuthorController.get_author(author_id=x.author_id)
            flush()
            temp = BookAuthor.get(book=book, author=author) or BookAuthor(book=book, author=author)
            temp.roles = [AuthorController.get_role(role_id=y) for y in x.role_ids]
        book.description = updates.description
        book.format = updates.format
        book.image_url = updates.image_url
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
        book.isbn_13 = updates.identifiers.isbn
        book.library_thing_id = updates.identifiers.library_thing_id
        book.open_library_id = updates.identifiers.open_library_id

        flush()
        return book

    @classmethod
    def delete_book(cls, book_id: int):
        book = cls.get_book(book_id=book_id)
        book.delete()

    @classmethod
    def _parse_open_library(cls, isbn: str | None, open_library_id: str | None) -> NewBook:
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
                role = AuthorController.create_role(new_role=NewRole(name="Writer"))
            authors[author].add(role)
        for entry in result["edition"].contributors:
            try:
                author = AuthorController.get_author_by_name(name=entry.name)
            except HTTPException:
                author = AuthorController.create_author(new_author=NewAuthor(name=entry.name))
            if author not in authors:
                authors[author] = set()
            try:
                role = AuthorController.get_role_by_name(name=entry.role)
            except HTTPException:
                role = AuthorController.create_role(new_role=NewRole(name=entry.role))
            authors[author].add(role)
        authors = [
            NewBookAuthor(author_id=key.author_id, role_ids=[x.role_id for x in value])
            for key, value in authors.items()
        ]

        publisher_list = []
        for x in result["edition"].publishers:
            for y in x.split(";"):
                try:
                    publisher = PublisherController.get_publisher_by_name(name=y.strip())
                except HTTPException:
                    publisher = PublisherController.create_publisher(
                        new_publisher=NewPublisher(name=y.strip())
                    )
                publisher_list.append(publisher)
        publisher = next(iter(sorted(publisher_list, key=lambda x: x.name)), None)

        return NewBook(
            authors=authors,
            description=result["edition"].get_description() or result["work"].get_description(),
            format=result["edition"].physical_format,
            identifiers=Identifiers(
                goodreads_id=next(iter(result["edition"].identifiers.goodreads), None),
                google_books_id=next(iter(result["edition"].identifiers.google), None),
                isbn=isbn,
                library_thing_id=next(iter(result["edition"].identifiers.librarything), None),
                open_library_id=result["edition"].edition_id,
            ),
            image_url=f"https://covers.openlibrary.org/b/OLID/{result['edition'].edition_id}-L.jpg",
            publisher_id=publisher.publisher_id if publisher else None,
            series=[],
            subtitle=result["edition"].subtitle,
            title=result["edition"].title,
        )

    @classmethod
    def lookup_book(cls, isbn: str, wisher_id: int | None) -> Book:
        if book := Book.get(isbn_13=isbn):
            if wisher_id and (wisher := UserController.get_user(user_id=wisher_id)):
                if wisher in book.wishers:
                    raise HTTPException(status_code=409, detail="Book already wished for.")
                book.wishers.add(wisher)
                return book
            raise HTTPException(status_code=409, detail="Book already exists.")

        new_book = cls._parse_open_library(isbn=isbn, open_library_id=None)
        if wisher_id:
            new_book.wisher_ids = [wisher_id]
        return cls.create_book(new_book=new_book)

    @classmethod
    def reset_book(cls, book_id: int) -> Book:
        if not (book := cls.get_book(book_id=book_id)):
            raise HTTPException(status_code=404, detail="Book not found.")

        updates = cls._parse_open_library(isbn=book.isbn_13, open_library_id=book.open_library_id)
        updates.reader_ids = [x.user_id for x in book.readers]
        updates.series = [
            NewBookSeries(series_id=x.series.series_id, number=x.number) for x in book.series
        ]
        updates.wisher_ids = [x.user_id for x in book.wishers]
        return cls.update_book(book_id=book_id, updates=updates)
