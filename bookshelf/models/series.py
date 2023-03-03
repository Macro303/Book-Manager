__all__ = ["Series", "SeriesIn"]

from bookshelf.models._base import BaseModel


class BaseSeries(BaseModel):
    name: str


class Series(BaseSeries):
    number: int | None = None
    series_id: int

    @property
    def display_name(self) -> str:
        output = self.name
        if self.number:
            output += f" (#{self.number})"
        return output

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Series):
            raise NotImplementedError()
        if self.name != other.name:
            return self.name < other.name
        return (self.number or 0) < (other.number or 0)

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Series):
            raise NotImplementedError()
        return (self.name, self.number or 0) == (other.name, other.number or 0)

    def __hash__(self):
        return hash((type(self), self.name, self.number or 0))


class SeriesIn(BaseSeries):
    pass
