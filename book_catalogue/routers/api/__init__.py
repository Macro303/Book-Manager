from __future__ import annotations

__all__ = ["api_router"]

from fastapi import APIRouter

from book_catalogue import __version__
from book_catalogue.responses import ErrorResponse
from book_catalogue.routers.api._author import router as author_router
from book_catalogue.routers.api._book import router as book_router
from book_catalogue.routers.api._format import router as format_router
from book_catalogue.routers.api._publisher import router as publisher_router
from book_catalogue.routers.api._series import router as series_router
from book_catalogue.routers.api._user import router as user_router

api_router = APIRouter(
    prefix=f"/api/v{__version__.split('.')[0]}",
    responses={
        422: {"description": "Validation error", "model": ErrorResponse},
    },
)
api_router.include_router(author_router)
api_router.include_router(book_router)
api_router.include_router(format_router)
api_router.include_router(publisher_router)
api_router.include_router(series_router)
api_router.include_router(user_router)
