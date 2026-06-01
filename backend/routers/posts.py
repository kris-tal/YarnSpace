from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends, status
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
    post = crud.create_post(
        db,
        author_id=user_id,
        content=payload.content,
        image_url=payload.imageUrl,
        reblogged_project_id=payload.rebloggedProjectId
    )
    post = crud.get_post(db, post.id)
    return post

@router.delete("/{post_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_post_endpoint(post_id: int, user_id: CurrentUserId, db: DbDep):
    crud.delete_post(db=db, post_id=post_id, current_user_id=user_id)
    return None


@router.get("/{post_id}", response_model=PostReadDTO)
def get_post(post_id: int, db: DbDep):
    return crud.get_post(db, post_id)
