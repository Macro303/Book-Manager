__all__ = ["router"]

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from book_catalogue.controllers.publisher import PublisherController
from book_catalogue.database.tables import User
from book_catalogue.routers.html._utils import get_token_user, templates

router = APIRouter(prefix="/publishers", tags=["Publishers"])


@router.get(path="", response_class=HTMLResponse)
def list_publishers(
    *,
    request: Request,
    token_user: User | None = Depends(get_token_user),
    name: str = "",
):
    if not token_user:
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
                "token_user": token_user.to_schema(),
                "publisher_list": sorted({x.to_schema() for x in publisher_list}),
                "filters": {"name": name},
            },
        )


@router.get(path="/{publisher_id}", response_class=HTMLResponse)
def view_publisher(
    *, request: Request, publisher_id: int, token_user: User | None = Depends(get_token_user)
):
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
            },
        )


@router.get(path="/{publisher_id}/edit", response_class=HTMLResponse)
def edit_publisher(
    *, request: Request, publisher_id: int, token_user: User | None = Depends(get_token_user)
):
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
            },
        )
