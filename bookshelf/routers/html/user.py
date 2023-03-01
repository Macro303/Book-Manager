__all__ = ["router"]

from datetime import date

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from bookshelf.controllers.book import BookController
from bookshelf.controllers.creator import CreatorController
from bookshelf.controllers.format import FormatController
from bookshelf.controllers.publisher import PublisherController
from bookshelf.controllers.series import SeriesController
from bookshelf.controllers.user import UserController
from bookshelf.database.tables import User
from bookshelf.routers.html.utils import get_token_user, templates
from bookshelf.models.format import Format
from bookshelf.models.publisher import Publisher

router = APIRouter(prefix="/users", tags=["Users"])


@router.get(path="", response_class=HTMLResponse)
def list_users(
    *,
    request: Request,
    token_user: User | None = Depends(get_token_user),
    username: str = "",
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        user_list = UserController.list_users()
        if username:
            user_list = [
                x
                for x in user_list
                if username.casefold() in x.username.casefold()
                or x.username.casefold() in username.casefold()
            ]
        return templates.TemplateResponse(
            "list_users.html",
            {
                "request": request,
                "token_user": token_user.to_model(),
                "user_list": sorted({x.to_model() for x in user_list}),
                "filters": {"username": username},
            },
        )


@router.get(path="/{user_id}", response_class=HTMLResponse)
def view_user(*, request: Request, user_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        user = UserController.get_user(user_id=user_id)
        last_read = [
            x.book.to_model()
            for x in sorted(
                user.read_books,
                reverse=True,
                key=lambda x: (x.read_date or x.book.publish_date or date(2000, 1, 1), x.book),
            )[:5]
        ]
        return templates.TemplateResponse(
            "view_user.html",
            {"request": request, "token_user": token_user, "user": user, "last_read": last_read},
        )


@router.get(path="/{user_id}/edit", response_class=HTMLResponse)
def edit_user(*, request: Request, user_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        user = UserController.get_user(user_id=user_id)
        if token_user.user_id != user.user_id and (
            token_user.role < 4 or token_user.role <= user.role
        ):
            return RedirectResponse(f"/users/{user_id}")
        return templates.TemplateResponse(
            "edit_user.html",
            {
                "request": request,
                "token_user": token_user.to_model(),
                "user": user.to_model(),
            },
        )


@router.get(path="/{user_id}/wishlist", response_class=HTMLResponse)
def user_wishlist(
    *,
    request: Request,
    user_id: int,
    token_user: User | None = Depends(get_token_user),
    creator_id: int = 0,
    format_id: int = 0,
    publisher_id: int = 0,
    series_id: int = 0,
    title: str = "",
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        user = UserController.get_user(user_id=user_id)
        all_wishlist = wishlist = {
            *user.wished_books,
            *[x for x in BookController.list_books() if not x.is_collected and not x.wishers],
        }
        if creator_id:
            if creator_id == -1:
                wishlist = [x for x in wishlist if not x.creators]
            else:
                creator = CreatorController.get_creator(creator_id=creator_id)
                wishlist = [x for x in wishlist if creator in [y.creator for y in x.creators]]
        if format_id:
            if format_id == -1:
                wishlist = [x for x in wishlist if not x.format]
            else:
                format = FormatController.get_format(format_id=format_id)
                wishlist = [x for x in wishlist if x.format == format]
        if publisher_id:
            if publisher_id == -1:
                wishlist = [x for x in wishlist if not x.publisher]
            else:
                publisher = PublisherController.get_publisher(publisher_id=publisher_id)
                wishlist = [x for x in wishlist if publisher == x.publisher]
        if series_id:
            if series_id == -1:
                wishlist = [x for x in wishlist if not x.series]
            else:
                series = SeriesController.get_series(series_id=series_id)
                wishlist = [x for x in wishlist if series in [y.series for y in x.series]]
        if title:
            wishlist = [
                x
                for x in wishlist
                if (title.casefold() in x.title.casefold())
                or (x.title.casefold() in title.casefold())
                or (
                    x.subtitle
                    and (
                        (title.casefold() in x.subtitle.casefold())
                        or (x.subtitle.casefold() in title.casefold())
                    )
                )
            ]
        return templates.TemplateResponse(
            "user_wishlist.html",
            {
                "request": request,
                "token_user": token_user.to_model(),
                "user": user.to_model(),
                "wishlist": sorted({x.to_model() for x in wishlist}),
                "creator_list": sorted(
                    {y.creator.to_model() for x in all_wishlist for y in x.creators}
                ),
                "format_list": sorted(
                    {
                        x.format.to_model() if x.format else Format(format_id=-1, name="None")
                        for x in all_wishlist
                    }
                ),
                "publisher_list": sorted(
                    {
                        x.publisher.to_model()
                        if x.publisher
                        else Publisher(publisher_id=-1, name="None")
                        for x in all_wishlist
                    }
                ),
                "series_list": sorted(
                    {y.series.to_model() for x in all_wishlist for y in x.series}
                ),
                "filters": {
                    "creator_id": creator_id,
                    "format_id": format_id,
                    "publisher_id": publisher_id,
                    "series_id": series_id,
                    "title": title,
                },
            },
        )
