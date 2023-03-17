__all__ = ["Identifiers", "Creator", "CreatorIn"]

from pydantic import Field

from bookshelf.models._base import BaseModel
from bookshelf.models.role import Role


class Identifiers(BaseModel):
    goodreads_id: str | None = None
    library_thing_id: str | None = None
    open_library_id: str | None = None


class BaseCreator(BaseModel):
    bio: str | None = None
    identifiers: Identifiers = Identifiers()
    image_url: str | None = None
    name: str


class Creator(BaseCreator):
    creator_id: int
    roles: list[Role] = Field(default_factory=list)

    @property
    def display_name(self) -> str:
        return f"{self.name} ({', '.join(x.name for x in self.roles)})"

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Creator):
            raise NotImplementedError
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Creator):
            raise NotImplementedError
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class CreatorIn(BaseCreator):
    role_ids: list[int] = Field(default_factory=list)
