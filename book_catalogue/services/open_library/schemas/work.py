__all__ = ["Work"]

from pydantic import Field

from book_catalogue.services.open_library.schemas import (
    BaseModel,
    DatetimeResource,
    Link,
    Resource,
    TextResource,
)


class Author(BaseModel):
    author: Resource
    type: Resource

    @property
    def author_id(self) -> str:
        return self.author.key.split("/")[-1]


class Excerpt(BaseModel):
    excerpt: str | TextResource
    comment: str | None = None
    author: Resource | None = None
    page: str | None = None


class Work(BaseModel):
    authors: list[Author]
    covers: list[int] = Field(default_factory=list)
    created: DatetimeResource
    description: str | TextResource | None = None
    dewey_number: list[str] = Field(default_factory=list)
    excerpts: list[Excerpt] = Field(default_factory=list)
    first_publish_date: str | None = None
    first_sentence: TextResource | None = None
    key: str
    last_modified: DatetimeResource
    latest_revision: int
    lc_classifications: list[str] = Field(default_factory=list)
    links: list[Link] = Field(default_factory=list)
    location: str | None = None
    revision: int
    subject_people: list[str] = Field(default_factory=list)
    subject_places: list[str] = Field(default_factory=list)
    subject_times: list[str] = Field(default_factory=list)
    subjects: list[str] = Field(default_factory=list)
    subtitle: str | None = None
    title: str
    type: Resource

    @property
    def work_id(self) -> str:
        return self.key.split("/")[-1]

    @property
    def location_id(self) -> str:
        return self.location.split("/")[-1]

    def get_description(self) -> str | None:
        if self.description:
            if isinstance(self.description, TextResource):
                return self.description.value
            return self.description
        return None
