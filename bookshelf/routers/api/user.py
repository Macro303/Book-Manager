__all__ = ["router"]

from fastapi import APIRouter
from pony.orm import db_session

from bookshelf.controllers.user import UserController
from bookshelf.models.user import User, UserIn
from bookshelf.responses import ErrorResponse

router = APIRouter(prefix="/users", tags=["Users"])


@router.get(path="")
def list_users(*, username: str = "") -> list[User]:
    with db_session:
        results = UserController.list_users()
        if username:
            results = [
                x
                for x in results
                if (x.username.casefold() in username.casefold())
                or (username.casefold() in x.username.casefold())
            ]
        return sorted({x.to_model() for x in results})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_user(*, new_user: UserIn) -> User:
    with db_session:
        return UserController.create_user(new_user=new_user).to_model()


@router.get(path="/{user_id}", responses={404: {"model": ErrorResponse}})
def get_user(*, user_id: int) -> User:
    with db_session:
        return UserController.get_user(user_id=user_id).to_model()


@router.patch(path="/{user_id}", responses={404: {"model": ErrorResponse}})
def update_user(*, user_id: int, updates: UserIn) -> User:
    with db_session:
        return UserController.update_user(user_id=user_id, updates=updates).to_model()


@router.delete(path="/{user_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_user(*, user_id: int):
    with db_session:
        UserController.delete_user(user_id=user_id)
