__all__ = ["GenreController"]

from fastapi import HTTPException
from pony.orm import flush

from bookshelf.database.tables import Genre
from bookshelf.models.genre import GenreIn


class GenreController:
    @classmethod
    def list_genres(cls) -> list[Genre]:
        return Genre.select()

    @classmethod
    def create_genre(cls, new_genre: GenreIn) -> Genre:
        if Genre.get(name=new_genre.name):
            raise HTTPException(status_code=409, detail="Genre already exists.")
        genre = Genre(name=new_genre.name)
        flush()
        return genre

    @classmethod
    def get_genre(cls, genre_id: int) -> Genre:
        if genre := Genre.get(genre_id=genre_id):
            return genre
        raise HTTPException(status_code=404, detail="Genre not found.")

    @classmethod
    def update_genre(cls, genre_id: int, updates: GenreIn):
        genre = cls.get_genre(genre_id=genre_id)
        genre.name = updates.name
        flush()
        return genre

    @classmethod
    def delete_genre(cls, genre_id: int):
        genre = cls.get_genre(genre_id=genre_id)
        genre.delete()

    @classmethod
    def get_genre_by_name(cls, name: str) -> Genre:
        if genre := Genre.get(name=name):
            return genre
        raise HTTPException(status_code=404, detail="Genre not found.")
