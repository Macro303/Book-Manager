__all__ = ["GenreRead", "GenreWrite"]

from book_catalogue.schemas._base import BaseModel


class BaseGenre(BaseModel):
    name: str

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, BaseGenre):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, BaseGenre):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class GenreRead(BaseGenre):
    genre_id: int


class GenreWrite(BaseGenre):
    pass
