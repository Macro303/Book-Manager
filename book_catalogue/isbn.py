__all__ = ["convert_to_isbn"]

import logging

LOGGER = logging.getLogger(__name__)


def convert_to_isbn(value: str | None) -> str | None:
    if not value:
        return None
    value = value.replace("-", "").strip()
    if len(value) == 10:
        return _to_isbn_13(value)
    if _validate_isbn_13(value):
        return value
    return None


def _to_isbn_13(isbn_10: str) -> str | None:
    LOGGER.info(f"Converting Isbn 10 to Isbn 13: {isbn_10}")
    value = f"978{isbn_10[:-1]}"
    check_digit = _get_check_digit(value)
    isbn_13 = value + str(check_digit)
    if _validate_isbn_13(isbn_13):
        return isbn_13
    return None


def _get_check_digit(value: str) -> int:
    weights = [1, 3]
    output = 0
    for index, x in enumerate(value):
        output += int(x) * weights[index % 2]
    return 0 if output % 10 == 0 else 10 - output % 10


def _validate_isbn_13(isbn_13: str) -> bool:
    return int(isbn_13[12]) == _get_check_digit(isbn_13[:12])
