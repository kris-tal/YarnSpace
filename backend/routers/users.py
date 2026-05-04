from __future__ import annotations

from typing import Annotated, List

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

import crud
from db import get_db
from deps import get_current_user_id, get_optional_user_id
from schemas import AccentColorUpdateDTO, UserPrivateDTO, ProfilePublicDTO, PostReadDTO, ProjectReadDTO, UserPublicDTO

router = APIRouter(prefix="/users", tags=["users"])

DbDep = Annotated[Session, Depends(get_db)]
CurrentUserId = Annotated[int, Depends(get_current_user_id)]
OptionalUserId = Annotated[int | None, Depends(get_optional_user_id)]


@router.get("/me", response_model=UserPrivateDTO)
def get_me(user_id: CurrentUserId, db: DbDep):
    user = crud.get_user_by_id(db, user_id)
    return user


@router.patch("/me/accent-color", response_model=UserPrivateDTO)
def update_my_accent_color(
    payload: AccentColorUpdateDTO,
    user_id: CurrentUserId,
    db: DbDep,
):
    user = crud.get_user_by_id(db, user_id)
    user.accent_color = payload.accentColor
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


@router.get("/search", response_model=List[UserPublicDTO])
def search_users(
    db: DbDep,
    q: str = Query(..., min_length=1),
    limit: int = 20
):
    return crud.search_users(db, query=q, limit=limit)


@router.get("/{username}", response_model=ProfilePublicDTO)
def get_public_profile(
    username: str,
    viewer_id: OptionalUserId,
    db: DbDep,
):
    user = crud.get_user_by_username(db, username)
    counts = crud.get_profile_counts(db, user.id)

    is_followed_by_me = None
    if viewer_id is not None:
        is_followed_by_me = crud.is_following(db, follower_id=viewer_id, followee_id=user.id)

    return {
        **user.__dict__,
        **counts,
        "isFollowedByMe": is_followed_by_me,
    }


@router.get("/{username}/posts", response_model=list[PostReadDTO])
def list_user_posts(
    username: str,
    db: DbDep,
    limit: int = 20,
    offset: int = 0,
):
    user = crud.get_user_by_username(db, username)
    posts = crud.list_posts_by_user(db, user_id=user.id, limit=limit, offset=offset)
    return posts


@router.get("/{username}/projects", response_model=list[ProjectReadDTO])
def list_user_projects(
    username: str,
    db: DbDep,
    limit: int = 20,
    offset: int = 0,
):
    user = crud.get_user_by_username(db, username)
    projects = crud.list_projects_by_user(db, user_id=user.id, limit=limit, offset=offset)
    return projects


@router.get("/me/saved-projects", response_model=list[ProjectReadDTO])
def list_my_saved_projects(
    user_id: CurrentUserId,
    db: DbDep,
    limit: int = 20,
    offset: int = 0,
):
    projects = crud.list_saved_projects(db, user_id=user_id, limit=limit, offset=offset)
    return projects


@router.post("/{username}/follow")
def follow(
    username: str,
    user_id: CurrentUserId,
    db: DbDep,
):
    followee = crud.get_user_by_username(db, username)
    crud.follow_user(db, follower_id=user_id, followee_id=followee.id)
    return {"status": "ok"}


@router.delete("/{username}/follow")
def unfollow(
    username: str,
    user_id: CurrentUserId,
    db: DbDep,
):
    followee = crud.get_user_by_username(db, username)
    crud.unfollow_user(db, follower_id=user_id, followee_id=followee.id)
    return {"status": "ok"}
