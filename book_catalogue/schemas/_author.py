from __future__ import annotations

__all__ = ["Author", "Identifiers", "NewAuthor", "NewRole", "Role"]

from book_catalogue.schemas._base import BaseModel


class Identifiers(BaseModel):
    amazon_id: str | None = None
    author_id: int
    goodreads_id: str | None = None
    library_thing_id: str | None = None
    open_library_id: str | None = None


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


class Author(BaseModel):
    bio: str | None = None
    identifiers: Identifiers
    image_url: str | None = None
    name: str
    roles: list[Role]

    @property
    def display_name(self) -> str:
        return f"{self.name} ({', '.join(x.name for x in self.roles)})"

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Author):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Author):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class NewAuthorIdentifiers(BaseModel):
    amazon_id: str | None = None
    goodreads_id: str | None = None
    library_thing_id: str | None = None
    open_library_id: str | None = None


class NewRole(BaseModel):
    name: str


class NewAuthor(BaseModel):
    bio: str | None = None
    identifiers: NewAuthorIdentifiers = NewAuthorIdentifiers()
    image_url: str | None = None
    name: str
