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

from book_catalogue import __version__, get_project_root
from book_catalogue.routing.api import router as api_router
from book_catalogue.routing.html import router as html_router

LOGGER = logging.getLogger("book_catalogue")


def create_app() -> FastAPI:
    app = FastAPI(title="Book Catalogue", version=__version__)
    app.include_router(html_router)
    app.include_router(api_router)

    LOGGER.info(f"{app.title} v{app.version} started")
    return app


app = create_app()
app.mount("/static", StaticFiles(directory=get_project_root() / "static"), name="static")


@app.middleware(middleware_type="http")
async def logger_middleware(request: Request, call_next: Callable) -> Any:  # noqa: ANN401
    LOGGER.info(
        f"{request.method.upper():<7} {request.scope['path']} - {request.headers['user-agent']}"
    )
    response = await call_next(request)
    LOGGER.info(f"{request.method.upper():<7} {request.scope['path']} - {response.status_code}")
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
