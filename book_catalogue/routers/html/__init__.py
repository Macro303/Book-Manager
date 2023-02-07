__all__ = ["html_router"]

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse

from book_catalogue.database.tables import User
from book_catalogue.routers.html._utils import get_token_user, templates
from book_catalogue.routers.html.author import router as author_router
from book_catalogue.routers.html.book import router as book_router
from book_catalogue.routers.html.format import router as format_router
from book_catalogue.routers.html.genre import router as genre_router
from book_catalogue.routers.html.publisher import router as publisher_router
from book_catalogue.routers.html.series import router as series_router
from book_catalogue.routers.html.user import router as user_router

html_router = APIRouter(include_in_schema=False)


@html_router.get("/", response_class=HTMLResponse)
def index(request: Request, token_user: User | None = Depends(get_token_user)):
    if token_user:
        return RedirectResponse(f"/users/{token_user.user_id}")
    return templates.TemplateResponse("index.html", {"request": request})


html_router.include_router(author_router)
html_router.include_router(book_router)
html_router.include_router(format_router)
html_router.include_router(genre_router)
html_router.include_router(publisher_router)
html_router.include_router(series_router)
html_router.include_router(user_router)
