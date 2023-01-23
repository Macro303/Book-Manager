from __future__ import annotations
__all__ = ["Author"]

from pydantic import Field

from book_catalogue.services.open_library.schemas import (
    BaseModel,
    DatetimeResource,
    Resource,
    TextResource,
)


class Identifiers(BaseModel):
    amazon: str | None = None
    goodreads: str | None = None
    isni: str | None = None
    librarything: str | None = None
    viaf: str | None = None
    wikidata: str | None = None


class Link(BaseModel):
    title: str
    type: Resource
    url: str


class Author(BaseModel):
    alternate_names: list[str] = Field(default_factory=list)
    bio: str | TextResource | None = None
    birth_date: str | None = None
    created: DatetimeResource | None = None
    death_date: str | None = None
    entity_type: str | None = None
    id: int | None = None
    key: str
    last_modified: DatetimeResource
    latest_revision: int | None = None
    links: list[Link] = Field(default_factory=list)
    name: str
    personal_name: str | None = None
    photos: list[int] = Field(default_factory=list)
    remote_ids: Identifiers = Identifiers()
    revision: int
    source_records: list[str] = Field(default_factory=list)
    title: str | None = None
    type: Resource
    wikipedia: str | None = None

    @property
    def author_id(self) -> str:
        return self.key.split("/")[-1]
    
    def get_bio(self) -> str | None:
        if self.bio:
            if isinstance(self.bio, TextResource):
                return self.bio.value
            return self.bio
        return None
