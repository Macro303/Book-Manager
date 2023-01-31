from __future__ import annotations

__all__ = ["router"]

from fastapi import APIRouter
from pony.orm import db_session

from book_catalogue.controllers.user import UserController
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas.user import UserRead, UserWrite

router = APIRouter(prefix="/users", tags=["Users"])


@router.get(path="")
def list_users(*, username: str = "") -> list[UserRead]:
    with db_session:
        results = UserController.list_users()
        if username:
            results = [
                x
                for x in results
                if (x.username.casefold() in username.casefold())
                or (username.casefold() in x.username.casefold())
            ]
        return sorted({x.to_schema() for x in results})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_user(*, new_user: UserWrite) -> UserRead:
    with db_session:
        return UserController.create_user(new_user=new_user).to_schema()


@router.get(path="/{user_id}", responses={404: {"model": ErrorResponse}})
def get_user(*, user_id: int) -> UserRead:
    with db_session:
        return UserController.get_user(user_id=user_id).to_schema()


@router.patch(path="/{user_id}", responses={404: {"model": ErrorResponse}})
def update_user(*, user_id: int, updates: UserWrite) -> UserRead:
    with db_session:
        return UserController.update_user(user_id=user_id, updates=updates).to_schema()


@router.delete(path="/{user_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_user(*, user_id: int):
    with db_session:
        UserController.delete_user(user_id=user_id)
