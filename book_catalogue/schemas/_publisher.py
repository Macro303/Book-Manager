from __future__ import annotations

__all__ = ["NewPublisher", "Publisher"]

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


class Publisher(BasePublisher):
    publisher_id: int


class NewPublisher(BasePublisher):
    pass
