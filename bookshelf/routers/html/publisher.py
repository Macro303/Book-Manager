__all__ = ["router"]

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from bookshelf.controllers.publisher import PublisherController
from bookshelf.routers.html.utils import CurrentUser, templates

router = APIRouter(prefix="/publishers", tags=["Publishers"])


@router.get(path="", response_class=HTMLResponse)
def list_publishers(
    *,
    request: Request,
    current_user: CurrentUser,
    name: str = "",
):
    if not current_user:
        return RedirectResponse("/")
    with db_session:
        publisher_list = PublisherController.list_publishers()
        if name:
            publisher_list = [
                x
                for x in publisher_list
                if name.casefold() in x.name.casefold() or x.name.casefold() in name.casefold()
            ]
        return templates.TemplateResponse(
            "list_publishers.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "publisher_list": sorted({x.to_model() for x in publisher_list}),
                "filters": {"name": name},
            },
        )


@router.get(path="/{publisher_id}", response_class=HTMLResponse)
def view_publisher(
    *,
    request: Request,
    publisher_id: int,
    current_user: CurrentUser,
):
    if not current_user:
        return RedirectResponse("/")
    with db_session:
        publisher = PublisherController.get_publisher(publisher_id=publisher_id)
        book_list = sorted({x.to_model() for x in publisher.books})
        return templates.TemplateResponse(
            "view_publisher.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "publisher": publisher.to_model(),
                "book_list": book_list,
            },
        )


@router.get(path="/{publisher_id}/edit", response_class=HTMLResponse)
def edit_publisher(
    *,
    request: Request,
    publisher_id: int,
    current_user: CurrentUser,
):
    if not current_user:
        return RedirectResponse("/")
    if current_user.role < 2:
        return RedirectResponse(f"/publishers/{publisher_id}")
    with db_session:
        publisher = PublisherController.get_publisher(publisher_id=publisher_id)
        return templates.TemplateResponse(
            "edit_publisher.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "publisher": publisher.to_model(),
            },
        )
