__all__ = ["router"]

from fastapi import APIRouter, Request, Depends
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from book_catalogue.routers.html._utils import templates, get_token_user
from book_catalogue.controllers.author import AuthorController
from book_catalogue.database.tables import User

router = APIRouter(prefix="/authors", tags=["Authors"])

@router.get(path="", response_class=HTMLResponse)
def list_authors(
    *,
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


@router.get(path="/{author_id}", response_class=HTMLResponse)
def view_author(
    *, request: Request, author_id: int, token_user: User | None = Depends(get_token_user)
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


@router.get(path="/{author_id}/edit", response_class=HTMLResponse)
def edit_author(
    *, request: Request, author_id: int, token_user: User | None = Depends(get_token_user)
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
