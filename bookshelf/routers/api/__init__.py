__all__ = ["api_router"]

from fastapi import APIRouter
from pony.orm import db_session

from bookshelf import __version__
from bookshelf.controllers.creator import CreatorController
from bookshelf.controllers.format import FormatController
from bookshelf.controllers.genre import GenreController
from bookshelf.controllers.publisher import PublisherController
from bookshelf.controllers.role import RoleController
from bookshelf.controllers.series import SeriesController
from bookshelf.responses import ErrorResponse
from bookshelf.routers.api.book import router as book_router
from bookshelf.routers.api.creator import router as creator_router
from bookshelf.routers.api.format import router as format_router
from bookshelf.routers.api.genre import router as genre_router
from bookshelf.routers.api.publisher import router as publisher_router
from bookshelf.routers.api.role import router as role_router
from bookshelf.routers.api.series import router as series_router
from bookshelf.routers.api.user import router as user_router

api_router = APIRouter(
    prefix=f"/api/v{__version__.split('.')[0]}",
    responses={
        422: {"description": "Validation error", "model": ErrorResponse},
    },
)
api_router.include_router(creator_router)
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
        for _creator in sorted(CreatorController.list_creators(), key=lambda x: x.name):
            if not _creator.books:
                results.append(("Creator", _creator.creator_id, _creator.name))
        for _format in sorted(FormatController.list_formats(), key=lambda x: x.name):
            if not _format.books:
                results.append(("Format", _format.format_id, _format.name))
        for _genre in sorted(GenreController.list_genres(), key=lambda x: x.name):
            if not _genre.books:
                results.append(("Genre", _genre.genre_id, _genre.name))
        for _publisher in sorted(PublisherController.list_publishers(), key=lambda x: x.name):
            if not _publisher.books:
                results.append(("Publisher", _publisher.publisher_id, _publisher.name))
        for _role in sorted(RoleController.list_roles(), key=lambda x: x.name):
            if not _role.creators:
                results.append(("Role", _role.role_id, _role.name))
        for _series in sorted(SeriesController.list_series(), key=lambda x: x.name):
            if not _series.books:
                results.append(("Series", _series.series_id, _series.name))
        return results
