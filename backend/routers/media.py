from __future__ import annotations

import shutil
import uuid
from pathlib import Path

from typing import Annotated

from fastapi import APIRouter, Depends, File, HTTPException, Request, UploadFile

from deps import get_current_user_id

router = APIRouter(prefix="/media", tags=["media"])


@router.post("/images")
def upload_image(
    user_id: Annotated[int, Depends(get_current_user_id)],
    request: Request,
    file: UploadFile = File(...),
):

    allowed_types = {
        "image/jpeg": ".jpg",
        "image/jpg": ".jpg",
        "image/png": ".png",
        "image/webp": ".webp",
    }

    if file.content_type not in allowed_types:
        raise HTTPException(status_code=400, detail="Unsupported image type")

    uploads_dir = Path(__file__).resolve().parent.parent / "static" / "uploads"
    uploads_dir.mkdir(parents=True, exist_ok=True)

    ext = allowed_types[file.content_type]
    filename = f"{uuid.uuid4().hex}{ext}"
    dest_path = uploads_dir / filename

    # Soft size limit: 10MB (based on Content-Length when available)
    content_length = request.headers.get("content-length")
    if content_length is not None:
        try:
            if int(content_length) > 10 * 1024 * 1024:
                raise HTTPException(status_code=413, detail="File too large")
        except ValueError:
            pass

    try:
        with dest_path.open("wb") as out:
            shutil.copyfileobj(file.file, out)
    finally:
        file.file.close()

    rel_url = f"/static/uploads/{filename}"

    return {"imageUrl": rel_url}



