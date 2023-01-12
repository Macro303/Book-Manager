__all__ = ["Author"]

from book_catalogue.services.open_library.schemas import BaseModel, DatetimeResource, Resource


class Author(BaseModel):
    alternate_names: list[str]
    created: DatetimeResource
    key: str
    last_modified: DatetimeResource
    latest_revision: int
    name: str
    photos: list[int]
    revision: int
    type: Resource

    @property
    def author_id(self) -> str:
        return self.key.split("/")[-1]
