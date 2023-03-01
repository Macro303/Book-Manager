__all__ = ["Genre", "GenreIn"]

from bookshelf.models._base import BaseModel


class BaseGenre(BaseModel):
    name: str


class Genre(BaseGenre):
    genre_id: int

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Genre):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Genre):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class GenreIn(BaseGenre):
    pass
