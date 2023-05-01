__all__ = ["Settings"]

import tomllib as tomlreader
from pathlib import Path
from typing import ClassVar

import tomli_w as tomlwriter
from pydantic import BaseModel, Extra

from bookshelf import get_config_root


class SettingsModel(BaseModel):
    class Config:
        allow_population_by_field_name = True
        anystr_strip_whitespace = True
        validate_assignment = True
        extra = Extra.ignore


class DatabaseSettings(SettingsModel):
    name: str = "bookshelf.sqlite"


class WebsiteSettings(SettingsModel):
    host: str = "localhost"
    port: int = 8003
    reload: bool = False


class _Settings(SettingsModel):
    FILENAME: ClassVar[Path] = get_config_root() / "settings.toml"
    _instance: ClassVar["_Settings"] = None
    database: DatabaseSettings = DatabaseSettings()
    website: WebsiteSettings = WebsiteSettings()

    @classmethod
    def load(cls) -> "_Settings":
        if not cls.FILENAME.exists():
            _Settings().save()
        with cls.FILENAME.open("rb") as stream:
            content = tomlreader.load(stream)
        return _Settings(**content)

    def save(self) -> "_Settings":
        with self.FILENAME.open("wb") as stream:
            content = self.dict(by_alias=False)
            tomlwriter.dump(content, stream)
        return self


def Settings() -> _Settings:  # noqa: N802
    if _Settings._instance is None:
        _Settings._instance = _Settings.load()
    return _Settings._instance
