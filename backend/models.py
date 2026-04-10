from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, func
from db import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, nullable=False, index=True)
    email = Column(String(120), unique=True, nullable=False, index=True)
    nick = Column(String(50), nullable=False)
    theme = Column(String(50), nullable=False, default="light")

class Post(Base):
    __tablename__ = "posts"

    id = Column(Integer, primary_key=True, index=True)
    author_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    content = Column(Text, nullable=False)
    image_url = Column(String(500), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

class Project(Base):
    __tablename__ = "projects"

    id = Column(Integer, primary_key=True, index=True)
    author_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    title = Column(String(80), nullable=False, index=True)
    image_url = Column(String(500), nullable=False)

    content = Column(Text, nullable=True)
    hook_size = Column(String(100), nullable=True)
    pattern = Column(Text, nullable=True)
    yarn_type = Column(String(100), nullable=True)
    yarn_amount = Column(String(100), nullable=True)
    time_to_complete = Column(String(100), nullable=True)
    additional_materials = Column(Text, nullable=True)

    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
