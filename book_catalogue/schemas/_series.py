__all__ = ["Series", "CreateSeries"]

from book_catalogue.schemas._base import BaseModel


class Series(BaseModel):
    number: int | None = None
    series_id: int
    title: str

    @property
    def display_name(self) -> str:
        output = self.title
        if self.number:
            output += f" (#{self.number})"
        return output

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Series):
            raise NotImplementedError()
        if self.title != other.title:
            return self.title < other.title
        return (self.number or -1) < (other.number or -1)

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Series):
            raise NotImplementedError()
        return (self.title, (self.number or -1)) == (other.title, (other.title or -1))

    def __hash__(self):
        return hash((type(self), self.title, (self.number or -1)))


class CreateSeries(BaseModel):
    title: str
