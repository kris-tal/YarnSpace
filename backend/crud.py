from __future__ import annotations

from typing import List, Optional

from fastapi import HTTPException, status
from sqlalchemy import select, func, delete, case
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, joinedload

import models
from security import verify_password


# ===========================================================
#                           USERS
# ===========================================================


def create_user(
    db: Session,
    *,
    username: str,
    email: str,
    display_name: str,
    accent_color: str,
    password_hash: str,
    avatar_url: Optional[str] = None
) -> models.User:

    user = models.User(
        username=username,
        email=email,
        display_name=display_name,
        accent_color=accent_color,
        password_hash=password_hash,
        avatar_url=avatar_url,
    )
    db.add(user)

    try:
        db.commit()
    except IntegrityError as e:
        db.rollback()
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Username or email already exists") from e

    db.refresh(user)
    return user


def get_user_by_id(db: Session, user_id: int) -> models.User:
    user = db.get(models.User, user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
    return user


def get_user_by_username(db: Session, username: str) -> models.User:
    stmt = select(models.User).where(models.User.username == username)
    user = db.execute(stmt).scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
    return user


def get_user_by_email(db: Session, email: str) -> models.User:
    stmt = select(models.User).where(models.User.email == email)
    user = db.execute(stmt).scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
    return user


def get_user_by_identifier(db: Session, identifier: str) -> models.User:
    # identifier = username | email
    if "@" in identifier:
        return get_user_by_email(db, identifier)
    return get_user_by_username(db, identifier)


def authenticate_user(db: Session, *, identifier: str, password: str) -> models.User:
    try:
        user = get_user_by_identifier(db, identifier)
    except HTTPException as e:
        if e.status_code == status.HTTP_404_NOT_FOUND:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid credentials"
            )
        raise e

    if not verify_password(password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials"
        )
    return user


def search_users(db: Session, query: str, limit: int = 20) -> List[models.User]:
    stmt = (
        select(models.User)
        .where(models.User.username.ilike(f"{query}%"))
        .order_by(models.User.username.asc())
        .limit(limit)
    )
    return list(db.execute(stmt).scalars().all())


def get_profile_counts(db: Session, user_id: int) -> dict:
    followers_count = db.execute(
        select(func.count()).select_from(models.Follow).where(models.Follow.followee_id == user_id)
    ).scalar_one()
    following_count = db.execute(
        select(func.count()).select_from(models.Follow).where(models.Follow.follower_id == user_id)
    ).scalar_one()
    posts_count = db.execute(select(func.count()).select_from(models.Post).where(models.Post.author_id == user_id)).scalar_one()
    projects_count = db.execute(
        select(func.count()).select_from(models.Project).where(models.Project.author_id == user_id)
    ).scalar_one()
    saved_projects_count = db.execute(
        select(func.count()).select_from(models.SavedProject).where(models.SavedProject.user_id == user_id)
    ).scalar_one()

    return {
        "followersCount": int(followers_count),
        "followingCount": int(following_count),
        "postsCount": int(posts_count),
        "projectsCount": int(projects_count),
        "savedProjectsCount": int(saved_projects_count),
    }


def is_following(db: Session, *, follower_id: int, followee_id: int) -> bool:
    stmt = select(models.Follow).where(
        models.Follow.follower_id == follower_id,
        models.Follow.followee_id == followee_id,
    )
    return db.execute(stmt).scalar_one_or_none() is not None


# ===========================================================
#                           POSTS
# ===========================================================


def create_post(db: Session, *, author_id: int, content: Optional[str], image_url: Optional[str], reblogged_project_id: Optional[int] = None) -> models.Post:
    get_user_by_id(db, author_id)
    post = models.Post(author_id=author_id, content=content, image_url=image_url, reblogged_project_id=reblogged_project_id)
    db.add(post)
    db.commit()
    db.refresh(post)

    db.refresh(post, attribute_names=["author"])
    if post.reblogged_project_id:
        db.refresh(post, attribute_names=["reblogged_project"])
    return post


def list_posts_by_user(db: Session, *, user_id: Optional[int], limit: int = 20, offset: int = 0) -> List[models.Post]:
    stmt = (
        select(models.Post)
        .options(joinedload(models.Post.author), joinedload(models.Post.reblogged_project).joinedload(models.Project.author))
    )
    if user_id is not None:
        stmt = stmt.where(models.Post.author_id == user_id)

    stmt = stmt.order_by(models.Post.created_at.desc()).limit(limit).offset(offset)
    return list(db.execute(stmt).scalars().all())


