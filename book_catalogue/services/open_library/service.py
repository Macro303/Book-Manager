from __future__ import annotations
__all__ = ["OpenLibrary"]

import logging
import platform
from typing import Any
from urllib.parse import urlencode

from fastapi.exceptions import HTTPException
from pydantic import ValidationError, parse_obj_as
from ratelimit import limits, sleep_and_retry
from requests import get
from requests.exceptions import ConnectionError, HTTPError, JSONDecodeError, ReadTimeout

from book_catalogue import __version__
from book_catalogue.console import CONSOLE
from book_catalogue.services.open_library.schemas.author import Author
from book_catalogue.services.open_library.schemas.edition import Edition
from book_catalogue.services.open_library.schemas.work import Work
from book_catalogue.services.sqlite_cache import SQLiteCache

MINUTE = 60
LOGGER = logging.getLogger("book_catalogue.services.open_library")


class OpenLibrary:
    API_URL = "https://openlibrary.org"

    def __init__(self, timeout: int = 30, cache: SQLiteCache | None = None):
        self.headers = {
            "Accept": "application/json",
            "User-Agent": f"Book-Catalogue/{__version__}/{platform.system()}: {platform.release()}",
        }
        self.cache = cache
        self.timeout = timeout

    @sleep_and_retry
    @limits(calls=20, period=MINUTE)
    def _perform_get_request(self, url: str, params: dict[str, str] = None) -> dict[str, Any]:
        if params is None:
            params = {}

        try:
            response = get(url, params=params, headers=self.headers, timeout=self.timeout)
            LOGGER.info(f"{'GET':<7} {url.removeprefix(self.API_URL)} - {response.status_code}")
            response.raise_for_status()
            return response.json()
        except ConnectionError as err:
            CONSOLE.print_exception(theme="ansi_dark")
            raise HTTPException(status_code=500, detail=f"Unable to connect to '{url}'") from err
        except HTTPError as err:
            CONSOLE.print_exception(theme="ansi_dark")
            try:
                raise HTTPException(status_code=500, detail=err.response.json()["error"]) from err
            except JSONDecodeError as sub_err:
                CONSOLE.print_exception(theme="ansi_dark")
                raise HTTPException(
                    status_code=500, detail=f"Unable to parse error response from '{url}' as Json"
                ) from sub_err
        except JSONDecodeError as err:
            CONSOLE.print_exception(theme="ansi_dark")
            raise HTTPException(
                status_code=500, detail=f"Unable to parse response from '{url}' as Json"
            ) from err
        except ReadTimeout as err:
            CONSOLE.print_exception(theme="ansi_dark")
            raise HTTPException(status_code=500, detail="Service took too long to respond") from err

    def _get_request(
        self,
        endpoint: str,
        params: dict[str, str] = None,
        skip_cache: bool = False,
    ) -> dict[str, Any]:
        cache_params = f"?{urlencode(params)}" if params else ""

        url = self.API_URL + endpoint
        cache_key = f"{url}{cache_params}"

        if self.cache and not skip_cache:  # noqa: SIM102
            if cached_response := self.cache.select(cache_key):
                return cached_response

        response = self._perform_get_request(url=url, params=params)

        if self.cache and not skip_cache:
            self.cache.insert(cache_key, response)

        return response

    def get_edition_by_isbn(self, isbn: str) -> Edition:
        try:
            result = self._get_request(endpoint=f"/isbn/{isbn}.json")
            return parse_obj_as(Edition, result)
        except ValidationError as err:
            CONSOLE.print_exception(theme="ansi_dark")
            raise HTTPException(
                status_code=500,
                detail=f"Unable to validate OpenLibrary Edition with isbn: {isbn}",
            ) from err

    def get_edition(self, edition_id: str) -> Edition:
        try:
            result = self._get_request(endpoint=f"/edition/{edition_id}.json")
            return parse_obj_as(Edition, result)
        except ValidationError as err:
            CONSOLE.print_exception(theme="ansi_dark")
            raise HTTPException(
                status_code=500,
                detail=f"Unable to validate OpenLibrary Edition with id: {edition_id}",
            ) from err

    def get_work(self, work_id: str) -> Work:
        try:
            result = self._get_request(endpoint=f"/work/{work_id}.json")
            return parse_obj_as(Work, result)
        except ValidationError as err:
            CONSOLE.print_exception(theme="ansi_dark")
            raise HTTPException(
                status_code=500, detail=f"Unable to validate OpenLibrary Work with id: {work_id}"
            ) from err

    def get_author(self, author_id: str) -> Author:
        try:
            result = self._get_request(endpoint=f"/author/{author_id}.json")
            return parse_obj_as(Author, result)
        except ValidationError as err:
            CONSOLE.print_exception(theme="ansi_dark")
            raise HTTPException(
                status_code=500,
                detail=f"Unable to validate OpenLibrary Author with id: {author_id}",
            ) from err
