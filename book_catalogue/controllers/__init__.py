__all__ = [
    "AuthorController",
    "BookController",
    "PublisherController",
    "SeriesController",
    "UserController",
]

from book_catalogue.controllers._author import AuthorController
from book_catalogue.controllers._book import BookController
from book_catalogue.controllers._publisher import PublisherController
from book_catalogue.controllers._series import SeriesController
from book_catalogue.controllers._user import UserController
