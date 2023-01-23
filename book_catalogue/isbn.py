from __future__ import annotations

__all__ = ["to_isbn_13"]

import logging

LOGGER = logging.getLogger(__name__)


def to_isbn_13(value: str | None) -> str | None:
    if not value:
        return None
    value = value.replace("-", "").strip()
    if len(value) == 10:
        value = _isbn_10_to_isbn_13(value)
    if _validate_isbn_13(value):
        return value
    return None


def _isbn_10_to_isbn_13(isbn_10: str) -> str:
    LOGGER.info(f"Converting Isbn 10 to Isbn 13: {isbn_10}")
    value = f"978{isbn_10[:-1]}"
    check_digit = _get_check_digit(value)
    return value + str(check_digit)


def _get_check_digit(value: str) -> int:
    weights = [1, 3]
    output = 0
    for index, x in enumerate(value):
        output += int(x) * weights[index % 2]
    return 0 if output % 10 == 0 else 10 - output % 10


def _validate_isbn_13(isbn_13: str) -> bool:
    return int(isbn_13[12]) == _get_check_digit(isbn_13[:12])
