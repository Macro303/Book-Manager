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


class WebSettings(SettingsModel):
    host: str = "localhost"
    port: int = 8001


class Settings(SettingsModel):
    FILENAME: ClassVar = get_config_root() / "settings.toml"
    web: WebSettings = WebSettings()

    @classmethod
    def load(cls) -> "Settings":
        if not cls.FILENAME.exists():
            Settings().save()
        with cls.FILENAME.open("rb") as stream:
            content = tomlreader.load(stream)
        return Settings(**content)

    def save(self) -> "Settings":
        with self.FILENAME.open("wb") as stream:
            content = self.dict(by_alias=False)
            tomlwriter.dump(content, stream)
        return self
