from __future__ import annotations

__all__ = ["router"]

from fastapi import APIRouter
from pony.orm import db_session

from book_catalogue.controllers.publisher import PublisherController
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas.publisher import PublisherRead, PublisherWrite

router = APIRouter(prefix="/publishers", tags=["Publishers"])


@router.get(path="")
def list_publishers() -> list[PublisherRead]:
    with db_session:
        return sorted({x.to_schema() for x in PublisherController.list_publishers()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_publisher(new_publisher: PublisherWrite) -> PublisherRead:
    with db_session:
        return PublisherController.create_publisher(new_publisher=new_publisher).to_schema()


@router.get(path="/{publisher_id}", responses={404: {"model": ErrorResponse}})
def get_publisher(publisher_id: int) -> PublisherRead:
    with db_session:
        return PublisherController.get_publisher(publisher_id=publisher_id).to_schema()


@router.patch(path="/{publisher_id}", responses={404: {"model": ErrorResponse}})
def update_publisher(publisher_id: int, updates: PublisherWrite) -> PublisherRead:
    with db_session:
        return PublisherController.update_publisher(
            publisher_id=publisher_id, updates=updates
        ).to_schema()


@router.delete(path="/{publisher_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_publisher(publisher_id: int):
    with db_session:
        PublisherController.delete_publisher(publisher_id=publisher_id)
