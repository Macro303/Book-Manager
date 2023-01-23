from __future__ import annotations
__all__ = ["PublisherController"]

import logging

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import Publisher
from book_catalogue.schemas._publisher import NewPublisher

LOGGER = logging.getLogger(__name__)


class PublisherController:
    @classmethod
    def list_publishers(cls) -> list[Publisher]:
        return Publisher.select()

    @classmethod
    def create_publisher(cls, new_publisher: NewPublisher) -> Publisher:
        if Publisher.get(name=new_publisher.name):
            raise HTTPException(status_code=409, detail="Publisher already exists.")
        publisher = Publisher(name=new_publisher.name)
        flush()
        return publisher

    @classmethod
    def get_publisher(cls, publisher_id: int) -> Publisher:
        if publisher := Publisher.get(publisher_id=publisher_id):
            return publisher
        raise HTTPException(status_code=404, detail="Publisher not found.")

    @classmethod
    def update_publisher(cls, publisher_id: int, updates: NewPublisher):
        publisher = cls.get_publisher(publisher_id=publisher_id)
        publisher.name = updates.name
        flush()
        return publisher

    @classmethod
    def delete_publisher(cls, publisher_id: int):
        publisher = cls.get_publisher(publisher_id=publisher_id)
        publisher.delete()
        
    @classmethod
    def get_publisher_by_name(cls, name: str) -> Publisher:
        if publisher := Publisher.get(name=name):
            return publisher
        raise HTTPException(status_code=404, detail="Publisher not found.")
