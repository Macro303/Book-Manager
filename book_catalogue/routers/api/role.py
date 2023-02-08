__all__ = ["router"]

from fastapi import APIRouter
from pony.orm import db_session

from book_catalogue.controllers.role import RoleController
from book_catalogue.responses import ErrorResponse
from book_catalogue.schemas.role import RoleRead, RoleWrite

router = APIRouter(prefix="/roles", tags=["Roles"])


@router.get(path="")
def list_roles() -> list[RoleRead]:
    with db_session:
        return sorted({x.to_schema() for x in RoleController.list_roles()})


@router.post(path="", status_code=201, responses={409: {"model": ErrorResponse}})
def create_role(new_role: RoleWrite) -> RoleRead:
    with db_session:
        return RoleController.create_role(new_role=new_role).to_schema()


@router.get(path="/{role_id}", responses={404: {"model": ErrorResponse}})
def get_role(role_id: int) -> RoleRead:
    with db_session:
        return RoleController.get_role(role_id=role_id).to_schema()


@router.patch(path="/{role_id}", responses={404: {"model": ErrorResponse}})
def update_role(role_id: int, updates: RoleWrite) -> RoleRead:
    with db_session:
        return RoleController.update_role(role_id=role_id, updates=updates).to_schema()


@router.delete(path="/{role_id}", status_code=204, responses={404: {"model": ErrorResponse}})
def delete_role(role_id: int):
    with db_session:
        RoleController.delete_role(role_id=role_id)
