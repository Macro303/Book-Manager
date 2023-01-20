__all__ = ["router"]

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, RedirectResponse, Response
from fastapi.templating import Jinja2Templates
from pony.orm import db_session

from book_catalogue import get_project_root
from book_catalogue.controllers import UserController, BookController, AuthorController, SeriesController, PublisherController

router = APIRouter(include_in_schema=False)
templates = Jinja2Templates(directory=get_project_root() / "templates")


@router.get("/", response_class=HTMLResponse)
def index(request: Request) -> Response:
    return templates.TemplateResponse("index.html", {"request": request})


@router.get("/{user_id}", response_class=HTMLResponse)
def home(request: Request, user_id: int) -> Response:
    with db_session:
        user = UserController.get_user(user_id=user_id)
        return templates.TemplateResponse("home.html", {"request": request, "user": user})


@router.get("/{user_id}/collection", response_class=HTMLResponse)
def collection(
    request: Request,
    user_id: int,
    title: str = None,
    author: int = None,
    format: str = None,  # noqa: A002
    series: int = None,
    publisher: int = None,
    read: bool = False,
) -> Response:
    with db_session:
        user = UserController.get_user(user_id=user_id)
        all_books = books = {x for x in BookController.list_books() if not x.wisher}
        if title:
            books = {
                x
                for x in books
                if (title in x.title)
                or (x.title in title)
                or (x.subtitle and ((title in x.subtitle) or (x.subtitle in title)))
            }
        if author:
            _author = AuthorController.get_author(author_id=author)
            books = {x for x in books if _author in [y.author for y in x.authors]}
        if format:
            if format == "None":
                books = {x for x in books if not x.format}
            else:
                books = {x for x in books if format == x.format}
        if series:
            _series = SeriesController.get_series(series_id=series)
            books = {x for x in books if _series in [y.series for y in x.series]}
        if publisher:
            _publisher = PublisherController.get_publisher(publisher_id=publisher)
            books = {x for x in books if _publisher == x.publisher}
        if read:
            books = {x for x in books if user in x.readers}
        else:
            books = {x for x in books if user not in x.readers}
        return templates.TemplateResponse(
            "collection.html",
            {
                "request": request,
                "user": user.to_schema(),
                "book_list": sorted(x.to_schema() for x in books),
                "author_list": sorted({y.to_schema() for x in all_books for y in x.authors}),
                "author_id": author,
                "format_list": sorted({x.format or "None" for x in all_books}),
                "format": format,
                "series_list": sorted({y.series.to_schema() for x in all_books for y in x.series}),
                "series_id": series,
                "publisher_list": sorted({x.publisher.to_schema() for x in all_books}),
                "publisher_id": publisher,
                "read": read,
            },
        )


@router.get("/{user_id}/wishlist", response_class=HTMLResponse)
def wishlist(
    request: Request,
    user_id: int,
    title: str = None,
    author: int = None,
    format: str = None,  # noqa: A002
    series: int = None,
    publisher: int = None,
) -> Response:
    with db_session:
        user = UserController.get_user(user_id=user_id)
        all_books = books = {x for x in BookController.list_books() if x.wisher}
        if title:
            books = {
                x
                for x in books
                if (title in x.title)
                or (x.title in title)
                or (x.subtitle and ((title in x.subtitle) or (x.subtitle in title)))
            }
        if author:
            _author = AuthorController.get_author(author_id=author)
            books = {x for x in books if _author in [y.author for y in x.authors]}
        if format:
            if format == "None":
                books = {x for x in books if not x.format}
            else:
                books = {x for x in books if format == x.format}
        if series:
            _series = SeriesController.get_series(series_id=series)
            books = {x for x in books if _series in [y.series for y in x.series]}
        if publisher:
            _publisher = PublisherController.get_publisher(publisher_id=publisher)
            books = {x for x in books if _publisher == x.publisher}
        return templates.TemplateResponse(
            "wishlist.html",
            {
                "request": request,
                "user": user.to_schema(),
                "book_list": sorted(x.to_schema() for x in books),
                "author_list": sorted({y.to_schema() for x in all_books for y in x.authors}),
                "author_id": author,
                "format_list": sorted({x.format or "None" for x in all_books}),
                "format": format,
                "series_list": sorted({y.series.to_schema() for x in all_books for y in x.series}),
                "series_id": series,
                "publisher_list": sorted({x.publisher.to_schema() for x in all_books}),
                "publisher_id": publisher,
            },
        )


@router.get(path="/{user_id}/books/{book_id}", response_class=HTMLResponse)
def view_book(request: Request, user_id: int, book_id: int) -> Response:
    with db_session:
        user = UserController.get_user(user_id=user_id)
        book = BookController.get_book(book_id=book_id)
        return templates.TemplateResponse(
            "view_book.html",
            {
                "request": request,
                "user": user.to_schema(),
                "book": book.to_schema(),
            },
        )


@router.get(path="/{user_id}/books/{book_id}/update", response_class=HTMLResponse)
def update_book(request: Request, user_id: int, book_id: int) -> Response:
    with db_session:
        user = UserController.get_user(user_id=user_id)
        if user.role < 1:
            return RedirectResponse(url=f"/{user_id}/books/{book_id}")
        book = BookController.get_book(book_id=book_id)
        author_list = AuthorController.list_authors()
        format_list = {x.format for x in BookController.list_books() if x.format}
        publisher_list = PublisherController.list_publishers()
        return templates.TemplateResponse(
            "update_book.html",
            {
                "request": request,
                "user": user.to_schema(),
                "book": book.to_schema(),
                "author_list": sorted({x.to_schema() for x in author_list}),
                "format_list": sorted(format_list),
                "publisher_list": sorted({x.to_schema() for x in publisher_list}),
            },
        )
