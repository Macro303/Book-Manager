__all__ = ["templates", "get_token_user"]

from fastapi import Cookie
from fastapi.templating import Jinja2Templates
from pony.orm import db_session

from book_catalogue import get_project_root
from book_catalogue.database.tables import User
from book_catalogue.controllers.user import UserController


templates = Jinja2Templates(directory=get_project_root() / "templates")


def get_token_user(token: int | None = Cookie(default=None)) -> User | None:
    if not token:
        return None
    with db_session:
        return UserController.get_user(user_id=token)
