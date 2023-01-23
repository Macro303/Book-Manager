from __future__ import annotations
__all__ = ["router"]

from fastapi import APIRouter, Request, Depends, Header, Cookie
from fastapi.responses import HTMLResponse, RedirectResponse, Response
from fastapi.templating import Jinja2Templates
from pony.orm import db_session

from book_catalogue import get_project_root
from book_catalogue.console import CONSOLE
from book_catalogue.controllers import UserController, BookController, AuthorController, SeriesController, PublisherController
from book_catalogue.database.tables import User

router = APIRouter(include_in_schema=False)
templates = Jinja2Templates(directory=get_project_root() / "templates")


def get_token_user(token: int | None = Cookie(default=None)) -> User | None:
    if not token:
        return None
    with db_session:
        if temp := UserController.get_user(user_id=token):
            return temp
        return None


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
            author_list = [x for x in author_list if name.casefold() in x.name.casefold() or x.name.casefold() in name.casefold()]
        return templates.TemplateResponse(
            "list_authors.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "author_list": sorted({x.to_schema() for x in author_list}),
                "filters": {
                    "name": name
                }
            }
        )


@router.get("/authors/{author_id}", response_class=HTMLResponse)
def view_author(request: Request, author_id: int, token_user: User | None = Depends(get_token_user)):
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
            }
        )


@router.get("/authors/{author_id}/edit", response_class=HTMLResponse)
def edit_author(request: Request, author_id: int, token_user: User | None = Depends(get_token_user)):
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
            }
        )


@router.get("/publishers", response_class=HTMLResponse)
def list_publishers(
    request: Request,
    token_user: User | None = Depends(get_token_user),
    name: str = "",
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        publisher_list = PublisherController.list_publishers()
        if name:
            publisher_list = [x for x in publisher_list if name.casefold() in x.name.casefold() or x.name.casefold() in name.casefold()]
        return templates.TemplateResponse(
            "list_publishers.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "publisher_list": sorted({x.to_schema() for x in publisher_list}),
                "filters": {
                    "name": name
                }
            }
        )


@router.get("/publishers/{publisher_id}", response_class=HTMLResponse)
def view_publisher(request: Request, publisher_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        publisher = PublisherController.get_publisher(publisher_id=publisher_id)
        book_list = sorted({x.to_schema() for x in publisher.books})
        return templates.TemplateResponse(
            "view_publisher.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "publisher": publisher.to_schema(),
                "book_list": book_list,
            }
        )


@router.get("/publishers/{publisher_id}/edit", response_class=HTMLResponse)
def edit_publisher(request: Request, publisher_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    if token_user.role < 2:
        return RedirectResponse(f"/publishers/{publisher_id}")
    with db_session:
        publisher = PublisherController.get_publisher(publisher_id=publisher_id)
        return templates.TemplateResponse(
            "edit_publisher.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "publisher": publisher.to_schema(),
            }
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
            user_list = [x for x in user_list if username.casefold() in x.username.casefold() or x.username.casefold() in username.casefold()]
        return templates.TemplateResponse(
            "list_users.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "user_list": sorted({x.to_schema() for x in user_list}),
                "filters": {
                    "username": username
                }
            }
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
                "token_user": token_user.to_schema(),
                "user": user.to_schema(),
            }
        )
        
        
@router.get("/users/{user_id}/edit", response_class=HTMLResponse)
def edit_user(request: Request, user_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        user = UserController.get_user(user_id=user_id)
        if token_user.user_id != user.user_id and (token_user.role < 8 or token_user.role <= user.role):
            return RedirectResponse(f"/users/{user_id}")
        return templates.TemplateResponse(
            "edit_user.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "user": user.to_schema(),
            }
        )


@router.get("/users/{user_id}/wishlist", response_class=HTMLResponse)
def user_wishlist(
    request: Request,
    user_id: int,
    token_user: User | None = Depends(get_token_user),
    author_id: int = 0,
    format: str = "",
    series_id: int = 0,
    title: str = "",
    publisher_id: int = 0
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        user = UserController.get_user(user_id=user_id)
        wishlist = user.wished_books
        if author_id:
            author = AuthorController.get_author(author_id=author_id)
            wishlist = [x for x in wishlist if author in [y.author for y in x.authors]]
        if format:
            if format == "None":
                wishlist = [x for x in wishlist if not x.format]
            else:
                wishlist = [x for x in wishlist if format == x.format]
        if series_id:
            series = SeriesController.get_series(series_id=series_id)
            wishlist = [x for x in wishlist if series in [y.series for y in x.series]]
        if title:
            wishlist = [x for x in wishlist if (title.casefold() in x.title.casefold()) or (x.title.casefold() in title.casefold()) or (x.subtitle and ((title.casefold() in x.subtitle.casefold()) or (x.subtitle.casefold() in title.casefold())))]
        if publisher_id:
            publisher = PublisherController.get_publisher(publisher_id=publisher_id)
            wishlist = [x for x in wishlist if publisher == x.publisher]
        return templates.TemplateResponse(
            "user_wishlist.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "user": user.to_schema(),
                "wishlist": sorted({x.to_schema() for x in wishlist}),
                "author_list": sorted({y.author.to_schema() for x in wishlist for y in x.authors}),
                "format_list": sorted({x.format or "None" for x in wishlist}),
                "series_list": sorted({y.series.to_schema() for x in wishlist for y in x.series}),
                "publisher_list": sorted({x.publisher.to_schema() for x in wishlist}),
                "filters": {
                    "author_id": author_id,
                    "format": format,
                    "series_id": series_id,
                    "title": title,
                    "publisher_id": publisher_id
                }
            }
        )


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
