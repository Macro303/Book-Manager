__all__ = ["router"]

from fastapi import APIRouter, Body, HTTPException
from pony.orm import db_session, flush

from bookshelf.controllers.book import BookController
from bookshelf.controllers.creator import CreatorController
from bookshelf.controllers.role import RoleController
from bookshelf.database.tables import BookCreator
from bookshelf.models.creator import Creator, CreatorBookIn, CreatorIn
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
    path="/{creator_id}",
    responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}},
)
def update_creator(creator_id: int, updates: CreatorIn) -> Creator:
    with db_session:
        return CreatorController.update_creator(creator_id=creator_id, updates=updates).to_model()


@router.delete(path="/{creator_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_creator(creator_id: int):
    with db_session:
        CreatorController.delete_creator(creator_id=creator_id)


@router.put(path="/{creator_id}", responses={404: {"model": ErrorResponse}})
def refresh_creator(creator_id: int) -> Creator:
    with db_session:
        return CreatorController.reset_creator(creator_id=creator_id).to_model()


@router.post(
    path="/{creator_id}/books",
    responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}},
)
def add_book(*, creator_id: int, new_book: CreatorBookIn) -> Creator:
    with db_session:
        creator = CreatorController.get_creator(creator_id=creator_id)
        book = BookController.get_book(book_id=new_book.book_id)
        if BookCreator.get(book=book, creator=creator):
            raise HTTPException(
                status_code=409,
                detail="The Book is already linked to this Creator.",
            )
        BookCreator(
            book=book,
            creator=creator,
            roles=[RoleController.get_role(role_id=x) for x in new_book.role_ids],
        )
        flush()
        return book.to_model()


@router.delete(
    path="/{creator_id}/books",
    responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}},
)
def remove_book(*, creator_id: int, book_id: int = Body(embed=True)) -> Creator:
    with db_session:
        creator = CreatorController.get_creator(creator_id=creator_id)
        book = BookController.get_book(book_id=book_id)
        book_creator = BookCreator.get(book=book, creator=creator)
        if not book_creator:
            raise HTTPException(
                status_code=400,
                detail="The Book isnt associated with this Creator.",
            )
        book_creator.delete()
        flush()
        return book.to_model()
