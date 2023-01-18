__all__ = ["router"]

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, RedirectResponse, Response
from fastapi.templating import Jinja2Templates
from pony.orm import db_session

from book_catalogue import controller, get_project_root

router = APIRouter(tags=["WebInterface"], include_in_schema=False)
templates = Jinja2Templates(directory=get_project_root() / "templates")


@router.get("/", response_class=HTMLResponse)
def index(request: Request) -> Response:
    return templates.TemplateResponse("index.html", {"request": request})


@router.get("/{user_id}", response_class=HTMLResponse)
def home(request: Request, user_id: int) -> Response:
    with db_session:
        user = controller.get_user_by_id(user_id=user_id)
        return templates.TemplateResponse("home.html", {"request": request, "user": user})


@router.get("/{user_id}/collection", response_class=HTMLResponse)
def collection(
    request: Request,
    user_id: int,
    title: str = "",
    author: int = 0,
    format: str = "",  # noqa: A002
    series: int = 0,
    publisher: int = 0,
) -> Response:
    with db_session:
        user = controller.get_user_by_id(user_id=user_id)
        all_books = books = {x for x in controller.list_books() if not x.wisher}
        if title:
            books = {
                x
                for x in books
                if (title in x.title)
                or (x.title in title)
                or (x.subtitle and ((title in x.subtitle) or (x.subtitle in title)))
            }
        if author:
            _author = controller.get_author_by_id(author_id=author)
            books = {x for x in books if _author in x.authors}
        if format:
            if format == "None":
                books = {x for x in books if not x.format}
            else:
                books = {x for x in books if format == x.format}
        if series:
            _series = controller.get_series_by_id(series_id=series)
            books = {x for x in books if _series in [y.series for y in x.series]}
        if publisher:
            _publisher = controller.get_publisher_by_id(publisher_id=publisher)
            books = {x for x in books if _publisher == x.publisher}
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
                "series_list": sorted({y.series for x in all_books for y in x.series}),
                "series_id": series,
                "publisher_list": sorted({x.publisher for x in all_books}),
                "publisher_id": publisher,
            },
        )


@router.get("/{user_id}/wishlist", response_class=HTMLResponse)
def wishlist(
    request: Request,
    user_id: int,
    title: str = "",
    author: int = 0,
    format: str = "",  # noqa: A002
    series: int = 0,
    publisher: int = 0,
) -> Response:
    with db_session:
        user = controller.get_user_by_id(user_id=user_id)
        all_books = books = {x for x in controller.list_books() if x.wisher}
        if title:
            books = {
                x
                for x in books
                if (title in x.title)
                or (x.title in title)
                or (x.subtitle and ((title in x.subtitle) or (x.subtitle in title)))
            }
        if author:
            _author = controller.get_author_by_id(author_id=author)
            books = {x for x in books if _author in x.authors}
        if format:
            if format == "None":
                books = {x for x in books if not x.format}
            else:
                books = {x for x in books if format == x.format}
        if series:
            _series = controller.get_series_by_id(series_id=series)
            books = {x for x in books if _series in [y.series for y in x.series]}
        if publisher:
            _publisher = controller.get_publisher_by_id(publisher_id=publisher)
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
                "series_list": sorted({y.series for x in all_books for y in x.series}),
                "series_id": series,
                "publisher_list": sorted({x.publisher for x in all_books}),
                "publisher_id": publisher,
            },
        )


def view_book(request: Request, user_id: int, book_id: int) -> Response:
    with db_session:
        user = controller.get_user_by_id(user_id=user_id)
        book = controller.get_book_by_id(book_id=book_id)
        return templates.TemplateResponse(
            "view_book.html",
            {
                "request": request,
                "user": user.to_schema(),
                "book": book.to_schema(),
            },
        )


@router.get(path="/{user_id}/collection/{book_id}", response_class=HTMLResponse)
def view_collection_book(request: Request, user_id: int, book_id: int) -> Response:
    return view_book(request=request, user_id=user_id, book_id=book_id)


@router.get(path="/{user_id}/wishlist/{book_id}", response_class=HTMLResponse)
def view_wishlist_book(request: Request, user_id: int, book_id: int) -> Response:
    return view_book(request=request, user_id=user_id, book_id=book_id)


def edit_book(request: Request, user_id: int, book_id: int, is_wishlist: bool = False) -> Response:
    with db_session:
        user = controller.get_user_by_id(user_id=user_id)
        if user.role < 1:
            if is_wishlist:
                return RedirectResponse(url=f"/{user_id}/wishlist/{book_id}")
            return RedirectResponse(url=f"/{user_id}/collection/{book_id}")
        book = controller.get_book_by_id(book_id=book_id)
        author_list = controller.list_authors()
        return templates.TemplateResponse(
            "edit_book.html",
            {
                "request": request,
                "user": user.to_schema(),
                "book": book.to_schema(),
                "author_list": sorted({x.to_schema() for x in author_list}),
            },
        )


@router.get(path="/{user_id}/collection/{book_id}/edit", response_class=HTMLResponse)
def edit_collection_book(request: Request, user_id: int, book_id: int) -> Response:
    return edit_book(request=request, user_id=user_id, book_id=book_id)


@router.get(path="/{user_id}/wishlist/{book_id}/edit", response_class=HTMLResponse)
def edit_wishlist_book(request: Request, user_id: int, book_id: int) -> Response:
    return edit_book(request=request, user_id=user_id, book_id=book_id, is_wishlist=True)
