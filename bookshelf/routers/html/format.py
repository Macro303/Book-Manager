__all__ = ["router"]

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from bookshelf.controllers.format import FormatController
from bookshelf.routers.html.utils import CurrentUser, templates

router = APIRouter(prefix="/formats", tags=["Formats"])


@router.get(path="", response_class=HTMLResponse)
def list_formats(
    *,
    request: Request,
    current_user: CurrentUser,
    name: str = "",
):
    if not current_user:
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
                "current_user": current_user.to_model(),
                "format_list": sorted({x.to_model() for x in format_list}),
                "selected": {"name": name},
            },
        )


@router.get(path="/{format_id}", response_class=HTMLResponse)
def view_format(
    *,
    request: Request,
    format_id: int,
    current_user: CurrentUser,
):
    if not current_user:
        return RedirectResponse("/")
    with db_session:
        format = FormatController.get_format(format_id=format_id)
        book_list = sorted({x.to_model() for x in format.books})
        return templates.TemplateResponse(
            "view_format.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "format": format.to_model(),
                "book_list": book_list,
            },
        )


@router.get(path="/{format_id}/edit", response_class=HTMLResponse)
def edit_format(
    *,
    request: Request,
    format_id: int,
    current_user: CurrentUser,
):
    if not current_user:
        return RedirectResponse("/")
    if current_user.role < 2:
        return RedirectResponse(f"/formats/{format_id}")
    with db_session:
        format = FormatController.get_format(format_id=format_id)
        return templates.TemplateResponse(
            "edit_format.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "format": format.to_model(),
            },
        )
