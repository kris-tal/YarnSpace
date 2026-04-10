# YarnSpace

**A social media app for fiber artists.** *Project for the Mobile Programming course (2025/2026) at Jagiellonian University, Theoretical Computer Science.*

### About
Social media platform for crochet enthusiasts and influencers. Users can share finished projects, ideas, and their work-in-progress. They can also save favorite projects from others and share them via reblogging.

### External Technologies & Peripherals
* **Database** – to store information about users, posts, and projects.
* **Server** – to handle app logic and communication between the user and the database.
* **Camera/Gallery** – enables adding photos to a post or project.
* **Speaker** – button interaction sounds.
* **(Optional) Notifications** – external system notifications about new interactions.

### Features
* **Account creation.**
* **Profile editing** – changing profile picture, bio, and display name.
* **Adding posts and projects.**
* **Reblogging** – sharing other users' posts on your profile.
* **Viewing other users' profiles** along with their posts/projects.
* **Timeline (feed)** – browsing posts/projects from people you follow.
* **Activity view** – seeing recent interactions with your own profile.
* **Search engine** – finding creators and inspirations with a filtering system.

---

## Backend tech stack

- **FastAPI** (Python) – REST API.
- **Uvicorn** – ASGI server running the FastAPI app.
- **SQLAlchemy** – ORM for defining DB models and connecting to the database.
- **PostgreSQL** – relational database.
- **Docker + Docker Compose** – running backend + database locally.

---

## Backend + Database (development)

### Requirements
- Docker + Docker Compose

### Setup
1. Create `.env` based on `.env.example` (do not commit `.env`).
2. Start services:

```bash
docker compose up --build
```

### Quick checks
- API health:

```bash
curl http://localhost:8000/health
```

- API docs:

```text
http://localhost:8000/docs
```

- List tables in Postgres:

```bash
docker exec -it yarnspace_db psql -U $POSTGRES_USER -d $POSTGRES_DB -c "\\dt"
```
