__all__ = ["lookup_book"]

import logging
import platform
from typing import Any

from requests import get
from requests.exceptions import ConnectionError, HTTPError, JSONDecodeError, ReadTimeout

from book_manager import __version__
from book_manager.models.book import Book, Identifiers
from book_manager.models.isbn import to_isbn

LOGGER = logging.getLogger(__name__)


def _perform_get_request(endpoint: str, params: dict[str, str] = None) -> dict[str, Any]:
    if not params:
        params = {}
    headers = {
        "Accept": "application/json",
        "User-Agent": f"Book-Manager/{__version__}/{platform.system()}: {platform.version()}",
    }

    url = f"https://openlibrary.org/{endpoint}"
    try:
        response = get(url, params=params, headers=headers, timeout=30)
        response.raise_for_status()
        return response.json()
    except ConnectionError:
        LOGGER.error(f"Unable to connect to `{url}`")
    except HTTPError as err:
        try:
            LOGGER.error(err.response.json()["error"])
        except JSONDecodeError:
            LOGGER.error(f"Unable to parse response from `{url}` as Json")
    except JSONDecodeError:
        LOGGER.error(f"Unable to parse response from `{url}` as Json")
    except ReadTimeout:
        LOGGER.error("Service took too long to respond")


def lookup_book(isbn: str) -> Book:
    book = search_book(isbn)
    edition_id = book["identifiers"]["openlibrary"][0]
    edition = get_book(edition_id)
    work_id = edition["works"][0]["key"].split("/")[-1]
    work = get_work(work_id)

    isbn = None
    if "isbn_13" in edition:
        isbn = edition["isbn_13"][0]
    elif "isbn_10" in edition:
        isbn = edition["isbn_10"][0]
    authors = [x["name"] for x in book["authors"]]

    return Book(
        publisher="; ".join(edition["publishers"]),
        page_count=edition["number_of_pages"] if "number_of_pages" in edition else None,
        title=edition["title"],
        format=edition["physical_format"] if "physical_format" in edition else None,
        publish_date=edition["publish_date"],
        isbn=to_isbn(isbn) if isbn else None,
        series=edition["series"] if "series" in edition else [],
        description=work["description"]
        if "description" in work and isinstance(work["description"], str)
        else work["description"]["value"]
        if "description" in work and "value" in work["description"]
        else None,
        subjects=work["subjects"] if "subjects" in work else [],
        authors=authors,
        identifiers=Identifiers(
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
        ),
    )


def search_book(isbn: str) -> dict[str, Any]:
    response = _perform_get_request(
        endpoint="/api/books", params={"bibkeys": f"ISBN:{isbn}", "format": "json", "jscmd": "data"}
    )
    return list(response.values())[0]


def get_book(edition_id: str) -> dict[str, Any]:
    response = _perform_get_request(endpoint=f"/books/{edition_id}.json")
    return response


def get_work(work_id: str):
    response = _perform_get_request(endpoint=f"/works/{work_id}.json")
    return response
