from __future__ import annotations

__all__ = ["router"]

from fastapi import APIRouter, Cookie, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from pony.orm import db_session, flush

from book_catalogue import get_project_root
from book_catalogue.controllers.author import AuthorController
from book_catalogue.controllers.book import BookController
from book_catalogue.controllers.format import FormatController
from book_catalogue.controllers.publisher import PublisherController
from book_catalogue.controllers.series import SeriesController
from book_catalogue.controllers.user import UserController
from book_catalogue.database.tables import User

router = APIRouter(include_in_schema=False)
templates = Jinja2Templates(directory=get_project_root() / "templates")


def get_token_user(token: int | None = Cookie(default=None)) -> User | None:
    if not token:
        return None
    with db_session:
        return UserController.get_user(user_id=token)


@router.get("/", response_class=HTMLResponse)
def index(request: Request, token_user: User | None = Depends(get_token_user)):
    if token_user:
        return RedirectResponse(f"/users/{token_user.user_id}")
    return templates.TemplateResponse("index.html", {"request": request})


@router.get("/authors", response_class=HTMLResponse)
def list_authors(
    request: Request,
    token_user: User | None = Depends(get_token_user),
    name: str = "",
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        author_list = AuthorController.list_authors()
        if name:
            author_list = [
                x
                for x in author_list
                if name.casefold() in x.name.casefold() or x.name.casefold() in name.casefold()
            ]
        return templates.TemplateResponse(
            "list_authors.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "author_list": sorted({x.to_schema() for x in author_list}),
                "filters": {"name": name},
            },
        )


@router.get("/authors/{author_id}", response_class=HTMLResponse)
def view_author(
    request: Request, author_id: int, token_user: User | None = Depends(get_token_user)
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        author = AuthorController.get_author(author_id=author_id)
        book_list = sorted({x.book.to_schema() for x in author.books})
        return templates.TemplateResponse(
            "view_author.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "author": author.to_schema(),
                "book_list": book_list,
            },
        )


@router.get("/authors/{author_id}/edit", response_class=HTMLResponse)
def edit_author(
    request: Request, author_id: int, token_user: User | None = Depends(get_token_user)
):
    if not token_user:
        return RedirectResponse("/")
    if token_user.role < 2:
        return RedirectResponse(f"/authors/{author_id}")
    with db_session:
        author = AuthorController.get_author(author_id=author_id)
        return templates.TemplateResponse(
            "edit_author.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "author": author.to_schema(),
            },
        )


@router.get("/books", response_class=HTMLResponse)
def list_books(
    request: Request,
    token_user: User | None = Depends(get_token_user),
    author_id: int = 0,
    format_id: int = 0,
    series_id: int = 0,
    title: str = "",
    publisher_id: int = 0,
    read: bool = False,
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
        return templates.TemplateResponse(
            "list_books.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "book_list": sorted({x.to_schema() for x in book_list}),
                "author_list": sorted({y.author.to_schema() for x in all_books for y in x.authors}),
                "format_list": sorted({x.format.to_schema() for x in all_books if x.format}),
                "series_list": sorted({y.series.to_schema() for x in all_books for y in x.series}),
                "publisher_list": sorted(
                    {x.publisher.to_schema() for x in all_books if x.publisher}
                ),
                "filters": {
                    "author_id": author_id,
                    "format_id": format_id,
                    "series_id": series_id,
                    "title": title,
                    "publisher_id": publisher_id,
                    "read": read,
                },
            },
        )


@router.get("/books/{book_id}", response_class=HTMLResponse)
def view_book(request: Request, book_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        book = BookController.get_book(book_id=book_id)
        flush()
        return templates.TemplateResponse(
            "view_book.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "book": book.to_schema(),
            },
        )


@router.get("/books/{book_id}/edit", response_class=HTMLResponse)
def edit_book(request: Request, book_id: int, token_user: User | None = Depends(get_token_user)):
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


@router.get("/series", response_class=HTMLResponse)
def list_series(
    request: Request,
    token_user: User | None = Depends(get_token_user),
    title: str = "",
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        series_list = SeriesController.list_series()
        if title:
            series_list = [
                x
                for x in series_list
                if title.casefold() in x.title.casefold() or x.title.casefold() in title.casefold()
            ]
        return templates.TemplateResponse(
            "list_series.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "series_list": sorted({x.to_schema() for x in series_list}),
                "filters": {"title": title},
            },
        )


