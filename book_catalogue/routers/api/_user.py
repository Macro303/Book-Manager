from __future__ import annotations

__all__ = ["router"]

from fastapi import APIRouter
from pony.orm import db_session

from book_catalogue.controllers import UserController
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas import User
from book_catalogue.schemas._user import NewUser

router = APIRouter(prefix="/users", tags=["Users"])


@router.get(path="")
def list_users() -> list[User]:
    with db_session:
        return sorted({x.to_schema() for x in UserController.list_users()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_user(new_user: NewUser) -> User:
    with db_session:
        return UserController.create_user(new_user=new_user).to_schema()


@router.patch(path="/{user_id}", responses={404: {"model": ErrorResponse}})
def update_user(user_id: int, updates: NewUser) -> User:
    with db_session:
        return UserController.update_user(user_id=user_id, updates=updates)


@router.delete(path="/{user_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_user(user_id: int):
    with db_session:
        UserController.delete_user(user_id=user_id)


@router.get(path="/{username}", responses={404: {"model": ErrorResponse}})
def get_user_by_username(username: str) -> User:
    with db_session:
        return UserController.get_user_by_username(username=username).to_schema()
