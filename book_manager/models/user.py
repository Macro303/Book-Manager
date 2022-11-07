__all__ = ["User"]

from pydantic import BaseModel


class User(BaseModel):
    username: str
