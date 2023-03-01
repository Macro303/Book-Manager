__all__ = ["router"]

from fastapi import APIRouter
from pony.orm import db_session

from bookshelf.controllers.creator import CreatorController
from bookshelf.models.creator import Creator, CreatorIn
from bookshelf.responses import ErrorResponse

router = APIRouter(prefix="/creators", tags=["Creators"])


@router.get(path="")
def list_creators() -> list[Creator]:
    with db_session:
        return sorted({x.to_model() for x in CreatorController.list_creators()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_creator(new_creator: CreatorIn) -> Creator:
    with db_session:
        return CreatorController.create_creator(new_creator=new_creator).to_model()


@router.get(path="/{creator_id}", responses={404: {"model": ErrorResponse}})
def get_creator(creator_id: int) -> Creator:
    with db_session:
        return CreatorController.get_creator(creator_id=creator_id).to_model()


@router.patch(
    path="/{creator_id}", responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}}
)
def update_creator(creator_id: int, updates: CreatorIn) -> Creator:
    with db_session:
        return CreatorController.update_creator(creator_id=creator_id, updates=updates).to_model()


@router.delete(path="/{creator_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_creator(creator_id: int):
    with db_session:
        CreatorController.delete_creator(creator_id=creator_id)


@router.put(path="/{creator_id}", responses={404: {"model": ErrorResponse}})
def reset_creator(creator_id: int) -> Creator:
    with db_session:
        return CreatorController.reset_creator(creator_id=creator_id).to_model()
