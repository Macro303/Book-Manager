__all__ = ["UserController"]

from fastapi import HTTPException
from pony.orm import flush

from bookshelf.database.tables import User
from bookshelf.models.user import UserIn


class UserController:
    @classmethod
    def list_users(cls) -> list[User]:
        return User.select()

    @classmethod
    def create_user(cls, new_user: UserIn) -> User:
        if User.get(username=new_user.username):
            raise HTTPException(status_code=409, detail="User already exists.")
        user = User(username=new_user.username, role=new_user.role, image_url=new_user.image_url)
        flush()
        return user

    @classmethod
    def get_user(cls, user_id: int) -> User:
        if user := User.get(user_id=user_id):
            return user
        raise HTTPException(status_code=404, detail="User not found.")

    @classmethod
    def update_user(cls, user_id: int, updates: UserIn) -> User:
        user = cls.get_user(user_id=user_id)
        user.image_url = updates.image_url
        user.role = updates.role
        user.username = updates.username
        flush()
        return user

    @classmethod
    def delete_user(cls, user_id: int):
        user = cls.get_user(user_id=user_id)
        user.delete()
