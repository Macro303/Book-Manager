__all__ = ["__version__", "get_cache_root", "get_config_root", "get_data_root", "get_project_root"]
__version__ = "0.1.0"

import os
from pathlib import Path


def get_cache_root() -> Path:
    cache_home = os.getenv("XDG_CACHE_HOME", default=str(Path.home() / ".cache"))
    folder = Path(cache_home).resolve() / "book-catalogue"
    folder.mkdir(exist_ok=True, parents=True)
    return folder


def get_config_root() -> Path:
    config_home = os.getenv("XDG_CONFIG_HOME", default=str(Path.home() / ".config"))
    folder = Path(config_home).resolve() / "book-catalogue"
    folder.mkdir(exist_ok=True, parents=True)
    return folder


def get_data_root() -> Path:
    data_home = os.getenv("XDG_DATA_HOME", default=str(Path.home() / ".local" / "share"))
    folder = Path(data_home).resolve() / "book-catalogue"
    folder.mkdir(exist_ok=True, parents=True)
    return folder


def get_project_root() -> Path:
    return Path(__file__).parent.parent
