from __future__ import annotations

__all__ = ["AuthorController"]

import logging

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import Author, Role
from book_catalogue.schemas._author import NewAuthor, NewAuthorIdentifiers, NewRole
from book_catalogue.services.open_library.service import OpenLibrary

LOGGER = logging.getLogger(__name__)


class AuthorController:
    @classmethod
    def list_authors(cls) -> list[Author]:
        return Author.select()

    @classmethod
    def create_author(cls, new_author: NewAuthor) -> Author:
        if Author.get(name=new_author.name) or (
            new_author.identifiers.open_library_id
            and Author.get(open_library_id=new_author.identifiers.open_library_id)
        ):
            raise HTTPException(status_code=409, detail="Author already exists.")
        author = Author(
            bio=new_author.bio,
            image_url=new_author.image_url,
            name=new_author.name,
            amazon_id=new_author.identifiers.amazon_id,
            goodreads_id=new_author.identifiers.goodreads_id,
            library_thing_id=new_author.identifiers.library_thing_id,
            open_library_id=new_author.identifiers.open_library_id,
        )
        flush()
        return author

    @classmethod
    def get_author(cls, author_id: int) -> Author:
        if author := Author.get(author_id=author_id):
            return author
        raise HTTPException(status_code=404, detail="Author not found.")

    @classmethod
    def update_author(cls, author_id: int, updates: NewAuthor) -> Author:
        author = cls.get_author(author_id=author_id)
        author.bio = updates.bio
        author.image_url = updates.image_url
        author.name = updates.name

        author.amazon_id = updates.identifiers.amazon_id
        author.goodreads_id = updates.identifiers.goodreads_id
        author.library_thing_id = updates.identifiers.library_thing_id
        author.open_library_id = updates.identifiers.open_library_id
        flush()
        return author

    @classmethod
    def delete_author(cls, author_id: int):
        author = cls.get_author(author_id=author_id)
        author.delete()

    @classmethod
    def get_author_by_name(cls, name: str) -> Author:
        if author := Author.get(name=name):
            return author
        raise HTTPException(status_code=404, detail="Author not found.")

    @classmethod
    def _parse_open_library(cls, open_library_id: str) -> NewAuthor:
        session = OpenLibrary(cache=None)
        result = session.get_author(author_id=open_library_id)

        return NewAuthor(
            bio=result.get_bio(),
            identifiers=NewAuthorIdentifiers(
                amazon_id=result.remote_ids.amazon,
                goodreads_id=result.remote_ids.goodreads,
                library_thing_id=result.remote_ids.librarything,
                open_library_id=open_library_id,
            ),
            # TODO image_url
            name=result.name,
        )

    @classmethod
    def lookup_author(cls, open_library_id: str) -> Author:
        if author := Author.get(open_library_id=open_library_id):
            return author

        new_author = cls._parse_open_library(open_library_id=open_library_id)
        if author := Author.get(name=new_author.name):
            author.open_library_id = new_author.open_library_id
            flush()
            return author
        return cls.create_author(new_author=new_author)

    @classmethod
    def reset_author(cls, author_id: int) -> Author:
        if not (author := cls.get_author(author_id=author_id)):
            raise HTTPException(status_code=404, detail="Author not found.")
        if not author.open_library_id:
            raise HTTPException(status_code=400, detail="Author doesn't have an OpenLibrary Id.")

        updates = cls._parse_open_library(open_library_id=author.open_library_id)
        updates.image_url = author.image_url
        return cls.update_author(author_id=author_id, updates=updates)

    @classmethod
    def list_roles(cls) -> list[Role]:
        return Role.select()

    @classmethod
    def create_role(cls, new_role: NewRole) -> Role:
        if Role.get(name=new_role.name):
            raise HTTPException(status_code=409, detail="Role already exists.")
        role = Role(name=new_role.name)
        flush()
        return role

    @classmethod
    def get_role(cls, role_id: int) -> Role:
        if role := Role.get(role_id=role_id):
            return role
        raise HTTPException(status_code=404, detail="Role not found.")

    @classmethod
    def update_role(cls, role_id: int, updates: NewRole) -> Role:
        role = cls.get_role(role_id=role_id)
        role.name = updates.name
        flush()
        return role

    @classmethod
    def delete_role(cls, role_id: int):
        role = cls.get_role(role_id=role_id)
        role.delete()

    @classmethod
    def get_role_by_name(cls, name: str) -> Role:
        if role := Role.get(name=name):
            return role
        raise HTTPException(status_code=404, detail="Role not found.")
