from fastapi import FastAPI
from sqlalchemy import text
from db import engine, Base
import models

app = FastAPI(
    title="YarnSpace API",
    description="Backend for YarnSpace",
    version="1.0.0"
)

@app.on_event("startup")
def startup_event():
    Base.metadata.create_all(bind=engine)
    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        print("Database connection successful!")
    except Exception as e:
        print(f"Database connection failed: {e}")

@app.get("/")
async def root():
    return {
        "message": "Welcome to the YarnSpace API :)",
        "status": "Running",
        "docs_url": "/docs"
    }

@app.get("/health")
async def health_check():
    return {"status": "healthy"}