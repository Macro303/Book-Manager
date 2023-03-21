__all__ = ["html_router"]

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from bookshelf.controllers.user import UserController
from bookshelf.routers.html.book import router as book_router
from bookshelf.routers.html.creator import router as creator_router
from bookshelf.routers.html.format import router as format_router
from bookshelf.routers.html.genre import router as genre_router
from bookshelf.routers.html.publisher import router as publisher_router
from bookshelf.routers.html.role import router as role_router
from bookshelf.routers.html.series import router as series_router
from bookshelf.routers.html.user import router as user_router
from bookshelf.routers.html.utils import CurrentUser, templates

html_router = APIRouter(include_in_schema=False)


@html_router.get("/", response_class=HTMLResponse)
def index(request: Request, current_user: CurrentUser):
    if current_user:
        return RedirectResponse(f"/users/{current_user.user_id}")
    with db_session:
        user_list = UserController.list_users()
        return templates.TemplateResponse(
            "index.html",
            {"request": request, "fresh": len(user_list) == 0},
        )


html_router.include_router(creator_router)
html_router.include_router(book_router)
html_router.include_router(format_router)
html_router.include_router(genre_router)
html_router.include_router(publisher_router)
html_router.include_router(role_router)
html_router.include_router(series_router)
html_router.include_router(user_router)
