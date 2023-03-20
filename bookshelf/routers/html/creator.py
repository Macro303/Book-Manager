__all__ = ["router"]

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from bookshelf.controllers.creator import CreatorController
from bookshelf.routers.html.utils import CurrentUser, templates

router = APIRouter(prefix="/creators", tags=["Creators"])


@router.get(path="", response_class=HTMLResponse)
def list_creators(
    *,
    request: Request,
    current_user: CurrentUser,
    name: str = "",
):
    if not current_user:
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
                "current_user": current_user.to_model(),
                "creator_list": sorted({x.to_model() for x in creator_list}),
                "filters": {"name": name},
            },
        )


@router.get(path="/{creator_id}", response_class=HTMLResponse)
def view_creator(
    *,
    request: Request,
    creator_id: int,
    current_user: CurrentUser,
):
    if not current_user:
        return RedirectResponse("/")
    with db_session:
        creator = CreatorController.get_creator(creator_id=creator_id)
        book_dict = {}
        for temp in creator.books:
            temp_book = temp.book.to_model()
            roles = sorted({x.to_model() for x in temp.roles})
            book_dict[temp_book] = roles
        book_list = sorted(
            [(key, values) for key, values in book_dict.items()],
            key=lambda x: (x[1][0], x[0]),
        )
        return templates.TemplateResponse(
            "view_creator.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "creator": creator.to_model(),
                "book_list": book_list,
            },
        )


@router.get(path="/{creator_id}/edit", response_class=HTMLResponse)
def edit_creator(
    *,
    request: Request,
    creator_id: int,
    current_user: CurrentUser,
):
    if not current_user:
        return RedirectResponse("/")
    if current_user.role < 2:
        return RedirectResponse(f"/creators/{creator_id}")
    with db_session:
        creator = CreatorController.get_creator(creator_id=creator_id)
        return templates.TemplateResponse(
            "edit_creator.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "creator": creator.to_model(),
            },
        )
