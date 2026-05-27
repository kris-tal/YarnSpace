from __future__ import annotations

from datetime import datetime, timezone
from typing import Annotated, List

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

import crud
from db import get_db
from deps import get_current_user_id, get_optional_current_user_id
from schemas import ProjectCreateDTO, ProjectReadDTO

router = APIRouter(prefix="/projects", tags=["projects"])

DbDep = Annotated[Session, Depends(get_db)]
CurrentUserId = Annotated[int, Depends(get_current_user_id)]
OptionalCurrentUserId = Annotated[int | None, Depends(get_optional_current_user_id)]


def _attach_extra_fields(db: Session, project: crud.models.Project, current_user_id: int | None):
    dto = ProjectReadDTO.model_validate(project)
    if current_user_id:
        dto.isSavedByMe = crud.is_project_saved_by_user(db, user_id=current_user_id, project_id=project.id)
        dto.isRebloggedByMe = crud.is_project_reblogged_by_user(db, user_id=current_user_id, project_id=project.id)
    else:
        dto.isSavedByMe = False
        dto.isRebloggedByMe = False
    return dto


@router.post("/", response_model=ProjectReadDTO)
def create_project(
    payload: ProjectCreateDTO,
    user_id: CurrentUserId,
    db: DbDep,
):
    data = {
        "title": payload.title,
        "image_url": payload.imageUrl,
        "content": payload.content,
        "hook_size": payload.hookSize,
        "pattern": payload.pattern,
        "yarn_type": payload.yarnType,
        "yarn_amount": payload.yarnAmount,
        "time_to_complete": payload.timeToComplete,
        "additional_materials": payload.additionalMaterials,
    }
    project = crud.create_project(db, author_id=user_id, data=data)
    project = crud.get_project(db, project.id)
    return _attach_extra_fields(db, project, user_id)


@router.get("/search", response_model=List[ProjectReadDTO])
def search_projects(
    user_id: OptionalCurrentUserId,
    db: DbDep,
    q: str = Query(..., min_length=1),
    hasPattern: bool = Query(False),
    createdAfter: int | None = Query(None, ge=0),
    createdBefore: int | None = Query(None, ge=0),
    limit: int = 20,
    offset: int = 0,
):
    created_after_dt: datetime | None = None
    created_before_dt: datetime | None = None

    if createdAfter is not None:
        created_after_dt = datetime.fromtimestamp(createdAfter / 1000, tz=timezone.utc)
    if createdBefore is not None:
        created_before_dt = datetime.fromtimestamp(createdBefore / 1000, tz=timezone.utc)

    projects = crud.search_projects(
        db,
        query=q,
        limit=limit,
        offset=offset,
        created_after=created_after_dt,
        created_before=created_before_dt,
        has_pattern_only=hasPattern,
    )
    return [_attach_extra_fields(db, p, user_id) for p in projects]


@router.get("/{project_id}", response_model=ProjectReadDTO)
def get_project(project_id: int, db: DbDep, user_id: OptionalCurrentUserId):
    project = crud.get_project(db, project_id)
    return _attach_extra_fields(db, project, user_id)


@router.post("/{project_id}/save")
def save_project(
    project_id: int,
    user_id: CurrentUserId,
    db: DbDep,
):
    crud.get_project(db, project_id)
    crud.save_project(db, user_id=user_id, project_id=project_id)
    return {"status": "ok"}


@router.delete("/{project_id}/save")
def unsave_project(
    project_id: int,
    user_id: CurrentUserId,
    db: DbDep,
):
    crud.unsave_project(db, user_id=user_id, project_id=project_id)
    return {"status": "ok"}
