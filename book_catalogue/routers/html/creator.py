__all__ = ["router"]

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from book_catalogue.controllers.creator import CreatorController
from book_catalogue.database.tables import User
from book_catalogue.routers.html._utils import get_token_user, templates

router = APIRouter(prefix="/creators", tags=["Creators"])


@router.get(path="", response_class=HTMLResponse)
def list_creators(
    *,
    request: Request,
    token_user: User | None = Depends(get_token_user),
    name: str = "",
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        creator_list = CreatorController.list_creators()
        if name:
            creator_list = [
                x
                for x in creator_list
                if name.casefold() in x.name.casefold() or x.name.casefold() in name.casefold()
            ]
        return templates.TemplateResponse(
            "list_creators.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "creator_list": sorted({x.to_schema() for x in creator_list}),
                "filters": {"name": name},
            },
        )


@router.get(path="/{creator_id}", response_class=HTMLResponse)
def view_creator(
    *, request: Request, creator_id: int, token_user: User | None = Depends(get_token_user)
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        creator = CreatorController.get_creator(creator_id=creator_id)
        book_dict = {}
        for temp in creator.books:
            temp_book = temp.book.to_schema()
            roles = sorted({x.to_schema() for x in temp.roles})
            book_dict[temp_book] = roles
        book_list = sorted(
            [(key, values) for key, values in book_dict.items()], key=lambda x: (x[1][0], x[0])
        )
        return templates.TemplateResponse(
            "view_creator.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "creator": creator.to_schema(),
                "book_list": book_list,
            },
        )


@router.get(path="/{creator_id}/edit", response_class=HTMLResponse)
def edit_creator(
    *, request: Request, creator_id: int, token_user: User | None = Depends(get_token_user)
):
    if not token_user:
        return RedirectResponse("/")
    if token_user.role < 2:
        return RedirectResponse(f"/creators/{creator_id}")
    with db_session:
        creator = CreatorController.get_creator(creator_id=creator_id)
        return templates.TemplateResponse(
            "edit_creator.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "creator": creator.to_schema(),
            },
        )