def get_post(db: Session, post_id: int) -> models.Post:
    stmt = select(models.Post).options(joinedload(models.Post.author), joinedload(models.Post.reblogged_project).joinedload(models.Project.author)).where(models.Post.id == post_id)
    post = db.execute(stmt).scalar_one_or_none()
    if not post:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Post not found")
    return post


# ===========================================================
#                          PROJECTS
# ===========================================================


def create_project(db: Session, *, author_id: int, data: dict) -> models.Project:
    get_user_by_id(db, author_id)
    project = models.Project(author_id=author_id, **data)
    db.add(project)
    db.commit()
    db.refresh(project)
    db.refresh(project, attribute_names=["author"])
    return project


def list_projects_by_user(db: Session, *, user_id: Optional[int], limit: int = 20, offset: int = 0) -> List[models.Project]:
    stmt = select(models.Project).options(joinedload(models.Project.author))
    if user_id is not None:
        stmt = stmt.where(models.Project.author_id == user_id)

    stmt = stmt.order_by(models.Project.created_at.desc()).limit(limit).offset(offset)
    return list(db.execute(stmt).scalars().all())


def _escape_like(value: str) -> str:
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") # so % and _ are treated literally


def search_projects(db: Session, *, query: str, limit: int = 20, offset: int = 0) -> List[models.Project]:
    # prefix first then substring

    q = query.strip()
    if not q:
        return []

    escaped = _escape_like(q)
    prefix_pat = f"{escaped}%"
    substr_pat = f"%{escaped}%"

    score = case(
        (models.Project.title.ilike(prefix_pat, escape="\\"), 2),
        (models.Project.title.ilike(substr_pat, escape="\\"), 1),
        else_=0,
    )

    stmt = (
        select(models.Project)
        .options(joinedload(models.Project.author))
        .where(models.Project.title.ilike(substr_pat, escape="\\"))
        .order_by(score.desc(), func.length(models.Project.title).asc(), models.Project.created_at.desc())
        .limit(limit)
        .offset(offset)
    )
    return list(db.execute(stmt).scalars().all())


def get_project(db: Session, project_id: int) -> models.Project:
    stmt = select(models.Project).options(joinedload(models.Project.author)).where(models.Project.id == project_id)
    project = db.execute(stmt).scalar_one_or_none()
    if not project:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Project not found")
    return project


def list_saved_projects(db: Session, *, user_id: int, limit: int = 20, offset: int = 0) -> List[models.Project]:
    stmt = (
        select(models.Project)
        .join(models.SavedProject, models.SavedProject.project_id == models.Project.id)
        .options(joinedload(models.Project.author))
        .where(models.SavedProject.user_id == user_id)
        .order_by(models.SavedProject.created_at.desc())
        .limit(limit)
        .offset(offset)
    )
    return list(db.execute(stmt).scalars().all())


def is_project_saved_by_user(db: Session, *, user_id: int, project_id: int) -> bool:
    stmt = select(models.SavedProject).where(
        models.SavedProject.user_id == user_id,
        models.SavedProject.project_id == project_id,
    )
    return db.execute(stmt).scalar_one_or_none() is not None


def is_project_reblogged_by_user(db: Session, *, user_id: int, project_id: int) -> bool:
    stmt = (
        select(models.Post.id)
        .where(
            models.Post.author_id == user_id,
            models.Post.reblogged_project_id == project_id,
        )
        .limit(1)
    )
    return db.execute(stmt).scalar_one_or_none() is not None


# ===========================================================
#                       FOLLOW / SAVE
# ===========================================================


def follow_user(db: Session, *, follower_id: int, followee_id: int) -> None:
    if follower_id == followee_id:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Cannot follow yourself")

    link = models.Follow(follower_id=follower_id, followee_id=followee_id)
    db.add(link)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()


def unfollow_user(db: Session, *, follower_id: int, followee_id: int) -> None:
    stmt = delete(models.Follow).where(
        models.Follow.follower_id == follower_id,
        models.Follow.followee_id == followee_id,
    )
    db.execute(stmt)
    db.commit()


def save_project(db: Session, *, user_id: int, project_id: int) -> None:
    link = models.SavedProject(user_id=user_id, project_id=project_id)
    db.add(link)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()


def unsave_project(db: Session, *, user_id: int, project_id: int) -> None:
    stmt = delete(models.SavedProject).where(
        models.SavedProject.user_id == user_id,
        models.SavedProject.project_id == project_id,
    )
    db.execute(stmt)
    db.commit()
