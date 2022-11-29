from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from natsort import humansorted as sorted
from natsort import ns
from sqlalchemy.orm import Session

from book_catalogue import controller, get_project_root
from book_catalogue.database import SessionLocal

router = APIRouter(prefix="/book-catalogue", tags=["WebInterface"], include_in_schema=False)
templates = Jinja2Templates(directory=get_project_root() / "templates")


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@router.get("", response_class=HTMLResponse)
def index(request: Request):
    return templates.TemplateResponse("index.html", {"request": request})


@router.get("/collection", response_class=HTMLResponse)
def collection(username: str, request: Request, db: Session = Depends(get_db)):
    db_book_list = controller.list_books(db)
    return templates.TemplateResponse(
        "collection.html",
        {
            "username": username,
            "books": sorted(
                {x.to_schema() for x in db_book_list if not x.wisher}, alg=ns.NA | ns.G
            ),
            "request": request,
        },
    )


@router.get("/wishlist", response_class=HTMLResponse)
def wishlist(username: str, request: Request, db: Session = Depends(get_db)):
    db_book_list = controller.list_books(db)
    return templates.TemplateResponse(
        "wishlist.html",
        {
            "username": username,
            "books": sorted({x.to_schema() for x in db_book_list if x.wisher}, alg=ns.NA | ns.G),
            "request": request,
        },
    )
