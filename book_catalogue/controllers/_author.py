__all__ = ["AuthorController"]

import logging

from book_catalogue.services.open_library.service import OpenLibrary
from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import Author, Role

LOGGER = logging.getLogger(__name__)


class AuthorController:
    @staticmethod
    def list_authors() -> list[Author]:
        return Author.select()

    @staticmethod
    def create_author(name: str) -> Author:
        if Author.get(name=name):
            raise HTTPException(status_code=409, detail="Author already exists.")
        author = Author(name=name)
        flush()
        return author

    @staticmethod
    def get_author(author_id: int) -> Author | None:
        if author := Author.get(author_id=author_id):
            return author
        return None

    @staticmethod
    def delete_author(author_id: int):
        if author := Author.get(author_id=author_id):
            return author.delete()
        raise HTTPException(status_code=404, detail="Author not found.")

    @staticmethod
    def get_author_by_name(name: str) -> Author | None:
        if author := Author.get(name=name):
            return author
        return None

    @staticmethod
    def lookup_author(open_library_id: str) -> Author:
        if author := Author.get(open_library_id=open_library_id):
            return author
        session = OpenLibrary(cache=None)
        result = session.get_author(author_id=open_library_id)
        if author := Author.get(name=result.name):
            author.open_library_id = open_library_id
            return author
        return Author(name=result.name, open_library_id=open_library_id)

    @staticmethod
    def list_roles() -> list[Role]:
        return Role.select()

    @staticmethod
    def create_role(name: str) -> Role:
        if Role.get(name=name):
            raise HTTPException(status_code=409, detail="Role already exists.")
        role = Role(name=name)
        flush()
        return role

    @staticmethod
    def get_role(role_id: int) -> Role | None:
        if role := Role.get(role_id=role_id):
            return role
        return None

    @staticmethod
    def delete_role(role_id: int):
        if role := Role.get(role_id=role_id):
            return role.delete()
        raise HTTPException(status_code=404, detail="Role not found.")

    @staticmethod
    def get_role_by_name(name: str) -> Role | None:
        if role := Role.get(name=name):
            return role
        return None
