__all__ = ["api_router"]

from fastapi import APIRouter
from pony.orm import db_session

from book_catalogue import __version__
from book_catalogue.controllers.author import AuthorController
from book_catalogue.controllers.format import FormatController
from book_catalogue.controllers.genre import GenreController
from book_catalogue.controllers.publisher import PublisherController
from book_catalogue.controllers.role import RoleController
from book_catalogue.controllers.series import SeriesController
from book_catalogue.responses import ErrorResponse
from book_catalogue.routers.api.author import router as author_router
from book_catalogue.routers.api.book import router as book_router
from book_catalogue.routers.api.format import router as format_router
from book_catalogue.routers.api.genre import router as genre_router
from book_catalogue.routers.api.publisher import router as publisher_router
from book_catalogue.routers.api.role import router as role_router
from book_catalogue.routers.api.series import router as series_router
from book_catalogue.routers.api.user import router as user_router

api_router = APIRouter(
    prefix=f"/api/v{__version__.split('.')[0]}",
    responses={
        422: {"description": "Validation error", "model": ErrorResponse},
    },
)
api_router.include_router(author_router)
api_router.include_router(book_router)
api_router.include_router(format_router)
api_router.include_router(genre_router)
api_router.include_router(publisher_router)
api_router.include_router(role_router)
api_router.include_router(series_router)
api_router.include_router(user_router)


@api_router.get(path="/empty", include_in_schema=False)
def list_empty_entries() -> list[tuple[str, int, str]]:
    results = []
    with db_session:
        for _author in sorted(AuthorController.list_authors(), key=lambda x: x.name):
            if not _author.books:
                results.append(("Author", _author.author_id, _author.name))
        for _role in sorted(RoleController.list_roles(), key=lambda x: x.name):
            if not _role.authors:
                results.append(("Role", _role.role_id, _role.name))
        for _format in sorted(FormatController.list_formats(), key=lambda x: x.name):
            if not _format.books:
                results.append(("Format", _format.format_id, _format.name))
        for _genre in sorted(GenreController.list_genres(), key=lambda x: x.name):
            if not _genre.books:
                results.append(("Genre", _genre.genre_id, _genre.name))
        for _publisher in sorted(PublisherController.list_publishers(), key=lambda x: x.name):
            if not _publisher.books:
                results.append(("Publisher", _publisher.publisher_id, _publisher.name))
        for _series in sorted(SeriesController.list_series(), key=lambda x: x.name):
            if not _series.books:
                results.append(("Series", _series.series_id, _series.name))
        return results
