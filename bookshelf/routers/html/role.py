__all__ = ["router"]

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from bookshelf.controllers.book import BookController
from bookshelf.controllers.creator import CreatorController
from bookshelf.controllers.role import RoleController
from bookshelf.routers.html.utils import CurrentUser, templates

router = APIRouter(prefix="/roles", tags=["Roles"])


@router.get(path="", response_class=HTMLResponse)
def list_roles(
    *,
    request: Request,
    current_user: CurrentUser,
    name: str = "",
):
    if not current_user:
        return RedirectResponse("/")
    with db_session:
        role_list = RoleController.list_roles()
        if name:
            role_list = [
                x
                for x in role_list
                if name.casefold() in x.name.casefold() or x.name.casefold() in name.casefold()
            ]
        return templates.TemplateResponse(
            "list_roles.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "role_list": sorted({x.to_model() for x in role_list}),
                "selected": {"name": name},
            },
        )


@router.get(path="/{role_id}", response_class=HTMLResponse)
def view_role(*, request: Request, role_id: int, current_user: CurrentUser):
    if not current_user:
        return RedirectResponse("/")
    with db_session:
        role = RoleController.get_role(role_id=role_id)
        creator_dict = {}
        for temp in role.creators:
            temp_creator = temp.creator.to_model()
            if temp_creator not in creator_dict:
                creator_dict[temp_creator] = set()
            creator_dict[temp_creator].add(temp.book.to_model())
        creator_dict = {key: sorted(creator_dict[key]) for key in sorted(creator_dict.keys())}
        return templates.TemplateResponse(
            "view_role.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "role": role.to_model(),
                "creators": creator_dict,
                "all_books": sorted({x.to_model() for x in BookController.list_books()}),
                "all_creators": sorted({x.to_model() for x in CreatorController.list_creators()}),
            },
        )


@router.get(path="/{role_id}/edit", response_class=HTMLResponse)
def edit_role(*, request: Request, role_id: int, current_user: CurrentUser):
    if not current_user:
        return RedirectResponse("/")
    if current_user.role < 2:
        return RedirectResponse(f"/roles/{role_id}")
    with db_session:
        role = RoleController.get_role(role_id=role_id)
        return templates.TemplateResponse(
            "edit_role.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "role": role.to_model(),
            },
        )
