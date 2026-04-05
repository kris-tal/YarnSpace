from fastapi import FastAPI

app = FastAPI(
    title="YarnSpace API",
    description="Backend for YarnSpace",
    version="1.0.0"
)

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