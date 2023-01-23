from __future__ import annotations

import logging
from collections.abc import Callable
from datetime import datetime
from http import HTTPStatus
from typing import Any

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles
from starlette.exceptions import HTTPException as StarletteHTTPException
from rich import inspect
from jinja2.exceptions import TemplateNotFound

from book_catalogue import __version__, get_project_root, setup_logging
from book_catalogue.console import CONSOLE
from book_catalogue.routers.api import api_router
from book_catalogue.routers.html import router as html_router
from book_catalogue.settings import Settings

LOGGER = logging.getLogger("book_catalogue")


def create_app() -> FastAPI:
    _app = FastAPI(title="Book Catalogue", version=__version__)
    _app.mount("/static", StaticFiles(directory=get_project_root() / "static"), name="static")
    _app.include_router(html_router)
    _app.include_router(api_router)
    return _app


app = create_app()


@app.on_event(event_type="startup")
async def startup_event() -> None:
    settings = Settings.load()
    setup_logging()

    LOGGER.info(f"Listening on {settings.website.host}:{settings.website.port}")
    LOGGER.info(f"{app.title} v{app.version} started")


@app.middleware(middleware_type="http")
async def logger_middleware(request: Request, call_next: Callable) -> Any:  # noqa: ANN401
    LOGGER.info(
        f"{request.method.upper():<7} {request.scope['path']} - {request.headers['user-agent']}"
    )
    response = await call_next(request)
    if response.status_code < 400:
        LOGGER.info(f"{request.method.upper():<7} {request.scope['path']} - {response.status_code}")
    elif response.status_code < 500:
        LOGGER.warning(f"{request.method.upper():<7} {request.scope['path']} - {response.status_code}")
    else:
        LOGGER.error(f"{request.method.upper():<7} {request.scope['path']} - {response.status_code}")
    return response


@app.exception_handler(exc_class_or_status_code=StarletteHTTPException)
async def http_exception_handler(request: Request, exc) -> JSONResponse:  # noqa: ARG001, ANN001
    status = HTTPStatus(exc.status_code)
    return JSONResponse(
        status_code=status,
        content={
            "timestamp": datetime.now().replace(microsecond=0).isoformat(),
            "status": f"{status.value}: {status.phrase}",
            "details": [exc.detail],
        },
        headers=exc.headers,
    )


@app.exception_handler(exc_class_or_status_code=RequestValidationError)
async def validation_exception_handler(
    request: Request, exc  # noqa: ARG001, ANN001
) -> JSONResponse:
    status = HTTPStatus(422)
    details = []
    for error in exc.errors():
        temp = ".".join(error["loc"])
        details.append(f"{temp}: {error['msg']}")
    return JSONResponse(
        status_code=status,
        content={
            "timestamp": datetime.now().replace(microsecond=0).isoformat(),
            "status": f"{status.value}: {status.phrase}",
            "details": details,
        },
    )

@app.exception_handler(exc_class_or_status_code=TemplateNotFound)
async def missing_template_exception_handler(
    request: Request, exc  # noqa: ARG001, ANN001
) -> JSONResponse:
    status = HTTPStatus(404)
    return JSONResponse(
        status_code=status,
        content={
            "timestamp": datetime.now().replace(microsecond=0).isoformat(),
            "status": f"{status.value}: {status.phrase}",
            "details": [f"{exc.message} not found."],
        },
    )