__all__ = ["Format", "FormatIn"]

from bookshelf.models._base import BaseModel


class BaseFormat(BaseModel):
    name: str


class Format(BaseFormat):
    format_id: int

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Format):
            raise NotImplementedError
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Format):
            raise NotImplementedError
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class FormatIn(BaseFormat):
    pass
