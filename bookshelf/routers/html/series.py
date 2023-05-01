__all__ = ["router"]

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from bookshelf.controllers.book import BookController
from bookshelf.controllers.series import SeriesController
from bookshelf.routers.html.utils import CurrentUser, templates

router = APIRouter(prefix="/series", tags=["Series"])


@router.get(path="", response_class=HTMLResponse)
def list_series(
    *,
    request: Request,
    current_user: CurrentUser,
    name: str = "",
):
    if not current_user:
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
                "current_user": current_user.to_model(),
                "series_list": sorted({x.to_model() for x in series_list}),
                "selected": {"name": name},
            },
        )


@router.get(path="/{series_id}", response_class=HTMLResponse)
def view_series(
    *,
    request: Request,
    series_id: int,
    current_user: CurrentUser,
):
    if not current_user:
        return RedirectResponse("/")
    with db_session:
        series = SeriesController.get_series(series_id=series_id)
        all_books = {
            x for x in BookController.list_books() if series not in [y.series for y in x.series]
        }
        book_list = sorted(
            {(x.book.to_model(), x.number) for x in series.books},
            key=lambda x: (x[1] or 0, x[0].title, x[0].subtitle or ""),
        )
        return templates.TemplateResponse(
            "view_series.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "series": series.to_model(),
                "book_list": book_list,
                "all_books": sorted({x.to_model() for x in all_books}),
            },
        )


@router.get(path="/{series_id}/edit", response_class=HTMLResponse)
def edit_series(
    *,
    request: Request,
    series_id: int,
    current_user: CurrentUser,
):
    if not current_user:
        return RedirectResponse("/")
    if current_user.role < 2:
        return RedirectResponse(f"/series/{series_id}")
    with db_session:
        series = SeriesController.get_series(series_id=series_id)
        return templates.TemplateResponse(
            "edit_series.html",
            {
                "request": request,
                "current_user": current_user.to_model(),
                "series": series.to_model(),
            },
        )
