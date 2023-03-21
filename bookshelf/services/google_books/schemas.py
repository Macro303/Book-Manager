__all__ = ["Book"]

from datetime import date, datetime
from decimal import Decimal

from pydantic import BaseModel, Extra, Field, validator

from bookshelf.isbn import to_isbn_13


def to_camel_case(value: str) -> str:
    temp = value.replace("_", " ").title().replace(" ", "")
    return temp[0].lower() + temp[1:]


class CamelModel(BaseModel):
    class Config:
        alias_generator = to_camel_case
        allow_population_by_field_name = True
        anystr_strip_whitespace = True
        extra = Extra.forbid


class Availability(CamelModel):
    is_available: bool
    acs_token_link: str | None = None


class AccessInfo(CamelModel):
    access_view_status: str
    country: str
    embeddable: bool
    epub: Availability
    pdf: Availability
    public_domain: bool
    quote_sharing_allowed: bool
    text_to_speech_permission: str
    viewability: str
    web_reader_link: str


class Layer(CamelModel):
    layer_id: str
    volume_annotations_version: str


class LayerInfo(CamelModel):
    layers: list[Layer]


class SaleInfo(CamelModel):
    country: str
    is_ebook: bool
    saleability: str


class SearchInfo(CamelModel):
    text_snippet: str


class Dimensions(CamelModel):
    height: str
    width: str | None = None
    thickness: str | None = None


class Identifier(CamelModel):
    identifier: str
    type: str


class ImageLinks(CamelModel):
    extra_large: str | None = None
    large: str | None = None
    medium: str | None = None
    small: str | None = None
    small_thumbnail: str
    thumbnail: str


class PanelizationSummary(CamelModel):
    contains_epub_bubbles: bool
    contains_image_bubbles: bool
    image_bubble_version: str | None = None


class ReadingModes(CamelModel):
    image: bool
    text: bool


class VolumeSeries(CamelModel):
    series_id: str
    series_book_type: str
    order_number: int


class SeriesInfo(CamelModel):
    kind: str
    book_display_number: str
    volume_series: list[VolumeSeries]


class VolumeInfo(CamelModel):
    allow_anon_logging: bool
    authors: list[str] = Field(default_factory=list)
    average_rating: Decimal | None = None
    canonical_volume_link: str
    categories: list[str] = Field(default_factory=list)
    comics_content: bool = False
    content_version: str
    description: str | None = None
    dimensions: Dimensions | None = None
    image_links: ImageLinks | None = None
    industry_identifiers: list[Identifier]
    info_link: str
    language: str
    maturity_rating: str
    page_count: int | None = None
    panelization_summary: PanelizationSummary | None = None
    preview_link: str
    print_type: str
    printed_page_count: int | None = None
    published_date: date
    publisher: str | None = None
    ratings_count: int | None = None
    reading_modes: ReadingModes
    series_info: SeriesInfo | None = None
    subtitle: str | None = None
    title: str

    @validator("published_date", pre=True)
    def validate_date(cls, v: str) -> date:
        try:
            return date.fromisoformat(v)
        except ValueError:
            try:
                return datetime.strptime(v, "%Y").date()
            except ValueError:
                return datetime.strptime(v, "%Y-%m").date()

    def get_isbn(self) -> str | None:
        if temp := next(iter([x for x in self.industry_identifiers if x.type == "ISBN_13"]), None):
            return to_isbn_13(temp.identifier)
        if temp := next(iter([x for x in self.industry_identifiers if x.type == "ISBN_10"]), None):
            return to_isbn_13(temp.identifier)
        return None


class Book(CamelModel):
    access_info: AccessInfo
    book_id: str = Field(alias="id")
    etag: str
    kind: str
    layer_info: LayerInfo | None = None
    sale_info: SaleInfo
    search_info: SearchInfo | None = None
    self_link: str
    volume_info: VolumeInfo
