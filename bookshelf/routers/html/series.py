__all__ = ["router"]

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from bookshelf.controllers.series import SeriesController
from bookshelf.database.tables import User
from bookshelf.routers.html.utils import get_token_user, templates

router = APIRouter(prefix="/series", tags=["Series"])


@router.get(path="", response_class=HTMLResponse)
def list_series(
    *,
    request: Request,
    token_user: User | None = Depends(get_token_user),
    name: str = "",
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        series_list = SeriesController.list_series()
        if name:
            series_list = [
                x
                for x in series_list
                if name.casefold() in x.name.casefold() or x.name.casefold() in name.casefold()
            ]
        return templates.TemplateResponse(
            "list_series.html",
            {
                "request": request,
                "token_user": token_user.to_model(),
                "series_list": sorted({x.to_model() for x in series_list}),
                "filters": {"name": name},
            },
        )


@router.get(path="/{series_id}", response_class=HTMLResponse)
def view_series(
    *,
    request: Request,
    series_id: int,
    token_user: User | None = Depends(get_token_user),
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        series = SeriesController.get_series(series_id=series_id)
        book_list = sorted(
            {(x.book.to_model(), x.number) for x in series.books},
            key=lambda x: (x[1] or 0, x[0].title, x[0].subtitle or ""),
        )
        return templates.TemplateResponse(
            "view_series.html",
            {
                "request": request,
                "token_user": token_user.to_model(),
                "series": series.to_model(),
                "book_list": book_list,
            },
        )


@router.get(path="/{series_id}/edit", response_class=HTMLResponse)
def edit_series(
    *,
    request: Request,
    series_id: int,
    token_user: User | None = Depends(get_token_user),
):
    if not token_user:
        return RedirectResponse("/")
    if token_user.role < 2:
        return RedirectResponse(f"/series/{series_id}")
    with db_session:
        series = SeriesController.get_series(series_id=series_id)
        return templates.TemplateResponse(
            "edit_series.html",
            {
                "request": request,
                "token_user": token_user.to_model(),
                "series": series.to_model(),
            },
        )
