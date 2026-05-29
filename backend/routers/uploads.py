import shutil
import uuid
from pathlib import Path
from fastapi import APIRouter, Depends, UploadFile, File, HTTPException, Request
from deps import get_current_user_id

router = APIRouter(prefix="/uploads", tags=["uploads"])

@router.post("/image")
def upload_image(
    request: Request,
    user_id: int = Depends(get_current_user_id),
    file: UploadFile = File(...)
):

    allowed_types = {"image/jpeg": ".jpg", "image/jpg": ".jpg", "image/png": ".png", "image/webp": ".webp"}
    if file.content_type not in allowed_types:
        raise HTTPException(status_code=400, detail="Unsupported file format. Use JPG, PNG or WEBP.")

    uploads_dir = Path(__file__).resolve().parent.parent / "static" / "uploads"
    uploads_dir.mkdir(parents=True, exist_ok=True)

    ext = allowed_types[file.content_type]
    filename = f"{uuid.uuid4().hex}{ext}"
    dest_path = uploads_dir / filename

    try:
        with dest_path.open("wb") as out:
            shutil.copyfileobj(file.file, out)
    finally:
        file.file.close()

    base_url = str(request.base_url).rstrip("/")
    image_url = f"{base_url}/static/uploads/{filename}"

    return {"imageUrl": image_url}