__all__ = ["retrieve_book"]

import logging
import platform
from typing import Any
from urllib.parse import urlencode

from fastapi.exceptions import HTTPException
from requests import get
from requests.exceptions import ConnectionError, HTTPError, JSONDecodeError, ReadTimeout
from sqlalchemy.orm import Session

from book_catalogue import __version__, controller
from book_catalogue.isbn import convert_to_isbn
from book_catalogue.models import Book

LOGGER = logging.getLogger(__name__)


def _perform_get_request(endpoint: str, params: dict[str, str] = None) -> dict[str, Any]:
    if not params:
        params = {}
    headers = {
        "Accept": "application/json",
        "User-Agent": f"Book-Catalogue/{__version__}/{platform.system()}: {platform.version()}",
    }

    url = f"https://openlibrary.org{endpoint}"
    full_url = url
    if params:
        full_url += f"?{urlencode({k: params[k] for k in sorted(params)})}"
    try:
        response = get(url, params=params, headers=headers, timeout=30)
        response.raise_for_status()
        return response.json()
    except ConnectionError:
        raise HTTPException(status_code=500, detail=f"Unable to connect to '{full_url}'")
    except HTTPError as err:
        try:
            raise HTTPException(status_code=500, detail=err.response.json()["error"])
        except JSONDecodeError:
            raise HTTPException(
                status_code=500, detail=f"Unable to parse response from '{full_url}' as Json"
            )
    except JSONDecodeError:
        raise HTTPException(
            status_code=500, detail=f"Unable to parse response from '{full_url}' as Json"
        )
    except ReadTimeout:
        raise HTTPException(status_code=500, detail="Open Library took too long to respond")


def retrieve_book(db: Session, isbn: str) -> Book:
    book = search_book(isbn)
    edition_id = book["identifiers"]["openlibrary"][0]
    edition = get_edition(edition_id)
    work_id = edition["works"][0]["key"].split("/")[-1]
    _ = get_work(work_id)

    isbn = None
    if "isbn_13" in edition:
        isbn = edition["isbn_13"][0]
    elif "isbn_10" in edition:
        isbn = edition["isbn_10"][0]
    isbn = convert_to_isbn(isbn)
    if not isbn:
        raise HTTPException(status_code=400, detail="Invalid ISBN value.")
    authors = [
        controller.get_author(db, x["name"]) or controller.create_author(db, x["name"])
        for x in book["authors"]
    ]
    series = (
        [controller.get_series(db, x) or controller.create_series(db, x) for x in edition["series"]]
        if "series" in edition
        else []
    )

    return Book(
        isbn=isbn,
        title=edition["title"],
        subtitle=edition["subtitle"] if "subtitle" in edition else None,
        authors=authors,
        format=edition["physical_format"] if "physical_format" in edition else None,
        series=series,
        publisher="; ".join(edition["publishers"]),
        open_library_id=edition_id,
        google_books_id=book["identifiers"]["google"][0]
        if "google" in book["identifiers"]
        else None,
        goodreads_id=book["identifiers"]["goodreads"][0]
        if "goodreads" in book["identifiers"]
        else None,
        library_thing_id=book["identifiers"]["librarything"][0]
        if "librarything" in book["identifiers"]
        else None,
    )


def search_book(isbn: str) -> dict[str, Any]:
    LOGGER.info(f"Searching for book: {isbn}")
    response = _perform_get_request(
        endpoint="/api/books", params={"bibkeys": f"ISBN:{isbn}", "format": "json", "jscmd": "data"}
    )
    return list(response.values())[0]


def get_edition(edition_id: str) -> dict[str, Any]:
    LOGGER.info(f"Getting edition: {edition_id}")
    response = _perform_get_request(endpoint=f"/books/{edition_id}.json")
    return response


def get_work(work_id: str) -> dict[str, Any]:
    LOGGER.info(f"Getting work: {work_id}")
    response = _perform_get_request(endpoint=f"/works/{work_id}.json")
    return response
