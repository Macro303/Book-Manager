from __future__ import annotations

__all__ = ["ErrorResponse"]

from datetime import datetime

from pydantic import BaseModel


class ErrorResponse(BaseModel):
    timestamp: datetime
    status: str
    reason: str
