from __future__ import annotations

__all__ = ["NewUser", "User"]

from book_catalogue.schemas._base import BaseModel


class User(BaseModel):
    role: int
    user_id: int
    username: str

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, User):
            raise NotImplementedError()
        return self.username < other.username

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, User):
            raise NotImplementedError()
        return self.username == other.username

    def __hash__(self):
        return hash((type(self), self.username))


class NewUser(BaseModel):
    role: int = 0
    username: str
