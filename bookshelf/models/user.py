__all__ = ["User", "UserIn"]

from bookshelf.models._base import BaseModel


class BaseUser(BaseModel):
    image_url: str | None = None
    role: int = 0
    username: str


class User(BaseUser):
    user_id: int

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, User):
            raise NotImplementedError
        return self.username < other.username

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, User):
            raise NotImplementedError
        return self.username == other.username

    def __hash__(self):
        return hash((type(self), self.username))


class UserIn(BaseUser):
    pass
