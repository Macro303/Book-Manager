__all__ = ["router"]

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from book_catalogue.controllers.role import RoleController
from book_catalogue.database.tables import User
from book_catalogue.routers.html._utils import get_token_user, templates

router = APIRouter(prefix="/roles", tags=["Roles"])


@router.get(path="", response_class=HTMLResponse)
def list_roles(
    *,
    request: Request,
    token_user: User | None = Depends(get_token_user),
    name: str = "",
):
    if not token_user:
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
                "token_user": token_user.to_schema(),
                "role_list": sorted({x.to_schema() for x in role_list}),
                "filters": {"name": name},
            },
        )


@router.get(path="/{role_id}", response_class=HTMLResponse)
def view_role(*, request: Request, role_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        role = RoleController.get_role(role_id=role_id)
        author_dict = {}
        for temp in role.authors:
            temp_author = temp.author.to_schema()
            if temp_author not in author_dict:
                author_dict[temp_author] = set()
            author_dict[temp_author].add(temp.book.to_schema())
        author_dict = {key: sorted(author_dict[key]) for key in sorted(author_dict.keys())}
        return templates.TemplateResponse(
            "view_role.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "role": role.to_schema(),
                "authors": author_dict,
            },
        )


@router.get(path="/{role_id}/edit", response_class=HTMLResponse)
def edit_role(*, request: Request, role_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    if token_user.role < 2:
        return RedirectResponse(f"/roles/{role_id}")
    with db_session:
        role = RoleController.get_role(role_id=role_id)
        return templates.TemplateResponse(
            "edit_role.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "role": role.to_schema(),
            },
        )
