__all__ = ["User", "Identifiers", "Images", "Book"]

from natsort import humansorted as sorted
from natsort import ns
from pydantic import BaseModel, Field


class User(BaseModel):
    username: str

    def __lt__(self, other):
        if not isinstance(other, User):
            raise NotImplementedError()
        return self.username < other.username

    def __eq__(self, other):
        if not isinstance(other, User):
            raise NotImplementedError()
        return self.username == other.username

    def __hash__(self):
        return hash((type(self), self.username))


class Identifiers(BaseModel):
    open_library_id: str
    google_books_id: str | None = None
    goodreads_id: str | None = None
    library_thing_id: str | None = None

    def __eq__(self, other):
        if not isinstance(other, Identifiers):
            raise NotImplementedError()
        return self.open_library_id == other.open_library_id

    def __hash__(self):
        return hash((type(self), self.open_library_id))


class Images(BaseModel):
    small: str | None = None
    medium: str | None = None
    large: str | None = None

    def __eq__(self, other):
        if not isinstance(other, Images):
            raise NotImplementedError()
        return (self.small, self.medium, self.large) == (other.small, other.medium, other.large)

    def __hash__(self):
        return hash((type(self), self.small, self.medium, self.large))


class Book(BaseModel):
    isbn: str
    title: str
    subtitle: str | None = None
    authors: list[str] = Field(default_factory=list)
    format: str | None = None
    series: list[str] = Field(default_factory=list)
    publisher: str | None = None
    wisher: str | None = None
    readers: list[str] = Field(default_factory=list)
    identifiers: Identifiers
    images: Images = Images()

    @property
    def first_series(self) -> str | None:
        temp = sorted(self.series, alg=ns.NA | ns.G)
        return temp[0] if temp else None

    @property
    def first_author(self) -> str | None:
        temp = sorted(self.authors, alg=ns.NA | ns.G)
        return temp[0] if temp else None

    def __lt__(self, other):
        if not isinstance(other, Book):
            raise NotImplementedError()
        if (self.first_series or "") != (other.first_series or ""):
            return (self.first_series or "") < (other.first_series or "")
        if self.title != other.title:
            return self.title < other.title
        if (self.subtitle or "") != (other.subtitle or ""):
            return self.subtitle < other.subtitle
        return (self.first_author or "") < (other.first_author or "")

    def __eq__(self, other):
        if not isinstance(other, Book):
            raise NotImplementedError()
        return (
            (self.first_series or ""),
            self.title,
            (self.subtitle or ""),
            (self.first_author or ""),
        ) == (
            (other.first_series or ""),
            other.title,
            (other.subtitle or ""),
            (other.first_author or ""),
        )

    def __hash__(self):
        return hash(
            (
                type(self),
                (self.first_series or ""),
                self.title,
                (self.subtitle or ""),
                (self.first_author or ""),
            )
        )
