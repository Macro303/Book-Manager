import logging
from datetime import datetime
from http import HTTPStatus

from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles
from rich.logging import RichHandler
from starlette.exceptions import HTTPException as StarletteHTTPException

from book_manager import __version__, get_project_root
from book_manager.console import CONSOLE
from book_manager.routing.api import router as api_router
from book_manager.routing.html import router as html_router

LOGGER = logging.getLogger("Book-Manager")


def setup_logging(debug: bool = False):
    logging.getLogger("uvicorn").handlers.clear()
    logging.basicConfig(
        format="%(message)s",
        datefmt="[%Y-%m-%d %H:%M:%S]",
        level=logging.DEBUG if debug else logging.INFO,
        handlers=[
            RichHandler(
                rich_tracebacks=True,
                tracebacks_show_locals=True,
                log_time_format="[%Y-%m-%d %H:%M:%S]",
                omit_repeated_times=False,
                console=CONSOLE,
            )
        ],
    )


setup_logging()


def create_app() -> FastAPI:
    app = FastAPI(name="Book-Manager", version=__version__)
    app.include_router(html_router)
    app.include_router(api_router)
    return app


app = create_app()
app.mount("/static", StaticFiles(directory=get_project_root() / "static"), name="static")


@app.get("/")
def redirect():
    return RedirectResponse(url="/Book-Manager")


@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request, exc):
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


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
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
