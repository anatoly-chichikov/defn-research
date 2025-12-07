"""Repository for research sessions."""
from __future__ import annotations

from abc import ABC
from abc import abstractmethod
from typing import Final

from src.domain.session import ResearchSession
from src.storage.file import JsonFile


class Loadable(ABC):
    """Object that can load data."""

    @abstractmethod
    def load(self) -> tuple[ResearchSession, ...]:
        """Return all sessions."""
        ...


class Savable(ABC):
    """Object that can save data."""

    @abstractmethod
    def save(self, sessions: tuple[ResearchSession, ...]) -> None:
        """Persist all sessions."""
        ...


class SessionsRepository(Loadable, Savable):
    """Repository for research sessions."""

    def __init__(self, file: JsonFile) -> None:
        """Initialize with JSON file."""
        self._file: Final[JsonFile] = file

    def load(self) -> tuple[ResearchSession, ...]:
        """Return all sessions."""
        if not self._file.exists():
            return tuple()
        data = self._file.read()
        return tuple(
            ResearchSession.deserialize(s) for s in data.get("sessions", [])
        )

    def save(self, sessions: tuple[ResearchSession, ...]) -> None:
        """Persist all sessions."""
        self._file.write(
            {
                "version": "1.0.0",
                "sessions": [s.serialize() for s in sessions],
            }
        )

    def append(self, session: ResearchSession) -> None:
        """Add session to repository."""
        existing = self.load()
        self.save(existing + (session,))

    def find(self, identifier: str) -> ResearchSession | None:
        """Find session by identifier."""
        for session in self.load():
            if session.id() == identifier:
                return session
        return None

    def update(self, session: ResearchSession) -> None:
        """Update existing session."""
        sessions = self.load()
        updated = tuple(
            session if s.id() == session.id() else s for s in sessions
        )
        self.save(updated)
