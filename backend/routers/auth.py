from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session

import crud
from db import get_db
from deps import get_optional_current_user_id
from schemas import AuthResponseDTO, UserRegisterDTO
from security import create_access_token, hash_password

router = APIRouter(prefix="/auth", tags=["auth"])

DbDep = Annotated[Session, Depends(get_db)]


@router.post("/register", response_model=AuthResponseDTO)
def register(payload: UserRegisterDTO, db: DbDep):
    user = crud.create_user(
        db,
        username=payload.username,
        email=payload.email,
        display_name=payload.displayName,
        accent_color=payload.accentColor,
        password_hash=hash_password(payload.password),
        avatar_icon=payload.avatarIcon,
    )
    token = create_access_token(user_id=user.id)

    return {
        "accessToken": token,
        "tokenType": "bearer",
        "user": user,
    }


@router.post("/login", response_model=AuthResponseDTO)
def login(form: Annotated[OAuth2PasswordRequestForm, Depends()], db: DbDep):
    user = crud.authenticate_user(
        db,
        identifier=form.username,
        password=form.password
    )
    token = create_access_token(user_id=user.id)

    return {
        "accessToken": token,
        "tokenType": "bearer",
        "user": user,
    }


@router.post("/logout")
def logout(user_id: Annotated[int | None, Depends(get_optional_current_user_id)]):
    # frontend has to deal with deleting the token
    return {"status": "ok"}

