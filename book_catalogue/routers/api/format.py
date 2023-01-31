from __future__ import annotations

__all__ = ["router"]

from fastapi import APIRouter
from pony.orm import db_session

from book_catalogue.controllers.format import FormatController
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas.format import FormatRead, FormatWrite

router = APIRouter(prefix="/formats", tags=["Formats"])


@router.get(path="")
def list_formats() -> list[FormatRead]:
    with db_session:
        return sorted({x.to_schema() for x in FormatController.list_formats()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_format(new_format: FormatWrite) -> FormatRead:
    with db_session:
        return FormatController.create_format(new_format=new_format).to_schema()


@router.get(path="/{format_id}", responses={404: {"model": ErrorResponse}})
def get_format(format_id: int) -> FormatRead:
    with db_session:
        return FormatController.get_format(format_id=format_id).to_schema()


@router.patch(path="/{format_id}", responses={404: {"model": ErrorResponse}})
def update_format(format_id: int, updates: FormatWrite) -> FormatRead:
    with db_session:
        return FormatController.update_format(format_id=format_id, updates=updates).to_schema()


@router.delete(path="/{format_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_format(format_id: int):
    with db_session:
        FormatController.delete_format(format_id=format_id)
