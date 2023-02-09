__all__ = ["router"]

from fastapi import APIRouter, Body, HTTPException
from pony.orm import db_session, flush

from book_catalogue.controllers.book import BookController
from book_catalogue.controllers.creator import CreatorController
from book_catalogue.controllers.role import RoleController
from book_catalogue.database.tables import BookCreator
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas.role import RoleRead, RoleWrite

router = APIRouter(prefix="/roles", tags=["Roles"])


@router.get(path="")
def list_roles() -> list[RoleRead]:
    with db_session:
        return sorted({x.to_schema() for x in RoleController.list_roles()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_role(new_role: RoleWrite) -> RoleRead:
    with db_session:
        return RoleController.create_role(new_role=new_role).to_schema()


@router.get(path="/{role_id}", responses={404: {"model": ErrorResponse}})
def get_role(role_id: int) -> RoleRead:
    with db_session:
        return RoleController.get_role(role_id=role_id).to_schema()


@router.patch(path="/{role_id}", responses={404: {"model": ErrorResponse}})
def update_role(role_id: int, updates: RoleWrite) -> RoleRead:
    with db_session:
        return RoleController.update_role(role_id=role_id, updates=updates).to_schema()


@router.delete(path="/{role_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_role(role_id: int):
    with db_session:
        RoleController.delete_role(role_id=role_id)


@router.post(path="/{role_id}/books", responses={404: {"model": ErrorResponse}})
def add_book_creator_to_role(
    role_id: int, book_id: int = Body(embed=True), creator_id: int = Body(embed=True)
) -> RoleRead:
    with db_session:
        role = RoleController.get_role(role_id=role_id)
        book = BookController.get_book(book_id=book_id)
        creator = CreatorController.get_creator(creator_id=creator_id)
        book_creator = BookCreator.get(book=book, creator=creator) or BookCreator(
            book=book, creator=creator
        )
        if book_creator and role in book_creator.roles:
            raise HTTPException(
                status_code=400, detail="Role has already been assigned to BookCreator"
            )
        book_creator.roles.add(role)
        flush()
        return role.to_schema()


@router.delete(
    path="/{role_id}/books/{creator_id}/{book_id}", responses={404: {"model": ErrorResponse}}
)
def remove_book_creator_from_role(role_id: int, creator_id: int, book_id: int):
    with db_session:
        role = RoleController.get_role(role_id=role_id)
        book = BookController.get_book(book_id=book_id)
        creator = CreatorController.get_creator(creator_id=creator_id)
        book_creator = BookCreator.get(book=book, creator=creator)
        if not book_creator or role not in book_creator.roles:
            raise HTTPException(status_code=400, detail="Role hasn't been assigned to BookCreator")
        book_creator.roles.remove(role)
        flush()
        return role.to_schema()
