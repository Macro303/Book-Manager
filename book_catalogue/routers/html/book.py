__all__ = ["router"]

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from book_catalogue.controllers.author import AuthorController
from book_catalogue.controllers.book import BookController
from book_catalogue.controllers.format import FormatController
from book_catalogue.controllers.publisher import PublisherController
from book_catalogue.controllers.series import SeriesController
from book_catalogue.database.tables import User
from book_catalogue.routers.html._utils import get_token_user, templates

router = APIRouter(prefix="/books", tags=["Books"])


@router.get(path="", response_class=HTMLResponse)
def list_books(
    *,
    request: Request,
    token_user: User | None = Depends(get_token_user),
    author_id: int = 0,
    format_id: int = 0,
    publisher_id: int = 0,
    read: bool = False,
    series_id: int = 0,
    title: str = "",
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        all_books = book_list = [x for x in BookController.list_books() if x.is_collected]
        if author_id:
            if author_id == -1:
                book_list = [x for x in book_list if not x.authors]
            else:
                author = AuthorController.get_author(author_id=author_id)
                book_list = [x for x in book_list if author in [y.author for y in x.authors]]
        if format_id:
            if format_id == -1:
                book_list = [x for x in book_list if not x.format]
            else:
                format = FormatController.get_format(format_id=format_id)
                book_list = [x for x in book_list if format == x.format]
        if publisher_id:
            if publisher_id == -1:
                book_list = [x for x in book_list if not x.publisher]
            else:
                publisher = PublisherController.get_publisher(publisher_id=publisher_id)
                book_list = [x for x in book_list if publisher == x.publisher]
        if read:
            book_list = [
                x for x in book_list if token_user.user_id in [y.user_id for y in x.readers]
            ]
        else:
            book_list = [
                x for x in book_list if token_user.user_id not in [y.user_id for y in x.readers]
            ]
        if series_id:
            if series_id == -1:
                book_list = [x for x in book_list if not x.series]
            else:
                series = SeriesController.get_series(series_id=series_id)
                book_list = [x for x in book_list if series in [y.series for y in x.series]]
        if title:
            book_list = [
                x
                for x in book_list
                if (title.casefold() in x.title.casefold())
                or (x.title.casefold() in title.casefold())
                or (
                    x.subtitle
                    and (
                        (title.casefold() in x.subtitle.casefold())
                        or (x.subtitle.casefold() in title.casefold())
                    )
                )
            ]
        return templates.TemplateResponse(
            "list_books.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "book_list": sorted({x.to_schema() for x in book_list}),
                "author_list": sorted({y.author.to_schema() for x in all_books for y in x.authors}),
                "format_list": sorted({x.format.to_schema() for x in all_books if x.format}),
                "publisher_list": sorted(
                    {x.publisher.to_schema() for x in all_books if x.publisher}
                ),
                "series_list": sorted({y.series.to_schema() for x in all_books for y in x.series}),
                "filters": {
                    "author_id": author_id,
                    "format_id": format_id,
                    "publisher_id": publisher_id,
                    "read": read,
                    "series_id": series_id,
                    "title": title,
                },
            },
        )


@router.get(path="/{book_id}", response_class=HTMLResponse)
def view_book(*, request: Request, book_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        book = BookController.get_book(book_id=book_id)
        return templates.TemplateResponse(
            "view_book.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "book": book.to_schema(),
            },
        )


@router.get(path="/{book_id}/edit", response_class=HTMLResponse)
def edit_book(*, request: Request, book_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    if token_user.role < 2:
        return RedirectResponse(f"/books/{book_id}")
    with db_session:
        book = BookController.get_book(book_id=book_id)
        author_list = AuthorController.list_authors()
        role_list = AuthorController.list_roles()
        format_list = FormatController.list_formats()
        publisher_list = PublisherController.list_publishers()
        series_list = SeriesController.list_series()
        return templates.TemplateResponse(
            "edit_book.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "book": book.to_schema(),
                "author_list": sorted({x.to_schema() for x in author_list}),
                "role_list": sorted({x.to_schema() for x in role_list}),
                "format_list": sorted({x.to_schema() for x in format_list}),
                "publisher_list": sorted({x.to_schema() for x in publisher_list}),
                "series_list": sorted({x.to_schema() for x in series_list}),
            },
        )
