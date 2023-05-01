__all__ = [
    "Identifiers",
    "Book",
    "BookIn",
    "BookCreatorIn",
    "BookSeriesIn",
    "ImportBook",
]

from datetime import date

from pydantic import Field, validator

from bookshelf.isbn import to_isbn_13
from bookshelf.models._base import BaseModel
from bookshelf.models.creator import Creator
from bookshelf.models.format import Format
from bookshelf.models.genre import Genre
from bookshelf.models.publisher import Publisher
from bookshelf.models.series import Series
from bookshelf.models.user import User


class Identifiers(BaseModel):
    goodreads_id: str | None = None
    google_books_id: str | None = None
    isbn: str | None = None
    library_thing_id: str | None = None
    open_library_id: str | None = None

    @validator("isbn", pre=True)
    def validate_isbn(cls, v: str) -> str | None:
        return to_isbn_13(value=v)


class BaseBook(BaseModel):
    description: str | None = None
    identifiers: Identifiers
    image_url: str
    is_collected: bool = False
    publish_date: date | None = None
    subtitle: str | None = None
    title: str

    @property
    def publish_date_str(self) -> str:
        def suffix(day: int) -> str:
            date_suffix = ["th", "st", "nd", "rd"]
            if day % 10 in [1, 2, 3] and day not in [11, 12, 13]:
                return date_suffix[day % 10]
            return date_suffix[0]

        return self.publish_date.strftime(f"%-d{suffix(self.publish_date.day)} %b %Y")


class Book(BaseBook):
    creators: list[Creator] = Field(default_factory=list)
    book_id: int
    format: Format | None = None
    genres: list[Genre] = Field(default_factory=list)
    publisher: Publisher | None = None
    readers: list[User] = Field(default_factory=list)
    series: list[Series] = Field(default_factory=list)
    wishers: list[User] = Field(default_factory=list)

    @property
    def is_available(self) -> bool:
        return (self.publish_date <= date.today()) if self.publish_date else False

    def get_first_series(self) -> Series | None:
        if temp := sorted({x for x in self.series if not x.is_reading_order}):
            return temp[0]
        if temp := sorted(self.series):
            return temp[0]
        return None

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Book):
            raise NotImplementedError
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
            raise NotImplementedError
        if self.get_first_series() and other.get_first_series():
            return (
                self.get_first_series(),
                self.title,
                (self.subtitle or ""),
                self.identifiers.isbn,
            ) == (
                other.get_first_series(),
                other.title,
                (other.subtitle or ""),
                other.identifiers.isbn,
            )
        if not self.get_first_series() and not other.get_first_series():
            return (self.title, (self.subtitle or ""), self.identifiers.isbn) == (
                other.title,
                (other.subtitle or ""),
                other.identifiers.isbn,
            )
        return False

    def __hash__(self):
        return hash((type(self), self.get_first_series(), self.title, (self.subtitle or "")))


class BookCreatorIn(BaseModel):
    creator_id: int
    role_ids: list[int] = Field(default_factory=list)


class BookSeriesIn(BaseModel):
    series_id: int
    number: int | None = None


class BookIn(BaseBook):
    creators: list[BookCreatorIn] = Field(default_factory=list)
    format_id: int | None = None
    genre_ids: list[int] = Field(default_factory=list)
    publisher_id: int | None = None
    reader_ids: list[int] = Field(default_factory=list)
    series: list[BookSeriesIn] = Field(default_factory=list)
    wisher_ids: list[int] = Field(default_factory=list)


class ImportBook(BaseModel):
    collect: bool = False
    edition_id: str | None = None
    isbn: str | None = None
    wisher_id: int | None = None

    @validator("isbn", pre=True)
    def validate_isbn(cls, v: str | None) -> str | None:
        return to_isbn_13(value=v)
