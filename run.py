import logging

import uvicorn

from book_catalogue import setup_logging
from book_catalogue.settings import Settings

LOGGER = logging.getLogger("book_catalogue")


def main() -> None:
    settings = Settings.load().save()
    setup_logging()

    LOGGER.info(f"Listening on {settings.website.host}:{settings.website.port}")

    uvicorn.run(
        "book_catalogue.__main__:app",
        host=settings.website.host,
        port=settings.website.port,
        use_colors=True,
        server_header=False,
        log_level=logging.CRITICAL,
    )


if __name__ == "__main__":
    main()
