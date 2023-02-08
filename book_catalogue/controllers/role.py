__all__ = ["RoleController"]

from fastapi import HTTPException
from pony.orm import flush

from book_catalogue.database.tables import Role
from book_catalogue.schemas.role import RoleWrite


class RoleController:
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
    def update_role(cls, role_id: int, updates: RoleWrite):
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
