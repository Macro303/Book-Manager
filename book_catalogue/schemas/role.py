__all__ = ["RoleRead", "RoleWrite"]

from book_catalogue.schemas._base import BaseModel


class BaseRole(BaseModel):
    name: str

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, BaseRole):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, BaseRole):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class RoleRead(BaseRole):
    role_id: int


class RoleWrite(BaseRole):
    pass
