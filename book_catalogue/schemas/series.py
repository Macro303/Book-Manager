__all__ = ["SeriesRead", "SeriesWrite"]

from book_catalogue.schemas._base import BaseModel


class BaseSeries(BaseModel):
    name: str

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, BaseSeries):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, BaseSeries):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash(
            (
                type(self),
                self.name,
            )
        )


class SeriesRead(BaseSeries):
    number: int | None = None
    series_id: int

    @property
    def display_name(self) -> str:
        output = self.name
        if self.number:
            output += f" (#{self.number})"
        return output

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, SeriesRead):
            raise NotImplementedError()
        if self.name != other.name:
            return self.name < other.name
        return (self.number or -1) < (other.number or -1)

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, SeriesRead):
            raise NotImplementedError()
        return (self.name, (self.number or -1)) == (other.name, (other.number or -1))

    def __hash__(self):
        return hash((type(self), self.name, (self.number or -1)))


class SeriesWrite(BaseSeries):
    pass
