from __future__ import annotations

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, AliasChoices, field_validator


def _datetime_to_millis(value: datetime) -> int:
    return int(value.timestamp() * 1000)


class ORMBaseModel(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)


# ===========================================================
#                          USERS
# ===========================================================

class UserCreateDTO(BaseModel):
    username: str = Field(min_length=3, max_length=50)
    email: str = Field(max_length=120)
    display_name: str = Field(min_length=1, max_length=50, validation_alias=AliasChoices("displayName", "display_name"), serialization_alias="displayName")
    accentColor: str = Field(default="sage", validation_alias=AliasChoices("accent_color", "accentColor"))
    avatarUrl: Optional[str] = Field(default=None, validation_alias=AliasChoices("avatar_url", "avatarUrl"))


class UserRegisterDTO(UserCreateDTO):
    password: str = Field(min_length=8, max_length=200)


class UserPublicDTO(ORMBaseModel):
    id: int
    username: str
    display_name: str = Field(validation_alias=AliasChoices("display_name", "displayName"), serialization_alias="displayName")
    accentColor: str = Field(validation_alias=AliasChoices("accent_color", "accentColor"))
    avatarUrl: Optional[str] = Field(default=None, validation_alias=AliasChoices("avatar_url", "avatarUrl"))


class UserPrivateDTO(UserPublicDTO):
    email: str


class ProfilePublicDTO(UserPublicDTO):
    followersCount: int
    followingCount: int
    postsCount: int
    projectsCount: int
    savedProjectsCount: int
    isFollowedByMe: Optional[bool] = None


# ===========================================================
#                          POSTS
# ===========================================================

class PostCreateDTO(BaseModel):
    content: str = Field(min_length=1)
    imageUrl: Optional[str] = None


class PostReadDTO(ORMBaseModel):
    id: int
    authorId: int = Field(validation_alias=AliasChoices("author_id", "authorId"))
    author: UserPublicDTO
    content: str
    imageUrl: Optional[str] = Field(default=None, validation_alias=AliasChoices("image_url", "imageUrl"))
    createdAt: int = Field(validation_alias=AliasChoices("created_at", "createdAt"))

    @field_validator("createdAt", mode="before")
    @classmethod
    def _parse_created_at(cls, v):
        if isinstance(v, datetime):
            return _datetime_to_millis(v)
        return v


# ===========================================================
#                          PROJECTS
# ===========================================================

class ProjectCreateDTO(BaseModel):
    title: str = Field(min_length=1, max_length=80)
    imageUrl: str

    content: Optional[str] = None
    hookSize: Optional[str] = None
    pattern: Optional[str] = None
    yarnType: Optional[str] = None
    yarnAmount: Optional[str] = None
    timeToComplete: Optional[str] = None
    additionalMaterials: Optional[str] = None


class ProjectReadDTO(ORMBaseModel):
    id: int
    authorId: int = Field(validation_alias=AliasChoices("author_id", "authorId"))
    author: UserPublicDTO

    title: str
    imageUrl: str = Field(validation_alias=AliasChoices("image_url", "imageUrl"))

    content: Optional[str] = None
    hookSize: Optional[str] = Field(default=None, validation_alias=AliasChoices("hook_size", "hookSize"))
    pattern: Optional[str] = None
    yarnType: Optional[str] = Field(default=None, validation_alias=AliasChoices("yarn_type", "yarnType"))
    yarnAmount: Optional[str] = Field(default=None, validation_alias=AliasChoices("yarn_amount", "yarnAmount"))
    timeToComplete: Optional[str] = Field(default=None, validation_alias=AliasChoices("time_to_complete", "timeToComplete"))
    additionalMaterials: Optional[str] = Field(
        default=None, validation_alias=AliasChoices("additional_materials", "additionalMaterials")
    )

    createdAt: int = Field(validation_alias=AliasChoices("created_at", "createdAt"))

    @field_validator("createdAt", mode="before")
    @classmethod
    def _parse_created_at(cls, v):
        if isinstance(v, datetime):
            return _datetime_to_millis(v)
        return v


# ===========================================================
#                           AUTH
# ===========================================================

class AuthLoginDTO(BaseModel):
    identifier: str
    password: str


class TokenDTO(BaseModel):
    accessToken: str
    tokenType: str = "bearer"


class AuthResponseDTO(TokenDTO):
    user: UserPublicDTO


class AccentColorUpdateDTO(BaseModel):
    accentColor: str
