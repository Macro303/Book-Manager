__all__ = ["AuthorController"]

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import Author, Role
from book_catalogue.schemas.author import AuthorWrite, RoleWrite
from book_catalogue.settings import Settings


class AuthorController:
    @classmethod
    def list_authors(cls) -> list[Author]:
        return Author.select()

    @classmethod
    def create_author(cls, new_author: AuthorWrite) -> Author:
        if Author.get(name=new_author.name) or (
            new_author.identifiers.open_library_id
            and Author.get(open_library_id=new_author.identifiers.open_library_id)
        ):
            raise HTTPException(status_code=409, detail="Author already exists.")
        author = Author(
            bio=new_author.bio,
            image_url=new_author.image_url,
            name=new_author.name,
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
    def update_author(cls, author_id: int, updates: AuthorWrite) -> Author:
        author = cls.get_author(author_id=author_id)
        author.bio = updates.bio
        author.image_url = updates.image_url
        author.name = updates.name

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
    def lookup_author(cls, open_library_id: str) -> Author:
        if author := Author.get(open_library_id=open_library_id):
            return author

        from book_catalogue.services import open_library

        settings = Settings.load()
        if settings.source.open_library:
            new_author = open_library.lookup_author(open_library_id=open_library_id)
            if author := Author.get(name=new_author.name):
                return cls.update_author(author_id=author.author_id, updates=new_author)
        elif settings.source.google_books:
            raise HTTPException(
                status_code=400, detail="No author lookup available for GoogleBooks."
            )
        else:  # noqa: RET506
            raise HTTPException(
                status_code=500, detail="Incorrect config setup, review source settings."
            )
        return cls.create_author(new_author=new_author)

    @classmethod
    def reset_author(cls, author_id: int) -> Author:
        if not (author := cls.get_author(author_id=author_id)):
            raise HTTPException(status_code=404, detail="Author not found.")
        if not author.open_library_id:
            raise HTTPException(status_code=400, detail="Author doesn't have an OpenLibrary Id.")

        from book_catalogue.services import open_library

        settings = Settings.load()
        if settings.source.open_library:
            updates = open_library.lookup_author(open_library_id=author.open_library_id)
        elif settings.source.google_books:
            raise HTTPException(
                status_code=400, detail="No author lookup available for GoogleBooks."
            )
        else:  # noqa: RET506
            raise HTTPException(
                status_code=500, detail="Incorrect config setup, review source settings."
            )
        return cls.update_author(author_id=author_id, updates=updates)

    @classmethod
    def list_roles(cls) -> list[Role]:
        return Role.select()

    @classmethod
    def create_role(cls, new_role: RoleWrite) -> Role:
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
    def update_role(cls, role_id: int, updates: RoleWrite) -> Role:
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
