__all__ = ["router"]

from fastapi import APIRouter
from pony.orm import db_session

from bookshelf.controllers.format import FormatController
from bookshelf.models.format import Format, FormatIn
from bookshelf.responses import ErrorResponse

router = APIRouter(prefix="/formats", tags=["Formats"])


@router.get(path="")
def list_formats() -> list[Format]:
    with db_session:
        return sorted({x.to_model() for x in FormatController.list_formats()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_format(new_format: FormatIn) -> Format:
    with db_session:
        return FormatController.create_format(new_format=new_format).to_model()


@router.get(path="/{format_id}", responses={404: {"model": ErrorResponse}})
def get_format(format_id: int) -> Format:
    with db_session:
        return FormatController.get_format(format_id=format_id).to_model()


@router.patch(path="/{format_id}", responses={404: {"model": ErrorResponse}})
def update_format(format_id: int, updates: FormatIn) -> Format:
    with db_session:
        return FormatController.update_format(format_id=format_id, updates=updates).to_model()


@router.delete(path="/{format_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_format(format_id: int):
    with db_session:
        FormatController.delete_format(format_id=format_id)
