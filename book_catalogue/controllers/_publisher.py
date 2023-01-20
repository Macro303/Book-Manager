__all__ = ["PublisherController"]

import logging

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import Publisher

LOGGER = logging.getLogger(__name__)


class PublisherController:
    @staticmethod
    def list_publishers() -> list[Publisher]:
        return Publisher.select()

    @staticmethod
    def create_publisher(name: str) -> Publisher:
        if Publisher.get(name=name):
            raise HTTPException(status_code=409, detail="Publisher already exists.")
        publisher = Publisher(name=name)
        flush()
        return publisher

    @staticmethod
    def get_publisher(publisher_id: int) -> Publisher | None:
        if publisher := Publisher.get(publisher_id=publisher_id):
            return publisher
        return None

    @staticmethod
    def delete_publisher(publisher_id: int):
        if publisher := Publisher.get(publisher_id=publisher_id):
            return publisher.delete()
        raise HTTPException(status_code=404, detail="Publisher not found.")

    @staticmethod
    def get_publisher_by_name(name: str) -> Publisher | None:
        if publisher := Publisher.get(name=name):
            return publisher
        return None
