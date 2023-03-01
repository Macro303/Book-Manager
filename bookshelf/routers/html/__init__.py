__all__ = ["html_router"]

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse

from bookshelf.database.tables import User
from bookshelf.routers.html.book import router as book_router
from bookshelf.routers.html.creator import router as creator_router
from bookshelf.routers.html.format import router as format_router
from bookshelf.routers.html.genre import router as genre_router
from bookshelf.routers.html.publisher import router as publisher_router
from bookshelf.routers.html.role import router as role_router
from bookshelf.routers.html.series import router as series_router
from bookshelf.routers.html.user import router as user_router
from bookshelf.routers.html.utils import get_token_user, templates

html_router = APIRouter(include_in_schema=False)


@html_router.get("/", response_class=HTMLResponse)
def index(request: Request, token_user: User | None = Depends(get_token_user)):
    if token_user:
        return RedirectResponse(f"/users/{token_user.user_id}")
    return templates.TemplateResponse("index.html", {"request": request})


html_router.include_router(creator_router)
html_router.include_router(book_router)
html_router.include_router(format_router)
html_router.include_router(genre_router)
html_router.include_router(publisher_router)
html_router.include_router(role_router)
html_router.include_router(series_router)
html_router.include_router(user_router)
