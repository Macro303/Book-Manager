__all__ = ["lookup_book", "lookup_creator"]

from fastapi import HTTPException

from bookshelf.controllers.creator import CreatorController
from bookshelf.controllers.format import FormatController
from bookshelf.controllers.genre import GenreController
from bookshelf.controllers.publisher import PublisherController
from bookshelf.controllers.role import RoleController
from bookshelf.models.book import BookCreatorIn, BookIn, Identifiers
from bookshelf.models.creator import CreatorIn, Identifiers as CreatorIdentifiers
from bookshelf.models.format import FormatIn
from bookshelf.models.genre import GenreIn
from bookshelf.models.publisher import PublisherIn
from bookshelf.models.role import RoleIn
from bookshelf.services.open_library.service import OpenLibrary


def lookup_book(
    isbn: str,
    open_library_id: str | None = None,
    google_books_id: str | None = None,
) -> BookIn:
    session = OpenLibrary()
    if open_library_id:
        edition = session.get_edition(edition_id=open_library_id)
    else:
        edition = session.get_edition_by_isbn(isbn=isbn)
    work = session.get_work(work_id=edition.works[0].key.split("/")[-1])

    creators = {}
    for entry in work.authors:
        creator = CreatorController.lookup_creator(open_library_id=entry.author_id)
        if creator not in creators:
            creators[creator] = set()
        try:
            role = RoleController.get_role_by_name(name="Author")
        except HTTPException:
            role = RoleController.create_role(new_role=RoleIn(name="Author"))
        creators[creator].add(role)
    for entry in edition.contributors:
        try:
            creator = CreatorController.get_creator_by_name(name=entry.name)
        except HTTPException:
            creator = CreatorController.create_creator(new_creator=CreatorIn(name=entry.name))
        if creator not in creators:
            creators[creator] = set()
        try:
            role = RoleController.get_role_by_name(name=entry.role)
        except HTTPException:
            role = RoleController.create_role(new_role=RoleIn(name=entry.role))
        creators[creator].add(role)
    creators = [
        BookCreatorIn(creator_id=key.creator_id, role_ids=[x.role_id for x in value])
        for key, value in creators.items()
    ]

    format = None
    if edition.physical_format:
        try:
            format = FormatController.get_format_by_name(name=edition.physical_format)
        except HTTPException:
            format = FormatController.create_format(
                new_format=FormatIn(name=edition.physical_format),
            )

    genre_ids = set()
    for _genre in edition.genres:
        try:
            genre = GenreController.get_genre_by_name(name=_genre)
        except HTTPException:
            genre = GenreController.create_genre(new_genre=GenreIn(name=_genre))
        genre_ids.add(genre.genre_id)

    publisher_list = []
    for x in edition.publishers:
        for y in x.split(";"):
            try:
                publisher = PublisherController.get_publisher_by_name(name=y.strip())
            except HTTPException:
                publisher = PublisherController.create_publisher(
                    new_publisher=PublisherIn(name=y.strip()),
                )
            publisher_list.append(publisher)
    publisher = next(iter(sorted(publisher_list, key=lambda x: x.name)), None)

    return BookIn(
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


def lookup_creator(open_library_id: str | None = None) -> CreatorIn:
    session = OpenLibrary()
    creator = session.get_author(author_id=open_library_id)

    photo_id = next(iter(creator.photos), None)

    return CreatorIn(
        bio=creator.get_bio(),
        identifiers=CreatorIdentifiers(
            goodreads_id=creator.remote_ids.goodreads,
            library_thing_id=creator.remote_ids.librarything,
            open_library_id=open_library_id,
        ),
        image_url=f"https://covers.openlibrary.org/a/id/{photo_id}-L.jpg" if photo_id else None,
        name=creator.name,
    )
