__all__ = ["GoogleBooks"]

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
from book_catalogue.services.google_books.schemas import Book
from book_catalogue.services.sqlite_cache import SQLiteCache

MINUTE = 60
LOGGER = logging.getLogger("book_catalogue.services.google_books")


class GoogleBooks:
    API_URL = "https://www.googleapis.com/books/v1"

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
            CONSOLE.print(err)
            raise HTTPException(status_code=500, detail=f"Unable to connect to '{url}'") from err
        except HTTPError as err:
            CONSOLE.print(err)
            try:
                raise HTTPException(status_code=500, detail=err.response.json()["error"]) from err
            except JSONDecodeError as sub_err:
                CONSOLE.print(err)
                raise HTTPException(
                    status_code=500, detail=f"Unable to parse error response from '{url}' as Json"
                ) from sub_err
        except JSONDecodeError as err:
            CONSOLE.print(err)
            raise HTTPException(
                status_code=500, detail=f"Unable to parse response from '{url}' as Json"
            ) from err
        except ReadTimeout as err:
            CONSOLE.print(err)
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

    def get_book_by_isbn(self, isbn: str) -> Book:
        try:
            results = self._get_request(endpoint="/volumes", params={"q": f"isbn:{isbn}"})
            results = results.get("items", [])
            results = parse_obj_as(list[Book], results)
            if len(results) > 1:
                results = [x for x in results if x.volume_info.get_isbn() == isbn]
            if result := next(iter(results), None):
                return result
            raise HTTPException(status_code=400, detail="No GoogleBooks result found.")
        except ValidationError as err:
            CONSOLE.print(err)
            raise HTTPException(
                status_code=500,
                detail=f"Unable to validate GoogleBooks book with isbn: {isbn}",
            ) from err

    def get_book(self, book_id: str) -> Book:
        try:
            result = self._get_request(endpoint=f"/volumes/{book_id}")
            return parse_obj_as(Book, result)
        except ValidationError as err:
            CONSOLE.print(err)
            raise HTTPException(
                status_code=500,
                detail=f"Unable to validate GoogleBooks book with id: {book_id}",
            ) from err
