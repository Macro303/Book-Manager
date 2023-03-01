__all__ = ["Role", "RoleIn"]

from bookshelf.models._base import BaseModel


class BaseRole(BaseModel):
    name: str


class Role(BaseRole):
    role_id: int

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Role):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Role):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class RoleIn(BaseRole):
    pass
