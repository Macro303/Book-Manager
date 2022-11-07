__all__ = ["to_isbn"]


def to_isbn(number: str | None) -> str | None:
    if not number:
        return None
    if not _valid_isbn(number):
        return None

    if len(number.replace("-", "")) == 10:
        return _to_isbn_13(number)
    return number.replace("-", "")


def _to_isbn_13(isbn_10: str) -> str:
    digits = f"978{isbn_10[:-1]}"
    check_digit = str(_get_check_digit(digits))
    return digits + check_digit


def _valid_isbn(value: str) -> bool:
    value = value.replace("-", "")
    if len(value) == 13:
        return _valid_isbn_13(value)
    return False


def _valid_isbn_13(value: str) -> bool:
    return int(value[12]) == _get_check_digit(value[:12])


def _get_check_digit(value: str) -> int:
    weights = [1, 3]
    output = 0
    for index, x in enumerate(value):
        output += int(x) * weights[index % 2]
    return 0 if output % 10 == 0 else 10 - output % 10
