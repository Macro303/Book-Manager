__all__ = ["lookup_book"]

from fastapi import HTTPException

from book_catalogue.controllers.author import AuthorController
from book_catalogue.controllers.format import FormatController
from book_catalogue.controllers.publisher import PublisherController
from book_catalogue.schemas.author import AuthorWrite, RoleWrite
from book_catalogue.schemas.book import BookAuthorWrite, BookWrite, Identifiers
from book_catalogue.schemas.format import FormatWrite
from book_catalogue.schemas.publisher import PublisherWrite
from book_catalogue.services.open_library.service import OpenLibrary


def lookup_book(isbn: str, open_library_id: str | None = None) -> BookWrite:
    session = OpenLibrary()
    if open_library_id:
        edition = session.get_edition(edition_id=open_library_id)
    else:
        edition = session.get_edition_by_isbn(isbn=isbn)
    work = session.get_work(work_id=edition.works[0].key.split("/")[-1])

    authors = {}
    for entry in work.authors:
        author = AuthorController.lookup_author(open_library_id=entry.author_id)
        if author not in authors:
            authors[author] = set()
        try:
            role = AuthorController.get_role_by_name(name="Writer")
        except HTTPException:
            role = AuthorController.create_role(new_role=RoleWrite(name="Writer"))
        authors[author].add(role)
    for entry in edition.contributors:
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

    format = None
    if edition.physical_format:
        try:
            format = FormatController.get_format_by_name(name=edition.physical_format)
        except HTTPException:
            format = FormatController.create_format(
                new_format=FormatWrite(name=edition.physical_format)
            )

    return BookWrite(
        authors=authors,
        description=edition.get_description() or work.get_description(),
        format_id=format.format_id if format else None,
        identifiers=Identifiers(
            goodreads_id=next(iter(edition.identifiers.goodreads), None),
            google_books_id=next(iter(edition.identifiers.google), None),
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


def lookup_author(open_library_id: str | None = None) -> AuthorWrite:
    session = OpenLibrary()
    author = session.get_author(author_id=open_library_id)

    photo_id = next(iter(author.photos), None)

    return AuthorWrite(
        bio=author.get_bio(),
        identifiers=Identifiers(
            goodreads_id=author.remote_ids.goodreads,
            library_thing_id=author.remote_ids.librarything,
            open_library_id=open_library_id,
        ),
        image_url=f"https://covers.openlibrary.org/a/id/{photo_id}-L.jpg" if photo_id else None,
        name=author.name,
    )
