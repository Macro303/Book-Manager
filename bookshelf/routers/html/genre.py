__all__ = ["router"]

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from bookshelf.controllers.book import BookController
from bookshelf.controllers.genre import GenreController
from bookshelf.routers.html.utils import CurrentUser, templates

router = APIRouter(prefix="/genres", tags=["Genres"])


@router.get(path="", response_class=HTMLResponse)
def list_genres(
    *,
    request: Request,
    current_user: CurrentUser,
    name: str = "",
):
    if not current_user:
        return RedirectResponse("/")
    with db_session:
        genre_list = GenreController.list_genres()
        if name:
            genre_list = [
                x
                for x in genre_list
                if name.casefold() in x.name.casefold() or x.name.casefold() in name.casefold()
            ]
        return templates.TemplateResponse(
            "list_genres.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "genre_list": sorted({x.to_model() for x in genre_list}),
                "filters": {"name": name},
            },
        )


@router.get(path="/{genre_id}", response_class=HTMLResponse)
def view_genre(
    *,
    request: Request,
    genre_id: int,
    current_user: CurrentUser,
):
    if not current_user:
        return RedirectResponse("/")
    with db_session:
        genre = GenreController.get_genre(genre_id=genre_id)
        all_books = {x for x in BookController.list_books() if genre not in x.genres}
        book_list = sorted({x.to_model() for x in genre.books})
        return templates.TemplateResponse(
            "view_genre.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "genre": genre.to_model(),
                "book_list": book_list,
                "all_books": sorted({x.to_model() for x in all_books}),
            },
        )


@router.get(path="/{genre_id}/edit", response_class=HTMLResponse)
def edit_genre(
    *,
    request: Request,
    genre_id: int,
    current_user: CurrentUser,
):
    if not current_user:
        return RedirectResponse("/")
    if current_user.role < 2:
        return RedirectResponse(f"/genres/{genre_id}")
    with db_session:
        genre = GenreController.get_genre(genre_id=genre_id)
        return templates.TemplateResponse(
            "edit_genre.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "genre": genre.to_model(),
            },
        )
