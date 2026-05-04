from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

import crud
from db import get_db
from deps import get_current_user_id
from schemas import PostCreateDTO, PostReadDTO

router = APIRouter(prefix="/posts", tags=["posts"])

DbDep = Annotated[Session, Depends(get_db)]
CurrentUserId = Annotated[int, Depends(get_current_user_id)]


@router.post("/", response_model=PostReadDTO)
def create_post(
    payload: PostCreateDTO,
    user_id: CurrentUserId,
    db: DbDep,
):
    post = crud.create_post(db, author_id=user_id, content=payload.content, image_url=payload.imageUrl)
    post = crud.get_post(db, post.id)
    return post


@router.get("/{post_id}", response_model=PostReadDTO)
def get_post(post_id: int, db: DbDep):
    return crud.get_post(db, post_id)



