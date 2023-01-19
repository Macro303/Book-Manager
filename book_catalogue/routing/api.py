__all__ = ["router"]

from fastapi import APIRouter, Body
from fastapi.exceptions import HTTPException
from pony.orm import db_session, flush

from book_catalogue import __version__, controller
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas import Book, Publisher, User

router = APIRouter(
    prefix=f"/api/v{__version__.split('.')[0]}",
    tags=["API"],
    responses={
        422: {"description": "Validation error", "model": ErrorResponse},
    },
)


@router.post(path="/users", status_code=201, responses={409: {"model": ErrorResponse}})
def create_user(username: str = Body(embed=True)) -> User:  # noqa: B008
    with db_session:
        return controller.create_user(username=username).to_schema()


@router.get(path="/users/{username}", responses={404: {"model": ErrorResponse}})
def get_user_by_username(username: str) -> User:
    with db_session:
        return controller.get_user_by_username(username=username).to_schema()


@router.post(
    path="/books",
    status_code=201,
    responses={
        400: {"model": ErrorResponse},
        404: {"model": ErrorResponse},
        409: {"model": ErrorResponse},
    },
)
def add_book_by_isbn(
    isbn: str | None = Body(embed=True, default=None),  # noqa: B008
    open_library_id: str | None = Body(embed=True, default=None),  # noqa: B008
    wisher_id: int = Body(embed=True),  # noqa: B008
) -> Book:
    if not isbn and not open_library_id:
        raise HTTPException(
            status_code=400, detail="Invalid request, isbn or open_library_id must be provided."
        )
    with db_session:
        wisher = controller.get_user_by_id(user_id=wisher_id)
        return controller.add_book_by_isbn(isbn=isbn, wisher=wisher).to_schema()


@router.get(path="/books", responses={404: {"model": ErrorResponse}})
def list_books() -> list[Book]:
    with db_session:
        return sorted({x.to_schema() for x in controller.list_books()})


@router.put(
    path="/books",
    status_code=204,
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def refresh_all_books() -> None:
    with db_session:
        for book in controller.list_books():
            controller.refresh_book(book_id=book.book_id)


@router.get(
    path="/books/{book_id}",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def get_book_by_id(book_id: int) -> Book:
    with db_session:
        return controller.get_book_by_id(book_id=book_id).to_schema()


@router.delete(
    path="/books/{book_id}",
    status_code=204,
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def delete_book(book_id: int) -> None:
    with db_session:
        controller.delete_book(book_id)


@router.patch(
    path="/books/{book_id}",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def update_book(
    book_id: int,
    title: str = Body(embed=True),  # noqa: B008
    subtitle: str | None = Body(embed=True, default=None),  # noqa: B008
    format: str = Body(embed=True),  # noqa: A002, B008
    description: str | None = Body(embed=True, default=None),  # noqa: B008
    publisher_id: int = Body(embed=True),  # noqa: B008
    goodreads_id: str | None = Body(embed=True, default=None),  # noqa: B008
    library_thing_id: str | None = Body(embed=True, default=None),  # noqa: B008
    open_library_id: str | None = Body(embed=True, default=None),  # noqa: B008
) -> Book:
    with db_session:
        book = controller.get_book_by_id(book_id=book_id)
        book.title = title
        book.subtitle = subtitle or None
        book.format = format
        book.description = description or None
        book.publisher = controller.get_publisher_by_id(publisher_id=publisher_id)
        book.goodreads_id = goodreads_id or None
        book.library_thing_id = library_thing_id or None
        book.open_library_id = open_library_id or None
        flush()
        return book.to_schema()


@router.put(
    path="/books/{book_id}",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def refresh_book(book_id: int) -> Book:
    with db_session:
        return controller.refresh_book(book_id=book_id).to_schema()


@router.post(
    path="/books/{book_id}/collect",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def collect_book(book_id: int) -> Book:
    with db_session:
        book = controller.get_book_by_id(book_id=book_id)
        if not book.wisher:
            raise HTTPException(status_code=400, detail="Book has already been collected.")
        book.wisher = None
        flush()
        return book.to_schema()


@router.post(
    path="/books/{book_id}/read",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def read_book(
    book_id: int,
    user_id: int = Body(embed=True),  # noqa: B008
) -> Book:
    with db_session:
        book = controller.get_book_by_id(book_id=book_id)
        user = controller.get_user_by_id(user_id=user_id)
        if user in book.readers:
            book.readers.remove(user)
        else:
            book.readers.add(user)
        flush()
        return book.to_schema()


@router.post(path="/publishers", status_code=201, responses={409: {"model": ErrorResponse}})
def create_publisher(name: str = Body(embed=True)) -> Publisher:
    with db_session:
        return controller.create_publisher(name=name).to_schema()
