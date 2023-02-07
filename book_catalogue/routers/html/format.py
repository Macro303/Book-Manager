__all__ = ["router"]

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from book_catalogue.controllers.format import FormatController
from book_catalogue.database.tables import User
from book_catalogue.routers.html._utils import get_token_user, templates

router = APIRouter(prefix="/formats", tags=["Formats"])


@router.get(path="", response_class=HTMLResponse)
def list_formats(
    *,
    request: Request,
    token_user: User | None = Depends(get_token_user),
    name: str = "",
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        format_list = FormatController.list_formats()
        if name:
            format_list = [
                x
                for x in format_list
                if name.casefold() in x.name.casefold() or x.name.casefold() in name.casefold()
            ]
        return templates.TemplateResponse(
            "list_formats.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "format_list": sorted({x.to_schema() for x in format_list}),
                "filters": {"name": name},
            },
        )


@router.get(path="/{format_id}", response_class=HTMLResponse)
def view_format(
    *, request: Request, format_id: int, token_user: User | None = Depends(get_token_user)
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        format = FormatController.get_format(format_id=format_id)
        book_list = sorted({x.to_schema() for x in format.books})
        return templates.TemplateResponse(
            "view_format.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "format": format.to_schema(),
                "book_list": book_list,
            },
        )


@router.get(path="/{format_id}/edit", response_class=HTMLResponse)
def edit_format(
    *, request: Request, format_id: int, token_user: User | None = Depends(get_token_user)
):
    if not token_user:
        return RedirectResponse("/")
    if token_user.role < 2:
        return RedirectResponse(f"/formats/{format_id}")
    with db_session:
        format = FormatController.get_format(format_id=format_id)
        return templates.TemplateResponse(
            "edit_format.html",
            {
                "request": request,
                "token_user": token_user.to_schema(),
                "format": format.to_schema(),
            },
        )
