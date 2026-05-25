from __future__ import annotations

import shutil
import uuid
from pathlib import Path
from typing import Annotated, List

from fastapi import APIRouter, Depends, Query, UploadFile, File, HTTPException, Request
from sqlalchemy.orm import Session

import crud
from db import get_db
from deps import get_current_user_id, get_optional_current_user_id
from schemas import (
    AccentColorUpdateDTO,
    ProfileUpdateDTO,
    UserPrivateDTO,
    ProfilePublicDTO,
    PostReadDTO,
    ProjectReadDTO,
    UserPublicDTO,
)

router = APIRouter(prefix="/users", tags=["users"])

DbDep = Annotated[Session, Depends(get_db)]
CurrentUserId = Annotated[int, Depends(get_current_user_id)]
OptionalUserId = Annotated[int | None, Depends(get_optional_current_user_id)]


def _attach_extra_to_project(db: Session, project_obj: crud.models.Project, current_user_id: int | None):
    dto = ProjectReadDTO.model_validate(project_obj)
    if current_user_id:
        dto.isSavedByMe = crud.is_project_saved_by_user(db, user_id=current_user_id, project_id=project_obj.id)
        dto.isRebloggedByMe = crud.is_project_reblogged_by_user(db, user_id=current_user_id, project_id=project_obj.id)
    else:
        dto.isSavedByMe = False
        dto.isRebloggedByMe = False
    return dto


def _attach_extra_to_post(db: Session, post_obj: crud.models.Post, current_user_id: int | None):
    dto = PostReadDTO.model_validate(post_obj)
    if dto.rebloggedProject and post_obj.reblogged_project_id:
        if current_user_id:
            dto.rebloggedProject.isSavedByMe = crud.is_project_saved_by_user(
                db, user_id=current_user_id, project_id=post_obj.reblogged_project_id
            )
            dto.rebloggedProject.isRebloggedByMe = crud.is_project_reblogged_by_user(
                db, user_id=current_user_id, project_id=post_obj.reblogged_project_id
            )
        else:
            dto.rebloggedProject.isSavedByMe = False
            dto.rebloggedProject.isRebloggedByMe = False
    return dto


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


@router.patch("/me", response_model=UserPrivateDTO)
def update_me(
    payload: ProfileUpdateDTO,
    user_id: CurrentUserId,
    db: DbDep,
):
    user = crud.get_user_by_id(db, user_id)

    if payload.display_name is not None:
        user.display_name = payload.display_name
    if payload.accentColor is not None:
        user.accent_color = payload.accentColor.value
    if payload.avatarUrl is not None:
        user.avatar_url = payload.avatarUrl

    db.add(user)
    db.commit()
    db.refresh(user)
    return user


@router.post("/me/avatar")
def upload_my_avatar(
    user_id: CurrentUserId,
    db: DbDep,
    request: Request,
    file: UploadFile = File(...),
):
    allowed_types = {"image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp"}
    if file.content_type not in allowed_types:
        raise HTTPException(status_code=400, detail="Unsupported image type")

    avatars_dir = Path(__file__).resolve().parent.parent / "static" / "avatars"
    avatars_dir.mkdir(parents=True, exist_ok=True)

    ext = allowed_types[file.content_type]
    filename = f"{uuid.uuid4().hex}{ext}"
    dest_path = avatars_dir / filename

    try:
        with dest_path.open("wb") as out:
            shutil.copyfileobj(file.file, out)
    finally:
        file.file.close()

    user = crud.get_user_by_id(db, user_id)
    old = user.avatar_url
    user.avatar_url = None
    db.add(user)
    db.commit()

    rel_url = f"/static/avatars/{filename}"
    base_url = str(request.base_url).rstrip("/")
    avatar_url = f"{base_url}{rel_url}"

    user.avatar_url = avatar_url
    db.add(user)
    db.commit()
    db.refresh(user)

    if old and "/static/avatars/" in old:
        try:
            old_name = old.split("/static/avatars/")[-1]
            old_path = avatars_dir / old_name
            if old_path.exists():
                old_path.unlink()
        except Exception:
            pass

    return {"avatarUrl": avatar_url}


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
    viewer_id: OptionalUserId,
    db: DbDep,
    limit: int = 20,
    offset: int = 0,
):
    user = crud.get_user_by_username(db, username)
    posts = crud.list_posts_by_user(db, user_id=user.id, limit=limit, offset=offset)
    return [_attach_extra_to_post(db, p, viewer_id) for p in posts]


@router.get("/{username}/projects", response_model=list[ProjectReadDTO])
def list_user_projects(
    username: str,
    viewer_id: OptionalUserId,
    db: DbDep,
    limit: int = 20,
    offset: int = 0,
):
    user = crud.get_user_by_username(db, username)
    projects = crud.list_projects_by_user(db, user_id=user.id, limit=limit, offset=offset)
    return [_attach_extra_to_project(db, p, viewer_id) for p in projects]


@router.get("/me/saved-projects", response_model=list[ProjectReadDTO])
def list_my_saved_projects(
    user_id: CurrentUserId,
    db: DbDep,
    limit: int = 20,
    offset: int = 0,
):
    projects = crud.list_saved_projects(db, user_id=user_id, limit=limit, offset=offset)
    result = []
    for p in projects:
        dto = _attach_extra_to_project(db, p, user_id)
        dto.isSavedByMe = True
        result.append(dto)
    return result


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
