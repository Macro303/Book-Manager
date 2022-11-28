__version__ = "0.1.0"
__all__ = ["__version__", "get_project_root", "get_config_root"]

from pathlib import Path


def get_project_root() -> Path:
    return Path(__file__).parent.parent


def get_config_root() -> Path:
    root = Path.home() / ".config" / "book-catalogue"
    root.mkdir(parents=True, exist_ok=True)
    return root
