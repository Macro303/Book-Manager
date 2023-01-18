__all__ = ["Book", "Identifiers"]

from pydantic import Field

from book_catalogue.schemas._author import Author
from book_catalogue.schemas._base import BaseModel
from book_catalogue.schemas._publisher import Publisher
from book_catalogue.schemas._series import Series
from book_catalogue.schemas._user import User


class Identifiers(BaseModel):
    book_id: int
    goodreads_id: str | None = None
    google_books_id: str | None = None
    isbn: str
    library_thing_id: str | None = None
    open_library_id: str | None = None


class Book(BaseModel):
    authors: list[Author] = Field(default_factory=list)
    description: str | None = None
    format: str | None = None
    identifiers: Identifiers
    image_url: str
    publisher: Publisher | None = None
    readers: list[User] = Field(default_factory=list)
    series: list[Series] = Field(default_factory=list)
    subtitle: str | None = None
    title: str
    wisher: User | None = None

    def get_first_series(self) -> Series | None:
        if temp := sorted(self.series):
            return temp[0]
        return None

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Book):
            raise NotImplementedError()
        self_first_series = self.get_first_series()
        other_first_series = other.get_first_series()
        if self_first_series and other_first_series and self_first_series != other_first_series:
            return self_first_series < other_first_series
        if self_first_series and not other_first_series:
            return False
        if not self_first_series and other_first_series:
            return True

        if self.title != other.title:
            return self.title < other.title
        return (self.subtitle or "") < (other.subtitle or "")

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Book):
            raise NotImplementedError()
        return (self.get_first_series(), self.title, (self.subtitle or "")) == (
            other.get_first_series(),
            other.title,
            (other.subtitle or ""),
        )

    def __hash__(self):
        return hash((type(self), self.get_first_series(), self.title, (self.subtitle or "")))
