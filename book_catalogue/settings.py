from __future__ import annotations

__all__ = ["Settings"]

from typing import ClassVar

import tomli_w as tomlwriter
import tomllib as tomlreader
from pydantic import BaseModel, Extra

from book_catalogue import get_config_root


class SettingsModel(BaseModel):
    class Config:
        allow_population_by_field_name = True
        anystr_strip_whitespace = True
        validate_assignment = True
        extra = Extra.ignore


class DatabaseSettings(SettingsModel):
    name: str = "book-catalogue.sqlite"


class WebsiteSettings(SettingsModel):
    host: str = "localhost"
    port: int = 8003
    reload: bool = False


class Settings(SettingsModel):
    FILENAME: ClassVar = get_config_root() / "settings.toml"
    database: DatabaseSettings = DatabaseSettings()
    website: WebsiteSettings = WebsiteSettings()

    @classmethod
    def load(cls) -> Settings:
        if not cls.FILENAME.exists():
            Settings().save()
        with cls.FILENAME.open("rb") as stream:
            content = tomlreader.load(stream)
        return Settings(**content)

    def save(self) -> Settings:
        with self.FILENAME.open("wb") as stream:
            content = self.dict(by_alias=False)
            tomlwriter.dump(content, stream)
        return self
