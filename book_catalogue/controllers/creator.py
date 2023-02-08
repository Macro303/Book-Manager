__all__ = ["CreatorController"]

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import Creator
from book_catalogue.schemas.creator import CreatorWrite
from book_catalogue.settings import Settings


class CreatorController:
    @classmethod
    def list_creators(cls) -> list[Creator]:
        return Creator.select()

    @classmethod
    def create_creator(cls, new_creator: CreatorWrite) -> Creator:
        if Creator.get(name=new_creator.name) or (
            new_creator.identifiers.open_library_id
            and Creator.get(open_library_id=new_creator.identifiers.open_library_id)
        ):
            raise HTTPException(status_code=409, detail="Creator already exists.")
        creator = Creator(
            bio=new_creator.bio,
            image_url=new_creator.image_url,
            name=new_creator.name,
            goodreads_id=new_creator.identifiers.goodreads_id,
            library_thing_id=new_creator.identifiers.library_thing_id,
            open_library_id=new_creator.identifiers.open_library_id,
        )
        flush()
        return creator

    @classmethod
    def get_creator(cls, creator_id: int) -> Creator:
        if creator := Creator.get(creator_id=creator_id):
            return creator
        raise HTTPException(status_code=404, detail="Creator not found.")

    @classmethod
    def update_creator(cls, creator_id: int, updates: CreatorWrite) -> Creator:
        creator = cls.get_creator(creator_id=creator_id)
        creator.bio = updates.bio
        creator.image_url = updates.image_url
        creator.name = updates.name

        creator.goodreads_id = updates.identifiers.goodreads_id
        creator.library_thing_id = updates.identifiers.library_thing_id
        creator.open_library_id = updates.identifiers.open_library_id
        flush()
        return creator

    @classmethod
    def delete_creator(cls, creator_id: int):
        creator = cls.get_creator(creator_id=creator_id)
        creator.delete()

    @classmethod
    def get_creator_by_name(cls, name: str) -> Creator:
        if creator := Creator.get(name=name):
            return creator
        raise HTTPException(status_code=404, detail="Creator not found.")

    @classmethod
    def lookup_creator(cls, open_library_id: str) -> Creator:
        if creator := Creator.get(open_library_id=open_library_id):
            return creator

        from book_catalogue.services import open_library

        settings = Settings.load()
        if settings.source.open_library:
            new_creator = open_library.lookup_creator(open_library_id=open_library_id)
            if creator := Creator.get(name=new_creator.name):
                return cls.update_creator(creator_id=creator.creator_id, updates=new_creator)
        elif settings.source.google_books:
            raise HTTPException(
                status_code=400, detail="No creator lookup available for GoogleBooks."
            )
        else:  # noqa: RET506
            raise HTTPException(
                status_code=500, detail="Incorrect config setup, review source settings."
            )
        return cls.create_creator(new_creator=new_creator)

    @classmethod
    def reset_creator(cls, creator_id: int) -> Creator:
        if not (creator := cls.get_creator(creator_id=creator_id)):
            raise HTTPException(status_code=404, detail="Creator not found.")
        if not creator.open_library_id:
            raise HTTPException(status_code=400, detail="Creator doesn't have an OpenLibrary Id.")

        from book_catalogue.services import open_library

        settings = Settings.load()
        if settings.source.open_library:
            updates = open_library.lookup_creator(open_library_id=creator.open_library_id)
        elif settings.source.google_books:
            raise HTTPException(
                status_code=400, detail="No creator lookup available for GoogleBooks."
            )
        else:  # noqa: RET506
            raise HTTPException(
                status_code=500, detail="Incorrect config setup, review source settings."
            )
        return cls.update_creator(creator_id=creator_id, updates=updates)
