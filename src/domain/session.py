"""Research session domain object."""
from __future__ import annotations

from abc import ABC
from abc import abstractmethod
from datetime import datetime
from typing import Final
from uuid import uuid4

from src.domain.result import Serializable
from src.domain.task import ResearchTask


class Topical(ABC):
    """Object with research topic."""

    @abstractmethod
    def topic(self) -> str:
        """Return research topic."""
        ...


class ResearchSession(Topical, Serializable):
    """Immutable research session containing tasks."""

    def __init__(
        self,
        topic: str,
        tasks: tuple[ResearchTask, ...],
        identifier: str | None = None,
        created: datetime | None = None,
    ) -> None:
        """Initialize session with topic and tasks."""
        self._id: Final[str] = identifier or str(uuid4())
        self._topic: Final[str] = topic
        self._tasks: Final[tuple[ResearchTask, ...]] = tasks
        self._created: Final[datetime] = created or datetime.now()

    def id(self) -> str:
        """Return session identifier."""
        return self._id

    def topic(self) -> str:
        """Return session topic."""
        return self._topic

    def tasks(self) -> tuple[ResearchTask, ...]:
        """Return tuple of tasks."""
        return self._tasks

    def created(self) -> datetime:
        """Return creation timestamp."""
        return self._created

    def extend(self, task: ResearchTask) -> ResearchSession:
        """Return new session with added task."""
        return ResearchSession(
            topic=self._topic,
            tasks=self._tasks + (task,),
            identifier=self._id,
            created=self._created,
        )

    def serialize(self) -> dict:
        """Return dictionary representation."""
        return {
            "id": self._id,
            "topic": self._topic,
            "tasks": [t.serialize() for t in self._tasks],
            "created": self._created.isoformat(),
        }

    @classmethod
    def deserialize(cls, data: dict) -> ResearchSession:
        """Create ResearchSession from dictionary."""
        return cls(
            topic=data["topic"],
            tasks=tuple(ResearchTask.deserialize(t) for t in data["tasks"]),
            identifier=data["id"],
            created=datetime.fromisoformat(data["created"]),
        )
