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
    from routers.users import _attach_extra_to_post, _attach_extra_to_project

    if viewer_id is not None:
        posts = crud.get_followed_posts(db, viewer_id=viewer_id, limit=limit)
        projects = crud.get_followed_projects(db, viewer_id=viewer_id, limit=limit)

    feed = []
    for p in posts:
        feed.append(_attach_extra_to_post(db, p, viewer_id))
    for pr in projects:
        feed.append(_attach_extra_to_project(db, pr, viewer_id))

    feed.sort(key=lambda x: x.createdAt, reverse=True)
    return feed[:limit]