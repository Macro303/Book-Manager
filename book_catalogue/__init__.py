__version__ = "0.1.0b0"
__all__ = ["__version__", "get_project_root"]

from pathlib import Path


def get_project_root() -> Path:
    return Path(__file__).parent.parent
