__all__ = ["router"]

from fastapi import APIRouter, HTTPException
from pony.orm import db_session

from book_catalogue.controllers import AuthorController
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas import Author, AuthorRole
from book_catalogue.schemas._author import CreateAuthor, CreateRole

router = APIRouter(prefix="/authors", tags=["Authors"])


@router.get(path="")
def list_authors() -> list[Author]:
    with db_session:
        return sorted({x.to_schema() for x in AuthorController.list_authors()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_author(new_author: CreateAuthor) -> Author:
    with db_session:
        return AuthorController.create_author(name=new_author.name).to_schema()


@router.get(path="/{author_id}", responses={404: {"model": ErrorResponse}})
def get_author(author_id: int) -> Author:
    with db_session:
        if author := AuthorController.get_author(author_id=author_id):
            return author.to_schema()
        raise HTTPException(status_code=404, detail="Author not found.")


@router.delete(path="/{author_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_author(author_id: int) -> None:
    with db_session:
        AuthorController.delete_author(author_id=author_id)


@router.get(path="/roles")
def list_roles() -> list[AuthorRole]:
    with db_session:
        return sorted({x.to_schema() for x in AuthorController.list_roles()})


@router.post(path="/roles", status_code=201, responses={409: {"model": ErrorResponse}})
def create_role(new_role: CreateRole) -> AuthorRole:
    with db_session:
        return AuthorController.create_role(name=new_role.name).to_schema()


@router.get(path="/roles/{role_id}", responses={404: {"model": ErrorResponse}})
def get_role(role_id: int) -> AuthorRole:
    with db_session:
        if role := AuthorController.get_role(role_id=role_id):
            return role.to_schema()
        raise HTTPException(status_code=404, detail="Role not found.")


@router.delete(path="/roles/{role_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_role(role_id: int) -> None:
    with db_session:
        AuthorController.delete_role(role_id=role_id)
