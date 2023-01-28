from __future__ import annotations

__all__ = ["Author", "Identifiers", "NewAuthor", "NewRole", "Role"]

from book_catalogue.schemas._base import BaseModel


class Identifiers(BaseModel):
    goodreads_id: str | None = None
    library_thing_id: str | None = None
    open_library_id: str | None = None


class BaseAuthor(BaseModel):
    bio: str | None = None
    identifiers: Identifiers = Identifiers()
    image_url: str | None = None
    name: str

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, BaseAuthor):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, BaseAuthor):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class Role(BaseModel):
    name: str
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


class Author(BaseAuthor):
    author_id: int
    roles: list[Role]

    @property
    def display_name(self) -> str:
        return f"{self.name} ({', '.join(x.name for x in self.roles)})"


class NewRole(BaseModel):
    name: str


class NewAuthor(BaseAuthor):
    pass
