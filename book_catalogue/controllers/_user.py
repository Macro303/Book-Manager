__all__ = ["UserController"]

import logging

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import User

LOGGER = logging.getLogger(__name__)


class UserController:
    @staticmethod
    def list_users() -> list[User]:
        return User.select()

    @staticmethod
    def create_user(username: str, role: int = 0) -> User:
        if User.get(username=username):
            raise HTTPException(status_code=409, detail="User already exists.")
        user = User(username=username, role=role)
        flush()
        return user

    @staticmethod
    def get_user(user_id: int) -> User:
        if user := User.get(user_id=user_id):
            return user
        raise HTTPException(status_code=404, detail="User not found.")

    @staticmethod
    def delete_user(user_id: int):
        if user := User.get(user_id=user_id):
            return user.delete()
        raise HTTPException(status_code=404, detail="User not found.")

    @staticmethod
    def get_user_by_username(username: str):
        if user := User.get(username=username):
            return user
        raise HTTPException(status_code=404, detail="User not found.")
