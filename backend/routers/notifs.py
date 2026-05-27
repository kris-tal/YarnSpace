from __future__ import annotations

from typing import Annotated, List

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

import crud
from db import get_db
from deps import get_current_user_id
from schemas import NotificationReadDTO, UnreadCountDTO

router = APIRouter(prefix="/notifs", tags=["notifs"])

DbDep = Annotated[Session, Depends(get_db)]
CurrentUserId = Annotated[int, Depends(get_current_user_id)]


@router.get("/", response_model=List[NotificationReadDTO])
def list_my_notifications(
    user_id: CurrentUserId,
    db: DbDep,
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
):
    return crud.list_notifications(db, user_id=user_id, limit=limit, offset=offset)


@router.get("/unread-count", response_model=UnreadCountDTO)
def get_unread_count(user_id: CurrentUserId, db: DbDep):
    count = crud.count_unread_notifications(db, user_id=user_id)
    return {"count": count}


@router.post("/mark-all-read")
def mark_all_read(user_id: CurrentUserId, db: DbDep):
    changed = crud.mark_all_notifications_read(db, user_id=user_id)
    return {"status": "ok", "changed": changed}


