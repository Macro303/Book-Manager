__all__ = ["PublisherRead", "PublisherWrite"]

from book_catalogue.schemas._base import BaseModel


class BasePublisher(BaseModel):
    name: str

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, BasePublisher):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, BasePublisher):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class PublisherRead(BasePublisher):
    publisher_id: int


class PublisherWrite(BasePublisher):
    pass
