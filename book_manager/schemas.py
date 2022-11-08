__all__ = ["User", "Identifiers", "Images", "Book"]

from pydantic import BaseModel, Field


class User(BaseModel):
    username: str


class Identifiers(BaseModel):
    open_library_id: str
    google_books_id: str | None = None
    goodreads_id: str | None = None
    library_thing_id: str | None = None


class Images(BaseModel):
    small: str | None = None
    medium: str | None = None
    large: str | None = None


class Book(BaseModel):
    isbn: str
    title: str
    authors: list[str] = Field(default_factory=list)
    format: str | None = None
    series: list[str] = Field(default_factory=list)
    publisher: str
    wisher: str | None = None
    readers: list[str] = Field(default_factory=list)
    identifiers: Identifiers
    images: Images = Images()

    def __lt__(self, other):
        if not isinstance(other, Book):
            raise NotImplementedError()

        self_author = sorted(self.authors)
        self_author = self_author[0] if self_author else None
        other_author = sorted(other.authors)
        other_author = other_author[0] if other_author else None
        if self_author != other_author:
            return self_author < other_author

        self_series = sorted(self.series)
        self_series = self_series[0] if self_series else None
        other_series = sorted(other.series)
        other_series = other_series[0] if other_series else None
        if self_series != other_series:
            return self_series < other_series

        return self.title < other.title