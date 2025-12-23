"""Research task domain object."""
from __future__ import annotations

from abc import ABC
from abc import abstractmethod
from datetime import datetime
from typing import Final
from uuid import uuid4

from src.domain.result import Serializable
from src.domain.result import TaskResult


class Identifiable(ABC):
    """Object with unique identifier."""

    @abstractmethod
    def id(self) -> str:
        """Return unique identifier."""
        ...


class Queryable(ABC):
    """Object with research query."""

    @abstractmethod
    def query(self) -> str:
        """Return research query."""
        ...


class Served(ABC):
    """Object with service name."""

    @abstractmethod
    def service(self) -> str:
        """Return service name."""
        ...


class ResearchTask(Identifiable, Queryable, Served, Serializable):
    """Immutable research task with query and result."""

    def __init__(
        self,
        query: str,
        status: str,
        result: TaskResult | None,
        language: str = "русский",
        service: str = "parallel.ai",
        identifier: str | None = None,
        created: datetime | None = None,
        completed: datetime | None = None,
    ) -> None:
        """Initialize task with query, status, and optional result."""
        self._id: Final[str] = identifier or str(uuid4())
        self._query: Final[str] = query
        self._status: Final[str] = status
        self._result: Final[TaskResult | None] = result
        self._language: Final[str] = language
        self._service: Final[str] = service
        self._created: Final[datetime] = created or datetime.now()
        self._completed: Final[datetime | None] = completed

    def id(self) -> str:
        """Return task identifier."""
        return self._id

    def query(self) -> str:
        """Return research query."""
        return self._query

    def status(self) -> str:
        """Return task status."""
        return self._status

    def result(self) -> TaskResult | None:
        """Return task result if completed."""
        return self._result

    def language(self) -> str:
        """Return research language."""
        return self._language

    def service(self) -> str:
        """Return service name."""
        return self._service

    def created(self) -> datetime:
        """Return creation timestamp."""
        return self._created

    def completed(self) -> datetime | None:
        """Return completion timestamp."""
        return self._completed

    def complete(self, result: TaskResult) -> ResearchTask:
        """Return new task marked as completed with result."""
        return ResearchTask(
            query=self._query,
            status="completed",
            result=result,
            language=self._language,
            service=self._service,
            identifier=self._id,
            created=self._created,
            completed=datetime.now(),
        )

    def serialize(self) -> dict:
        """Return dictionary representation."""
        data = {
            "id": self._id,
            "query": self._query,
            "status": self._status,
            "language": self._language,
            "service": self._service,
            "created": self._created.isoformat(),
        }
        if self._completed:
            data["completed"] = self._completed.isoformat()
        if self._result:
            data["result"] = self._result.serialize()
        return data

    @classmethod
    def deserialize(cls, data: dict) -> ResearchTask:
        """Create ResearchTask from dictionary."""
        result = None
        if "result" in data:
            result = TaskResult.deserialize(data["result"])
        completed = None
        if "completed" in data:
            completed = datetime.fromisoformat(data["completed"])
        return cls(
            query=data["query"],
            status=data["status"],
            result=result,
            language=data.get("language", "русский"),
            service=data.get("service") or "parallel.ai",
            identifier=data["id"],
            created=datetime.fromisoformat(data["created"]),
            completed=completed,
        )
