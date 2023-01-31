__all__ = ["FormatRead", "FormatWrite"]

from book_catalogue.schemas._base import BaseModel


class BaseFormat(BaseModel):
    name: str

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, BaseFormat):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, BaseFormat):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class FormatRead(BaseFormat):
    format_id: int


class FormatWrite(BaseFormat):
    pass
