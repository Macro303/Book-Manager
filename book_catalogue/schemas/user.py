__all__ = ["UserRead", "UserWrite"]

from book_catalogue.schemas._base import BaseModel


class BaseUser(BaseModel):
    image_url: str | None = None
    role: int = 0
    username: str

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, BaseUser):
            raise NotImplementedError()
        return self.username < other.username

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, BaseUser):
            raise NotImplementedError()
        return self.username == other.username

    def __hash__(self):
        return hash((type(self), self.username))


class UserRead(BaseUser):
    user_id: int


class UserWrite(BaseUser):
    pass
