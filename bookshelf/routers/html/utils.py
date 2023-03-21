__all__ = ["templates", "CurrentUser"]

from typing import Annotated

from fastapi import Cookie, Depends
from fastapi.templating import Jinja2Templates
from pony.orm import db_session

from bookshelf import get_project_root
from bookshelf.database.tables import User

templates = Jinja2Templates(directory=get_project_root() / "templates")


def get_token_user(token: int | None = Cookie(default=None)) -> User | None:
    if not token:
        return None
    with db_session:
        return User.get(user_id=token)


CurrentUser = Annotated[User | None, Depends(get_token_user)]
