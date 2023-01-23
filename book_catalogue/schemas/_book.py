from __future__ import annotations
__all__ = ["Book", "NewBook"]

from pydantic import Field, validator

from book_catalogue.schemas._author import Author
from book_catalogue.schemas._base import BaseModel
from book_catalogue.schemas._publisher import Publisher
from book_catalogue.schemas._series import Series
from book_catalogue.schemas._user import User
from book_catalogue.isbn import to_isbn_13


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
    wishers: list[User] = Field(default_factory=list)

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


class NewBookAuthor(BaseModel):
    author_id: int
    role_ids: list[int] = Field(default_factory=list)


class NewBookIdentifiers(BaseModel):
    goodreads_id: str | None = None
    google_books_id: str | None = None
    isbn: str
    library_thing_id: str | None = None
    open_library_id: str | None = None
    
    @validator("isbn", pre=True)
    def validate_isbn(cls, v: str) -> str:
        return to_isbn_13(value=v)


class NewBookSeries(BaseModel):
    series_id: int
    number: int | None = None


class NewBook(BaseModel):
    authors: list[NewBookAuthor] = Field(default_factory=list)
    description: str | None = None
    format: str | None = None
    identifiers: NewBookIdentifiers
    image_url: str
    publisher_id: int | None = None
    reader_ids: list[int] = Field(default_factory=list)
    series: list[NewBookSeries] = Field(default_factory=list)
    subtitle: str | None = None
    title: str
    wisher_ids: list[int] = Field(default_factory=list)


class LookupBook(BaseModel):
    isbn: str
    wisher_id: int | None = None
    
    @validator("isbn", pre=True)
    def validate_isbn(cls, v: str) -> str:
        return to_isbn_13(value=v)
