import uvicorn

from book_catalogue.settings import Settings


def main() -> None:
    settings = Settings().save()

    uvicorn.run(
        "book_catalogue.__main__:app",
        host=settings.website.host,
        port=settings.website.port,
        use_colors=True,
        server_header=False,
        reload=settings.website.reload,
        log_config=None,
    )


if __name__ == "__main__":
    main()
