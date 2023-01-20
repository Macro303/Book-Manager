__all__ = ["UserController"]

import logging

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import User
from book_catalogue.schemas._user import NewUser

LOGGER = logging.getLogger(__name__)


class UserController:
    @classmethod
    def list_users(cls) -> list[User]:
        return User.select()

    @classmethod
    def create_user(cls, new_user: NewUser) -> User:
        if User.get(username=new_user.username):
            raise HTTPException(status_code=409, detail="User already exists.")
        user = User(username=new_user.username, role=new_user.role)
        flush()
        return user

    @classmethod
    def get_user(cls, user_id: int) -> User:
        if user := User.get(user_id=user_id):
            return user
        raise HTTPException(status_code=404, detail="User not found.")

    @classmethod
    def update_user(cls, user_id: int, updates: NewUser) -> User:
        user = cls.get_user(user_id=user_id)
        user.role = updates.role
        user.username = updates.username
        flush()
        return user

    @classmethod
    def delete_user(cls, user_id: int):
        user = cls.get_user(user_id=user_id)
        user.delete()
        
    @classmethod
    def get_user_by_username(cls, username: str):
        if user := User.get(username=username):
            return user
        raise HTTPException(status_code=404, detail="User not found.")
