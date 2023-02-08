__all__ = ["router"]

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from book_catalogue.controllers.book import BookController
from book_catalogue.controllers.creator import CreatorController
from book_catalogue.controllers.format import FormatController
from book_catalogue.controllers.genre import GenreController
from book_catalogue.controllers.publisher import PublisherController
from book_catalogue.controllers.role import RoleController
from book_catalogue.controllers.series import SeriesController
from book_catalogue.database.tables import User
from book_catalogue.routers.html._utils import get_token_user, templates
from book_catalogue.schemas.format import FormatRead
from book_catalogue.schemas.publisher import PublisherRead

router = APIRouter(prefix="/books", tags=["Books"])


@router.get(path="", response_class=HTMLResponse)
def list_books(
    *,
    request: Request,
    token_user: User | None = Depends(get_token_user),
    creator_id: int = 0,
    format_id: int = 0,
    genre_id: int = 0,
    publisher_id: int = 0,
    read: bool = False,
    series_id: int = 0,
    title: str = "",
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        all_books = book_list = [x for x in BookController.list_books() if x.is_collected]
        if creator_id:
            if creator_id == -1:
                book_list = [x for x in book_list if not x.creators]
            else:
                creator = CreatorController.get_creator(creator_id=creator_id)
                book_list = [x for x in book_list if creator in [y.creator for y in x.creators]]
        if format_id:
            if format_id == -1:
                book_list = [x for x in book_list if not x.format]
            else:
                format = FormatController.get_format(format_id=format_id)
                book_list = [x for x in book_list if format == x.format]
        if genre_id:
            if genre_id == -1:
                book_list = [x for x in book_list if not x.genres]
            else:
                genre = GenreController.get_genre(genre_id=genre_id)
                book_list = [x for x in book_list if genre == x.genre]
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
                "creator_list": sorted(
                    {y.creator.to_schema() for x in all_books for y in x.creators}
                ),
                "format_list": sorted(
                    {
                        x.format.to_schema() if x.format else FormatRead(format_id=-1, name="None")
                        for x in all_books
                    }
                ),
                "genre_list": sorted({y.genre.to_schema() for x in all_books for y in x.genres}),
                "publisher_list": sorted(
                    {
                        x.publisher.to_schema()
                        if x.publisher
                        else PublisherRead(publisher_id=-1, name="None")
                        for x in all_books
                    }
                ),
                "series_list": sorted({y.series.to_schema() for x in all_books for y in x.series}),
                "filters": {
                    "creator_id": creator_id,
                    "format_id": format_id,
                    "genre_id": genre_id,
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
        creator_list = CreatorController.list_creators()
        format_list = FormatController.list_formats()
        genre_list = GenreController.list_genres()
        publisher_list = PublisherController.list_publishers()
        role_list = RoleController.list_roles()
        series_list = SeriesController.list_series()
        return templates.TemplateResponse(
            "edit_book.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "book": book.to_schema(),
                "creator_list": sorted({x.to_schema() for x in creator_list}),
                "format_list": sorted({x.to_schema() for x in format_list}),
                "genre_list": sorted({x.to_schema() for x in genre_list}),
                "publisher_list": sorted({x.to_schema() for x in publisher_list}),
                "role_list": sorted({x.to_schema() for x in role_list}),
                "series_list": sorted({x.to_schema() for x in series_list}),
            },
        )
