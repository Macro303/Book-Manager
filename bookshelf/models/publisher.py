__all__ = ["Publisher", "PublisherIn"]

from bookshelf.models._base import BaseModel


class BasePublisher(BaseModel):
    name: str


class Publisher(BasePublisher):
    publisher_id: int

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Publisher):
            raise NotImplementedError
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Publisher):
            raise NotImplementedError
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class PublisherIn(BasePublisher):
    pass
