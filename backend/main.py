from fastapi import FastAPI
from sqlalchemy import text
from db import engine, Base
import models
from routers import auth, users, posts, projects, feed

app = FastAPI(
    title="YarnSpace API",
    description="Backend for YarnSpace",
    version="1.0.0"
)

app.include_router(auth.router)
app.include_router(users.router)
app.include_router(posts.router)
app.include_router(projects.router)
app.include_router(feed.router)

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
