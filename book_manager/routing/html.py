from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from pony.orm import db_session

from book_manager import get_project_root
from book_manager.database.tables import BookTable

router = APIRouter(prefix="/Book-Manager", tags=["WebInterface"], include_in_schema=False)
templates = Jinja2Templates(directory=get_project_root() / "templates")


@router.get("", response_class=HTMLResponse)
def index(request: Request):
    return templates.TemplateResponse("index.html", {"request": request})


@router.get("/collection", response_class=HTMLResponse)
def collection(username: str, request: Request):
    with db_session:
        return templates.TemplateResponse(
            "collection.html",
            {
                "username": username,
                "books": sorted(x.to_model() for x in BookTable.select(lambda x: not x.wished)[:]),
                "request": request,
            },
        )


@router.get("/wishlist", response_class=HTMLResponse)
def wishlist(username: str, request: Request):
    with db_session:
        return templates.TemplateResponse(
            "wishlist.html",
            {
                "username": username,
                "books": sorted(x.to_model() for x in BookTable.select(lambda x: x.wished)[:]),
                "request": request,
            },
        )
