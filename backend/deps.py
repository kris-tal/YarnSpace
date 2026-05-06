from __future__ import annotations

from typing import Optional, Annotated

from fastapi import Depends
from fastapi.security import OAuth2PasswordBearer

from security import decode_access_token


oauth2_required = OAuth2PasswordBearer(tokenUrl="/auth/login")
oauth2_optional = OAuth2PasswordBearer(tokenUrl="/auth/login", auto_error=False)


def get_current_user_id(token: Annotated[str, Depends(oauth2_required)]) -> int:
    return decode_access_token(token)


def get_optional_current_user_id(token: Annotated[Optional[str], Depends(oauth2_optional)]) -> Optional[int]:
    if not token:
        return None
    try:
        return decode_access_token(token)
    except Exception:
        return None
