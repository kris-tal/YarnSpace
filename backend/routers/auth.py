from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session

import crud
from db import get_db
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
        display_name=payload.display_name,
        accent_color=payload.accentColor,
        password_hash=hash_password(payload.password),
        avatar_url=payload.avatarUrl,
    )

    token = create_access_token(user_id=user.id)
    return {
        "accessToken": token,
        "tokenType": "bearer",
        "user": user,
    }


@router.post("/login", response_model=AuthResponseDTO)
def login(form: Annotated[OAuth2PasswordRequestForm, Depends()], db: DbDep):
    user = crud.authenticate_user(db, identifier=form.username, password=form.password)
    token = create_access_token(user_id=user.id)
    return {
        "accessToken": token,
        "tokenType": "bearer",
        "user": user,
    }
