__all__ = ["router"]

from fastapi import APIRouter
from pony.orm import db_session

from bookshelf.controllers.publisher import PublisherController
from bookshelf.models.publisher import Publisher, PublisherIn
from bookshelf.responses import ErrorResponse

router = APIRouter(prefix="/publishers", tags=["Publishers"])


@router.get(path="")
def list_publishers() -> list[Publisher]:
    with db_session:
        return sorted({x.to_model() for x in PublisherController.list_publishers()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_publisher(new_publisher: PublisherIn) -> Publisher:
    with db_session:
        return PublisherController.create_publisher(new_publisher=new_publisher).to_model()


@router.get(path="/{publisher_id}", responses={404: {"model": ErrorResponse}})
def get_publisher(publisher_id: int) -> Publisher:
    with db_session:
        return PublisherController.get_publisher(publisher_id=publisher_id).to_model()


@router.patch(path="/{publisher_id}", responses={404: {"model": ErrorResponse}})
def update_publisher(publisher_id: int, updates: PublisherIn) -> Publisher:
    with db_session:
        return PublisherController.update_publisher(
            publisher_id=publisher_id,
            updates=updates,
        ).to_model()


@router.delete(path="/{publisher_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_publisher(publisher_id: int):
    with db_session:
        PublisherController.delete_publisher(publisher_id=publisher_id)