@router.get("/series/{series_id}", response_class=HTMLResponse)
def view_series(
    request: Request, series_id: int, token_user: User | None = Depends(get_token_user)
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        series = SeriesController.get_series(series_id=series_id)
        book_list = sorted({(x.book.to_schema(), x.number) for x in series.books})
        return templates.TemplateResponse(
            "view_series.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "series": series.to_schema(),
                "book_list": book_list,
            },
        )


@router.get("/series/{series_id}/edit", response_class=HTMLResponse)
def edit_series(
    request: Request, series_id: int, token_user: User | None = Depends(get_token_user)
):
    if not token_user:
        return RedirectResponse("/")
    if token_user.role < 2:
        return RedirectResponse(f"/series/{series_id}")
    with db_session:
        series = SeriesController.get_series(series_id=series_id)
        return templates.TemplateResponse(
            "edit_series.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "series": series.to_schema(),
            },
        )


@router.get("/users", response_class=HTMLResponse)
def list_users(
    request: Request,
    token_user: User | None = Depends(get_token_user),
    username: str = "",
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        user_list = UserController.list_users()
        if username:
            user_list = [
                x
                for x in user_list
                if username.casefold() in x.username.casefold()
                or x.username.casefold() in username.casefold()
            ]
        return templates.TemplateResponse(
            "list_users.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "user_list": sorted({x.to_schema() for x in user_list}),
                "filters": {"username": username},
            },
        )


@router.get("/users/{user_id}", response_class=HTMLResponse)
def view_user(request: Request, user_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        user = UserController.get_user(user_id=user_id)
        return templates.TemplateResponse(
            "view_user.html",
            {
                "request": request,
                "token_user": token_user,
                "user": user,
            },
        )


@router.get("/users/{user_id}/edit", response_class=HTMLResponse)
def edit_user(request: Request, user_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        user = UserController.get_user(user_id=user_id)
        if token_user.user_id != user.user_id and (
            token_user.role < 4 or token_user.role <= user.role
        ):
            return RedirectResponse(f"/users/{user_id}")
        return templates.TemplateResponse(
            "edit_user.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "user": user.to_schema(),
            },
        )


@router.get("/users/{user_id}/wishlist", response_class=HTMLResponse)
def user_wishlist(
    request: Request,
    user_id: int,
    token_user: User | None = Depends(get_token_user),
    author_id: int = 0,
    format_id: int = 0,
    series_id: int = 0,
    title: str = "",
    publisher_id: int = 0,
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        user = UserController.get_user(user_id=user_id)
        all_wishlist = wishlist = {
            *user.wished_books,
            *[x for x in BookController.list_books() if not x.is_collected and not x.wishers],
        }
        if author_id:
            if author_id == -1:
                wishlist = [x for x in wishlist if not x.authors]
            else:
                author = AuthorController.get_author(author_id=author_id)
                wishlist = [x for x in wishlist if author in [y.author for y in x.authors]]
        if format_id:
            if format_id == -1:
                wishlist = [x for x in wishlist if not x.format]
            else:
                format = FormatController.get_format(format_id=format_id)
                wishlist = [x for x in wishlist if x.format == format]
        if series_id:
            if series_id == -1:
                wishlist = [x for x in wishlist if not x.series]
            else:
                series = SeriesController.get_series(series_id=series_id)
                wishlist = [x for x in wishlist if series in [y.series for y in x.series]]
        if title:
            wishlist = [
                x
                for x in wishlist
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
        if publisher_id:
            if publisher_id == -1:
                wishlist = [x for x in wishlist if not x.publisher]
            else:
                publisher = PublisherController.get_publisher(publisher_id=publisher_id)
                wishlist = [x for x in wishlist if publisher == x.publisher]
        return templates.TemplateResponse(
            "user_wishlist.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "user": user.to_schema(),
                "wishlist": sorted({x.to_schema() for x in wishlist}),
                "author_list": sorted(
                    {y.author.to_schema() for x in all_wishlist for y in x.authors}
                ),
                "format_list": sorted({x.format or "None" for x in all_wishlist}),
                "series_list": sorted(
                    {y.series.to_schema() for x in all_wishlist for y in x.series}
                ),
                "publisher_list": sorted({x.publisher.to_schema() for x in all_wishlist}),
                "filters": {
                    "author_id": author_id,
                    "format_id": format_id,
                    "series_id": series_id,
                    "title": title,
                    "publisher_id": publisher_id,
                },
            },
        )
