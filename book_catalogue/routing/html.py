from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from pony.orm import db_session

from book_catalogue import controller, get_project_root

router = APIRouter(tags=["WebInterface"], include_in_schema=False)
templates = Jinja2Templates(directory=get_project_root() / "templates")


@router.get("/", response_class=HTMLResponse)
def index(request: Request):
    return templates.TemplateResponse("index.html", {"request": request})


@router.get("/{user_id}", response_class=HTMLResponse)
def home(request: Request, user_id: int):
    with db_session:
        user = controller.get_user_by_id(user_id=user_id)
        return templates.TemplateResponse("home.html", {"request": request, "user": user})


@router.get("/{user_id}/collection", response_class=HTMLResponse)
def collection(
    request: Request,
    user_id: int,
    title: str = "",
    author: int = 0,
    format: str = "",
    series: int = 0,
    publisher: int = 0,
):
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
            books = {x for x in books if _series in x.series}
        if publisher:
            _publisher = controller.get_publisher_by_id(publisher_id=publisher)
            books = {x for x in books if _publisher in x.publishers}
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
                "series_list": sorted({y.to_schema() for x in all_books for y in x.series}),
                "series_id": series,
                "publisher_list": sorted({y.to_schema() for x in all_books for y in x.publishers}),
                "publisher_id": publisher,
            },
        )


@router.get("/{user_id}/wishlist", response_class=HTMLResponse)
def wishlist(
    request: Request,
    user_id: int,
    title: str = "",
    author: int = 0,
    format: str = "",
    series: int = 0,
    publisher: int = 0,
):
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
            books = {x for x in books if _series in x.series}
        if publisher:
            _publisher = controller.get_publisher_by_id(publisher_id=publisher)
            books = {x for x in books if _publisher in x.publishers}
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
                "series_list": sorted({y.to_schema() for x in all_books for y in x.series}),
                "series_id": series,
                "publisher_list": sorted({y.to_schema() for x in all_books for y in x.publishers}),
                "publisher_id": publisher,
            },
        )
