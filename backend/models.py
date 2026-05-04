from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, func
from sqlalchemy.orm import relationship
from db import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, nullable=False, index=True)
    email = Column(String(120), unique=True, nullable=False, index=True)
    nick = Column(String(50), nullable=False)
    accent_color = Column(String(50), nullable=False, default="sage")
    avatar_url = Column(String(500), nullable=True)

    password_hash = Column(String(255), nullable=False)

    posts = relationship("Post", back_populates="author", cascade="all, delete-orphan")
    projects = relationship("Project", back_populates="author", cascade="all, delete-orphan")

    following_links = relationship(
        "Follow",
        foreign_keys="Follow.follower_id",
        back_populates="follower",
        cascade="all, delete-orphan",
    )
    follower_links = relationship(
        "Follow",
        foreign_keys="Follow.followee_id",
        back_populates="followee",
        cascade="all, delete-orphan",
    )

    saved_project_links = relationship(
        "SavedProject",
        back_populates="user",
        cascade="all, delete-orphan",
    )

class Post(Base):
    __tablename__ = "posts"

    id = Column(Integer, primary_key=True, index=True)
    author_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    content = Column(Text, nullable=False)
    image_url = Column(String(500), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    author = relationship("User", back_populates="posts")

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

    author = relationship("User", back_populates="projects")
    saved_by_links = relationship(
        "SavedProject",
        back_populates="project",
        cascade="all, delete-orphan",
    )


class Follow(Base):
    __tablename__ = "follows"

    follower_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    followee_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    follower = relationship("User", foreign_keys=[follower_id], back_populates="following_links")
    followee = relationship("User", foreign_keys=[followee_id], back_populates="follower_links")


class SavedProject(Base):
    __tablename__ = "saved_projects"

    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    project_id = Column(Integer, ForeignKey("projects.id", ondelete="CASCADE"), primary_key=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    user = relationship("User", back_populates="saved_project_links")
    project = relationship("Project", back_populates="saved_by_links")

