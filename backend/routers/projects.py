from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

import crud
from db import get_db
from deps import get_current_user_id
from schemas import ProjectCreateDTO, ProjectReadDTO

router = APIRouter(prefix="/projects", tags=["projects"])

DbDep = Annotated[Session, Depends(get_db)]
CurrentUserId = Annotated[int, Depends(get_current_user_id)]


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
    return project


@router.get("/{project_id}", response_model=ProjectReadDTO)
def get_project(project_id: int, db: DbDep):
    return crud.get_project(db, project_id)


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



