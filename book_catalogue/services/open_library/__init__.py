__all__ = ["lookup_book", "lookup_creator"]

from fastapi import HTTPException

from book_catalogue.controllers.creator import CreatorController
from book_catalogue.controllers.format import FormatController
from book_catalogue.controllers.genre import GenreController
from book_catalogue.controllers.publisher import PublisherController
from book_catalogue.controllers.role import RoleController
from book_catalogue.schemas.book import BookCreatorWrite, BookWrite, Identifiers
from book_catalogue.schemas.creator import CreatorWrite, Identifiers as CreatorIdentifiers
from book_catalogue.schemas.format import FormatWrite
from book_catalogue.schemas.genre import GenreWrite
from book_catalogue.schemas.publisher import PublisherWrite
from book_catalogue.schemas.role import RoleWrite
from book_catalogue.services.open_library.service import OpenLibrary


def lookup_book(
    isbn: str, open_library_id: str | None = None, google_books_id: str | None = None
) -> BookWrite:
    session = OpenLibrary()
    if open_library_id:
        edition = session.get_edition(edition_id=open_library_id)
    else:
        edition = session.get_edition_by_isbn(isbn=isbn)
    work = session.get_work(work_id=edition.works[0].key.split("/")[-1])

    creators = {}
    for entry in work.creators:
        creator = CreatorController.lookup_creator(open_library_id=entry.creator_id)
        if creator not in creators:
            creators[creator] = set()
        try:
            role = RoleController.get_role_by_name(name="Writer")
        except HTTPException:
            role = RoleController.create_role(new_role=RoleWrite(name="Writer"))
        creators[creator].add(role)
    for entry in edition.contributors:
        try:
            creator = CreatorController.get_creator_by_name(name=entry.name)
        except HTTPException:
            creator = CreatorController.create_creator(new_creator=CreatorWrite(name=entry.name))
        if creator not in creators:
            creators[creator] = set()
        try:
            role = RoleController.get_role_by_name(name=entry.role)
        except HTTPException:
            role = RoleController.create_role(new_role=RoleWrite(name=entry.role))
        creators[creator].add(role)
    creators = [
        BookCreatorWrite(creator_id=key.creator_id, role_ids=[x.role_id for x in value])
        for key, value in creators.items()
    ]

    format = None
    if edition.physical_format:
        try:
            format = FormatController.get_format_by_name(name=edition.physical_format)
        except HTTPException:
            format = FormatController.create_format(
                new_format=FormatWrite(name=edition.physical_format)
            )

    genre_ids = set()
    for _genre in edition.genres:
        try:
            genre = GenreController.get_genre_by_name(name=_genre)
        except HTTPException:
            genre = GenreController.create_genre(new_genre=GenreWrite(name=_genre))
        genre_ids.add(genre.genre_id)

    publisher_list = []
    for x in edition.publishers:
        for y in x.split(";"):
            try:
                publisher = PublisherController.get_publisher_by_name(name=y.strip())
            except HTTPException:
                publisher = PublisherController.create_publisher(
                    new_publisher=PublisherWrite(name=y.strip())
                )
            publisher_list.append(publisher)
    publisher = next(iter(sorted(publisher_list, key=lambda x: x.name)), None)

    return BookWrite(
        creators=creators,
        description=edition.get_description() or work.get_description(),
        format_id=format.format_id if format else None,
        genre_ids=genre_ids,
        identifiers=Identifiers(
            goodreads_id=next(iter(edition.identifiers.goodreads), None),
            google_books_id=google_books_id or next(iter(edition.identifiers.google), None),
            isbn=isbn,
            library_thing_id=next(iter(edition.identifiers.librarything), None),
            open_library_id=edition.edition_id,
        ),
        image_url=f"https://covers.openlibrary.org/b/OLID/{edition.edition_id}-L.jpg",
        # Is Collected
        publish_date=edition.get_publish_date(),
        publisher_id=publisher.publisher_id if publisher else None,
        # Reader Ids
        # TODO: Series
        subtitle=edition.subtitle,
        title=edition.title,
        # Wisher Ids
    )


def lookup_creator(open_library_id: str | None = None) -> CreatorWrite:
    session = OpenLibrary()
    creator = session.get_creator(creator_id=open_library_id)

    photo_id = next(iter(creator.photos), None)

    return CreatorWrite(
        bio=creator.get_bio(),
        identifiers=CreatorIdentifiers(
            goodreads_id=creator.remote_ids.goodreads,
            library_thing_id=creator.remote_ids.librarything,
            open_library_id=open_library_id,
        ),
        image_url=f"https://covers.openlibrary.org/a/id/{photo_id}-L.jpg" if photo_id else None,
        name=creator.name,
    )
