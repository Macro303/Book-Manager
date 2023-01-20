__all__ = ["Author", "NewAuthor", "Role", "NewRole"]

from book_catalogue.schemas._base import BaseModel


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


class NewRole(BaseModel):
    name: str


class Author(BaseModel):
    author_id: int
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


class NewAuthor(BaseModel):
    name: str
    open_library_id: str | None = None
