__all__ = [
    "Identifiers",
    "BookRead",
    "BookWrite",
    "BookAuthorWrite",
    "BookSeriesWrite",
    "LookupBook",
]

from datetime import date

from pydantic import Field, validator

from book_catalogue.isbn import to_isbn_13
from book_catalogue.schemas._base import BaseModel
from book_catalogue.schemas.author import AuthorRead
from book_catalogue.schemas.format import FormatRead
from book_catalogue.schemas.genre import GenreRead
from book_catalogue.schemas.publisher import PublisherRead
from book_catalogue.schemas.series import SeriesRead
from book_catalogue.schemas.user import UserRead


class Identifiers(BaseModel):
    goodreads_id: str | None = None
    google_books_id: str | None = None
    isbn: str
    library_thing_id: str | None = None
    open_library_id: str | None = None

    @validator("isbn", pre=True)
    def validate_isbn(cls, v: str) -> str:
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

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, BaseBook):
            raise NotImplementedError()
        if self.title != other.title:
            return self.title < other.title
        return (self.subtitle or "") < (other.subtitle or "")

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, BaseBook):
            raise NotImplementedError()
        return (self.title, (self.subtitle or ""), self.identifiers.isbn) == (
            other.title,
            (other.subtitle or ""),
            other.identifiers.isbn,
        )

    def __hash__(self):
        return hash((type(self), self.title, (self.subtitle or ""), self.identifiers.isbn))


class BookRead(BaseBook):
    authors: list[AuthorRead] = Field(default_factory=list)
    book_id: int
    format: FormatRead | None = None
    genres: list[GenreRead] = Field(default_factory=list)
    publisher: PublisherRead | None = None
    readers: list[UserRead] = Field(default_factory=list)
    series: list[SeriesRead] = Field(default_factory=list)
    wishers: list[UserRead] = Field(default_factory=list)

    def get_first_series(self) -> SeriesRead | None:
        if temp := sorted(self.series):
            return temp[0]
        return None

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, BookRead):
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
        if not isinstance(other, BookRead):
            raise NotImplementedError()
        return (
            self.get_first_series(),
            self.title,
            (self.subtitle or ""),
            self.identifiers.isbn,
        ) == (other.get_first_series(), other.title, (other.subtitle or ""), other.identifiers.isbn)

    def __hash__(self):
        return hash((type(self), self.get_first_series(), self.title, (self.subtitle or "")))


class BookAuthorWrite(BaseModel):
    author_id: int
    role_ids: list[int] = Field(default_factory=list)


class BookSeriesWrite(BaseModel):
    series_id: int
    number: int | None = None


class BookWrite(BaseBook):
    authors: list[BookAuthorWrite] = Field(default_factory=list)
    format_id: int | None = None
    genre_ids: list[int] = Field(default_factory=list)
    publisher_id: int | None = None
    reader_ids: list[int] = Field(default_factory=list)
    series: list[BookSeriesWrite] = Field(default_factory=list)
    wisher_ids: list[int] = Field(default_factory=list)


class LookupBook(BaseModel):
    isbn: str
    wisher_id: int | None = None

    @validator("isbn", pre=True)
    def validate_isbn(cls, v: str) -> str:
        return to_isbn_13(value=v)
