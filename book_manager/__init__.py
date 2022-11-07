__version__ = "0.1.0b0"
__all__ = ["__version__", "get_project_root", "get_config_root"]

from pathlib import Path


def get_project_root() -> Path:
    return Path(__file__).parent.parent


def get_config_root() -> Path:
    folder = Path.home() / ".config" / "book-manager"
    folder.mkdir(parents=True, exist_ok=True)
    return folder
