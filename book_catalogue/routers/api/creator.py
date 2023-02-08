__all__ = ["router"]

from fastapi import APIRouter
from pony.orm import db_session

from book_catalogue.controllers.creator import CreatorController
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas.creator import CreatorRead, CreatorWrite

router = APIRouter(prefix="/creators", tags=["Creators"])


@router.get(path="")
def list_creators() -> list[CreatorRead]:
    with db_session:
        return sorted({x.to_schema() for x in CreatorController.list_creators()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_creator(new_creator: CreatorWrite) -> CreatorRead:
    with db_session:
        return CreatorController.create_creator(new_creator=new_creator).to_schema()


@router.get(path="/{creator_id}", responses={404: {"model": ErrorResponse}})
def get_creator(creator_id: int) -> CreatorRead:
    with db_session:
        return CreatorController.get_creator(creator_id=creator_id).to_schema()


@router.patch(
    path="/{creator_id}", responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}}
)
def update_creator(creator_id: int, updates: CreatorWrite) -> CreatorRead:
    with db_session:
        return CreatorController.update_creator(creator_id=creator_id, updates=updates).to_schema()


@router.delete(path="/{creator_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_creator(creator_id: int):
    with db_session:
        CreatorController.delete_creator(creator_id=creator_id)


@router.put(path="", status_code=204)
def reset_all_creators():
    with db_session:
        for _creator in CreatorController.list_creators():
            if _creator.open_library_id:
                CreatorController.reset_creator(creator_id=_creator.creator_id)


@router.put(path="/{creator_id}", responses={404: {"model": ErrorResponse}})
def reset_creator(creator_id: int) -> CreatorRead:
    with db_session:
        return CreatorController.reset_creator(creator_id=creator_id).to_schema()
