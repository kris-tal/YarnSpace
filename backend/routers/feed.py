from __future__ import annotations
from typing import Annotated, List, Union
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
import crud
from db import get_db
from deps import get_optional_current_user_id
from schemas import PostReadDTO, ProjectReadDTO

router = APIRouter(prefix="/feed", tags=["feed"])

@router.get("/", response_model=List[Union[PostReadDTO, ProjectReadDTO]])
def get_global_feed(
    viewer_id: Annotated[int | None, Depends(get_optional_current_user_id)],
    db: Annotated[Session, Depends(get_db)],
    limit: int = 50
):
    posts = crud.list_posts_by_user(db, user_id=None, limit=limit)
    projects = crud.list_projects_by_user(db, user_id=None, limit=limit)

    from routers.users import _attach_extra_to_post, _attach_extra_to_project

    feed = []
    for p in posts:
        feed.append(_attach_extra_to_post(db, p, viewer_id))
    for pr in projects:
        feed.append(_attach_extra_to_project(db, pr, viewer_id))

    feed.sort(key=lambda x: x.createdAt, reverse=True)
    return feed[:limit]
