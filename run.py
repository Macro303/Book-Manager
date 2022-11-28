import uvicorn

from book_catalogue import get_project_root
from book_catalogue.settings import Settings


def main():
    settings = Settings.load().save()
    log_folder = get_project_root() / "logs"
    log_folder.mkdir(parents=True, exist_ok=True)
    uvicorn.run(
        "book_catalogue.__main__:app",
        host=settings.web.host,
        port=settings.web.port,
        use_colors=True,
        server_header=False,
        log_config="log_config.yaml",
    )


if __name__ == "__main__":
    main()
