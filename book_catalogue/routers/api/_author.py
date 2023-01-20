__all__ = ["router"]

from fastapi import APIRouter, HTTPException
from pony.orm import db_session

from book_catalogue.controllers import AuthorController
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas import Author, AuthorRole
from book_catalogue.schemas._author import NewAuthor, NewRole

router = APIRouter(prefix="/authors", tags=["Authors"])


@router.get(path="")
def list_authors() -> list[Author]:
    with db_session:
        return sorted({x.to_schema() for x in AuthorController.list_authors()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_author(new_author: NewAuthor) -> Author:
    with db_session:
        return AuthorController.create_author(new_author=new_author).to_schema()


@router.get(path="/{author_id}", responses={404: {"model": ErrorResponse}})
def get_author(author_id: int) -> Author:
    with db_session:
        return AuthorController.get_author(author_id=author_id).to_schema()
            

@router.patch(path="/{author_id}", responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}})
def update_author(author_id: int, updates: NewAuthor) -> Author:
    with db_session:
        return AuthorController.update_author(author_id=author_id, updates=updates)


@router.delete(path="/{author_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_author(author_id: int) -> None:
    with db_session:
        AuthorController.delete_author(author_id=author_id)


@router.get(path="/roles")
def list_roles() -> list[AuthorRole]:
    with db_session:
        return sorted({x.to_schema() for x in AuthorController.list_roles()})


@router.post(path="/roles", status_code=201, responses={409: {"model": ErrorResponse}})
def create_role(new_role: NewRole) -> AuthorRole:
    with db_session:
        return AuthorController.create_role(name=new_role.name).to_schema()


@router.get(path="/roles/{role_id}", responses={404: {"model": ErrorResponse}})
def get_role(role_id: int) -> AuthorRole:
    with db_session:
        return AuthorController.get_role(role_id=role_id).to_schema()
            

@router.patch(path="/roles/{role_id}", responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}})
def update_role(role_id: int, updates: NewRole) -> AuthorRole:
    with db_session:
        return AuthorController.update_role(role_id=role_id, updates=updates).to_schema()


@router.delete(path="/roles/{role_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_role(role_id: int) -> None:
    with db_session:
        AuthorController.delete_role(role_id=role_id)