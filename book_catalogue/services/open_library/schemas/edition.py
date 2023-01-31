from __future__ import annotations

__all__ = ["Edition"]

from datetime import date, datetime

from pydantic import Field

from book_catalogue.console import CONSOLE
from book_catalogue.services.open_library.schemas import (
    BaseModel,
    DatetimeResource,
    Link,
    Resource,
    TextResource,
)


class Contributor(BaseModel):
    name: str
    role: str


class Content(BaseModel):
    label: str | None = None
    level: int
    pagenum: str | None
    title: str
    type: Resource | None = None


class Identifiers(BaseModel):
    amazon: list[str] = Field(default_factory=list)
    better_world_books: list[str] = Field(default_factory=list)
    goodreads: list[str] = Field(default_factory=list)
    google: list[str] = Field(default_factory=list)
    librarything: list[str] = Field(default_factory=list)
    wikidata: list[str] = Field(default_factory=list)


class Edition(BaseModel):
    authors: list[Resource] = Field(default_factory=list)
    by_statement: str | None = None
    classifications: dict[int, int] = Field(default_factory=dict)
    contributions: list[str] = Field(default_factory=list)
    contributors: list[Contributor] = Field(default_factory=list)
    copyright_date: str | None = None
    covers: list[int] = Field(default_factory=list)
    created: DatetimeResource
    description: str | TextResource | None = None
    dewey_decimal_class: list[str] = Field(default_factory=list)
    edition_name: str | None = None
    first_sentence: str | TextResource | None = None
    full_title: str | None = None
    genres: list[str] = Field(default_factory=list)
    ia_box_id: list[str] = Field(default_factory=list)
    ia_loaded_id: list[str] = Field(default_factory=list)
    identifiers: Identifiers = Identifiers()
    isbn_10: list[str] = Field(default_factory=list)
    isbn_13: list[str] = Field(default_factory=list)
    key: str
    languages: list[Resource] = Field(default_factory=list)
    last_modified: DatetimeResource
    latest_revision: int
    lc_classifications: list[str] = Field(default_factory=list)
    lccn: list[str] = Field(default_factory=list)
    links: list[Link] = Field(default_factory=list)
    local_id: list[str] = Field(default_factory=list)
    location: list[str] = Field(default_factory=list)
    notes: str | TextResource | None = None
    number_of_pages: int | None = None
    ocaid: str | None = None
    oclc_numbers: list[str] = Field(default_factory=list)
    oclc_number: list[str] = Field(default_factory=list)
    other_titles: list[str] = Field(default_factory=list)
    pagination: str | None = None
    physical_dimensions: str | None = None
    physical_format: str | None = None
    publish_country: str | None = None
    publish_date: str | None = None
    publish_places: list[str] = Field(default_factory=list)
    publishers: list[str]
    revision: int
    series: list[str] = Field(default_factory=list)
    source_records: list[str] = Field(default_factory=list)
    subject_people: list[str] = Field(default_factory=list)
    subject_place: list[str] = Field(default_factory=list)
    subject_places: list[str] = Field(default_factory=list)
    subject_time: list[str] = Field(default_factory=list)
    subjects: list[str] = Field(default_factory=list)
    subtitle: str | None = None
    table_of_contents: list[Content] = Field(default_factory=list)
    title: str
    translated_from: list[Resource] = Field(default_factory=list)
    translation_of: str | None = None
    type: Resource
    weight: str | None = None
    work_title: list[str] = Field(default_factory=list)
    work_titles: list[str] = Field(default_factory=list)
    works: list[Resource]

    @property
    def edition_id(self) -> str:
        return self.key.split("/")[-1]

    def get_description(self) -> str | None:
        if self.description:
            if isinstance(self.description, TextResource):
                return self.description.value
            return self.description
        return None

    def get_publish_date(self) -> date | None:
        if not self.publish_date:
            return None
        try:
            return date.fromisoformat(self.publish_date)
        except ValueError as iso_err:
            for _format in [
                "%Y",
                "%b, %Y",
                "%Y-%b-%d",
                "%b %d, %Y",
                "%B %d, %Y",
                "%B %Y",
                "%d %b %Y",
            ]:
                try:
                    return datetime.strptime(self.publish_date, _format).date()
                except ValueError:
                    pass
            CONSOLE.print_exception(theme="ansi_dark")
            raise iso_err
